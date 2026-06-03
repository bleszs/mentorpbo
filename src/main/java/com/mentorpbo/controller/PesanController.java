package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.repository.GrupChatRepository;
import com.mentorpbo.repository.PesanRepository;
import com.mentorpbo.service.MentoringService;
import com.mentorpbo.service.PenggunaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * PesanController â€” mengelola halaman pesan dan API chat antara mentor dan mentee.
 */
@Controller
@RequestMapping("/pesan")
public class PesanController {

    private final PesanRepository pesanRepository;
    private final GrupChatRepository grupChatRepository;
    private final PenggunaService penggunaService;
    private final MentoringService mentoringService;

    @Autowired
    public PesanController(PesanRepository pesanRepository,
                           GrupChatRepository grupChatRepository,
                           PenggunaService penggunaService,
                           MentoringService mentoringService) {
        this.pesanRepository = pesanRepository;
        this.grupChatRepository = grupChatRepository;
        this.penggunaService = penggunaService;
        this.mentoringService = mentoringService;
    }

    /**
     * Halaman chat dengan partner tertentu.
     * GET /pesan/chat/{partnerId}
     */
    @GetMapping("/chat/{partnerId}")
    public String chatDengan(@PathVariable Long partnerId,
                              HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        Pengguna partner = penggunaService.getPenggunaById(partnerId).orElse(null);
        if (partner == null) return "redirect:/pesan";

        // Tandai pesan dari partner sebagai sudah dibaca
        List<Pesan> belumDibaca = pesanRepository.findBelumDibaca(partnerId, penggunaId);
        belumDibaca.forEach(p -> p.setSudahDibaca(true));
        if (!belumDibaca.isEmpty()) pesanRepository.saveAll(belumDibaca);

        // Ambil percakapan
        List<Pesan> percakapan = pesanRepository.findPercakapan(penggunaId, partnerId);

        // Ambil daftar partner unik
        List<Map<String, Object>> daftarPartner = getDaftarPartner(penggunaId, pengguna);

        model.addAttribute("pengguna", pengguna);
        model.addAttribute("partner", partner);
        model.addAttribute("percakapan", percakapan);
        model.addAttribute("daftarPartner", daftarPartner);
        model.addAttribute("partnerId", partnerId);
        model.addAttribute("notifBelumDibaca", penggunaService.hitungNotifikasiBelumDibaca(penggunaId));
        model.addAttribute("preferences", penggunaService.getOrCreatePreferences(penggunaId));
        model.addAttribute("inboxPercakapan", buildInboxPercakapan(penggunaId));
        try { model.addAttribute("daftarGrup", grupChatRepository.findByAnggotaId(penggunaId)); }
        catch (Exception e) { model.addAttribute("daftarGrup", Collections.emptyList()); }

        // Role
        if (pengguna instanceof Mahasiswa mhs) {
            model.addAttribute("mahasiswa", mhs);
            if (mhs.isMentor()) model.addAttribute("mentor", pengguna);
        } else if (pengguna instanceof Siswa siswa) {
            model.addAttribute("siswa", siswa);
        }

        return "dashboard/pesan-chat";
    }

    private static final String UPLOAD_DIR = "uploads/pesan/";

    /**
     * Kirim pesan teks baru.
     * POST /pesan/kirim
     */
    @PostMapping("/kirim")
    public String kirimPesan(@RequestParam Long penerimaId,
                              @RequestParam(required = false) String isi,
                              HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        if (isi == null || isi.isBlank()) {
            flash.addFlashAttribute("error", "Pesan tidak boleh kosong.");
            return "redirect:/pesan/chat/" + penerimaId;
        }

        try {
            Pengguna pengirim = penggunaService.getPenggunaById(penggunaId)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan."));
            Pengguna penerima = penggunaService.getPenggunaById(penerimaId)
                .orElseThrow(() -> new RuntimeException("Penerima tidak ditemukan."));

            Pesan pesan = new Pesan(isi.trim(), pengirim, penerima);
            pesanRepository.save(pesan);
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal mengirim pesan: " + e.getMessage());
        }
        return "redirect:/pesan/chat/" + penerimaId;
    }

