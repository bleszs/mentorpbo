package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.StatusSesi;
import com.mentorpbo.service.MentoringService;
import com.mentorpbo.service.PenggunaService;
import com.mentorpbo.service.SupervisorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller DashboardController - Menangani routing dashboard berdasarkan role.
 *
 * Setiap role memiliki dashboard berbeda:
 * - Siswa     → dashboard-siswa.html
 * - Mahasiswa → dashboard-mahasiswa.html
 * - Guru      → dashboard-guru.html
 * - Dosen     → dashboard-dosen.html
 *
 * Endpoint:
 * - GET /dashboard → Redirect ke dashboard sesuai role pengguna yang login
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final PenggunaService penggunaService;
    private final MentoringService mentoringService;
    private final SupervisorService supervisorService;

    @Autowired
    public DashboardController(PenggunaService penggunaService,
                               MentoringService mentoringService,
                               SupervisorService supervisorService) {
        this.penggunaService = penggunaService;
        this.mentoringService = mentoringService;
        this.supervisorService = supervisorService;
    }

    /**
     * Endpoint utama dashboard. Menentukan dashboard yang ditampilkan
     * berdasarkan role pengguna yang login (Polymorphism via getDashboardView()).
     */
    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        model.addAttribute("pengguna", pengguna);

        // Hitung notifikasi belum dibaca
        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        // Tentukan dashboard berdasarkan role menggunakan polimorfisme
        String roleStr = pengguna.getRole().name();
        switch (roleStr) {
            case "SISWA" -> siapkanDashboardSiswa(pengguna, model);
            case "MAHASISWA" -> siapkanDashboardMahasiswa(pengguna, model);
            case "GURU" -> siapkanDashboardGuru(pengguna, model);
            case "DOSEN" -> siapkanDashboardDosen(pengguna, model);
        }

        // Polimorfisme: getDashboardView() mengembalikan view berbeda per subclass
        return pengguna.getDashboardView();
    }

    // === Persiapan data dashboard per role ===

    private void siapkanDashboardSiswa(Pengguna pengguna, Model model) {
        Siswa siswa = (Siswa) pengguna;
        model.addAttribute("siswa", siswa);

        List<SesiMentoring> sesiMendatang = mentoringService
            .getSesiMendatangMentee(siswa.getId());
        model.addAttribute("sesiMendatang", sesiMendatang);

        List<SesiMentoring> semuaSesi = mentoringService
            .getSemuaSesiPengguna(siswa.getId());
        model.addAttribute("semuaSesi", semuaSesi);

        model.addAttribute("totalPoin", siswa.getTotalPoinProgres());
        model.addAttribute("rataRating", siswa.hitungRataRataRating());
    }

    private void siapkanDashboardMahasiswa(Pengguna pengguna, Model model) {
        Mahasiswa mahasiswa = (Mahasiswa) pengguna;
        model.addAttribute("mahasiswa", mahasiswa);

        if (mahasiswa.isMentor()) {
            // Data untuk dashboard-mentor.html
            model.addAttribute("mentor", mahasiswa);

            List<SesiMentoring> sesiMendatangMentor = mentoringService
                .getSesiMendatangMentor(mahasiswa.getId());
            model.addAttribute("sesiMendatangMentor", sesiMendatangMentor);

            // Semua sesi mentor (untuk filter derived lists, hanya panggil sekali)
            List<SesiMentoring> allSesiMentor = mentoringService
                .getSemuaSesiPengguna(mahasiswa.getId()).stream()
                .filter(s -> s.getMentor() != null
                          && s.getMentor().getId().equals(mahasiswa.getId()))
                .toList();

            List<SesiMentoring> sesiMenungguKonfirmasi = allSesiMentor.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.MENUNGGU_KONFIRMASI)
                .toList();
            model.addAttribute("sesiMenungguKonfirmasi", sesiMenungguKonfirmasi);

            List<SesiMentoring> sesiSedangBerlangsung = allSesiMentor.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.BERLANGSUNG)
                .toList();
            model.addAttribute("sesiSedangBerlangsung", sesiSedangBerlangsung);

            // Jadwal minggu ini: list 7 hari dengan sesi per hari
            LocalDate today = LocalDate.now();
            LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
            String[] dayLabels = {"SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"};
            List<Map<String, Object>> jadwalMinggu = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate day = startOfWeek.plusDays(i);
                LocalDate dayFinal = day;
                Map<String, Object> dayData = new LinkedHashMap<>();
                dayData.put("label", dayLabels[i]);
                dayData.put("isToday", day.equals(today));
                List<SesiMentoring> sesiHari = sesiMendatangMentor.stream()
                    .filter(s -> s.getWaktuMulai() != null
                              && s.getWaktuMulai().toLocalDate().equals(dayFinal))
                    .toList();
                dayData.put("sesi", sesiHari);
                jadwalMinggu.add(dayData);
            }
            model.addAttribute("jadwalMinggu", jadwalMinggu);

            Map<String, Object> statistik = new HashMap<>();
            statistik.put("totalPoin", mahasiswa.getTotalPoinProgres());
            statistik.put("rataRating",
                Math.round(mahasiswa.hitungRataRataRating() * 10.0) / 10.0);
            statistik.put("jumlahUlasan", mahasiswa.getJumlahPenilaian());
            statistik.put("sesiAktif", sesiMendatangMentor.size());
            statistik.put("sesiDiselesaikan", mahasiswa.getSesiDiselesaikan());
            statistik.put("permintaanBaru", sesiMenungguKonfirmasi.size());
            statistik.put("totalJamMengajar",
                Math.round(mahasiswa.getSesiDiselesaikan() * 1.5 * 10.0) / 10.0);
            model.addAttribute("statistik", statistik);
        } else {
            // Data untuk dashboard-mahasiswa.html (mentee)
            List<SesiMentoring> sesiMendatang = mentoringService
                .getSesiMendatangMentee(mahasiswa.getId());
            model.addAttribute("sesiMendatang", sesiMendatang);

            List<SesiMentoring> semuaSesi = mentoringService
                .getSemuaSesiPengguna(mahasiswa.getId());
            model.addAttribute("semuaSesi", semuaSesi);

            long sesiSelesai = semuaSesi.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
                .count();
            model.addAttribute("sesiSelesai", sesiSelesai);

            List<Pengguna> mentorSaya = semuaSesi.stream()
                .map(SesiMentoring::getMentor)
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
            model.addAttribute("mentorSaya", mentorSaya);

            model.addAttribute("totalPoin", mahasiswa.getTotalPoinProgres());
            model.addAttribute("rataRating", mahasiswa.hitungRataRataRating());
            model.addAttribute("skorAsdos", mahasiswa.hitungSkorKelayakanAsdos());
            model.addAttribute("isKandidatAsdos", mahasiswa.isKandidatAsdos());
        }
    }

    private void siapkanDashboardGuru(Pengguna pengguna, Model model) {
        Guru guru = (Guru) pengguna;
        model.addAttribute("guru", guru);

        // Sesi menunggu validasi
        List<SesiMentoring> sesiMenunggu = supervisorService
            .getSesiMenungguValidasi(guru.getId());
        model.addAttribute("sesiMenungguValidasi", sesiMenunggu);

        // Siswa berprestasi
        List<Siswa> siswaBerprestasi = supervisorService.getSiswaBerprestasi();
        model.addAttribute("siswaBerprestasi", siswaBerprestasi);

        // Ranking mentor siswa
        List<Siswa> rankingMentor = mentoringService.getRankingMentorSiswa();
        model.addAttribute("rankingMentor", rankingMentor);

        // Statistik
        Map<String, Object> statistik = supervisorService.getStatistikDashboard(guru.getId());
        model.addAttribute("statistik", statistik);
    }

    private void siapkanDashboardDosen(Pengguna pengguna, Model model) {
        Dosen dosen = (Dosen) pengguna;
        model.addAttribute("dosen", dosen);

        // Sesi menunggu validasi
        List<SesiMentoring> sesiMenunggu = supervisorService
            .getSesiMenungguValidasi(dosen.getId());
        model.addAttribute("sesiMenungguValidasi", sesiMenunggu);

        // Ranking mentor mahasiswa
        List<Mahasiswa> rankingMentor = mentoringService.getRankingMentorMahasiswa();
        model.addAttribute("rankingMentor", rankingMentor);

        // Kandidat Asdos
        List<Mahasiswa> kandidatAsdos = supervisorService.cariKandidatAsdos();
        model.addAttribute("kandidatAsdos", kandidatAsdos);

        // Asdos aktif
        List<Mahasiswa> asdosAktif = supervisorService.getAsdosAktif();
        model.addAttribute("asdosAktif", asdosAktif);

        // Statistik
        Map<String, Object> statistik = supervisorService.getStatistikDashboard(dosen.getId());
        model.addAttribute("statistik", statistik);
    }

    /**
     * Dashboard Manajemen Mentorship - halaman mentorship untuk mentor aktif.
     */
    @GetMapping("/mentorship")
    public String dashboardMentorship(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa) || !((Mahasiswa) pengguna).isMentor()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pengguna", pengguna);
        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        siapkanDashboardMahasiswa(pengguna, model);

        return "dashboard/mentorship";
    }

    /**
     * Halaman Materi - mentor melihat materi yang diunggah, mentee melihat materi dari mentor mereka.
     */
    @GetMapping("/materi")
    public String dashboardMateri(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (mahasiswa.isMentor()) {
            // === MENTOR: tampilkan materi yang diunggah ===
            model.addAttribute("pengguna", pengguna);
            model.addAttribute("mentor", pengguna);

            List<MateriBelajar> daftarMateri = mentoringService.getMateriByPengguna(penggunaId);
            model.addAttribute("daftarMateri", daftarMateri);

            List<String> kategoriList = daftarMateri.stream()
                .map(MateriBelajar::getMataPelajaran)
                .filter(mp -> mp != null && !mp.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            model.addAttribute("kategoriList", kategoriList);

            return "dashboard/materi";
        } else {
            // === MENTEE: tampilkan materi dari mentor ===
            model.addAttribute("mahasiswa", mahasiswa);

            List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(penggunaId);

            List<Pengguna> mentorUnik = semuaSesi.stream()
                .map(SesiMentoring::getMentor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
            model.addAttribute("mentorSaya", mentorUnik.isEmpty() ? null : mentorUnik.get(0));

            List<MateriBelajar> daftarMateri = mentorUnik.stream()
                .flatMap(m -> mentoringService.getMateriByPengguna(m.getId()).stream())
                .distinct()
                .collect(Collectors.toList());
            model.addAttribute("daftarMateri", daftarMateri);
            model.addAttribute("totalMateri", daftarMateri.size());

            model.addAttribute("totalPoin", mahasiswa.getTotalPoinProgres());
            model.addAttribute("rataRating", mahasiswa.hitungRataRataRating());

            List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentee(penggunaId);
            model.addAttribute("sesiMendatang", sesiMendatang);

            return "dashboard/materi-mentee";
        }
    }

    /**
     * Halaman Sumber Daya - mentor dan mentee dapat mengakses repositori sumber daya.
     */
    @GetMapping("/sumber-daya")
    public String dashboardSumberDaya(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (mahasiswa.isMentor()) {
            model.addAttribute("pengguna", pengguna);
            model.addAttribute("mentor", pengguna);
            return "dashboard/sumber-daya";
        } else {
            model.addAttribute("mahasiswa", mahasiswa);
            return "dashboard/sumber-daya-mentee";
        }
    }

    /**
     * Halaman Pengaturan - mentor dan mentee dapat mengakses pengaturan akun masing-masing.
     */
    @GetMapping("/pengaturan")
    public String dashboardPengaturan(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (mahasiswa.isMentor()) {
            model.addAttribute("pengguna", pengguna);
            model.addAttribute("mentor", pengguna);
            return "dashboard/pengaturan";
        } else {
            model.addAttribute("mahasiswa", mahasiswa);
            return "dashboard/pengaturan-mentee";
        }
    }

    /**
     * Halaman Bantuan - pusat bantuan dan FAQ untuk mentor.
     */
    @GetMapping("/bantuan")
    public String dashboardBantuan(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (mahasiswa.isMentor()) {
            model.addAttribute("pengguna", pengguna);
            model.addAttribute("mentor", pengguna);
            return "dashboard/bantuan";
        } else {
            model.addAttribute("mahasiswa", mahasiswa);
            return "dashboard/bantuan-mentee";
        }
    }

    /**
     * Halaman Pesan - pusat pesan antara mentor dan mentee.
     */
    @GetMapping("/pesan")
    public String dashboardPesan(HttpSession session, Model model) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) {
            return "redirect:/login";
        }

        Optional<Pengguna> optPengguna = penggunaService.getPenggunaById(penggunaId);
        if (optPengguna.isEmpty()) {
            return "redirect:/login";
        }

        Pengguna pengguna = optPengguna.get();
        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pengguna", pengguna);
        long notifBelumDibaca = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifBelumDibaca);

        if (mahasiswa.isMentor()) {
            // ===== MENTOR: percakapan = mentee unik dari sesi =====
            model.addAttribute("mentor", pengguna);
            List<SesiMentoring> sesiMentor = mentoringService.getSemuaSesiPengguna(penggunaId).stream()
                .filter(s -> s.getMentor() != null && s.getMentor().getId().equals(penggunaId))
                .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
                .toList();

            Set<Long> menteeIds = new LinkedHashSet<>();
            List<Pengguna> menteeUnik = new ArrayList<>();
            Map<Long, SesiMentoring> sesiTerakhirMentee = new LinkedHashMap<>();
            for (SesiMentoring s : sesiMentor) {
                if (s.getMentee() != null && !menteeIds.contains(s.getMentee().getId())) {
                    menteeIds.add(s.getMentee().getId());
                    menteeUnik.add(s.getMentee());
                    sesiTerakhirMentee.put(s.getMentee().getId(), s);
                }
            }
            List<Map<String, Object>> percakapan = new ArrayList<>();
            for (Pengguna mentee : menteeUnik) {
                Map<String, Object> conv = new LinkedHashMap<>();
                conv.put("mentee", mentee);
                conv.put("sesiTerakhir", sesiTerakhirMentee.get(mentee.getId()));
                percakapan.add(conv);
            }
            model.addAttribute("percakapan", percakapan);
            return "dashboard/pesan";

        } else {
            // ===== MENTEE: percakapan = mentor unik dari sesi =====
            model.addAttribute("mahasiswa", mahasiswa);
            List<SesiMentoring> sesiMentee = mentoringService.getSemuaSesiPengguna(penggunaId).stream()
                .filter(s -> s.getMentee() != null && s.getMentee().getId().equals(penggunaId))
                .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
                .toList();

            Set<Long> mentorIds = new LinkedHashSet<>();
            List<Pengguna> mentorUnik = new ArrayList<>();
            Map<Long, SesiMentoring> sesiTerakhirMentor = new LinkedHashMap<>();
            for (SesiMentoring s : sesiMentee) {
                if (s.getMentor() != null && !mentorIds.contains(s.getMentor().getId())) {
                    mentorIds.add(s.getMentor().getId());
                    mentorUnik.add(s.getMentor());
                    sesiTerakhirMentor.put(s.getMentor().getId(), s);
                }
            }
            List<Map<String, Object>> percakapan = new ArrayList<>();
            for (Pengguna mentor : mentorUnik) {
                Map<String, Object> conv = new LinkedHashMap<>();
                conv.put("mentor", mentor);
                conv.put("sesiTerakhir", sesiTerakhirMentor.get(mentor.getId()));
                percakapan.add(conv);
            }
            model.addAttribute("percakapan", percakapan);
            return "dashboard/pesan-mentee";
        }
    }

    /**
     * Legacy redirect untuk /dashboard/mentor → mentorship page.
     */
    @GetMapping("/mentor")
    public String dashboardMentor(HttpSession session) {
        return "redirect:/dashboard/mentorship";
    }
}
