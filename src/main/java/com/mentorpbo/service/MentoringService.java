package com.mentorpbo.service;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.*;
import com.mentorpbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * MentoringService — Lapisan logika bisnis inti untuk sistem mentoring.
 *
 * Mengelola seluruh lifecycle sesi bimbingan:
 *   MENUNGGU_KONFIRMASI → DIJADWALKAN → BERLANGSUNG → SELESAI → (validasi supervisor)
 *
 * Demonstrasi konsep OOP:
 * - POLIMORFISME: buatDanJadwalkanSesi() menerima SesiMentoring abstrak,
 *   perilaku spesifik dieksekusi oleh subclass (SesiOnline/Offline/Video).
 * - INTERFACE: Schedulable dipakai via sesi.jadwalkanSesi(),
 *              Ratable dipakai via beriRating() pada subclass Pengguna.
 */
@Service
@Transactional
public class MentoringService {

    private final SesiMentoringRepository sesiRepository;
    private final SiswaRepository siswaRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final ReviewRatingRepository reviewRepository;
    private final MateriBelajarRepository materiRepository;
    private final PenggunaRepository penggunaRepository;
    private final NotifikasiRepository notifikasiRepository;

    @Autowired
    public MentoringService(SesiMentoringRepository sesiRepository,
                            SiswaRepository siswaRepository,
                            MahasiswaRepository mahasiswaRepository,
                            ReviewRatingRepository reviewRepository,
                            MateriBelajarRepository materiRepository,
                            PenggunaRepository penggunaRepository,
                            NotifikasiRepository notifikasiRepository) {
        this.sesiRepository = sesiRepository;
        this.siswaRepository = siswaRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.reviewRepository = reviewRepository;
        this.materiRepository = materiRepository;
        this.penggunaRepository = penggunaRepository;
        this.notifikasiRepository = notifikasiRepository;
    }

    // ============================================================
    // PENCARIAN MENTOR
    // ============================================================

    /**
     * Mencari mentor siswa berdasarkan mata pelajaran keahlian.
     */
    public List<Siswa> cariMentorSiswa(String mataPelajaran) {
        if (mataPelajaran == null || mataPelajaran.isBlank()) {
            return siswaRepository.findByIsMentorTrue();
        }
        return siswaRepository.cariMentorBerdasarkanMataPelajaran(mataPelajaran);
    }

    /**
     * Mencari mentor mahasiswa berdasarkan mata kuliah atau topik keahlian.
     * Menggabungkan dua hasil pencarian dan menghilangkan duplikat — demonstrasi
     * penggabungan koleksi yang efisien menggunakan Set.
     */
    public List<Mahasiswa> cariMentorMahasiswa(String kataKunci) {
        if (kataKunci == null || kataKunci.isBlank()) {
            return mahasiswaRepository.findByIsMentorTrue();
        }
        List<Mahasiswa> dariMataKuliah = mahasiswaRepository.cariMentorBerdasarkanMataKuliah(kataKunci);
        List<Mahasiswa> dariTopik      = mahasiswaRepository.cariMentorBerdasarkanTopik(kataKunci);

        Set<Long> sudahAda = new HashSet<>();
        List<Mahasiswa> gabungan = new ArrayList<>();
        for (Mahasiswa m : dariMataKuliah) { if (sudahAda.add(m.getId())) gabungan.add(m); }
        for (Mahasiswa m : dariTopik)      { if (sudahAda.add(m.getId())) gabungan.add(m); }
        return gabungan;
    }

    public List<Siswa>     getRankingMentorSiswa()     { return siswaRepository.getRankingMentorSiswa(); }
    public List<Mahasiswa> getRankingMentorMahasiswa() { return mahasiswaRepository.getRankingMentorMahasiswa(); }

    // ============================================================
    // LIFECYCLE SESI — STATE TRANSITION
    // ============================================================

