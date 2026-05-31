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
 * DataSeeder - Komponen untuk mengisi data awal (seed data) ke database H2.
 *
 * Dijalankan otomatis saat aplikasi pertama kali distart.
 * Menyediakan data contoh untuk semua role pengguna:
 * Guru, Dosen, Siswa (Mentor & Mentee), Mahasiswa (Mentor & Mentee).
 * Juga membuat beberapa sesi mentoring contoh.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SiswaRepository siswaRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final GuruRepository guruRepository;
    private final DosenRepository dosenRepository;
    private final SesiMentoringRepository sesiRepository;

    @Autowired
    public DataSeeder(SiswaRepository siswaRepository,
                      MahasiswaRepository mahasiswaRepository,
                      GuruRepository guruRepository,
                      DosenRepository dosenRepository,
                      SesiMentoringRepository sesiRepository) {
        this.siswaRepository = siswaRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.guruRepository = guruRepository;
        this.dosenRepository = dosenRepository;
        this.sesiRepository = sesiRepository;
    }

    @Override
    public void run(String... args) {
        log.info("=== Memulai pengisian data awal (DataSeeder) ===");

        // Cek apakah data sudah ada
        if (siswaRepository.count() > 0) {
            log.info("Data sudah ada. Melewati proses seeding.");
            return;
        }

        // === GURU ===
        Guru guru1 = new Guru("Budi Santoso, S.Pd.", "budi.guru@sekolah.id", "guru123",
            "198501012010011001", "SMA Negeri 1 Jakarta", "Matematika, Fisika");
        guru1.setBidangKeahlian("Olimpiade Matematika dan Sains");
        guru1.setTahunPengalaman(15);
        guru1.setBio("Guru berpengalaman dengan spesialisasi pembinaan olimpiade.");
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
        dosen1.setBio("Guru Besar bidang Informatika dengan fokus riset AI.");
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
        siswa1.setTotalPoinProgres(350);
        siswa1.setTotalRating(22.5);
        siswa1.setJumlahPenilaian(5);
        siswa1.setSesiDiselesaikan(12);
        siswa1.setBio("Juara 1 Olimpiade Matematika Nasional. Siap membantu teman-teman belajar!");
        siswaRepository.save(siswa1);

        Siswa siswa2 = new Siswa("Dewi Lestari", "dewi@siswa.id", "siswa123",
            "XI IPA 2", "SMA Negeri 1 Jakarta", "0012345002");
        siswa2.aktifkanSebagaiMentor("Bahasa Inggris, Bahasa Indonesia");
        siswa2.setTotalPoinProgres(280);
        siswa2.setTotalRating(18.0);
        siswa2.setJumlahPenilaian(4);
        siswa2.setSesiDiselesaikan(8);
        siswaRepository.save(siswa2);

        // === SISWA MENTEE ===
        Siswa siswa3 = new Siswa("Riko Fadillah", "riko@siswa.id", "siswa123",
            "X IPA 1", "SMA Negeri 1 Jakarta", "0012345003");
        siswa3.setTotalPoinProgres(50);
        siswa3.setBio("Butuh bantuan di Matematika dan Fisika.");
        siswaRepository.save(siswa3);

        // === MAHASISWA MENTOR ===
        Mahasiswa mhs1 = new Mahasiswa("Rizky Ramadhan", "rizky@mahasiswa.id", "mhs123",
            "A11.2021.001", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 6);
        mhs1.setIpk(3.85);
        mhs1.aktifkanSebagaiMentor("Pemrograman Java, Struktur Data, OOP",
            "Spring Boot, Design Patterns, Clean Code");
        mhs1.setTotalPoinProgres(480);
        mhs1.setTotalRating(23.5);
        mhs1.setJumlahPenilaian(5);
        mhs1.setSesiDiselesaikan(15);
        mhs1.setBio("Asisten Lab Pemrograman. Spesialisasi Java & Spring Framework.");
        mahasiswaRepository.save(mhs1);

        Mahasiswa mhs2 = new Mahasiswa("Putri Handayani", "putri@mahasiswa.id", "mhs123",
            "A11.2021.002", "Informatika", "Fakultas Teknik", "Universitas Indonesia", 6);
        mhs2.setIpk(3.72);
        mhs2.aktifkanSebagaiMentor("Basis Data, SQL, Data Analytics",
            "PostgreSQL, MySQL, Data Visualization");
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
        mhs4.setBio("Mahasiswa semester 2, butuh bimbingan OOP dan Struktur Data.");
        mahasiswaRepository.save(mhs4);

        // === SESI MENTORING CONTOH ===
        // Sesi Online
        SesiOnline sesiOnline = new SesiOnline("Pengenalan OOP & Class Diagram", 60,
            mhs1, mhs4, "https://meet.google.com/abc-defg-hij", "Google Meet");
        sesiOnline.setKodeAkses("MENTOR2024");
        sesiOnline.setDeskripsi("Sesi pembahasan dasar OOP: Encapsulation, Inheritance, Polymorphism.");
        sesiOnline.jadwalkanSesi(LocalDateTime.now().plusDays(2), 60);
        sesiOnline.setSupervisor(dosen1);
        sesiRepository.save(sesiOnline);

        // Sesi Offline
        SesiOffline sesiOffline = new SesiOffline("Latihan Soal Matematika SBMPTN", 90,
            siswa1, siswa3, "Perpustakaan SMA N 1 Jakarta", "R.204");
        sesiOffline.setAlamatLengkap("Jl. Budi Utomo No. 7, Jakarta Pusat");
        sesiOffline.setDeskripsi("Latihan soal intensif matematika untuk persiapan SBMPTN.");
        sesiOffline.jadwalkanSesi(LocalDateTime.now().plusDays(3), 90);
        sesiOffline.setSupervisor(guru1);
        sesiRepository.save(sesiOffline);

        // Sesi Video
        SesiVideo sesiVideo = new SesiVideo("Tutorial Spring Boot REST API", 45,
            mhs1, mhs4, "https://youtu.be/spring-boot-tutorial", "YouTube");
        sesiVideo.setKualitasVideo("1080p");
        sesiVideo.setMemilikiSubtitle(true);
        sesiVideo.setDeskripsi("Video tutorial membuat REST API dengan Spring Boot 3.");
        sesiVideo.jadwalkanSesi(LocalDateTime.now().plusDays(1), 45);
        sesiVideo.setSupervisor(dosen1);
        sesiRepository.save(sesiVideo);

        // Sesi yang sudah selesai (untuk testing review)
        SesiOnline sesiSelesai = new SesiOnline("Pembahasan Struktur Data: Linked List", 60,
            mhs1, mhs4, "https://zoom.us/j/123456789", "Zoom");
        sesiSelesai.setWaktuMulai(LocalDateTime.now().minusDays(3));
        sesiSelesai.setStatusSesi(StatusSesi.DIJADWALKAN);
        sesiSelesai.selesaikanSesi(10, 7);
        sesiSelesai.validasi(dosen1.getId(), "Sesi berjalan baik.");
        sesiSelesai.setSupervisor(dosen1);
        sesiRepository.save(sesiSelesai);

        log.info("=== Data awal berhasil dimuat! ===");
        log.info("Akun Login Tersedia:");
        log.info("  Guru   : budi.guru@sekolah.id / guru123");
        log.info("  Dosen  : ahmad.dosen@kampus.id / dosen123");
        log.info("  Siswa  : andi@siswa.id / siswa123");
        log.info("  MHS    : rizky@mahasiswa.id / mhs123");
        log.info("  Mentee : dina@mahasiswa.id / mhs123");
    }
}