    /**
     * Kirim pesan dengan lampiran file atau gambar.
     * POST /pesan/kirim-file
     */
    @PostMapping("/kirim-file")
    public String kirimPesanFile(@RequestParam Long penerimaId,
                                  @RequestParam(required = false) String isi,
                                  @RequestParam("lampiran") MultipartFile lampiran,
                                  HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            Pengguna pengirim = penggunaService.getPenggunaById(penggunaId)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan."));
            Pengguna penerima = penggunaService.getPenggunaById(penerimaId)
                .orElseThrow(() -> new RuntimeException("Penerima tidak ditemukan."));

            Pesan pesan = new Pesan();
            pesan.setIsi(isi != null && !isi.isBlank() ? isi.trim() : null);
            pesan.setPengirim(pengirim);
            pesan.setPenerima(penerima);

            if (!lampiran.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                Files.createDirectories(uploadPath);
                String ext = getExtension(lampiran.getOriginalFilename());
                String filename = "pesan_" + penggunaId + "_" + System.currentTimeMillis() + ext;
                Files.copy(lampiran.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                pesan.setLampiran("/uploads/pesan/" + filename);
                pesan.setNamaFile(lampiran.getOriginalFilename());
                String ct = lampiran.getContentType() != null ? lampiran.getContentType() : "";
                pesan.setTipeKonten(ct.startsWith("image/") ? "GAMBAR" : "FILE");
            }
            pesanRepository.save(pesan);
        } catch (IOException e) {
            flash.addFlashAttribute("error", "Gagal upload lampiran: " + e.getMessage());
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal mengirim pesan: " + e.getMessage());
        }
        return "redirect:/pesan/chat/" + penerimaId;
    }

    /**
     * Buat grup percakapan baru — simpan ke GrupChat entity.
     * POST /pesan/buat-grup
     */
    @PostMapping("/buat-grup")
    public String buatGrup(@RequestParam String namaGrup,
                           @RequestParam(required = false) List<Long> anggotaIds,
                           HttpSession session,
                           RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";
        if (anggotaIds == null || anggotaIds.isEmpty()) {
            flash.addFlashAttribute("error", "Pilih minimal satu anggota grup.");
            return "redirect:/dashboard";
        }

        Pengguna pembuat = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pembuat == null) return "redirect:/login";

        GrupChat grup = new GrupChat();
        grup.setNama(namaGrup);
        grup.setDibuatOleh(pembuat);

        List<Pengguna> anggota = new ArrayList<>();
        anggota.add(pembuat);
        for (Long anggotaId : anggotaIds) {
            penggunaService.getPenggunaById(anggotaId).ifPresent(anggota::add);
        }
        grup.setAnggota(anggota);
        GrupChat saved = grupChatRepository.save(grup);

        // Kirim pesan sistem ke grup
        Pesan p = new Pesan();
        p.setPengirim(pembuat);
        p.setGrupId(saved.getId());
        p.setIsi("📢 Grup \"" + namaGrup + "\" dibuat. Selamat berdiskusi!");
        p.setTipeKonten("TEKS");
        pesanRepository.save(p);

