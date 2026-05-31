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
 * Service MentoringService - Lapisan logika bisnis inti untuk sistem mentoring.
 *
 * Menangani seluruh alur bisnis utama:
 * - Pencarian mentor berdasarkan mata pelajaran/kuliah, topik, dan rating
 * - Penjadwalan dan manajemen sesi mentoring (Online, Offline, Video)
 * - Sistem penilaian (rating dan review) setelah sesi
 * - Pengelolaan materi belajar
 * - Sistem poin progres
 *
 * Service ini mendemonstrasikan penggunaan POLIMORFISME dimana
 * SesiMentoring diperlakukan secara generik melalui interface Schedulable,
 * namun perilaku spesifik tetap dijalankan oleh subclass masing-masing.
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
     * Mengembalikan daftar siswa yang aktif sebagai mentor dan memiliki
     * keahlian di mata pelajaran yang dicari.
     *
     * @param mataPelajaran kata kunci mata pelajaran
     * @return daftar Siswa mentor yang cocok
     */
    public List<Siswa> cariMentorSiswa(String mataPelajaran) {
        if (mataPelajaran == null || mataPelajaran.isBlank()) {
            return siswaRepository.findByIsMentorTrue();
        }
        return siswaRepository.cariMentorBerdasarkanMataPelajaran(mataPelajaran);
    }

    /**
     * Mencari mentor mahasiswa berdasarkan mata kuliah atau topik keahlian.
     *
     * @param kataKunci kata kunci pencarian (mata kuliah atau topik)
     * @return daftar Mahasiswa mentor yang cocok
     */
    public List<Mahasiswa> cariMentorMahasiswa(String kataKunci) {
        if (kataKunci == null || kataKunci.isBlank()) {
            return mahasiswaRepository.findByIsMentorTrue();
        }
        // Gabungkan hasil pencarian dari mata kuliah dan topik
        List<Mahasiswa> hasilMataKuliah = mahasiswaRepository
            .cariMentorBerdasarkanMataKuliah(kataKunci);
        List<Mahasiswa> hasilTopik = mahasiswaRepository
            .cariMentorBerdasarkanTopik(kataKunci);

        // Gabungkan dan hilangkan duplikat menggunakan Set
        Set<Long> idSudahAda = new HashSet<>();
        List<Mahasiswa> hasilGabungan = new ArrayList<>();

        for (Mahasiswa m : hasilMataKuliah) {
            if (idSudahAda.add(m.getId())) {
                hasilGabungan.add(m);
            }
        }
        for (Mahasiswa m : hasilTopik) {
            if (idSudahAda.add(m.getId())) {
                hasilGabungan.add(m);
            }
        }
        return hasilGabungan;
    }

    /**
     * Mendapatkan ranking mentor siswa berdasarkan rating tertinggi.
     */
    public List<Siswa> getRankingMentorSiswa() {
        return siswaRepository.getRankingMentorSiswa();
    }

    /**
     * Mendapatkan ranking mentor mahasiswa berdasarkan rating tertinggi.
     */
    public List<Mahasiswa> getRankingMentorMahasiswa() {
        return mahasiswaRepository.getRankingMentorMahasiswa();
    }

    // ============================================================
    // PENJADWALAN SESI MENTORING
    // ============================================================

    /**
     * Membuat dan menjadwalkan sesi mentoring baru.
     * Method ini mendemonstrasikan POLIMORFISME: parameter bertipe SesiMentoring (abstract)
     * tetapi yang diterima adalah instance konkret (SesiOnline/SesiOffline/SesiVideo).
     *
     * @param sesi objek sesi mentoring (SesiOnline, SesiOffline, atau SesiVideo)
     * @param waktuMulai waktu mulai yang dijadwalkan
     * @param durasiMenit durasi dalam menit
     * @return sesi yang sudah disimpan dan dijadwalkan
     */
    public SesiMentoring buatDanJadwalkanSesi(SesiMentoring sesi,
                                               LocalDateTime waktuMulai,
                                               int durasiMenit) {
        // Polimorfisme: memanggil jadwalkanSesi() yang diimplementasikan di SesiMentoring
        boolean berhasilDijadwalkan = sesi.jadwalkanSesi(waktuMulai, durasiMenit);
        if (!berhasilDijadwalkan) {
            throw new IllegalArgumentException(
                "Gagal menjadwalkan sesi. Pastikan waktu mulai valid (di masa depan).");
        }

        SesiMentoring sesiTersimpan = sesiRepository.save(sesi);

        // Kirim notifikasi ke mentor
        Notifikasi notifMentor = new Notifikasi(
            "Sesi Mentoring Baru",
            "Anda memiliki sesi mentoring baru: " + sesi.getTopikPembahasan(),
            "JADWAL",
            sesi.getMentor()
        );
        notifikasiRepository.save(notifMentor);

        return sesiTersimpan;
    }

    /**
     * Mengubah jadwal sesi mentoring yang sudah ada.
     */
    public SesiMentoring ubahJadwalSesi(Long sesiId, LocalDateTime waktuBaru) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException(
                "Sesi mentoring tidak ditemukan dengan ID: " + sesiId));

        boolean berhasil = sesi.ubahJadwal(waktuBaru);
        if (!berhasil) {
            throw new IllegalStateException("Sesi tidak dapat dijadwalkan ulang.");
        }

        return sesiRepository.save(sesi);
    }

    /**
     * Membatalkan sesi mentoring.
     */
    public SesiMentoring batalkanSesi(Long sesiId, String alasan) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException(
                "Sesi mentoring tidak ditemukan dengan ID: " + sesiId));

        sesi.batalkanJadwal(alasan);
        return sesiRepository.save(sesi);
    }

    /**
     * Menyelesaikan sesi mentoring dan mengalokasikan poin progres.
     * Poin ditentukan berdasarkan durasi sesi:
     * - < 30 menit: 5 poin mentor, 3 poin mentee
     * - 30-60 menit: 10 poin mentor, 7 poin mentee
     * - > 60 menit: 15 poin mentor, 10 poin mentee
     */
    public SesiMentoring selesaikanSesi(Long sesiId) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException(
                "Sesi mentoring tidak ditemukan dengan ID: " + sesiId));

        // Hitung poin berdasarkan durasi
        int poinMentor, poinMentee;
        if (sesi.getDurasiMenit() < 30) {
            poinMentor = 5;
            poinMentee = 3;
        } else if (sesi.getDurasiMenit() <= 60) {
            poinMentor = 10;
            poinMentee = 7;
        } else {
            poinMentor = 15;
            poinMentee = 10;
        }

        sesi.selesaikanSesi(poinMentor, poinMentee);

        // Tambahkan poin ke profil mentor dan mentee
        tambahPoinKePengguna(sesi.getMentor(), poinMentor);
        tambahPoinKePengguna(sesi.getMentee(), poinMentee);

        // Update jumlah sesi diselesaikan untuk mentor
        incrementSesiDiselesaikan(sesi.getMentor());

        return sesiRepository.save(sesi);
    }

    // ============================================================
    // SISTEM PENILAIAN (RATING & REVIEW)
    // ============================================================

    /**
     * Memberikan review dan rating untuk sesi mentoring yang sudah selesai.
     * Secara otomatis memperbarui total rating di profil mentor.
     */
    public ReviewRating beriReviewDanRating(Long sesiId, Long pemberiId,
                                             int nilaiRating, String ulasan) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException("Sesi tidak ditemukan."));

        if (sesi.getStatusSesi() != StatusSesi.SELESAI) {
            throw new IllegalStateException("Rating hanya bisa diberikan untuk sesi yang selesai.");
        }

        Pengguna pemberi = penggunaRepository.findById(pemberiId)
            .orElseThrow(() -> new NoSuchElementException("Pemberi review tidak ditemukan."));

        Pengguna penerima = sesi.getMentor();

        // Buat review baru
        ReviewRating review = new ReviewRating(nilaiRating, ulasan, sesi, pemberi, penerima);
        ReviewRating reviewTersimpan = reviewRepository.save(review);

        // Update rating di profil mentor (menerapkan interface Ratable secara polimorfis)
        if (penerima instanceof Siswa siswa) {
            siswa.beriRating(nilaiRating, ulasan);
            siswaRepository.save(siswa);
        } else if (penerima instanceof Mahasiswa mahasiswa) {
            mahasiswa.beriRating(nilaiRating, ulasan);
            mahasiswaRepository.save(mahasiswa);
        }

        return reviewTersimpan;
    }

    /**
     * Mendapatkan semua review yang diterima oleh pengguna tertentu.
     */
    public List<ReviewRating> getReviewUntukPengguna(Long penggunaId) {
        return reviewRepository.findByPenerimaReviewId(penggunaId);
    }

    // ============================================================
    // MATERI BELAJAR
    // ============================================================

    /**
     * Mengunggah materi belajar baru.
     */
    public MateriBelajar unggahMateri(MateriBelajar materi) {
        return materiRepository.save(materi);
    }

    /**
     * Mencari materi berdasarkan judul.
     */
    public List<MateriBelajar> cariMateri(String kataKunci) {
        return materiRepository.findByJudulContainingIgnoreCase(kataKunci);
    }

    /**
     * Mendapatkan materi terpopuler berdasarkan jumlah unduhan.
     */
    public List<MateriBelajar> getMateriPopuler() {
        return materiRepository.findAllByOrderByJumlahUnduhanDesc();
    }

    /**
     * Mendapatkan materi yang diunggah oleh pengguna tertentu.
     */
    public List<MateriBelajar> getMateriByPengguna(Long penggunaId) {
        return materiRepository.findByPengunggahId(penggunaId);
    }

    // ============================================================
    // SESI QUERIES
    // ============================================================

    /**
     * Mendapatkan semua sesi untuk pengguna tertentu.
     */
    public List<SesiMentoring> getSemuaSesiPengguna(Long penggunaId) {
        return sesiRepository.findSemuaSesiPengguna(penggunaId);
    }

    /**
     * Mendapatkan sesi mendatang untuk mentor.
     */
    public List<SesiMentoring> getSesiMendatangMentor(Long mentorId) {
        return sesiRepository.getSesiMendatangMentor(mentorId, LocalDateTime.now());
    }

    /**
     * Mendapatkan sesi mendatang untuk mentee.
     */
    public List<SesiMentoring> getSesiMendatangMentee(Long menteeId) {
        return sesiRepository.getSesiMendatangMentee(menteeId, LocalDateTime.now());
    }

    /**
     * Mendapatkan sesi berdasarkan ID.
     */
    public Optional<SesiMentoring> getSesiById(Long sesiId) {
        return sesiRepository.findById(sesiId);
    }

    /**
     * Menghitung total sesi selesai untuk pengguna sebagai mentor.
     */
    public long hitungSesiSelesaiMentor(Long mentorId) {
        return sesiRepository.countByMentorIdAndStatusSesi(mentorId, StatusSesi.SELESAI);
    }

    // ============================================================
    // HELPER METHODS (Private)
    // ============================================================

    /**
     * Menambahkan poin progres ke pengguna berdasarkan tipe konkretnya.
     * Mendemonstrasikan pengecekan tipe dan downcast yang aman.
     */
    private void tambahPoinKePengguna(Pengguna pengguna, int poin) {
        if (pengguna instanceof Siswa siswa) {
            siswa.tambahPoinProgres(poin);
            siswaRepository.save(siswa);
        } else if (pengguna instanceof Mahasiswa mahasiswa) {
            mahasiswa.tambahPoinProgres(poin);
            mahasiswaRepository.save(mahasiswa);
        }
    }

    /**
     * Menambahkan jumlah sesi diselesaikan untuk mentor.
     */
    private void incrementSesiDiselesaikan(Pengguna mentor) {
        if (mentor instanceof Siswa siswa) {
            siswa.setSesiDiselesaikan(siswa.getSesiDiselesaikan() + 1);
            siswaRepository.save(siswa);
        } else if (mentor instanceof Mahasiswa mahasiswa) {
            mahasiswa.setSesiDiselesaikan(mahasiswa.getSesiDiselesaikan() + 1);
            mahasiswaRepository.save(mahasiswa);
        }
    }
}
