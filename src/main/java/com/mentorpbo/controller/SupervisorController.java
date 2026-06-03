package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.service.MentoringService;
import com.mentorpbo.service.PenggunaService;
import com.mentorpbo.service.SupervisorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller SupervisorController - Menangani fitur khusus supervisor (Guru/Dosen).
 */
@Controller
public class SupervisorController {

    private final SupervisorService supervisorService;
    private final MentoringService mentoringService;
    private final PenggunaService penggunaService;

    @Autowired
    public SupervisorController(SupervisorService supervisorService,
                                MentoringService mentoringService,
                                PenggunaService penggunaService) {
        this.supervisorService = supervisorService;
        this.mentoringService = mentoringService;
        this.penggunaService = penggunaService;
    }

    /** Inject data notifikasi + preferences ke model (untuk semua halaman supervisor). */
    private void siapkanDataUmumSupervisor(Long penggunaId, Model model) {
        model.addAttribute("notifBelumDibaca", penggunaService.hitungNotifikasiBelumDibaca(penggunaId));
        model.addAttribute("daftarNotifikasi", penggunaService.getDaftarNotifikasi(penggunaId));
        model.addAttribute("preferences", penggunaService.getOrCreatePreferences(penggunaId));
    }

    // === ENDPOINT ===

    /** GET /validasi-program — Daftar sesi menunggu validasi */
    @GetMapping("/validasi-program")
    public String halamanValidasi(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);
        List<SesiMentoring> sesiMenunggu = supervisorService.getSesiMenungguValidasi(penggunaId);
        model.addAttribute("sesiMenungguValidasi", sesiMenunggu);
        model.addAttribute("totalSesiDivalidasi",
            supervisorService.getStatistikDashboard(penggunaId).get("sesiDivalidasi"));
        return "supervisor/validasi";
    }

    /** POST /validasi-program/{id} — Validasi sesi */
    @PostMapping("/validasi-program/{sesiId}")
    public String validasiSesi(@PathVariable Long sesiId,
                               @RequestParam String catatan,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            supervisorService.validasiSesi(sesiId, penggunaId, catatan);
            redirectAttributes.addFlashAttribute("sukses", "Sesi berhasil divalidasi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validasi-program";
    }

    /** POST /tinjau-sesi/{id} — Mulai tinjauan: BELUM_DITINJAU → SEDANG_DITINJAU */
    @PostMapping("/tinjau-sesi/{sesiId}")
    public String mulaiTinjauSesi(@PathVariable Long sesiId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            supervisorService.mulaiTinjauSesi(sesiId, penggunaId);
            redirectAttributes.addFlashAttribute("sukses", "Laporan mulai ditinjau.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validasi-program";
    }

    /** POST /tolak-sesi/{id} — Tolak validasi */
    @PostMapping("/tolak-sesi/{sesiId}")
    public String tolakSesi(@PathVariable Long sesiId,
                            @RequestParam String alasan,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            supervisorService.tolakValidasiSesi(sesiId, penggunaId, alasan);
            redirectAttributes.addFlashAttribute("sukses", "Sesi ditolak.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validasi-program";
    }

    /** GET /validasi-mentor — Daftar pendaftaran mentor menunggu persetujuan (scoped) */
    @GetMapping("/validasi-mentor")
    public String halamanValidasiMentor(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);
        model.addAttribute("mentorPending", supervisorService.getMentorPendingDalamScope(penggunaId));
        return "supervisor/validasi-mentor";
    }

    /** POST /validasi-mentor/{id}/setujui — Setujui pendaftaran mentor */
    @PostMapping("/validasi-mentor/{mentorId}/setujui")
    public String setujuiMentor(@PathVariable Long mentorId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            supervisorService.setujuiMentor(mentorId, penggunaId);
            redirectAttributes.addFlashAttribute("sukses", "Pendaftaran mentor berhasil disetujui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validasi-mentor";
    }

    /** POST /validasi-mentor/{id}/tolak — Tolak pendaftaran mentor */
    @PostMapping("/validasi-mentor/{mentorId}/tolak")
    public String tolakMentor(@PathVariable Long mentorId,
                              @RequestParam String alasan,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            supervisorService.tolakMentor(mentorId, penggunaId, alasan);
            redirectAttributes.addFlashAttribute("sukses", "Pendaftaran mentor ditolak.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/validasi-mentor";
    }

    /** GET /monitoring-mentor — Monitoring & ranking mentor */
    @GetMapping("/monitoring-mentor")
    public String halamanMonitoringMentor(
            @RequestParam(required = false, defaultValue = "KAMPUS") String lingkungan,
            HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);

        if ("SEKOLAH".equalsIgnoreCase(lingkungan)) {
            model.addAttribute("rankingMentor", mentoringService.getRankingMentorSiswa());
            model.addAttribute("lingkungan", "SEKOLAH");
        } else {
            model.addAttribute("rankingMentor", mentoringService.getRankingMentorMahasiswa());
            model.addAttribute("lingkungan", "KAMPUS");
        }

        return "supervisor/ranking-mentor";
    }

    /** GET /data-mahasiswa — Data mahasiswa & kandidat asdos */
    @GetMapping("/data-mahasiswa")
    public String halamanDataMahasiswa(
            @RequestParam(required = false, defaultValue = "3.0") double minIpk,
            @RequestParam(required = false, defaultValue = "4.0") double minRating,
            @RequestParam(required = false, defaultValue = "5") int minSesi,
            HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);

        List<Mahasiswa> kandidat = supervisorService.cariKandidatAsdos(penggunaId, minIpk, 5, minSesi, minRating);
        model.addAttribute("kandidatAsdos", kandidat);
        model.addAttribute("asdosAktif", supervisorService.getAsdosAktif(penggunaId));
        model.addAttribute("minIpk", minIpk);
        model.addAttribute("minRating", minRating);
        model.addAttribute("minSesi", minSesi);

        return "supervisor/data-mahasiswa";
    }

    /** POST /rekomendasiAsdos — Rekomendasikan sebagai asdos */
    @PostMapping("/rekomendasiAsdos")
    public String rekomendasiAsdos(@RequestParam Long mahasiswaId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Long dosenId = (Long) session.getAttribute("penggunaId");
        if (dosenId == null) return "redirect:/login";

        try {
            supervisorService.rekomendasikanSebagaiAsdos(mahasiswaId, dosenId);
            redirectAttributes.addFlashAttribute("sukses",
                "Mahasiswa berhasil direkomendasikan sebagai kandidat Asdos!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/data-mahasiswa";
    }

    /** GET /pengaturan — Pengaturan akun supervisor */
    @GetMapping("/pengaturan")
    public String halamanPengaturan(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> {
            model.addAttribute("pengguna", p);
            if (p instanceof Dosen d) model.addAttribute("dosen", d);
            else if (p instanceof Guru g) model.addAttribute("guru", g);
        });
        siapkanDataUmumSupervisor(penggunaId, model);
        return "supervisor/pengaturan";
    }

    /** GET /bantuan — Pusat bantuan supervisor */
    @GetMapping("/bantuan")
    public String halamanBantuan(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);
        return "dashboard/bantuan-dosen";
    }

    /** GET /laporan-akademik — Laporan akademik supervisor */
    @GetMapping("/laporan-akademik")
    public String halamanLaporanAkademik(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        penggunaService.getPenggunaById(penggunaId).ifPresent(p -> model.addAttribute("pengguna", p));
        siapkanDataUmumSupervisor(penggunaId, model);

        var statistik = supervisorService.getStatistikDashboard(penggunaId);
        model.addAttribute("statistik", statistik);

        return "supervisor/laporan-akademik";
    }

    /** POST /notifikasi/baca-semua — Tandai semua notifikasi sebagai dibaca */
    @PostMapping("/notifikasi/baca-semua")
    public String tandaiSemuaBaca(HttpSession session) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId != null) penggunaService.tandaiSemuaNotifikasiDibaca(penggunaId);
        return "redirect:/dashboard";
    }
}