    /**
     * [STEP 1 — MENTEE] Membuat permintaan sesi baru.
     * Status awal: MENUNGGU_KONFIRMASI — mentor harus mengkonfirmasi sebelum jadwal dikunci.
     *
     * Secara otomatis menetapkan supervisor berdasarkan institusi mentor.
     *
     * @param sesi      instance konkret (SesiOnline/Offline/Video) — POLIMORFISME
     * @param waktuMulai waktu yang diinginkan mentee
     * @param durasiMenit durasi dalam menit
     * @return sesi yang tersimpan dengan status MENUNGGU_KONFIRMASI
     */
    public SesiMentoring buatPermintaanSesi(SesiMentoring sesi,
                                            LocalDateTime waktuMulai,
                                            int durasiMenit) {
        // Validasi waktu: harus di masa depan
        if (waktuMulai == null || waktuMulai.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Waktu sesi harus di masa depan.");
        }
        if (durasiMenit < 15) {
            throw new IllegalArgumentException("Durasi minimum sesi adalah 15 menit.");
        }

        // Panggil interface Schedulable — implementasi ada di SesiMentoring
        sesi.jadwalkanSesi(waktuMulai, durasiMenit);

        // Override ke MENUNGGU_KONFIRMASI karena ini permintaan dari mentee
        sesi.setStatusSesi(StatusSesi.MENUNGGU_KONFIRMASI);

        // Auto-assign supervisor berdasarkan institusi mentor
        Pengguna supervisorOtomatis = cariSupervisorUntukSesi(sesi);
        if (supervisorOtomatis != null) {
            sesi.setSupervisor(supervisorOtomatis);
        }

        SesiMentoring tersimpan = sesiRepository.save(sesi);

        // Notifikasi ke mentor tentang permintaan baru
        kirimNotifikasi(
            "Permintaan Sesi Baru",
            sesi.getMentee().getNamaLengkap() + " meminta sesi: \"" + sesi.getTopikPembahasan() + "\".",
            "JADWAL",
            sesi.getMentor()
        );

        return tersimpan;
    }

    /**
     * [STEP 1 — MENTOR] Membuat dan langsung menjadwalkan sesi baru (undangan dari mentor).
     * Status: langsung DIJADWALKAN karena mentor yang memulai.
     *
     * Demonstrasi POLIMORFISME: parameter bertipe abstrak SesiMentoring,
     * perilaku jadwalkanSesi() dieksekusi oleh subclass masing-masing.
     */
    public SesiMentoring buatDanJadwalkanSesi(SesiMentoring sesi,
                                              LocalDateTime waktuMulai,
                                              int durasiMenit) {
        if (waktuMulai == null || waktuMulai.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Waktu sesi harus di masa depan.");
        }

        // Panggil method dari interface Schedulable — polimorfis
        boolean berhasil = sesi.jadwalkanSesi(waktuMulai, durasiMenit);
        if (!berhasil) {
            throw new IllegalArgumentException(
                "Gagal menjadwalkan sesi. Pastikan waktu mulai valid dan di masa depan.");
        }

        // Auto-assign supervisor
        Pengguna supervisor = cariSupervisorUntukSesi(sesi);
        if (supervisor != null) sesi.setSupervisor(supervisor);

        SesiMentoring tersimpan = sesiRepository.save(sesi);

        // Notifikasi ke mentee
        kirimNotifikasi(
            "Undangan Sesi Baru",
            sesi.getMentor().getNamaLengkap() + " mengundang Anda untuk sesi: \""
                + sesi.getTopikPembahasan() + "\".",
            "JADWAL",
            sesi.getMentee()
        );

        return tersimpan;
    }

    /**
     * [STEP 2 — MENTOR] Mengkonfirmasi permintaan sesi dari mentee.
     * Transisi: MENUNGGU_KONFIRMASI → DIJADWALKAN
     *
     * Validasi bisnis: hanya mentor dari sesi ini yang boleh mengkonfirmasi.
     */
    public SesiMentoring konfirmasiSesi(Long sesiId, Long mentorId) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        if (!sesi.getMentor().getId().equals(mentorId)) {
            throw new IllegalStateException("Hanya mentor sesi ini yang bisa mengkonfirmasi.");
        }
        if (sesi.getStatusSesi() != StatusSesi.MENUNGGU_KONFIRMASI) {
            throw new IllegalStateException(
                "Sesi ini tidak dalam status menunggu konfirmasi. Status saat ini: "
                    + sesi.getStatusSesi().getLabel());
        }

        sesi.setStatusSesi(StatusSesi.DIJADWALKAN);

