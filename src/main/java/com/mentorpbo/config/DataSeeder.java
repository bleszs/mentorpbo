package com.mentorpbo.config;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.*;
import com.mentorpbo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * DataSeeder — Mengisi database H2 file dengan data awal saat database kosong.
 *
 * Konfigurasi aktual: ddl-auto=update + H2 file (jdbc:h2:file:./data/...).
 * Database bersifat persisten antar restart lokal; seeder berjalan idempoten
 * (lewati jika data sudah ada).
 *
 * Data yang dibuat:
 * - 2 Guru (dari 2 sekolah berbeda untuk menguji scope filtering)
 * - 2 Dosen (dari 2 program studi berbeda)
 * - 3 Siswa mentor (statusValidasiMentor=DIVALIDASI) + 1 siswa mentee + 1 siswa mentor PENDING
 * - 3 Mahasiswa mentor (DIVALIDASI) + 2 mahasiswa mentee + 1 mahasiswa mentor PENDING
 * - Sesi dalam berbagai state: MENUNGGU_KONFIRMASI, DIJADWALKAN, BERLANGSUNG, SELESAI
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SiswaRepository         siswaRepository;
    private final MahasiswaRepository     mahasiswaRepository;
    private final GuruRepository          guruRepository;
    private final DosenRepository         dosenRepository;
    private final SesiMentoringRepository sesiRepository;

    @Autowired
    public DataSeeder(SiswaRepository siswaRepository,
                      MahasiswaRepository mahasiswaRepository,
                      GuruRepository guruRepository,
                      DosenRepository dosenRepository,
                      SesiMentoringRepository sesiRepository) {
        this.siswaRepository    = siswaRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.guruRepository     = guruRepository;
        this.dosenRepository    = dosenRepository;
        this.sesiRepository     = sesiRepository;
    }

    @Override
    public void run(String... args) {
        log.info("======================================================");
        log.info("  DataSeeder: Memeriksa data awal...");
        log.info("======================================================");

        // Idempoten: jika data sudah ada → lewati seeding.
        //
        // PERILAKU DI RAILWAY (cloud):
        //   Filesystem Railway bersifat EPHEMERAL — file H2 database terhapus
        //   setiap kali container di-restart atau di-redeploy.
        //   Artinya siswaRepository.count() selalu 0 saat startup di Railway
        //   → DataSeeder SELALU mengisi data tiruan secara otomatis. ✅
        //
        // PERILAKU DI LOKAL (development):
        //   File H2 tersimpan di ./data/ dan persisten antar restart.
        //   Jika data sudah ada, seeding dilewati (tidak ada duplikat). ✅
        if (siswaRepository.count() > 0) {
            log.info("  Data sudah ada. Melewati proses seeding (mode lokal/persisten).");
            log.info("======================================================");
            return;
        }

        log.info("  Database kosong — mengisi data awal (fresh deploy atau restart Railway)...");


        // === GURU ===
        Guru guru1 = new Guru("Budi Santoso, S.Pd.", "budi.guru@sekolah.id", "guru123",
            "198501012010011001", "SMA Negeri 1 Jakarta", "Matematika, Fisika");
        guru1.setBidangKeahlian("Olimpiade Matematika dan Sains");
        guru1.setTahunPengalaman(15);
        guru1.setBio("Guru berpengalaman, pembina olimpiade nasional.");
        guruRepository.save(guru1);

        Guru guru2 = new Guru("Siti Nurhaliza, M.Pd.", "siti.guru@sekolah.id", "guru123",
            "198803152012012002", "SMA Negeri 2 Bandung", "Bahasa Inggris, Bahasa Indonesia");
        guru2.setBidangKeahlian("Linguistik dan Sastra");
        guru2.setTahunPengalaman(10);
        guruRepository.save(guru2);

        // === DOSEN ===
        Dosen dosen1 = new Dosen("Prof. Dr. Ahmad Dahlan, M.Kom.", "ahmad.dosen@kampus.id", "dosen123",
            "0001016701", "Informatika", "Fakultas Teknik", "Universitas Indonesia");
        dosen1.setMataKuliahDiampu("Pemrograman Berorientasi Objek, Struktur Data, Algoritma");
        dosen1.setJabatanFungsional("Guru Besar");
        dosen1.setBidangRiset("Kecerdasan Buatan, Machine Learning");
        dosen1.setBio("Guru Besar bidang Informatika, fokus riset AI.");
        dosenRepository.save(dosen1);

        Dosen dosen2 = new Dosen("Dr. Maya Sari, M.T.", "maya.dosen@kampus.id", "dosen123",
            "0002027802", "Sistem Informasi", "Fakultas Teknik", "Universitas Indonesia");
        dosen2.setMataKuliahDiampu("Basis Data, Analisis Sistem, Rekayasa Perangkat Lunak");
        dosen2.setJabatanFungsional("Lektor Kepala");
        dosen2.setBidangRiset("Data Science, Big Data Analytics");
        dosenRepository.save(dosen2);

        // === SISWA MENTOR ===
        Siswa siswa1 = new Siswa("Andi Prasetyo", "andi@siswa.id", "siswa123",
            "XII IPA 1", "SMA Negeri 1 Jakarta", "0012345001");
        siswa1.aktifkanSebagaiMentor("Matematika, Fisika, Kimia");
        siswa1.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        siswa1.setTotalPoinProgres(350);
        siswa1.setTotalRating(22.5);
        siswa1.setJumlahPenilaian(5);
        siswa1.setSesiDiselesaikan(12);
        siswa1.setBio("Juara 1 Olimpiade Matematika Nasional. Siap membantu!");
        siswaRepository.save(siswa1);

        Siswa siswa2 = new Siswa("Dewi Lestari", "dewi@siswa.id", "siswa123",
            "XI IPA 2", "SMA Negeri 1 Jakarta", "0012345002");
        siswa2.aktifkanSebagaiMentor("Bahasa Inggris, Bahasa Indonesia");
        siswa2.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        siswa2.setTotalPoinProgres(280);
        siswa2.setTotalRating(18.0);
        siswa2.setJumlahPenilaian(4);
        siswa2.setSesiDiselesaikan(8);
        siswaRepository.save(siswa2);

        Siswa siswa3 = new Siswa("Faiz Ramadhan", "faiz@siswa.id", "siswa123",
            "XII IPS 1", "SMA Negeri 2 Bandung", "0012345005");
        siswa3.aktifkanSebagaiMentor("Ekonomi, Akuntansi, Sosiologi");
        siswa3.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        siswa3.setTotalPoinProgres(190);
        siswa3.setTotalRating(12.0);
        siswa3.setJumlahPenilaian(3);
        siswa3.setSesiDiselesaikan(6);
        siswaRepository.save(siswa3);

        // === SISWA MENTEE ===
        Siswa siswa4 = new Siswa("Riko Fadillah", "riko@siswa.id", "siswa123",
            "X IPA 1", "SMA Negeri 1 Jakarta", "0012345003");
        siswa4.setTotalPoinProgres(50);
        siswa4.setBio("Butuh bantuan di Matematika dan Fisika.");
        siswaRepository.save(siswa4);

        // === MAHASISWA MENTOR ===
        Mahasiswa mhs1 = new Mahasiswa("Rizky Ramadhan", "rizky@mahasiswa.id", "mhs123",
            "A11.2021.001", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 6);
        mhs1.setIpk(3.85);
        mhs1.aktifkanSebagaiMentor("Pemrograman Java, Struktur Data, OOP",
            "Spring Boot, Design Patterns, Clean Code");
        mhs1.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        mhs1.setTotalPoinProgres(480);
        mhs1.setTotalRating(23.5);
        mhs1.setJumlahPenilaian(5);
        mhs1.setSesiDiselesaikan(15);
        mhs1.setBio("Asisten Lab Pemrograman. Spesialisasi Java & Spring.");
        mahasiswaRepository.save(mhs1);

        Mahasiswa mhs2 = new Mahasiswa("Putri Handayani", "putri@mahasiswa.id", "mhs123",
            "A11.2021.002", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 6);
        mhs2.setIpk(3.72);
        mhs2.aktifkanSebagaiMentor("Basis Data, SQL, Data Analytics",
            "PostgreSQL, MySQL, Data Visualization");
        mhs2.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        mhs2.setTotalPoinProgres(320);
        mhs2.setTotalRating(21.0);
        mhs2.setJumlahPenilaian(5);
        mhs2.setSesiDiselesaikan(10);
        mahasiswaRepository.save(mhs2);

        Mahasiswa mhs3 = new Mahasiswa("Fajar Nugroho", "fajar@mahasiswa.id", "mhs123",
            "A11.2022.003", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 4);
        mhs3.setIpk(3.90);
        mhs3.aktifkanSebagaiMentor("Algoritma, Matematika Diskrit",
            "Competitive Programming, Problem Solving");
        mhs3.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
        mhs3.setTotalPoinProgres(400);
        mhs3.setTotalRating(24.0);
        mhs3.setJumlahPenilaian(6);
        mhs3.setSesiDiselesaikan(18);
        mahasiswaRepository.save(mhs3);

        // === MAHASISWA MENTEE ===
        Mahasiswa mhs4 = new Mahasiswa("Dina Safitri", "dina@mahasiswa.id", "mhs123",
            "A11.2023.004", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 2);
        mhs4.setIpk(3.20);
        mhs4.setTotalPoinProgres(45);
        mhs4.setBio("Semester 2, butuh bimbingan OOP dan Struktur Data.");
        mahasiswaRepository.save(mhs4);

        Mahasiswa mhs5 = new Mahasiswa("Bima Ardiansyah", "bima@mahasiswa.id", "mhs123",
            "A11.2023.005", "Sistem Informasi", "Fakultas Teknik", "Universitas Indonesia", 2);
        mhs5.setIpk(3.05);
        mhs5.setTotalPoinProgres(30);
        mhs5.setBio("Butuh bimbingan Basis Data dan Analisis Sistem.");
        mahasiswaRepository.save(mhs5);

        // === MENTOR PENDING (menunggu persetujuan supervisor) ===
        // Siswa pendaftar mentor baru di SMA Negeri 1 Jakarta → divalidasi oleh Guru Budi.
        Siswa siswaPending = new Siswa("Gilang Pratama", "gilang@siswa.id", "siswa123",
            "XII IPA 3", "SMA Negeri 1 Jakarta", "0012345009");
        siswaPending.setMentor(true);
        siswaPending.setStatusValidasiMentor(StatusValidasi.BELUM_DITINJAU);
        siswaPending.setMataPelajaranKeahlian("Biologi, Kimia");
        siswaPending.setBio("Mendaftar sebagai mentor, menunggu persetujuan.");
        siswaRepository.save(siswaPending);

        // Mahasiswa pendaftar mentor baru di Informatika → divalidasi oleh Dosen Ahmad.
        Mahasiswa mhsPending = new Mahasiswa("Hana Wijaya", "hana@mahasiswa.id", "mhs123",
            "A11.2022.009", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 5);
        mhsPending.setIpk(3.60);
        mhsPending.setMentor(true);
        mhsPending.setStatusValidasiMentor(StatusValidasi.BELUM_DITINJAU);
        mhsPending.setMataKuliahKeahlian("Pemrograman Web, JavaScript");
        mhsPending.setTopikKeahlian("React, Node.js");
        mhsPending.setBio("Mendaftar sebagai mentor, menunggu persetujuan.");
        mahasiswaRepository.save(mhsPending);

        // ============================================================
        // SESI MENTORING — berbagai state untuk demo lengkap
        // ============================================================

        // [1] MENUNGGU_KONFIRMASI — mentee baru kirim permintaan
        SesiOnline sesiMenunggu = new SesiOnline(
            "Pengenalan OOP & Class Diagram", 60,
            mhs1, mhs4, "", "Zoom");
        sesiMenunggu.setDeskripsi("Dasar-dasar OOP: Encapsulation, Inheritance, Polymorphism.");
        sesiMenunggu.jadwalkanSesi(LocalDateTime.now().plusDays(2), 60);
        sesiMenunggu.setStatusSesi(StatusSesi.MENUNGGU_KONFIRMASI);
        sesiMenunggu.setSupervisor(dosen1);
        sesiRepository.save(sesiMenunggu);

        // [2] DIJADWALKAN — mentor sudah konfirmasi
        SesiOffline sesiDijadwalkan = new SesiOffline(
            "Latihan Soal Matematika SBMPTN", 90,
            siswa1, siswa4, "Perpustakaan SMA N 1 Jakarta", "R.204");
        sesiDijadwalkan.setAlamatLengkap("Jl. Budi Utomo No. 7, Jakarta Pusat");
        sesiDijadwalkan.setDeskripsi("Latihan soal intensif matematika persiapan SBMPTN.");
        sesiDijadwalkan.jadwalkanSesi(LocalDateTime.now().plusDays(3), 90);
        sesiDijadwalkan.setStatusSesi(StatusSesi.DIJADWALKAN);
        sesiDijadwalkan.setSupervisor(guru1);
        sesiRepository.save(sesiDijadwalkan);

        // [3] BERLANGSUNG — sesi sedang aktif saat ini
        SesiOnline sesiBerlangsung = new SesiOnline(
            "Review Code Spring Boot REST API", 90,
            mhs1, mhs4, "https://meet.google.com/abc-defg-hij", "Google Meet");
        sesiBerlangsung.setKodeAkses("MENTOR2024");
        sesiBerlangsung.setDeskripsi("Review code project akhir semester: REST API dengan Spring Boot.");
        sesiBerlangsung.jadwalkanSesi(LocalDateTime.now().minusHours(1), 90);
        sesiBerlangsung.setStatusSesi(StatusSesi.BERLANGSUNG);
        sesiBerlangsung.setSupervisor(dosen1);
        sesiRepository.save(sesiBerlangsung);

        // [4] BERLANGSUNG — sesi video (asinkron)
        SesiVideo sesiVideoAktif = new SesiVideo(
            "Tutorial Spring Boot REST API", 45,
            mhs1, mhs5, "https://youtu.be/spring-boot-tutorial", "YouTube");
        sesiVideoAktif.setKualitasVideo("1080p");
        sesiVideoAktif.setMemilikiSubtitle(true);
        sesiVideoAktif.setDeskripsi("Video tutorial REST API dengan Spring Boot 3 + JPA.");
        sesiVideoAktif.jadwalkanSesi(LocalDateTime.now().minusDays(1), 45);
        sesiVideoAktif.setStatusSesi(StatusSesi.BERLANGSUNG);
        sesiVideoAktif.setSupervisor(dosen1);
        sesiRepository.save(sesiVideoAktif);

        // [5] SELESAI + BELUM_DITINJAU — menunggu validasi supervisor (laporan baru)
        SesiOnline sesiSelesaiBelumValidasi = new SesiOnline(
            "Struktur Data: Linked List & Tree", 60,
            mhs1, mhs4, "https://zoom.us/j/111111111", "Zoom");
        sesiSelesaiBelumValidasi.setDeskripsi("Pembahasan implementasi Linked List dan Binary Tree.");
        // Simulasi lifecycle lengkap:
        sesiSelesaiBelumValidasi.jadwalkanSesi(LocalDateTime.now().minusDays(2), 60);
        sesiSelesaiBelumValidasi.setStatusSesi(StatusSesi.BERLANGSUNG);   // DIJADWALKAN → BERLANGSUNG
        sesiSelesaiBelumValidasi.selesaikanSesi(10, 7);                   // BERLANGSUNG → SELESAI
        sesiSelesaiBelumValidasi.setSupervisor(dosen1);
        // statusValidasi sudah BELUM_DITINJAU secara default — siap divalidasi dosen
        sesiRepository.save(sesiSelesaiBelumValidasi);

        // [6] SELESAI + DIVALIDASI — laporan sudah disetujui supervisor
        SesiOffline sesiSelesaiDivalidasi = new SesiOffline(
            "Pembahasan Soal Fisika Gelombang", 60,
            siswa1, siswa4, "Lab IPA SMA N 1 Jakarta", "Lab IPA");
        sesiSelesaiDivalidasi.setAlamatLengkap("Jl. Budi Utomo No. 7, Jakarta Pusat");
        sesiSelesaiDivalidasi.jadwalkanSesi(LocalDateTime.now().minusDays(5), 60);
        sesiSelesaiDivalidasi.setStatusSesi(StatusSesi.BERLANGSUNG);
        sesiSelesaiDivalidasi.selesaikanSesi(10, 7);
        sesiSelesaiDivalidasi.validasi(guru1.getId(), "Sesi berjalan sangat baik. Materi disampaikan dengan jelas.");
        sesiSelesaiDivalidasi.setSupervisor(guru1);
        siswa1.setSesiDiselesaikan(siswa1.getSesiDiselesaikan() + 1);
        siswa1.tambahPoinProgres(10);
        siswa4.tambahPoinProgres(7);
        siswaRepository.save(siswa1);
        siswaRepository.save(siswa4);
        sesiRepository.save(sesiSelesaiDivalidasi);

        // [7] SELESAI + DIVALIDASI — untuk mahasiswa (menguji scope dosen)
        SesiOnline sesiMhsValidasi = new SesiOnline(
            "Pembahasan Algoritma Sorting Lanjut", 75,
            mhs3, mhs4, "https://zoom.us/j/222222222", "Zoom");
        sesiMhsValidasi.jadwalkanSesi(LocalDateTime.now().minusDays(4), 75);
        sesiMhsValidasi.setStatusSesi(StatusSesi.BERLANGSUNG);
        sesiMhsValidasi.selesaikanSesi(10, 7);
        sesiMhsValidasi.validasi(dosen1.getId(), "Materi disampaikan terstruktur. Sangat baik.");
        sesiMhsValidasi.setSupervisor(dosen1);
        mhs3.setSesiDiselesaikan(mhs3.getSesiDiselesaikan() + 1);
        mhs3.tambahPoinProgres(10);
        mhs4.tambahPoinProgres(7);
        mahasiswaRepository.save(mhs3);
        mahasiswaRepository.save(mhs4);
        sesiRepository.save(sesiMhsValidasi);

        // [8] DIBATALKAN — demo sesi yang gagal
        SesiOnline sesiDibatalkan = new SesiOnline(
            "Konsultasi Tugas Akhir", 60,
            mhs2, mhs5, "", "Teams");
        sesiDibatalkan.jadwalkanSesi(LocalDateTime.now().plusDays(1), 60);
        sesiDibatalkan.batalkanJadwal("Mentor berhalangan hadir mendadak.");
        sesiDibatalkan.setSupervisor(dosen2);
        sesiRepository.save(sesiDibatalkan);

        log.info("======================================================");
        log.info("  Data awal berhasil dimuat! Akun login:");
        log.info("  [GURU]   budi.guru@sekolah.id  / guru123");
        log.info("  [GURU]   siti.guru@sekolah.id  / guru123");
        log.info("  [DOSEN]  ahmad.dosen@kampus.id / dosen123");
        log.info("  [DOSEN]  maya.dosen@kampus.id  / dosen123");
        log.info("  [SISWA]  andi@siswa.id          / siswa123  (mentor)");
        log.info("  [SISWA]  riko@siswa.id          / siswa123  (mentee)");
        log.info("  [MHS]    rizky@mahasiswa.id     / mhs123    (mentor)");
        log.info("  [MHS]    dina@mahasiswa.id      / mhs123    (mentee)");
        log.info("  H2 Console: http://localhost:8080/h2-console");
        log.info("======================================================");
    }
}
