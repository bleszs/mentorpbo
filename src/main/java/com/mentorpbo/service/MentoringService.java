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

    // === Konstanta poin bisnis ===
    private static final int POIN_MENTOR_PENDEK  = 5;   // sesi < 30 menit
    private static final int POIN_MENTOR_SEDANG  = 10;  // sesi 30–60 menit
    private static final int POIN_MENTOR_PANJANG = 15;  // sesi > 60 menit
    private static final int POIN_MENTEE_SELESAI = 10;  // bonus selesai sesi
    private static final int POIN_BONUS_TEPAT_WAKTU = 2;  // bonus hadir tepat waktu
    private static final int POIN_BONUS_FEEDBACK    = 3;  // bonus memberi rating
    private static final int POIN_PENALTI_BATAL     = 25; // penalti pembatalan mendadak

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
        List<Siswa> hasil = (mataPelajaran == null || mataPelajaran.isBlank())
            ? siswaRepository.findByIsMentorTrue()
            : siswaRepository.cariMentorBerdasarkanMataPelajaran(mataPelajaran);
        // Hanya tampilkan mentor yang sudah disetujui supervisor
        return hasil.stream().filter(Siswa::isMentorTervalidasi).toList();
    }

    /**
     * Mencari mentor mahasiswa berdasarkan mata kuliah atau topik keahlian.
     * Menggabungkan dua hasil pencarian dan menghilangkan duplikat — demonstrasi
     * penggabungan koleksi yang efisien menggunakan Set.
     */
    public List<Mahasiswa> cariMentorMahasiswa(String kataKunci) {
        if (kataKunci == null || kataKunci.isBlank()) {
            return mahasiswaRepository.findByIsMentorTrue().stream()
                .filter(Mahasiswa::isMentorTervalidasi).toList();
        }
        List<Mahasiswa> dariMataKuliah = mahasiswaRepository.cariMentorBerdasarkanMataKuliah(kataKunci);
        List<Mahasiswa> dariTopik      = mahasiswaRepository.cariMentorBerdasarkanTopik(kataKunci);

        Set<Long> sudahAda = new HashSet<>();
        List<Mahasiswa> gabungan = new ArrayList<>();
        for (Mahasiswa m : dariMataKuliah) { if (sudahAda.add(m.getId())) gabungan.add(m); }
        for (Mahasiswa m : dariTopik)      { if (sudahAda.add(m.getId())) gabungan.add(m); }
        // Hanya tampilkan mentor yang sudah disetujui supervisor
        return gabungan.stream().filter(Mahasiswa::isMentorTervalidasi).toList();
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
     * Otorisasi: hanya mentor sesi ini yang boleh menyelesaikan.
     * Validasi bisnis ketat: sesi WAJIB berstatus BERLANGSUNG.
     * Setelah selesai: poin dialokasikan dan supervisor dinotifikasi
     * untuk memproses laporan akademik.
     *
     * Poin berdasarkan durasi:
     *   < 30 mnt  → 5 poin mentor, 3 poin mentee
     *   30–60 mnt → 10 poin mentor, 7 poin mentee
     *   > 60 mnt  → 15 poin mentor, 10 poin mentee
     *
     * @param sesiId   id sesi yang diselesaikan
     * @param mentorId id pengguna yang sedang login (harus mentor sesi ini)
     */
    public SesiMentoring selesaikanSesi(Long sesiId, Long mentorId) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        // === OTORISASI: hanya mentor sesi ini ===
        if (sesi.getMentor() == null || !sesi.getMentor().getId().equals(mentorId)) {
            throw new IllegalStateException("Hanya mentor sesi ini yang bisa menyelesaikan sesi.");
        }

        // === VALIDASI BISNIS KETAT ===
        if (sesi.getStatusSesi() != StatusSesi.BERLANGSUNG) {
            throw new IllegalStateException(
                "Sesi tidak bisa diselesaikan karena tidak sedang berlangsung. " +
                "Status saat ini: " + sesi.getStatusSesi().getLabel() +
                ". Pastikan sesi sudah dimulai terlebih dahulu.");
        }

        int poinMentor;
        if (sesi.getDurasiMenit() < 30) poinMentor = POIN_MENTOR_PENDEK;
        else if (sesi.getDurasiMenit() <= 60) poinMentor = POIN_MENTOR_SEDANG;
        else poinMentor = POIN_MENTOR_PANJANG;

        int poinMentee = POIN_MENTEE_SELESAI;
        boolean tepatWaktu = sesi.getWaktuMulai() != null &&
            !LocalDateTime.now().isBefore(sesi.getWaktuMulai()) &&
            LocalDateTime.now().isBefore(sesi.getWaktuMulai().plusMinutes(sesi.getDurasiMenit() + 5));
        if (tepatWaktu) poinMentee += POIN_BONUS_TEPAT_WAKTU;

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
        String bonusInfo = tepatWaktu ? " (+2 poin tepat waktu)" : "";
        kirimNotifikasi(
            "Sesi Selesai — Berikan Rating",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah selesai. " +
                "Anda mendapat +" + poinMentee + " poin" + bonusInfo + "! Jangan lupa berikan rating untuk mentor (+3 poin).",
            "SESI",
            sesi.getMentee()
        );

        // Notifikasi mentor
        kirimNotifikasi(
            "Sesi Selesai +" + poinMentor + " Poin",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah selesai. Terima kasih!",
            "SESI",
            sesi.getMentor()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * Membatalkan sesi mentoring.
     * Bisa dilakukan selama sesi masih dalam status aktif (bukan SELESAI/DIBATALKAN).
     *
     * Otorisasi: hanya peserta sesi (mentor ATAU mentee) yang boleh membatalkan.
     * Penalti pembatalan mendadak (-25 poin) dikenakan kepada PIHAK YANG MEMBATALKAN,
     * bukan selalu mentee — agar adil.
     *
     * @param sesiId    id sesi
     * @param pembatalId id pengguna yang sedang login (harus mentor/mentee sesi ini)
     * @param alasan    alasan pembatalan
     */
    public SesiMentoring batalkanSesi(Long sesiId, Long pembatalId, String alasan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        // === OTORISASI: hanya peserta sesi ===
        boolean isMentor = sesi.getMentor() != null && sesi.getMentor().getId().equals(pembatalId);
        boolean isMentee = sesi.getMentee() != null && sesi.getMentee().getId().equals(pembatalId);
        if (!isMentor && !isMentee) {
            throw new IllegalStateException("Hanya peserta sesi ini yang bisa membatalkan sesi.");
        }

        if (!sesi.getStatusSesi().isAktif()) {
            throw new IllegalStateException(
                "Sesi tidak bisa dibatalkan karena sudah berakhir. Status: "
                    + sesi.getStatusSesi().getLabel());
        }

        // Delegasi ke interface Schedulable
        sesi.batalkanJadwal(alasan);

        // Batal mendadak (< 24 jam sebelum sesi) = -25 poin untuk PIHAK YANG MEMBATALKAN
        boolean mendadak = sesi.getWaktuMulai() != null &&
            sesi.getWaktuMulai().isBefore(LocalDateTime.now().plusHours(24));
        if (mendadak) {
            Pengguna pembatal = isMentor ? sesi.getMentor() : sesi.getMentee();
            if (pembatal instanceof Siswa s) {
                s.setTotalPoinProgres(Math.max(0, s.getTotalPoinProgres() - POIN_PENALTI_BATAL));
                siswaRepository.save(s);
            } else if (pembatal instanceof Mahasiswa m) {
                m.setTotalPoinProgres(Math.max(0, m.getTotalPoinProgres() - POIN_PENALTI_BATAL));
                mahasiswaRepository.save(m);
            }
        }

        String penaltiInfo = mendadak ? " (-25 poin untuk pihak yang membatalkan karena pembatalan mendadak)" : "";
        // Notifikasi ke pihak yang membatalkan (sertakan info penalti)
        kirimNotifikasi(
            "Sesi Dibatalkan",
            "Anda membatalkan sesi \"" + sesi.getTopikPembahasan() + "\". Alasan: " + alasan + penaltiInfo,
            "JADWAL",
            isMentor ? sesi.getMentor() : sesi.getMentee()
        );
        // Notifikasi ke pihak lawan (tanpa penalti)
        kirimNotifikasi(
            "Sesi Dibatalkan",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" dibatalkan oleh "
                + (isMentor ? "mentor" : "mentee") + ". Alasan: " + alasan,
            "JADWAL",
            isMentor ? sesi.getMentee() : sesi.getMentor()
        );

        return sesiRepository.save(sesi);
    }

    /**
     * Mentor menolak permintaan sesi: MENUNGGU_KONFIRMASI → DIBATALKAN.
     * Mentee mendapat notifikasi penolakan dengan alasan.
     */
    public SesiMentoring tolakPermintaanSesi(Long sesiId, Long mentorId, String alasan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);

        if (!sesi.getMentor().getId().equals(mentorId)) {
            throw new IllegalStateException("Hanya mentor sesi ini yang dapat menolak permintaan.");
        }
        if (sesi.getStatusSesi() != StatusSesi.MENUNGGU_KONFIRMASI) {
            throw new IllegalStateException(
                "Hanya permintaan MENUNGGU_KONFIRMASI yang dapat ditolak. Status saat ini: "
                    + sesi.getStatusSesi().getLabel());
        }

        sesi.setStatusSesi(StatusSesi.DIBATALKAN);
        // Simpan alasan ke alasanPembatalan, BUKAN menimpa deskripsi asli mentee
        sesi.setAlasanPembatalan("Ditolak mentor: " + (alasan != null ? alasan : "-"));

        kirimNotifikasi(
            "Permintaan Sesi Ditolak",
            "Maaf, " + sesi.getMentor().getNamaLengkap() + " menolak permintaan sesi \""
                + sesi.getTopikPembahasan() + "\". Alasan: " + (alasan != null ? alasan : "-")
                + ". Coba cari mentor lain!",
            "JADWAL",
            sesi.getMentee()
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

        // Notifikasi mentee tentang perubahan jadwal
        kirimNotifikasi(
            "Jadwal Sesi Diperbarui",
            "Mentor telah memperbarui jadwal sesi \"" + sesi.getTopikPembahasan() + "\". " +
                "Cek detail sesi untuk jadwal baru.",
            "JADWAL",
            sesi.getMentee()
        );

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
        return beriReviewDanRatingLengkap(sesiId, pemberiId, nilaiRating, ulasan, null);
    }

    public ReviewRating beriReviewDanRatingLengkap(Long sesiId, Long pemberiId,
                                                    int nilaiRating, String ulasan,
                                                    String saranKritik) {
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
        if (saranKritik != null && !saranKritik.isBlank()) {
            review.setSaranKritik(saranKritik);
        }
        ReviewRating tersimpan = reviewRepository.save(review);

        // Hitung poin mentor berdasarkan rating: b5 +15, b4 +10, b3 +5, b2 -5, b1 -10
        int poinDariRating = switch (nilaiRating) {
            case 5 -> 15;
            case 4 -> 10;
            case 3 -> 5;
            case 2 -> -5;
            case 1 -> -10;
            default -> 0;
        };

        // Update rating dan poin di profil mentor
        if (penerima instanceof Siswa siswa) {
            siswa.beriRating(nilaiRating, ulasan);
            if (poinDariRating > 0) siswa.tambahPoinProgres(poinDariRating);
            else if (poinDariRating < 0) siswa.setTotalPoinProgres(Math.max(0, siswa.getTotalPoinProgres() + poinDariRating));
            siswaRepository.save(siswa);
        } else if (penerima instanceof Mahasiswa mahasiswa) {
            mahasiswa.beriRating(nilaiRating, ulasan);
            if (poinDariRating > 0) mahasiswa.tambahPoinProgres(poinDariRating);
            else if (poinDariRating < 0) mahasiswa.setTotalPoinProgres(Math.max(0, mahasiswa.getTotalPoinProgres() + poinDariRating));
            if (mahasiswa.memenuhiSyaratAsdos()) {
                mahasiswa.setKandidatAsdos(true);
                kirimNotifikasi(
                    "Selamat! Anda Memenuhi Syarat Asdos",
                    "Prestasi Anda memenuhi kriteria Asisten Dosen. Tunggu rekomendasi dari Dosen.",
                    "ASDOS", mahasiswa
                );
            }
            mahasiswaRepository.save(mahasiswa);
        }

        tambahPoinKePengguna(pemberi, POIN_BONUS_FEEDBACK);

        // Notifikasi mentor tentang rating baru
        String pesanRating = "Mentee memberi rating " + nilaiRating + " bintang (" +
            (poinDariRating >= 0 ? "+" : "") + poinDariRating + " poin) untuk sesi \"" +
            sesi.getTopikPembahasan() + "\".";
        kirimNotifikasi("Rating Baru Diterima", pesanRating, "RATING", penerima);

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
    public List<MateriBelajar> getMateriSajaByPengguna(Long id)   { return materiRepository.findByPengunggahIdAndTipeKonten(id, "MATERI"); }
    public List<MateriBelajar> getSumberDayaByPengguna(Long id)   { return materiRepository.findByPengunggahIdAndTipeKonten(id, "SUMBER_DAYA"); }
    public Optional<MateriBelajar> getMateriById(Long id)         { return materiRepository.findById(id); }
    public void hapusMateri(Long id)                              { materiRepository.deleteById(id); }

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
