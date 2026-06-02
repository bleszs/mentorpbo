package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.RolePengguna;
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
 * MentoringController — Menangani semua alur sesi mentoring.
 *
 * Demonstrasi POLIMORFISME di layer Controller:
 * Metode resolveViewSesi() menginspeksi tipe runtime sesi
 * (SesiOnline / SesiOffline / SesiVideo) dan mengembalikan
 * nama view Thymeleaf yang berbeda — tanpa if-else di tiap endpoint.
 *
 * Lifecycle endpoint:
 *   POST /minta-sesi           → MENUNGGU_KONFIRMASI (mentee meminta)
 *   POST /jadwalkan-sesi       → DIJADWALKAN         (mentor membuat undangan)
 *   POST /sesi/{id}/konfirmasi → DIJADWALKAN         (mentor terima permintaan)
 *   POST /sesi/{id}/mulai      → BERLANGSUNG         (mentor mulai)
 *   POST /sesi/{id}/selesai    → SELESAI             (mentor selesaikan)
 *   POST /sesi/{id}/batal      → DIBATALKAN
 */
@Controller
@RequestMapping("/mentoring")
public class MentoringController {

    private final MentoringService mentoringService;
    private final PenggunaService  penggunaService;

    @Autowired
    public MentoringController(MentoringService mentoringService,
                               PenggunaService penggunaService) {
        this.mentoringService = mentoringService;
        this.penggunaService  = penggunaService;
    }

    // ============================================================
    // PENCARIAN
    // ============================================================

