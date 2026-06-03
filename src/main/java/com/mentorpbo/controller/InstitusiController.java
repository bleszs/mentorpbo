package com.mentorpbo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
/**
 * Proxy ke api.co.id untuk data universitas dan sekolah.
 * API key tetap di server — tidak terekspos ke browser.
 *
 * GET /api/institusi?q=...&type=universitas|sekolah[&grade=SMA|SMK]
 * Returns: [{label, sub}, ...]
 */
@RestController
@RequestMapping("/api/institusi")
public class InstitusiController {

    @Value("${app.api.coid.key:Uwp6sB81fQBX8571QsqMpccvHc4SBjWuLUggHGuph3gcePEd3T}")
    private String apiKey;

    @Value("${app.api.coid.base-url:https://use.api.co.id}")
    private String baseUrl;

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> cari(
            @RequestParam String q,
            @RequestParam(defaultValue = "universitas") String type,
            @RequestParam(required = false) String grade) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        try {
            String encoded = URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            String url;
            if ("sekolah".equalsIgnoreCase(type)) {
                url = baseUrl + "/regional/indonesia/schools?name=" + encoded + "&size=10";
                if (grade != null && !grade.isBlank()) {
                    url += "&grade=" + URLEncoder.encode(grade.toUpperCase(), StandardCharsets.UTF_8);
                }
            } else {
                url = baseUrl + "/regional/indonesia/universities?name=" + encoded + "&size=10";
            }

            JsonNode body = callApi(url);
            if (body == null) return ResponseEntity.ok(List.of());

            JsonNode dataArr = body.path("data");
            if (!dataArr.isArray()) return ResponseEntity.ok(List.of());

            List<Map<String, String>> result = new ArrayList<>();
            for (JsonNode item : dataArr) {
                String name = item.path("name").asText("").trim();
                if (name.isBlank()) continue;

                Map<String, String> entry = new LinkedHashMap<>();
                if ("sekolah".equalsIgnoreCase(type)) {
                    String jenjang = item.path("grade").asText("");
                    String status  = "N".equals(item.path("status").asText("")) ? "Negeri" : "Swasta";
                    String prov    = item.path("province_name").asText("");
                    entry.put("label", name);
                    entry.put("sub", jenjang + " - " + status + (prov.isBlank() ? "" : " - " + prov));
                } else {
                    String shortName = item.path("short_name").asText("");
                    String uType     = item.path("university_type").asText("");
                    String group     = item.path("group").asText("");
                    String label     = shortName.isBlank() ? name : name + " (" + shortName + ")";
                    entry.put("label", label);
                    entry.put("sub", uType + (group.isBlank() ? "" : " - " + group));
                }
                result.add(entry);
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    private JsonNode callApi(String url) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(4000);
            factory.setReadTimeout(6000);
            RestTemplate rt = new RestTemplate(factory);

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
}