        kirimNotifikasi(
            "Sesi Dikonfirmasi ✓",
            sesi.getMentor().getNamaLengkap() + " telah mengkonfirmasi sesi \""
                + sesi.getTopikPembahasan() + "\". Jadwal dikunci!",
            "JADWAL",
            sesi.getMentee()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * [STEP 3 — MENTOR] Memulai sesi yang sudah dijadwalkan.
     * Transisi: DIJADWALKAN → BERLANGSUNG
     *
     * Validasi bisnis: sesi harus berstatus DIJADWALKAN sebelum bisa dimulai.
     */
    public SesiMentoring mulaiSesi(Long sesiId, Long mentorId) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        if (!sesi.getMentor().getId().equals(mentorId)) {
            throw new IllegalStateException("Hanya mentor sesi ini yang bisa memulai.");
        }
        if (sesi.getStatusSesi() != StatusSesi.DIJADWALKAN) {
            throw new IllegalStateException(
                "Hanya sesi berstatus DIJADWALKAN yang bisa dimulai. Status saat ini: "
                    + sesi.getStatusSesi().getLabel());
        }

        sesi.setStatusSesi(StatusSesi.BERLANGSUNG);

        // Polimorfisme: lakukanSesi() mengembalikan instruksi spesifik per tipe
        String instruksi = sesi.lakukanSesi();

        kirimNotifikasi(
            "Sesi Dimulai 🚀",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" sedang berlangsung. " + instruksi,
            "SESI",
            sesi.getMentee()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * [STEP 4 — MENTOR] Menyelesaikan sesi yang sedang berlangsung.
     * Transisi: BERLANGSUNG → SELESAI
     *
     * Validasi bisnis ketat: sesi WAJIB berstatus BERLANGSUNG.
     * Setelah selesai: poin dialokasikan dan supervisor dinotifikasi
     * untuk memproses laporan akademik.
     *
     * Poin berdasarkan durasi:
     *   < 30 mnt  → 5 poin mentor, 3 poin mentee
     *   30–60 mnt → 10 poin mentor, 7 poin mentee
     *   > 60 mnt  → 15 poin mentor, 10 poin mentee
     */
    public SesiMentoring selesaikanSesi(Long sesiId) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        // === VALIDASI BISNIS KETAT ===
        if (sesi.getStatusSesi() != StatusSesi.BERLANGSUNG) {
            throw new IllegalStateException(
                "Sesi tidak bisa diselesaikan karena tidak sedang berlangsung. " +
                "Status saat ini: " + sesi.getStatusSesi().getLabel() +
                ". Pastikan sesi sudah dimulai terlebih dahulu.");
        }

        // Hitung poin berdasarkan durasi
        int poinMentor, poinMentee;
        if (sesi.getDurasiMenit() < 30) {
            poinMentor = 5;  poinMentee = 3;
        } else if (sesi.getDurasiMenit() <= 60) {
            poinMentor = 10; poinMentee = 7;
        } else {
            poinMentor = 15; poinMentee = 10;
        }

        // Panggil method entity — mengubah status ke SELESAI dan menyimpan poin
        sesi.selesaikanSesi(poinMentor, poinMentee);

        // Tambahkan poin ke profil pengguna (polimorfis via instanceof)
        tambahPoinKePengguna(sesi.getMentor(), poinMentor);
        tambahPoinKePengguna(sesi.getMentee(), poinMentee);
        incrementSesiDiselesaikan(sesi.getMentor());

        // === TRIGGER LAPORAN AKADEMIK ===
        // Setelah sesi selesai, statusValidasi = BELUM_DITINJAU (default).
        // Supervisor mendapat notifikasi untuk memproses laporan.
        if (sesi.getSupervisor() != null) {
            kirimNotifikasi(
                "Laporan Akademik Baru",
                "Sesi \"" + sesi.getTopikPembahasan() + "\" antara " +
                    sesi.getMentor().getNamaLengkap() + " dan " +
                    sesi.getMentee().getNamaLengkap() +
                    " telah selesai dan menunggu validasi Anda.",
                "VALIDASI",
                sesi.getSupervisor()
            );
        }

        // Notifikasi mentee: minta review
        kirimNotifikasi(
            "Sesi Selesai — Berikan Rating",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah selesai. " +
                "Anda mendapat +" + poinMentee + " poin! Jangan lupa berikan rating untuk mentor.",
            "SESI",
            sesi.getMentee()
        );

        // Notifikasi mentor
        kirimNotifikasi(
            "Sesi Selesai ++" + poinMentor + " Poin",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah selesai. Terima kasih!",
            "SESI",
            sesi.getMentor()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * Membatalkan sesi mentoring.
     * Bisa dilakukan selama sesi masih dalam status aktif (bukan SELESAI/DIBATALKAN).
     */
    public SesiMentoring batalkanSesi(Long sesiId, String alasan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        if (!sesi.getStatusSesi().isAktif()) {
            throw new IllegalStateException(
                "Sesi tidak bisa dibatalkan karena sudah berakhir. Status: "
                    + sesi.getStatusSesi().getLabel());
        }

        // Delegasi ke interface Schedulable
        sesi.batalkanJadwal(alasan);

        kirimNotifikasi(
            "Sesi Dibatalkan",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" dibatalkan. Alasan: " + alasan,
            "JADWAL",
            sesi.getMentee()
        );
        kirimNotifikasi(
            "Sesi Dibatalkan",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" dibatalkan. Alasan: " + alasan,
            "JADWAL",
            sesi.getMentor()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * Mengubah jadwal sesi yang sudah ada.
     * Delegasi ke interface Schedulable — ubahJadwal() mengecek apakah sesi bisa dijadwal ulang.
     */
    public SesiMentoring ubahJadwalSesi(Long sesiId, LocalDateTime waktuBaru) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        boolean berhasil = sesi.ubahJadwal(waktuBaru);
        if (!berhasil) {
            throw new IllegalStateException(
                "Jadwal tidak dapat diubah. Sesi mungkin sudah berlangsung atau selesai.");
        }

        return sesiRepository.save(sesi);
    }

    // ============================================================
    // SISTEM PENILAIAN (Interface Ratable)
    // ============================================================

    /**
     * Memberikan review dan rating untuk sesi yang sudah selesai.
     *
     * Demonstrasi POLIMORFISME: beriRating() dipanggil pada Pengguna abstrak,
     * implementasi konkret ada di Siswa dan Mahasiswa (keduanya mengimplementasi Ratable).
     */
    public ReviewRating beriReviewDanRating(Long sesiId, Long pemberiId,
                                            int nilaiRating, String ulasan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        if (sesi.getStatusSesi() != StatusSesi.SELESAI) {
            throw new IllegalStateException(
                "Rating hanya bisa diberikan untuk sesi yang sudah selesai.");
        }
        if (nilaiRating < 1 || nilaiRating > 5) {
            throw new IllegalArgumentException("Nilai rating harus antara 1 dan 5.");
        }

        Pengguna pemberi  = penggunaRepository.findById(pemberiId)
            .orElseThrow(() -> new NoSuchElementException("Pemberi review tidak ditemukan."));
        Pengguna penerima = sesi.getMentor();

        ReviewRating review = new ReviewRating(nilaiRating, ulasan, sesi, pemberi, penerima);
        ReviewRating tersimpan = reviewRepository.save(review);

        // Update rating di profil mentor — polimorfisme via instanceof + interface Ratable
        if (penerima instanceof Siswa siswa) {
            siswa.beriRating(nilaiRating, ulasan);
            siswaRepository.save(siswa);
        } else if (penerima instanceof Mahasiswa mahasiswa) {
            mahasiswa.beriRating(nilaiRating, ulasan);
            // Cek apakah mahasiswa memenuhi syarat Asdos setelah rating diperbarui
            if (mahasiswa.memenuhiSyaratAsdos()) {
                mahasiswa.setKandidatAsdos(true);
                kirimNotifikasi(
                    "Selamat! Anda Memenuhi Syarat Asdos",
                    "Prestasi Anda memenuhi kriteria Asisten Dosen. Tunggu rekomendasi dari Dosen.",
                    "ASDOS",
                    mahasiswa
                );
            }
            mahasiswaRepository.save(mahasiswa);
        }

        return tersimpan;
    }

    public List<ReviewRating> getReviewUntukPengguna(Long penggunaId) {
        return reviewRepository.findByPenerimaReviewId(penggunaId);
    }

    // ============================================================
    // MATERI BELAJAR
    // ============================================================

    public MateriBelajar unggahMateri(MateriBelajar materi)      { return materiRepository.save(materi); }
    public List<MateriBelajar> cariMateri(String kataKunci)       { return materiRepository.findByJudulContainingIgnoreCase(kataKunci); }
    public List<MateriBelajar> getMateriPopuler()                 { return materiRepository.findAllByOrderByJumlahUnduhanDesc(); }
    public List<MateriBelajar> getMateriByPengguna(Long id)       { return materiRepository.findByPengunggahId(id); }

    // ============================================================
    // QUERY SESI
    // ============================================================

    public Optional<SesiMentoring> getSesiById(Long id)                 { return sesiRepository.findById(id); }
    public List<SesiMentoring> getSemuaSesiPengguna(Long id)            { return sesiRepository.findSemuaSesiPengguna(id); }
    public List<SesiMentoring> getSesiMendatangMentor(Long id)          { return sesiRepository.getSesiMendatangMentor(id, LocalDateTime.now()); }
    public List<SesiMentoring> getSesiMendatangMentee(Long id)          { return sesiRepository.getSesiMendatangMentee(id, LocalDateTime.now()); }
    public long hitungSesiSelesaiMentor(Long mentorId)                  { return sesiRepository.countByMentorIdAndStatusSesi(mentorId, StatusSesi.SELESAI); }

    // ============================================================
    // HELPER PRIVATE
    // ============================================================

    /**
     * Mencari supervisor yang paling sesuai untuk sesi berdasarkan institusi mentor.
     * - Mentor Siswa    → Guru pertama dari sekolah yang sama
     * - Mentor Mahasiswa → Dosen pertama dari program studi yang sama
     *
     * Menggunakan PenggunaRepository.findByRole() agar tidak perlu inject GuruRepository
     * / DosenRepository secara terpisah — cukup dengan satu repository yang sudah ada.
     */
    private Pengguna cariSupervisorUntukSesi(SesiMentoring sesi) {
        Pengguna mentor = sesi.getMentor();
        if (mentor instanceof Siswa siswa) {
            return penggunaRepository.findByRole(RolePengguna.GURU).stream()
                .filter(p -> p instanceof Guru g
                          && siswa.getNamaSekolah() != null
                          && siswa.getNamaSekolah().equals(g.getNamaSekolah()))
                .findFirst().orElse(null);
        } else if (mentor instanceof Mahasiswa mhs) {
            return penggunaRepository.findByRole(RolePengguna.DOSEN).stream()
                .filter(p -> p instanceof Dosen d
                          && mhs.getProgramStudi() != null
                          && mhs.getProgramStudi().equals(d.getProgramStudi()))
                .findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Menambahkan poin progres ke pengguna berdasarkan tipe konkretnya.
     * Demonstrasi penggunaan pattern-matching instanceof (Java 16+).
     */
    private void tambahPoinKePengguna(Pengguna pengguna, int poin) {
        if (pengguna instanceof Siswa siswa) {
            siswa.tambahPoinProgres(poin);
            siswaRepository.save(siswa);
        } else if (pengguna instanceof Mahasiswa mhs) {
            mhs.tambahPoinProgres(poin);
            mahasiswaRepository.save(mhs);
        }
    }

    private void incrementSesiDiselesaikan(Pengguna mentor) {
        if (mentor instanceof Siswa siswa) {
            siswa.setSesiDiselesaikan(siswa.getSesiDiselesaikan() + 1);
            siswaRepository.save(siswa);
        } else if (mentor instanceof Mahasiswa mhs) {
            mhs.setSesiDiselesaikan(mhs.getSesiDiselesaikan() + 1);
            mahasiswaRepository.save(mhs);
        }
    }

    private void kirimNotifikasi(String judul, String pesan, String kategori, Pengguna penerima) {
        if (penerima == null) return;
        notifikasiRepository.save(new Notifikasi(judul, pesan, kategori, penerima));
    }

    private SesiMentoring getSesiAtauLempar(Long sesiId) {
        return sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException("Sesi mentoring tidak ditemukan: " + sesiId));
    }
}