    /** Halaman pencarian mentor untuk mentee. */
    @GetMapping("/cari-mentor")
    public String cariMentor(@RequestParam(required = false) String kataKunci,
                             @RequestParam(required = false, defaultValue = "KAMPUS") String lingkungan,
                             HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        // Tentukan lingkungan otomatis berdasarkan tipe pengguna
        if (pengguna instanceof Mahasiswa m) { model.addAttribute("mahasiswa", m); lingkungan = "KAMPUS"; }
        else if (pengguna instanceof Siswa s) { model.addAttribute("siswa", s); lingkungan = "SEKOLAH"; }

        model.addAttribute("isMahasiswa", pengguna instanceof Mahasiswa);
        model.addAttribute("notifBelumDibaca", penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        if ("SEKOLAH".equalsIgnoreCase(lingkungan)) {
            model.addAttribute("daftarMentor", mentoringService.cariMentorSiswa(kataKunci));
            model.addAttribute("lingkungan", "SEKOLAH");
        } else {
            model.addAttribute("daftarMentor", mentoringService.cariMentorMahasiswa(kataKunci));
            model.addAttribute("lingkungan", "KAMPUS");
        }

        model.addAttribute("kataKunci", kataKunci);
        return "mentoring/cari-mentor";
    }

    /** Halaman pencarian mentee untuk mentor. */
    @GetMapping("/cari-mentee")
    public String cariMentee(@RequestParam(required = false) String kataKunci,
                             @RequestParam(required = false) String semester,
                             HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";
        model.addAttribute("mentor", pengguna);

        List<Mahasiswa> daftarMentee = penggunaService
            .getPenggunaByRole(RolePengguna.MAHASISWA).stream()
            .filter(p -> p instanceof Mahasiswa m && !m.isMentor())
            .map(p -> (Mahasiswa) p)
            .collect(Collectors.toList());

        if (kataKunci != null && !kataKunci.isBlank()) {
            String kw = kataKunci.toLowerCase();
            daftarMentee = daftarMentee.stream()
                .filter(m -> m.getNamaLengkap().toLowerCase().contains(kw)
                          || (m.getProgramStudi() != null && m.getProgramStudi().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        }
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

    // ============================================================
    // DAFTAR & DETAIL SESI
    // ============================================================

    /** Daftar semua sesi pengguna yang sedang login. */
    @GetMapping("/sesi")
    public String daftarSesi(HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("pengguna", pengguna);

        List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(penggunaId);
        model.addAttribute("semuaSesi", semuaSesi);
        model.addAttribute("notifBelumDibaca", penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        if (pengguna instanceof Mahasiswa mhs && !mhs.isMentor()) {
            model.addAttribute("mahasiswa", mhs);
            model.addAttribute("sesiMendatang", mentoringService.getSesiMendatangMentee(penggunaId));
            model.addAttribute("sesiSelesai", semuaSesi.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI).count());
            model.addAttribute("totalPoin", mhs.getTotalPoinProgres());
        }

        return "mentoring/daftar-sesi";
    }

    /**
     * Detail sesi dengan POLYMORPHIC VIEW ROUTING.
     *
     * Ketika sesi sedang BERLANGSUNG, controller menginspeksi tipe runtime sesi
     * dan mengarahkan ke template yang berbeda — ini adalah polimorfisme di layer
     * presentasi, bukan hanya di domain model.
     *
     * Mapping view:
     *   BERLANGSUNG + SesiOnline  → mentoring/sesi-online
     *   BERLANGSUNG + SesiOffline → mentoring/sesi-offline
     *   BERLANGSUNG + SesiVideo   → mentoring/sesi-video
     *   Semua status lain         → mentoring/detail-sesi
     */
    @GetMapping("/sesi/{id}")
    public String detailSesi(@PathVariable Long id, HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        SesiMentoring sesi = mentoringService.getSesiById(id).orElse(null);
        if (sesi == null) return "redirect:/mentoring/sesi";

        model.addAttribute("sesi", sesi);
        model.addAttribute("jenisSesi", sesi.getJenisSesi().name());
        model.addAttribute("instruksiPersiapan", sesi.getInstruksiPersiapan());
        model.addAttribute("ringkasanSesi", sesi.getRingkasanSesi());

        penggunaService.getPenggunaById(penggunaId)
            .ifPresent(p -> model.addAttribute("pengguna", p));

        model.addAttribute("reviews",
            mentoringService.getReviewUntukPengguna(sesi.getMentor().getId()));
        model.addAttribute("notifBelumDibaca",
            penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        // Tambahkan field spesifik subclass ke model agar template bisa mengaksesnya
        if (sesi instanceof SesiOnline online) {
            model.addAttribute("tautanMeeting",   online.getTautanMeeting());
            model.addAttribute("platformDaring",  online.getPlatformDaring());
            model.addAttribute("kodeAkses",       online.getKodeAkses());
        } else if (sesi instanceof SesiOffline offline) {
            model.addAttribute("namaLokasi",      offline.getNamaLokasi());
            model.addAttribute("alamatLengkap",   offline.getAlamatLengkap());
            model.addAttribute("nomorRuangan",    offline.getNomorRuangan());
        } else if (sesi instanceof SesiVideo video) {
            model.addAttribute("tautanVideo",     video.getTautanVideo());
            model.addAttribute("platformVideo",   video.getPlatformVideo());
            model.addAttribute("jumlahTontonan",  video.getJumlahTontonan());
        }

        // Polymorphic view resolution — view berbeda saat BERLANGSUNG
        return resolveViewSesi(sesi);
    }

    // ============================================================
    // FORM PERMINTAAN SESI (MENTEE → MENTOR)
    // ============================================================

    /** Form mentee memilih waktu dan topik sebelum mengirim permintaan ke mentor. */
    @GetMapping("/minta-sesi")
    public String formMintaSesi(@RequestParam Long mentorId,
                                HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna mentee = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (mentee == null) return "redirect:/login";

        Pengguna mentor = penggunaService.getPenggunaById(mentorId).orElse(null);
        if (mentor == null) return "redirect:/mentoring/cari-mentor";

        boolean mentorValid = (mentor instanceof Mahasiswa m && m.isMentor())
                           || (mentor instanceof Siswa s && s.isMentor());
        if (!mentorValid) return "redirect:/mentoring/cari-mentor";

        model.addAttribute("mentee",       mentee);
        model.addAttribute("mentorPilihan", mentor);
        if (mentee instanceof Mahasiswa mhs) model.addAttribute("mahasiswa", mhs);
        model.addAttribute("notifBelumDibaca",
            penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        return "mentoring/minta-sesi";
    }

    /**
     * Submit permintaan sesi dari mentee.
     * Status awal: MENUNGGU_KONFIRMASI — mentor harus konfirmasi sebelum jadwal aktif.
     *
     * FIX: tidak lagi memanggil jadwalkanSesi() secara manual di sini.
     * Delegasi penuh ke MentoringService.buatPermintaanSesi() yang menangani
     * semua logika jadwal dan notifikasi.
     */
    @PostMapping("/minta-sesi")
    public String submitMintaSesi(@RequestParam Long   mentorId,
                                  @RequestParam String topikPembahasan,
                                  @RequestParam String tanggal,
                                  @RequestParam String jamMulai,
                                  @RequestParam(defaultValue = "60") int durasiMenit,
                                  @RequestParam(defaultValue = "Zoom") String platformDaring,
                                  @RequestParam(required = false, defaultValue = "") String catatan,
                                  HttpSession session,
                                  RedirectAttributes flash) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        try {
            Pengguna mentee = getPenggunaAtauLempar(penggunaId, "Mentee tidak ditemukan.");
            Pengguna mentor = getPenggunaAtauLempar(mentorId, "Mentor tidak ditemukan.");

            LocalDateTime waktuMulai = parseWaktu(tanggal, jamMulai);

            SesiOnline sesi = new SesiOnline(topikPembahasan, durasiMenit,
                mentor, mentee, "", platformDaring);
            if (!catatan.isBlank()) sesi.setDeskripsi(catatan);

            // Delegasi ke service — TIDAK memanggil jadwalkanSesi() manual di sini
            mentoringService.buatPermintaanSesi(sesi, waktuMulai, durasiMenit);

            flash.addFlashAttribute("sukses",
                "Permintaan sesi \"" + topikPembahasan + "\" berhasil dikirim! Menunggu konfirmasi mentor.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal mengirim permintaan: " + e.getMessage());
        }
        return "redirect:/mentoring/sesi";
    }

    // ============================================================
    // FORM JADWALKAN SESI (MENTOR → MENTEE)
    // ============================================================

    /** Form mentor membuat undangan sesi baru ke mentee tertentu. */
    @GetMapping("/jadwalkan-sesi")
    public String formJadwalkanSesi(@RequestParam(required = false) Long menteeId,
                                    HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("mentor", pengguna);
        if (menteeId != null) model.addAttribute("selectedMenteeId", menteeId);

        // Daftar mentee sesuai tipe mentor (Siswa→Siswa, Mahasiswa→Mahasiswa)
        RolePengguna roleMentee = (pengguna instanceof Siswa) ? RolePengguna.SISWA : RolePengguna.MAHASISWA;
        List<Pengguna> daftarMentee = penggunaService.getPenggunaByRole(roleMentee).stream()
            .filter(p -> (p instanceof Siswa s && !s.isMentor())
                      || (p instanceof Mahasiswa m && !m.isMentor()))
            .collect(Collectors.toList());
        model.addAttribute("daftarMentee", daftarMentee);
        model.addAttribute("isSiswaMentor", pengguna instanceof Siswa);

        List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentor(penggunaId);
        model.addAttribute("sesiMendatang", sesiMendatang);

        int maxSlot = 15;
        int aktif   = Math.min(sesiMendatang.size(), maxSlot);
        model.addAttribute("maxSlot",          maxSlot);
        model.addAttribute("sesiAktifMingguIni", aktif);
        model.addAttribute("slotTersedia",     Math.max(0, maxSlot - aktif));
        model.addAttribute("persenTerpakai",   aktif * 100 / maxSlot);
        model.addAttribute("notifBelumDibaca",
            penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        return "mentoring/jadwalkan-sesi";
    }

    /** Submit undangan sesi online dari mentor — bisa ke banyak mentee sekaligus. */
    @PostMapping("/jadwalkan-sesi")
    public String buatSesiOnline(@RequestParam List<Long> menteeId,
                                 @RequestParam String topikPembahasan,
                                 @RequestParam String tanggal,
                                 @RequestParam String jamMulai,
                                 @RequestParam int    durasiMenit,
                                 @RequestParam(defaultValue = "Zoom") String platformDaring,
                                 @RequestParam(required = false, defaultValue = "") String tautanMeeting,
                                 @RequestParam(required = false, defaultValue = "") String catatan,
                                 HttpSession session, RedirectAttributes flash) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        int berhasil = 0;
        List<String> gagal = new ArrayList<>();
        try {
            Pengguna mentor = getPenggunaAtauLempar(penggunaId, "Mentor tidak ditemukan.");
            LocalDateTime waktuMulai = parseWaktu(tanggal, jamMulai);

            for (Long mid : menteeId) {
                try {
                    Pengguna mentee = getPenggunaAtauLempar(mid, "Mentee tidak ditemukan.");
                    SesiOnline sesi = new SesiOnline(topikPembahasan, durasiMenit,
                        mentor, mentee, tautanMeeting, platformDaring);
                    if (!catatan.isBlank()) sesi.setDeskripsi(catatan);
                    mentoringService.buatDanJadwalkanSesi(sesi, waktuMulai, durasiMenit);
                    berhasil++;
                } catch (Exception e) {
                    gagal.add(e.getMessage());
                }
            }

            if (berhasil > 0) {
                flash.addFlashAttribute("sukses",
                    "Undangan sesi \"" + topikPembahasan + "\" berhasil dikirim ke " + berhasil + " mentee!");
            }
            if (!gagal.isEmpty()) {
                flash.addFlashAttribute("error", "Beberapa gagal: " + String.join(", ", gagal));
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal membuat sesi: " + e.getMessage());
        }
        return "redirect:/mentoring/jadwal-sesi";
    }

    // ============================================================
    // AKSI STATE TRANSITION
    // ============================================================

    /** Mentor mengkonfirmasi permintaan sesi: MENUNGGU_KONFIRMASI → DIJADWALKAN. */
    @PostMapping("/sesi/{id}/konfirmasi")
    public String konfirmasiSesi(@PathVariable Long id,
                                 HttpSession session, RedirectAttributes flash) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.konfirmasiSesi(id, penggunaId);
            flash.addFlashAttribute("sukses", "Sesi berhasil dikonfirmasi! Jadwal telah dikunci.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + id;
    }

    /**
     * Mentor memulai sesi: DIJADWALKAN → BERLANGSUNG.
     * Setelah redirect ke detail-sesi, view akan diubah ke template spesifik
     * (sesi-online / sesi-offline / sesi-video) oleh resolveViewSesi().
     */
    @PostMapping("/sesi/{id}/mulai")
    public String mulaiSesi(@PathVariable Long id,
                            HttpSession session, RedirectAttributes flash) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.mulaiSesi(id, penggunaId);
            flash.addFlashAttribute("sukses", "Sesi dimulai! Halaman akan menampilkan ruang sesi aktif.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + id;
    }

    /**
     * Mentor menyelesaikan sesi: BERLANGSUNG → SELESAI.
     * Service otomatis memicu notifikasi laporan ke supervisor.
     */
    @PostMapping("/sesi/{id}/selesai")
    public String selesaikanSesi(@PathVariable Long id, RedirectAttributes flash) {
        try {
            mentoringService.selesaikanSesi(id);
            flash.addFlashAttribute("sukses",
                "Sesi selesai! Laporan akademik sudah diteruskan ke supervisor untuk divalidasi.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + id;
    }

    /** Membatalkan sesi (bisa dilakukan selama masih aktif). */
    @PostMapping("/sesi/{id}/batal")
    public String batalkanSesi(@PathVariable Long id,
                               @RequestParam String alasan, RedirectAttributes flash) {
        try {
            mentoringService.batalkanSesi(id, alasan);
            flash.addFlashAttribute("sukses", "Sesi berhasil dibatalkan.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi";
    }

    /** Mentee memberikan rating setelah sesi SELESAI. */
    @PostMapping("/review")
    public String beriReview(@RequestParam Long sesiId,
                             @RequestParam int  nilaiRating,
                             @RequestParam(required = false, defaultValue = "") String ulasan,
                             @RequestParam(required = false, defaultValue = "") String saranKritik,
                             HttpSession session, RedirectAttributes flash) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.beriReviewDanRatingLengkap(sesiId, penggunaId, nilaiRating,
                ulasan.isBlank() ? null : ulasan,
                saranKritik.isBlank() ? null : saranKritik);
            flash.addFlashAttribute("sukses", "Rating berhasil dikirim! Anda mendapat +3 poin. Terima kasih.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mentoring/sesi/" + sesiId;
    }

    // ============================================================
    // HALAMAN JADWAL (KHUSUS MENTOR)
    // ============================================================

    @GetMapping("/jadwal-sesi")
    public String jadwalSesi(HttpSession session, Model model) {
        Long penggunaId = requireLogin(session);
        if (penggunaId == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(penggunaId).orElse(null);
        if (pengguna == null) return "redirect:/login";

        model.addAttribute("mentor", pengguna);

        List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentor(penggunaId);
        List<SesiMentoring> semuaSesi     = mentoringService.getSemuaSesiPengguna(penggunaId);

        model.addAttribute("sesiMendatang", sesiMendatang);
        model.addAttribute("semuaSesi",     semuaSesi);
        model.addAttribute("sesiBerikutnya", sesiMendatang.isEmpty() ? null : sesiMendatang.get(0));

        long totalMenit = semuaSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
            .mapToLong(SesiMentoring::getDurasiMenit).sum();

        Map<String, Object> statistik = new HashMap<>();
        statistik.put("confirmedSessions", semuaSesi.stream().filter(s -> s.getStatusSesi() == StatusSesi.DIJADWALKAN).count());
        statistik.put("pendingRequests",   semuaSesi.stream().filter(s -> s.getStatusSesi() == StatusSesi.MENUNGGU_KONFIRMASI).count());
        statistik.put("ongoingSessions",   semuaSesi.stream().filter(s -> s.getStatusSesi() == StatusSesi.BERLANGSUNG).count());
        statistik.put("hoursCompleted",    totalMenit / 60);
        statistik.put("activeMentees",     sesiMendatang.stream().map(s -> s.getMentee().getId()).distinct().count());
        model.addAttribute("statistik", statistik);

        model.addAttribute("notifBelumDibaca",
            penggunaService.hitungNotifikasiBelumDibaca(penggunaId));

        return "mentoring/jadwal-sesi";
    }

    // ============================================================
    // POLYMORPHIC VIEW RESOLVER (PRIVATE HELPER)
    // ============================================================

    /**
     * Menentukan nama view Thymeleaf berdasarkan tipe runtime SesiMentoring.
     *
     * Ini adalah implementasi POLIMORFISME di layer presentasi:
     * Alih-alih satu template yang panjang penuh th:if/th:switch,
     * setiap tipe sesi mendapatkan template dedicated-nya sendiri
     * ketika sesi sedang BERLANGSUNG.
     *
     * Saat sesi tidak BERLANGSUNG (menunggu / dijadwalkan / selesai),
     * tampilan ringkasan generik detail-sesi.html tetap digunakan.
     *
     * @param sesi objek sesi yang tipenya akan diperiksa
     * @return nama view Thymeleaf yang sesuai
     */
    private String resolveViewSesi(SesiMentoring sesi) {
        if (sesi.getStatusSesi() == StatusSesi.BERLANGSUNG) {
            if (sesi instanceof SesiOnline)  return "mentoring/sesi-online";
            if (sesi instanceof SesiOffline) return "mentoring/sesi-offline";
            if (sesi instanceof SesiVideo)   return "mentoring/sesi-video";
        }
        return "mentoring/detail-sesi";
    }

    // ============================================================
    // HELPER PRIVATE
    // ============================================================

    private Long requireLogin(HttpSession session) {
        Object id = session.getAttribute("penggunaId");
        return (id instanceof Long l) ? l : null;
    }

    private Pengguna getPenggunaAtauLempar(Long id, String pesan) {
        return penggunaService.getPenggunaById(id)
            .orElseThrow(() -> new NoSuchElementException(pesan));
    }

    private LocalDateTime parseWaktu(String tanggal, String jam) {
        return LocalDateTime.parse(tanggal + "T" + jam,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}
