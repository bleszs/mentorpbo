package com.mentorpbo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * SearchController — Proxy pencarian institusi menggunakan api.co.id.
 *
 * Primary  : https://use.api.co.id (api.co.id — data nasional lengkap)
 * Fallback : JSON lokal (sekolah-indonesia.json / universitas-indonesia.json)
 *
 * Endpoints:
 *   GET /api/cari/sekolah?q={nama}          → cari sekolah SMA/SMK/MAN
 *   GET /api/cari/universitas?q={nama}       → cari universitas/PT
 *   GET /api/cari/sekolah-detail?npsn={npsn} → detail sekolah by NPSN
 *   GET /api/cari/pt-detail?q={nama}         → detail PT by nama lengkap
 */
@RestController
@RequestMapping("/api/cari")
public class SearchController {

    @Value("${app.api.coid.key:Uwp6sB81fQBX8571QsqMpccvHc4SBjWuLUggHGuph3gcePEd3T}")
    private String apiKey;

    @Value("${app.api.coid.base-url:https://use.api.co.id}")
    private String baseUrl;

    @Autowired
    private ResourceLoader resourceLoader;

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile List<String> sekolahCache    = null;
    private volatile List<String> universitasCache = null;

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    // ============================================================
    // SEARCH SEKOLAH — SMA / SMK / MAN
    // ============================================================

    /**
     * GET /api/cari/sekolah?q=SMAN1Jakarta
     * Mencari sekolah SMA/SMK/MAN/MA berdasarkan nama.
     * Sumber: api.co.id → fallback JSON lokal.
     */
    @GetMapping("/sekolah")
    public ResponseEntity<List<String>> cariSekolah(
            @RequestParam String q,
            @RequestParam(required = false) String grade) {
        if (q == null || q.trim().length() < 2) return ResponseEntity.ok(List.of());

        try {
            List<String> hasil = cariSekolahDariApi(q, grade);
            if (!hasil.isEmpty()) return ResponseEntity.ok(hasil);
        } catch (Exception ignored) {}

        return fallbackSekolah(q);
    }

    /**
     * GET /api/cari/sekolah-detail?npsn=20100216
     * Ambil detail sekolah berdasarkan NPSN (National Pokok Sekolah Nasional).
     */
    @GetMapping("/sekolah-detail")
    public ResponseEntity<Map<String, Object>> detailSekolahByNpsn(@RequestParam String npsn) {
        if (npsn == null || npsn.isBlank()) return ResponseEntity.badRequest().build();
        try {
            String url = baseUrl + "/regional/indonesia/schools?npsn="
                    + URLEncoder.encode(npsn.trim(), StandardCharsets.UTF_8);
            JsonNode body = panggilApi(url);
            if (body != null) {
                JsonNode dataArr = body.path("data");
                if (dataArr.isArray() && dataArr.size() > 0) {
                    JsonNode item = dataArr.get(0);
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("npsn",          item.path("npsn").asText(""));
                    detail.put("nama",           item.path("name").asText(""));
                    detail.put("jenjang",        item.path("grade").asText(""));
                    detail.put("status",         item.path("status").asText("") .equals("N") ? "Negeri" : "Swasta");
                    detail.put("alamat",         item.path("address").asText(""));
                    detail.put("provinsi",       item.path("province_name").asText(""));
                    detail.put("kabupaten",      item.path("regency_name").asText(""));
                    detail.put("kecamatan",      item.path("district_name").asText(""));
                    return ResponseEntity.ok(detail);
                }
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }

    // ============================================================
    // SEARCH UNIVERSITAS / PERGURUAN TINGGI
    // ============================================================

    /**
     * GET /api/cari/universitas?q=ITB
     * Mencari universitas/PT berdasarkan nama.
     * Sumber: api.co.id → fallback PDDikti → fallback JSON lokal.
     */
    @GetMapping("/universitas")
    public ResponseEntity<List<String>> cariUniversitas(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) return ResponseEntity.ok(List.of());

        // Primary: api.co.id
        try {
            String url = baseUrl + "/regional/indonesia/universities?name="
                    + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8) + "&page=1&size=10";
            JsonNode body = panggilApi(url);
            if (body != null) {
                JsonNode dataArr = body.path("data");
                if (dataArr.isArray() && dataArr.size() > 0) {
                    List<String> names = StreamSupport.stream(dataArr.spliterator(), false)
                            .map(n -> {
                                String nama      = n.path("name").asText("");
                                String shortName = n.path("short_name").asText("");
                                return shortName.isBlank() ? nama : nama + " (" + shortName + ")";
                            })
                            .filter(s -> !s.isBlank())
                            .distinct().limit(10)
                            .collect(Collectors.toList());
                    if (!names.isEmpty()) return ResponseEntity.ok(names);
                }
            }
        } catch (Exception ignored) {}

        // Fallback: PDDikti Kemdikbud
        try {
            RestTemplate rt = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = "https://pddikti.kemdikbud.go.id/api/pencarian/all/"
                    + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            ResponseEntity<JsonNode> resp = rt.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            if (body != null) {
                JsonNode ptArr = body.path("pt");
                if (ptArr.isArray()) {
                    List<String> names = StreamSupport.stream(ptArr.spliterator(), false)
                            .map(n -> n.path("nama_pt").asText(""))
                            .filter(s -> !s.isBlank()).distinct().limit(10)
                            .collect(Collectors.toList());
                    if (!names.isEmpty()) return ResponseEntity.ok(names);
                }
            }
        } catch (Exception ignored) {}

        return fallbackUniversitas(q);
    }

