package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.StatusSesi;
import com.mentorpbo.service.MentoringService;
import com.mentorpbo.service.PenggunaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller MentoringController - Menangani alur pencarian mentor dan sesi mentoring.
 *
 * Endpoint:
 * - GET  /mentoring/cari-mentor     → Halaman pencarian mentor
 * - GET  /mentoring/sesi            → Daftar sesi pengguna
 * - GET  /mentoring/sesi/{id}       → Detail sesi tertentu
 * - POST /mentoring/sesi/buat       → Buat sesi baru
 * - POST /mentoring/sesi/{id}/batal → Batalkan sesi
 * - POST /mentoring/review          → Beri review/rating
 * - GET  /mentoring/jadwal-sesi     → Halaman daftar jadwal sesi mentor
 * - GET  /mentoring/jadwalkan-sesi  → Form buat sesi baru
 * - POST /mentoring/jadwalkan-sesi  → Buat sesi online baru
 */
@Controller
@RequestMapping("/mentoring")
public class MentoringController {

    private final MentoringService mentoringService;
    private final PenggunaService penggunaService;

    @Autowired
    public MentoringController(MentoringService mentoringService,
                               PenggunaService penggunaService) {
        this.mentoringService = mentoringService;
        this.penggunaService = penggunaService;
    }

    /**
     * Halaman pencarian mentee (untuk mentor).
     */
    @GetMapping("/cari-mentee")
    public String cariMentee(@RequestParam(required = false) String kataKunci,
                             @RequestParam(required = false) String semester,
                             HttpSession session,
                             Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";
        model.addAttribute("mentor", pengguna);

        List<Mahasiswa> daftarMentee = penggunaService
            .getPenggunaByRole(com.mentorpbo.model.enums.RolePengguna.MAHASISWA).stream()
            .filter(p -> p instanceof Mahasiswa)
            .map(p -> (Mahasiswa) p)
            .filter(m -> !m.isMentor())
            .collect(Collectors.toList());

        // Filter kata kunci
        if (kataKunci != null && !kataKunci.isBlank()) {
            String kw = kataKunci.toLowerCase();
            daftarMentee = daftarMentee.stream()
                .filter(m -> m.getNamaLengkap().toLowerCase().contains(kw)
                    || (m.getProgramStudi() != null && m.getProgramStudi().toLowerCase().contains(kw))
                    || (m.getMataKuliahKeahlian() != null && m.getMataKuliahKeahlian().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        }

        // Filter semester
        if (semester != null && !semester.isBlank()) {
            try {
                int sem = Integer.parseInt(semester);
                daftarMentee = daftarMentee.stream()
                    .filter(m -> m.getSemester() == sem)
                    .collect(Collectors.toList());
            } catch (NumberFormatException ignored) {}
        }

        model.addAttribute("daftarMentee", daftarMentee);
        model.addAttribute("kataKunci", kataKunci);
        model.addAttribute("semester", semester);
        return "mentoring/cari-mentee";
    }

    /**
     * Halaman pencarian mentor.
     */
    @GetMapping("/cari-mentor")
    public String cariMentor(@RequestParam(required = false) String kataKunci,
                             @RequestParam(required = false, defaultValue = "KAMPUS") String lingkungan,
                             HttpSession session,
                             Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        boolean isMahasiswa = pengguna instanceof Mahasiswa;
        if (isMahasiswa) {
            model.addAttribute("mahasiswa", (Mahasiswa) pengguna);
            lingkungan = "KAMPUS";
        }
        model.addAttribute("isMahasiswa", isMahasiswa);
        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if ("SEKOLAH".equalsIgnoreCase(lingkungan)) {
            List<Siswa> mentorSiswa = mentoringService.cariMentorSiswa(kataKunci);
            model.addAttribute("daftarMentor", mentorSiswa);
            model.addAttribute("lingkungan", "SEKOLAH");
        } else {
            List<Mahasiswa> mentorMahasiswa = mentoringService.cariMentorMahasiswa(kataKunci);
            model.addAttribute("daftarMentor", mentorMahasiswa);
            model.addAttribute("lingkungan", "KAMPUS");
        }

        model.addAttribute("kataKunci", kataKunci);
        return "mentoring/cari-mentor";
    }

    /**
     * Halaman daftar sesi mentoring pengguna.
     */
    @GetMapping("/sesi")
    public String daftarSesi(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("pengguna", pengguna);

        List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(penggunaId);
        model.addAttribute("semuaSesi", semuaSesi);

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (pengguna instanceof Mahasiswa mahasiswa && !mahasiswa.isMentor()) {
            model.addAttribute("mahasiswa", mahasiswa);
            List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentee(penggunaId);
            model.addAttribute("sesiMendatang", sesiMendatang);
            long sesiSelesai = semuaSesi.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
                .count();
            model.addAttribute("sesiSelesai", sesiSelesai);
            model.addAttribute("totalPoin", mahasiswa.getTotalPoinProgres());
        }

        return "mentoring/daftar-sesi";
    }

    /**
     * Halaman detail sesi mentoring tertentu.
     */
    @GetMapping("/sesi/{id}")
    public String detailSesi(@PathVariable Long id,
                             HttpSession session,
                             Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        SesiMentoring sesi = mentoringService.getSesiById(id).orElse(null);
        if (sesi == null) return "redirect:/mentoring/sesi";

        model.addAttribute("sesi", sesi);

        // Tambahkan pengguna yang sedang login agar template bisa cek aksi yang diizinkan
        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));

        List<ReviewRating> reviews = mentoringService.getReviewUntukPengguna(sesi.getMentor().getId());
        model.addAttribute("reviews", reviews);

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        return "mentoring/detail-sesi";
    }