        flash.addFlashAttribute("sukses", "Grup \"" + namaGrup + "\" berhasil dibuat!");
        return "redirect:/pesan/grup/" + saved.getId();
    }

    /**
     * Halaman grup chat.
     * GET /pesan/grup/{grupId}
     */
    @GetMapping("/grup/{grupId}")
    public String grupChat(@PathVariable Long grupId,
                           HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        GrupChat grup = grupChatRepository.findById(grupId).orElse(null);
        if (grup == null || !grup.getAnggota().stream().anyMatch(a -> a.getId().equals(penggunaId)))
            return "redirect:/dashboard";

        List<Pesan> percakapan = pesanRepository.findByGrupId(grupId);
        List<GrupChat> daftarGrup = grupChatRepository.findByAnggotaId(penggunaId);
        List<Map<String, Object>> daftarPartner = getDaftarPartner(penggunaId, pengguna);

        model.addAttribute("pengguna", pengguna);
        model.addAttribute("grup", grup);
        model.addAttribute("percakapan", percakapan);
        model.addAttribute("daftarGrup", daftarGrup);
        model.addAttribute("daftarPartner", daftarPartner);
        model.addAttribute("notifBelumDibaca", penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        if (pengguna instanceof Mahasiswa mhs) {
            model.addAttribute("mahasiswa", mhs);
            if (mhs.isMentor()) model.addAttribute("mentor", pengguna);
        } else if (pengguna instanceof Siswa siswa) {
            model.addAttribute("siswa", siswa);
            if (siswa.isMentor()) model.addAttribute("mentor", pengguna);
        }
        return "dashboard/pesan-grup";
    }

    /**
     * Kirim pesan ke grup.
     * POST /pesan/grup/{grupId}/kirim
     */
    @PostMapping("/grup/{grupId}/kirim")
    public String kirimPesanGrup(@PathVariable Long grupId,
                                  @RequestParam String isi,
                                  HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengirim = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengirim == null || isi == null || isi.isBlank()) return "redirect:/pesan/grup/" + grupId;

        Pesan p = new Pesan();
        p.setPengirim(pengirim);
        p.setGrupId(grupId);
        p.setIsi(isi.trim());
        p.setTipeKonten("TEKS");
        pesanRepository.save(p);
        return "redirect:/pesan/grup/" + grupId;
    }

    /**
     * API polling pesan grup terbaru.
     * GET /pesan/grup/{grupId}/terbaru?since={id}
     */
    @GetMapping("/grup/{grupId}/terbaru")
    @ResponseBody
    public List<Map<String, Object>> pesanGrupTerbaru(@PathVariable Long grupId,
                                                       @RequestParam(defaultValue = "0") Long since,
                                                       HttpSession session) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return Collections.emptyList();

        return pesanRepository.findByGrupIdAfter(grupId, since).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("isi", p.getIsi());
            m.put("tipeKonten", p.getTipeKonten());
            m.put("lampiran", p.getLampiran());
            m.put("namaFile", p.getNamaFile());
            m.put("waktu", p.getWaktuKirim() != null ? p.getWaktuKirim().toString() : "");
            m.put("dariku", p.getPengirim() != null && p.getPengirim().getId().equals(penggunaId));
            m.put("nama", p.getPengirim() != null ? p.getPengirim().getNamaLengkap() : "");
            m.put("foto", p.getPengirim() != null ? p.getPengirim().getFotoProfil() : null);
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * API: ambil pesan terbaru untuk polling (JSON).
     * GET /pesan/api/{partnerId}/terbaru?since={id}
     */
    @GetMapping("/api/{partnerId}/terbaru")
    @ResponseBody
    public List<Map<String, Object>> pesanTerbaru(@PathVariable Long partnerId,
                                                   @RequestParam(defaultValue = "0") Long since,
                                                   HttpSession session) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return Collections.emptyList();

        return pesanRepository.findPercakapan(penggunaId, partnerId).stream()
            .filter(p -> p.getId() > since)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("isi", p.getIsi());
                m.put("tipeKonten", p.getTipeKonten());
                m.put("lampiran", p.getLampiran());
                m.put("namaFile", p.getNamaFile());
                m.put("waktu", p.getWaktuKirim().toString());
                m.put("dariku", p.getPengirim().getId().equals(penggunaId));
                m.put("nama", p.getPengirim().getNamaLengkap());
                m.put("foto", p.getPengirim().getFotoProfil());
                return m;
            })
            .collect(Collectors.toList());
    }

    // ============================================================
    // HELPER
    // ============================================================

    private List<Map<String, Object>> getDaftarPartner(Long penggunaId, Pengguna pengguna) {
        // Ambil partner dari sesi mentoring
        List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(penggunaId);
        Set<Long> partnerIds = new LinkedHashSet<>();
        List<Pengguna> partners = new ArrayList<>();

        for (SesiMentoring s : semuaSesi) {
            Pengguna partner = null;
            if (s.getMentor() != null && !s.getMentor().getId().equals(penggunaId))
                partner = s.getMentor();
            else if (s.getMentee() != null && !s.getMentee().getId().equals(penggunaId))
                partner = s.getMentee();
            if (partner != null && partnerIds.add(partner.getId()))
                partners.add(partner);
        }

        // Tambahkan partner dari pesan langsung (skip grup messages yang penerima null)
        pesanRepository.findSemuaPesanPengguna(penggunaId).forEach(p -> {
            if (p.getGrupId() != null) return; // skip group messages
            Pengguna partner = p.getPengirim() != null && p.getPengirim().getId().equals(penggunaId)
                ? p.getPenerima() : p.getPengirim();
            if (partner != null && partnerIds.add(partner.getId())) partners.add(partner);
        });

        List<Map<String, Object>> result = new ArrayList<>();
        for (Pengguna p : partners) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("partner", p);
            long belumDibaca = pesanRepository.findBelumDibaca(p.getId(), penggunaId).size();
            entry.put("belumDibaca", belumDibaca);
            result.add(entry);
        }
        return result;
    }

    private List<Map<String, Object>> buildInboxPercakapan(Long penggunaId) {
        List<SesiMentoring> sesiList = mentoringService.getSemuaSesiPengguna(penggunaId);
        Set<Long> seen = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SesiMentoring s : sesiList) {
            Pengguna partner = null;
            if (s.getMentor() != null && !s.getMentor().getId().equals(penggunaId)) partner = s.getMentor();
            else if (s.getMentee() != null && !s.getMentee().getId().equals(penggunaId)) partner = s.getMentee();
            if (partner != null && seen.add(partner.getId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("partner", partner);
                e.put("topik", s.getTopikPembahasan());
                result.add(e);
                if (result.size() >= 5) break;
            }
        }
        return result;
    }
}

