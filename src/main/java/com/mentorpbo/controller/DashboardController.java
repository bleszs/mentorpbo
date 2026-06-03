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

    /** Estimasi rata-rata durasi satu sesi mentoring dalam jam (dipakai untuk statistik). */
    private static final double JAM_PER_SESI = 1.5;

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
     * Menyiapkan data UMUM yang dibutuhkan SEMUA halaman dashboard:
     * - notifBelumDibaca (count badge)
     * - daftarNotifikasi (list 5 terbaru — menggantikan hardcode di popup)
     * - inboxPercakapan (list partner chat dari sesi — menggantikan hardcode di inbox popup)
     * - preferences (UserPreferences dari H2)
     */
    private void siapkanDataUmum(Long penggunaId, Pengguna pengguna, Model model) {
        // Notifikasi
        long notifCount = penggunaService.hitungNotifikasiBelumDibaca(penggunaId);
        model.addAttribute("notifBelumDibaca", notifCount);
        model.addAttribute("daftarNotifikasi", penggunaService.getDaftarNotifikasi(penggunaId));

        // Inbox: ambil percakapan unik dari sesi (mentor ↔ mentee)
        List<Map<String, Object>> inboxList = new ArrayList<>();
        List<SesiMentoring> semuaSesiUser = mentoringService.getSemuaSesiPengguna(penggunaId);
        Set<Long> sudahTambah = new LinkedHashSet<>();
        for (SesiMentoring s : semuaSesiUser.stream()
                .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
                .toList()) {
            Pengguna partner = null;
            if (s.getMentor() != null && !s.getMentor().getId().equals(penggunaId)) {
                partner = s.getMentor();
            } else if (s.getMentee() != null && !s.getMentee().getId().equals(penggunaId)) {
                partner = s.getMentee();
            }
            if (partner != null && sudahTambah.add(partner.getId())) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("partner", partner);
                entry.put("topik", s.getTopikPembahasan());
                entry.put("waktu", s.getWaktuMulai());
                inboxList.add(entry);
                if (inboxList.size() >= 5) break;
            }
        }
        model.addAttribute("inboxPercakapan", inboxList);
        model.addAttribute("jumlahPesan", inboxList.size());

        // Preferences dari H2
        model.addAttribute("preferences", penggunaService.getOrCreatePreferences(penggunaId));
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

        // Mentor yang masih menunggu/ditolak persetujuan supervisor dikunci —
        // dialihkan ke halaman status, belum bisa mengakses fitur mentor.
        if (mentorBelumDisetujui(pengguna)) {
            model.addAttribute("statusMentor", statusValidasiMentor(pengguna));
            return "auth/menunggu-persetujuan";
        }

        siapkanDataUmum(penggunaId, pengguna, model);

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

        List<SesiMentoring> semuaSesi = mentoringService.getSemuaSesiPengguna(siswa.getId());
        model.addAttribute("semuaSesi", semuaSesi);

        long sesiSelesai = semuaSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI).count();
        model.addAttribute("sesiSelesai", sesiSelesai);

        if (siswa.isMentor()) {
            // Data mentor siswa
            List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentor(siswa.getId());
            model.addAttribute("sesiMendatang", sesiMendatang);

            List<SesiMentoring> sesiMenunggu = semuaSesi.stream()
                .filter(s -> s.getStatusSesi() == StatusSesi.MENUNGGU_KONFIRMASI)
                .toList();
            model.addAttribute("sesiMenungguKonfirmasi", sesiMenunggu);
        } else {
            // Data mentee siswa
            List<SesiMentoring> sesiMendatang = mentoringService.getSesiMendatangMentee(siswa.getId());
            model.addAttribute("sesiMendatang", sesiMendatang);

            List<Pengguna> mentorSaya = semuaSesi.stream()
                .map(SesiMentoring::getMentor)
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
            model.addAttribute("mentorSaya", mentorSaya);
        }

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
                Math.round(mahasiswa.getSesiDiselesaikan() * JAM_PER_SESI * 10.0) / 10.0);
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

        // Siswa berprestasi — scoped ke sekolah Guru ini
        List<Siswa> siswaBerprestasi = supervisorService.getSiswaBerprestasi(guru.getId());
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

        // Kandidat Asdos — scoped ke program studi Dosen ini
        List<Mahasiswa> kandidatAsdos = supervisorService.cariKandidatAsdos(dosen.getId());
        model.addAttribute("kandidatAsdos", kandidatAsdos);

        // Asdos aktif — scoped ke program studi Dosen ini
        List<Mahasiswa> asdosAktif = supervisorService.getAsdosAktif(dosen.getId());
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
        if (!(pengguna instanceof Mahasiswa) || !((Mahasiswa) pengguna).isMentor()
                || mentorBelumDisetujui(pengguna)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);
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
        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);

        // Materi sendiri yang diunggah (hanya tipe MATERI, bukan SUMBER_DAYA)
        List<MateriBelajar> daftarMateri = mentoringService.getMateriSajaByPengguna(penggunaId);
        model.addAttribute("daftarMateri", daftarMateri);
        model.addAttribute("totalMateri", daftarMateri.size());
        model.addAttribute("kategoriList", daftarMateri.stream()
            .map(MateriBelajar::getMataPelajaran)
            .filter(mp -> mp != null && !mp.isEmpty())
            .distinct().sorted().toList());

        if (pengguna instanceof Mahasiswa mahasiswa) {
            model.addAttribute("mahasiswa", mahasiswa);
            if (mahasiswa.isMentor()) {
                model.addAttribute("mentor", pengguna);
                return "dashboard/materi";
            } else {
                // Mentee: gabungkan materi dari semua mentor yang pernah sesi
                List<Pengguna> mentorList = mentoringService.getSemuaSesiPengguna(penggunaId)
                    .stream().map(SesiMentoring::getMentor).filter(Objects::nonNull)
                    .distinct().toList();
                model.addAttribute("mentorSaya", mentorList.isEmpty() ? null : mentorList.get(0));
                List<MateriBelajar> materiMentor = mentorList.stream()
                    .flatMap(m -> mentoringService.getMateriSajaByPengguna(m.getId()).stream())
                    .distinct().toList();
                model.addAttribute("daftarMateri", materiMentor);
                model.addAttribute("totalMateri", materiMentor.size());
                model.addAttribute("totalPoin", mahasiswa.getTotalPoinProgres());
                model.addAttribute("sesiMendatang", mentoringService.getSesiMendatangMentee(penggunaId));
                return "dashboard/materi-mentee";
            }
        } else if (pengguna instanceof Siswa siswa) {
            model.addAttribute("siswa", siswa);
            model.addAttribute("mentor", pengguna);
            return siswa.isMentor() ? "dashboard/materi" : "dashboard/materi-mentee";
        }
        return "dashboard/materi";
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
        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);

        // Sumber daya: hanya konten bertipe SUMBER_DAYA
        List<MateriBelajar> semuaMateri = mentoringService.getSumberDayaByPengguna(penggunaId);
        model.addAttribute("daftarSumberDaya", semuaMateri);
        model.addAttribute("daftarMateri", semuaMateri);

        if (pengguna instanceof Mahasiswa mahasiswa) {
            model.addAttribute("mahasiswa", mahasiswa);
            if (mahasiswa.isMentor()) {
                model.addAttribute("mentor", pengguna);
                return "dashboard/sumber-daya";
            }
            return "dashboard/sumber-daya-mentee";
        } else if (pengguna instanceof Siswa siswa) {
            model.addAttribute("siswa", siswa);
            model.addAttribute("mentor", pengguna);
            return siswa.isMentor() ? "dashboard/sumber-daya" : "dashboard/sumber-daya-mentee";
        }
        model.addAttribute("mentor", pengguna);
        return "dashboard/sumber-daya";
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
        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);

        if (pengguna instanceof Mahasiswa mahasiswa) {
            model.addAttribute("mahasiswa", mahasiswa);
            if (mahasiswa.isMentor()) {
                model.addAttribute("mentor", pengguna);
                return "dashboard/pengaturan";
            }
            return "dashboard/pengaturan-mentee";
        } else if (pengguna instanceof Guru guru) {
            model.addAttribute("guru", guru);
            model.addAttribute("supervisor", pengguna);
            return "supervisor/pengaturan";
        } else if (pengguna instanceof Dosen dosen) {
            model.addAttribute("dosen", dosen);
            model.addAttribute("supervisor", pengguna);
            return "supervisor/pengaturan";
        } else if (pengguna instanceof Siswa siswa) {
            model.addAttribute("siswa", siswa);
            return siswa.isMentor() ? "dashboard/pengaturan" : "dashboard/pengaturan-mentee";
        }
        return "dashboard/pengaturan";
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
        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);

        if (pengguna instanceof Mahasiswa mahasiswa) {
            model.addAttribute("mahasiswa", mahasiswa);
            if (mahasiswa.isMentor()) { model.addAttribute("mentor", pengguna); return "dashboard/bantuan"; }
            return "dashboard/bantuan-mentee";
        } else if (pengguna instanceof Guru || pengguna instanceof Dosen) {
            return "dashboard/bantuan-dosen";
        }
        model.addAttribute("mentor", pengguna);
        return "dashboard/bantuan";
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

        // Siswa redirect ke pesan-chat jika ada percakapan
        if (pengguna instanceof Siswa) {
            List<SesiMentoring> sesiSiswa = mentoringService.getSemuaSesiPengguna(penggunaId);
            Optional<Long> firstPartner = sesiSiswa.stream()
                .map(s -> s.getMentor() != null && !s.getMentor().getId().equals(penggunaId)
                    ? s.getMentor() : (s.getMentee() != null && !s.getMentee().getId().equals(penggunaId) ? s.getMentee() : null))
                .filter(Objects::nonNull)
                .map(Pengguna::getId)
                .findFirst();
            if (firstPartner.isPresent()) return "redirect:/pesan/chat/" + firstPartner.get();
            return "redirect:/dashboard";
        }

        if (!(pengguna instanceof Mahasiswa mahasiswa)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("pengguna", pengguna);
        siapkanDataUmum(penggunaId, pengguna, model);

        if (mahasiswa.isMentor()) {
            // ===== MENTOR =====
            model.addAttribute("mentor", pengguna);
            List<SesiMentoring> sesiMentor = mentoringService.getSemuaSesiPengguna(penggunaId).stream()
                .filter(s -> s.getMentor() != null && s.getMentor().getId().equals(penggunaId))
                .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
                .toList();

            Set<Long> menteeIds = new LinkedHashSet<>();
            List<Pengguna> menteeUnik = new ArrayList<>();
            for (SesiMentoring s : sesiMentor) {
                if (s.getMentee() != null && menteeIds.add(s.getMentee().getId()))
                    menteeUnik.add(s.getMentee());
            }

            // Redirect langsung ke chat pertama jika ada
            if (!menteeUnik.isEmpty()) {
                return "redirect:/pesan/chat/" + menteeUnik.get(0).getId();
            }
            model.addAttribute("percakapan", new ArrayList<>());
            return "dashboard/pesan";

        } else {
            // ===== MENTEE =====
            model.addAttribute("mahasiswa", mahasiswa);
            List<SesiMentoring> sesiMentee = mentoringService.getSemuaSesiPengguna(penggunaId).stream()
                .filter(s -> s.getMentee() != null && s.getMentee().getId().equals(penggunaId))
                .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
                .toList();

            Set<Long> mentorIds = new LinkedHashSet<>();
            List<Pengguna> mentorUnik = new ArrayList<>();
            for (SesiMentoring s : sesiMentee) {
                if (s.getMentor() != null && mentorIds.add(s.getMentor().getId()))
                    mentorUnik.add(s.getMentor());
            }

            // Redirect ke chat pertama jika ada
            if (!mentorUnik.isEmpty()) {
                return "redirect:/pesan/chat/" + mentorUnik.get(0).getId();
            }
            model.addAttribute("percakapan", new ArrayList<>());
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

    // === Helper: status persetujuan mentor ===

    /** Mentor yang belum disetujui supervisor (status BELUM_DITINJAU atau DITOLAK). */
    private boolean mentorBelumDisetujui(Pengguna p) {
        return (p instanceof Mahasiswa m && m.isMentor() && !m.isMentorTervalidasi())
            || (p instanceof Siswa s && s.isMentor() && !s.isMentorTervalidasi());
    }

    /** Label status validasi mentor untuk ditampilkan di halaman tunggu. */
    private String statusValidasiMentor(Pengguna p) {
        var status = (p instanceof Mahasiswa m) ? m.getStatusValidasiMentor()
                   : (p instanceof Siswa s) ? s.getStatusValidasiMentor() : null;
        return status != null ? status.getLabel() : "Menunggu Ditinjau";
    }
}