    /**
     * Mentor mengkonfirmasi permintaan sesi dari mentee.
     */
    @PostMapping("/sesi/{id}/konfirmasi")
    public String konfirmasiSesi(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.konfirmasiSesi(id, penggunaId);
            redirectAttributes.addFlashAttribute("sukses", "Sesi berhasil dikonfirmasi dan dijadwalkan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + id;
    }

    /**
     * Membatalkan sesi mentoring.
     */
    @PostMapping("/sesi/{id}/batal")
    public String batalkanSesi(@PathVariable Long id,
                               @RequestParam String alasan,
                               RedirectAttributes redirectAttributes) {
        try {
            mentoringService.batalkanSesi(id, alasan);
            redirectAttributes.addFlashAttribute("sukses", "Sesi berhasil dibatalkan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi";
    }

    /**
     * Menyelesaikan sesi mentoring.
     */
    @PostMapping("/sesi/{id}/selesai")
    public String selesaikanSesi(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            mentoringService.selesaikanSesi(id);
            redirectAttributes.addFlashAttribute("sukses", "Sesi berhasil diselesaikan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + id;
    }

    /**
     * Memberikan review dan rating untuk sesi yang sudah selesai.
     */
    @PostMapping("/review")
    public String beriReview(@RequestParam Long sesiId,
                             @RequestParam int nilaiRating,
                             @RequestParam String ulasan,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.beriReviewDanRating(sesiId, penggunaId, nilaiRating, ulasan);
            redirectAttributes.addFlashAttribute("sukses", "Review berhasil dikirim!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + sesiId;
    }

    /**
     * Halaman Jadwal Sesi - daftar sesi mendatang dan riwayat.
     */
    @GetMapping("/jadwal-sesi")
    public String jadwalSesi(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("mentor", pengguna);

        List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentor(penggunaId);
        model.addAttribute("sesiMendatang", sesiMendatang);

        List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(penggunaId);
        model.addAttribute("semuaSesi", semuaSesi);

        long confirmedSessions = semuaSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.DIJADWALKAN).count();
        long pendingRequests = semuaSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.MENUNGGU_KONFIRMASI).count();
        long totalMenit = semuaSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
            .mapToLong(SesiMentoring::getDurasiMenit).sum();
        long hoursCompleted = totalMenit / 60;
        long activeMentees = sesiMendatang.stream()
            .map(s -> s.getMentee().getId()).distinct().count();

        Map<String, Object> statistik = new HashMap<>();
        statistik.put("confirmedSessions", confirmedSessions);
        statistik.put("pendingRequests", pendingRequests);
        statistik.put("hoursCompleted", hoursCompleted);
        statistik.put("activeMentees", activeMentees);
        model.addAttribute("statistik", statistik);

        model.addAttribute("sesiBerikutnya", sesiMendatang.isEmpty() ? null : sesiMendatang.get(0));

        return "mentoring/jadwal-sesi";
    }

    /**
     * Halaman form jadwalkan sesi baru.
     */
    @GetMapping("/jadwalkan-sesi")
    public String jadwalkanSesi(@RequestParam(required = false) Long menteeId,
                                HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("mentor", pengguna);
        if (menteeId != null) model.addAttribute("selectedMenteeId", menteeId);

        // Daftar mentee: Mahasiswa non-mentor untuk mentor Mahasiswa,
        // Siswa non-mentor untuk mentor Siswa, semua jika lainnya
        List<Pengguna> daftarMentee;
        if (pengguna instanceof Siswa) {
            daftarMentee = penggunaService
                .getPenggunaByRole(com.mentorpbo.model.enums.RolePengguna.SISWA).stream()
                .filter(p -> p instanceof Siswa s && !s.isMentor())
                .collect(Collectors.toList());
        } else {
            daftarMentee = penggunaService
                .getPenggunaByRole(com.mentorpbo.model.enums.RolePengguna.MAHASISWA).stream()
                .filter(p -> p instanceof Mahasiswa m && !m.isMentor())
                .collect(Collectors.toList());
        }
        model.addAttribute("daftarMentee", daftarMentee);

        List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentor(penggunaId);
        model.addAttribute("sesiMendatang", sesiMendatang);

        // Kapasitas slot minggu ini
        int maxSlot = 15;
        int sesiAktifMingguIni = Math.min(sesiMendatang.size(), maxSlot);
        int slotTersedia = Math.max(0, maxSlot - sesiAktifMingguIni);
        int persenTerpakai = sesiAktifMingguIni * 100 / maxSlot;
        model.addAttribute("maxSlot", maxSlot);
        model.addAttribute("sesiAktifMingguIni", sesiAktifMingguIni);
        model.addAttribute("slotTersedia", slotTersedia);
        model.addAttribute("persenTerpakai", persenTerpakai);

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        return "mentoring/jadwalkan-sesi";
    }

    /**
     * Halaman form mentee meminta sesi ke mentor tertentu.
     * Mendukung Mahasiswa mentee → Mahasiswa mentor, dan Siswa mentee → Siswa mentor.
     */
    @GetMapping("/minta-sesi")
    public String mintaSesi(@RequestParam Long mentorId,
                            HttpSession session,
                            Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        Pengguna mentorPengguna = penggunaService.getPenggunaById(mentorId).orElse(null);
        if (mentorPengguna == null) return "redirect:/mentoring/cari-mentor";

        // Validasi: mentor harus aktif sebagai mentor
        boolean mentorValid = (mentorPengguna instanceof Mahasiswa m && m.isMentor())
                           || (mentorPengguna instanceof Siswa s && s.isMentor());
        if (!mentorValid) return "redirect:/mentoring/cari-mentor";

        model.addAttribute("mentee", pengguna);
        model.addAttribute("mentorPilihan", mentorPengguna);

        // Kompatibilitas: tambahkan mahasiswa juga jika tipe Mahasiswa
        if (pengguna instanceof Mahasiswa mahasiswa) model.addAttribute("mahasiswa", mahasiswa);

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        return "mentoring/minta-sesi";
    }

    /**
     * Submit permintaan sesi dari mentee ke mentor.
     */
    @PostMapping("/minta-sesi")
    public String submitMintaSesi(
            @RequestParam Long mentorId,
            @RequestParam String topikPembahasan,
            @RequestParam String tanggal,
            @RequestParam String jamMulai,
            @RequestParam(defaultValue = "60") int durasiMenit,
            @RequestParam(defaultValue = "Zoom") String platformDaring,
            @RequestParam(required = false, defaultValue = "") String catatan,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            Pengguna mentee = penggunaService.getPenggunaById(penggunaId)
                .orElseThrow(() -> new IllegalStateException("Mentee tidak ditemukan."));
            Pengguna mentor = penggunaService.getPenggunaById(mentorId)
                .orElseThrow(() -> new IllegalArgumentException("Mentor tidak ditemukan."));

            LocalDateTime waktuMulai = LocalDateTime.parse(
                tanggal + "T" + jamMulai,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            SesiOnline sesi = new SesiOnline(topikPembahasan, durasiMenit,
                mentor, mentee, "", platformDaring);

            if (!catatan.isBlank()) sesi.setDeskripsi(catatan);

            // Set status MENUNGGU_KONFIRMASI agar mentor perlu konfirmasi dulu
            sesi.jadwalkanSesi(waktuMulai, durasiMenit);
            sesi.setStatusSesi(StatusSesi.MENUNGGU_KONFIRMASI);

            mentoringService.buatDanJadwalkanSesi(sesi, waktuMulai, durasiMenit);

            redirectAttributes.addFlashAttribute("sukses",
                "Permintaan sesi \"" + topikPembahasan + "\" berhasil dikirim! Menunggu konfirmasi mentor.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengirim permintaan: " + e.getMessage());
        }
        return "redirect:/mentoring/sesi";
    }

    /**
     * Membuat sesi online baru dari form jadwalkan-sesi.
     */
    @PostMapping("/jadwalkan-sesi")
    public String buatSesiOnline(
            @RequestParam Long menteeId,
            @RequestParam String topikPembahasan,
            @RequestParam String tanggal,
            @RequestParam String jamMulai,
            @RequestParam int durasiMenit,
            @RequestParam(defaultValue = "Zoom") String platformDaring,
            @RequestParam(required = false, defaultValue = "") String tautanMeeting,
            @RequestParam(required = false, defaultValue = "") String catatan,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            Pengguna mentor = penggunaService.getPenggunaById(penggunaId)
                .orElseThrow(() -> new IllegalStateException("Mentor tidak ditemukan."));
            Pengguna mentee = penggunaService.getPenggunaById(menteeId)
                .orElseThrow(() -> new IllegalArgumentException("Mentee tidak ditemukan."));

            LocalDateTime waktuMulai = LocalDateTime.parse(
                tanggal + "T" + jamMulai,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            SesiOnline sesi = new SesiOnline(topikPembahasan, durasiMenit,
                mentor, mentee, tautanMeeting, platformDaring);

            if (!catatan.isBlank()) sesi.setDeskripsi(catatan);

            mentoringService.buatDanJadwalkanSesi(sesi, waktuMulai, durasiMenit);

            redirectAttributes.addFlashAttribute("sukses",
                "Undangan sesi \"" + topikPembahasan + "\" berhasil dikirim!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal membuat sesi: " + e.getMessage());
        }
        return "redirect:/mentoring/jadwal-sesi";
    }
}