    /**
     * GET /api/cari/pt-detail?q=Institut+Teknologi+Bandung
     * Ambil detail PT berdasarkan nama lengkap atau singkatan.
     */
    @GetMapping("/pt-detail")
    public ResponseEntity<Map<String, Object>> detailPt(@RequestParam String q) {
        if (q == null || q.isBlank()) return ResponseEntity.badRequest().build();
        try {
            String url = baseUrl + "/regional/indonesia/universities?name="
                    + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8) + "&page=1&size=1";
            JsonNode body = panggilApi(url);
            if (body != null) {
                JsonNode dataArr = body.path("data");
                if (dataArr.isArray() && dataArr.size() > 0) {
                    JsonNode item = dataArr.get(0);
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("nama",        item.path("name").asText(""));
                    detail.put("singkatan",   item.path("short_name").asText(""));
                    detail.put("jenis",       item.path("university_type").asText(""));
                    detail.put("grup",        item.path("group").asText(""));
                    detail.put("provinsi",    item.path("province").asText(""));
                    detail.put("kabupaten",   item.path("regency").asText(""));
                    detail.put("alamat",      item.path("address").asText(""));
                    return ResponseEntity.ok(detail);
                }
            }
        } catch (Exception ignored) {}
        return ResponseEntity.notFound().build();
    }

    // ============================================================
    // HELPER PRIVATE
    // ============================================================

    /**
     * Memanggil api.co.id dengan header autentikasi yang benar.
     */
    private JsonNode panggilApi(String url) {
        try {
            RestTemplate rt = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-co-id", apiKey);
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "MentorPBO/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> resp = rt.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            if (body != null && body.path("is_success").asBoolean(false)) return body;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Mencari sekolah dari api.co.id dengan query nama.
     * Kalau grade=null → cari semua jenjang (SMA, SMK, MA, dll).
     */
    private List<String> cariSekolahDariApi(String q, String grade) {
        String url = baseUrl + "/regional/indonesia/schools?name="
                + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8) + "&page=1&size=10";
        if (grade != null) url += "&grade=" + grade;

        JsonNode body = panggilApi(url);
        if (body == null) return List.of();

        JsonNode dataArr = body.path("data");
        if (!dataArr.isArray()) return List.of();

        return StreamSupport.stream(dataArr.spliterator(), false)
                .filter(n -> {
                    String g = n.path("grade").asText("");
                    return g.equals("SMA") || g.equals("SMK") || g.equals("MA")
                        || g.equals("MAN") || g.equals("MAK") || g.isEmpty();
                })
                .map(n -> {
                    String nama = n.path("name").asText("");
                    String kab  = n.path("regency_name").asText("");
                    String prov = n.path("province_name").asText("");
                    if (!kab.isBlank()) nama += " - " + kab;
                    else if (!prov.isBlank()) nama += " - " + prov;
                    return nama;
                })
                .filter(s -> !s.isBlank())
                .distinct().limit(10)
                .collect(Collectors.toList());
    }

    private ResponseEntity<List<String>> fallbackUniversitas(String q) {
        try {
            if (universitasCache == null) muatUniversitas();
            String qLow = q.trim().toLowerCase();
            return ResponseEntity.ok(universitasCache.stream()
                    .filter(s -> s.toLowerCase().contains(qLow))
                    .limit(10).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    private ResponseEntity<List<String>> fallbackSekolah(String q) {
        try {
            if (sekolahCache == null) muatSekolah();
            String qLow = q.trim().toLowerCase();
            return ResponseEntity.ok(sekolahCache.stream()
                    .filter(s -> s.toLowerCase().contains(qLow))
                    .limit(10).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.ok(List.of()); }
    }

    private synchronized void muatUniversitas() throws Exception {
        if (universitasCache != null) return;
        Resource res = resourceLoader.getResource("classpath:static/data/universitas-indonesia.json");
        try (InputStream in = res.getInputStream()) {
            JsonNode arr = mapper.readTree(in);
            universitasCache = StreamSupport.stream(arr.spliterator(), false)
                    .map(JsonNode::asText).filter(s -> !s.isBlank()).collect(Collectors.toList());
        }
    }

    private synchronized void muatSekolah() throws Exception {
        if (sekolahCache != null) return;
        Resource res = resourceLoader.getResource("classpath:static/data/sekolah-indonesia.json");
        try (InputStream in = res.getInputStream()) {
            JsonNode arr = mapper.readTree(in);
            sekolahCache = StreamSupport.stream(arr.spliterator(), false)
                    .map(JsonNode::asText).filter(s -> !s.isBlank()).collect(Collectors.toList());
        }
    }
}
