package com.mentorpbo.service;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.StatusValidasi;
import com.mentorpbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service SupervisorService - Lapisan logika bisnis untuk fitur supervisor.
 *
 * Menangani fitur khusus Guru dan Dosen:
 * - Validasi kegiatan mentoring (menerapkan interface Verifiable)
 * - Pemantauan statistik dan ranking mentor
 * - Pencarian "bibit unggul" kandidat Asisten Dosen (Asdos)
 * - Pengangkatan dan manajemen Asdos
 */
@Service
@Transactional
public class SupervisorService {

    private final SesiMentoringRepository sesiRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final SiswaRepository siswaRepository;
    private final GuruRepository guruRepository;
    private final DosenRepository dosenRepository;
    private final NotifikasiRepository notifikasiRepository;
    private final PenggunaRepository penggunaRepository;

    @Autowired
    public SupervisorService(SesiMentoringRepository sesiRepository,
                             MahasiswaRepository mahasiswaRepository,
                             SiswaRepository siswaRepository,
                             GuruRepository guruRepository,
                             DosenRepository dosenRepository,
                             NotifikasiRepository notifikasiRepository,
                             PenggunaRepository penggunaRepository) {
        this.sesiRepository = sesiRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.siswaRepository = siswaRepository;
        this.guruRepository = guruRepository;
        this.dosenRepository = dosenRepository;
        this.notifikasiRepository = notifikasiRepository;
        this.penggunaRepository = penggunaRepository;
    }

    // ============================================================
    // VALIDASI KEGIATAN (Interface Verifiable)
    // ============================================================

    /**
     * Memvalidasi sesi mentoring oleh supervisor.
     * Mendemonstrasikan penggunaan interface Verifiable secara polimorfis.
     *
     * @param sesiId ID sesi yang akan divalidasi
     * @param supervisorId ID supervisor yang memvalidasi
     * @param catatan catatan dari supervisor
     * @return sesi yang sudah divalidasi
     */
    public SesiMentoring validasiSesi(Long sesiId, Long supervisorId, String catatan) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException("Sesi tidak ditemukan: " + sesiId));

        // Memanggil method dari interface Verifiable
        sesi.validasi(supervisorId, catatan);

        // Update counter validasi di profil supervisor
        penggunaRepository.findById(supervisorId).ifPresent(supervisor -> {
            if (supervisor instanceof Guru guru) {
                guru.catatValidasi();
                guruRepository.save(guru);
            } else if (supervisor instanceof Dosen dosen) {
                dosen.catatValidasi();
                dosenRepository.save(dosen);
            }
        });

        // Kirim notifikasi ke mentor
        Notifikasi notif = new Notifikasi(
            "Sesi Divalidasi",
            "Sesi '" + sesi.getTopikPembahasan() + "' telah divalidasi oleh supervisor.",
            "VALIDASI",
            sesi.getMentor()
        );
        notifikasiRepository.save(notif);

        return sesiRepository.save(sesi);
    }

    /**
     * Menolak validasi sesi mentoring oleh supervisor.
     */
    public SesiMentoring tolakValidasiSesi(Long sesiId, Long supervisorId, String alasan) {
        SesiMentoring sesi = sesiRepository.findById(sesiId)
            .orElseThrow(() -> new NoSuchElementException("Sesi tidak ditemukan: " + sesiId));

        sesi.tolakValidasi(supervisorId, alasan);

        // Kirim notifikasi penolakan
        Notifikasi notif = new Notifikasi(
            "Validasi Ditolak",
            "Sesi '" + sesi.getTopikPembahasan() + "' ditolak. Alasan: " + alasan,
            "VALIDASI",
            sesi.getMentor()
        );
        notifikasiRepository.save(notif);

        return sesiRepository.save(sesi);
    }

    /**
     * Mendapatkan daftar sesi yang menunggu validasi dari supervisor tertentu.
     */
    public List<SesiMentoring> getSesiMenungguValidasi(Long supervisorId) {
        return sesiRepository.getSesiMenungguValidasi(supervisorId);
    }

    /**
     * Mendapatkan semua sesi yang diawasi oleh supervisor.
     */
    public List<SesiMentoring> getSesiDiawasi(Long supervisorId) {
        return sesiRepository.findBySupervisorId(supervisorId);
    }

    // ============================================================
    // PENCARIAN KANDIDAT ASDOS (Fitur Khusus Dosen)
    // ============================================================

    /**
     * Mencari mahasiswa "bibit unggul" yang layak menjadi Asisten Dosen.
     * Kriteria default: IPK >= 3.0, Rating >= 4.0, Min 5 sesi, Min 5 penilaian.
     *
     * @return daftar kandidat Asdos diurutkan berdasarkan skor kelayakan
     */
    public List<Mahasiswa> cariKandidatAsdos() {
        return cariKandidatAsdos(3.0, 5, 5, 4.0);
    }

    /**
     * Mencari kandidat Asdos dengan kriteria kustom.
     */
    public List<Mahasiswa> cariKandidatAsdos(double minIpk, int minPenilaian,
                                              int minSesi, double minRating) {
        return mahasiswaRepository.cariKandidatAsdos(minIpk, minPenilaian, minSesi, minRating);
    }

    /**
     * Merekomendasikan mahasiswa sebagai kandidat Asdos.
     * Mengecek kelayakan berdasarkan method memenuhiSyaratAsdos() di entity Mahasiswa.
     */
    public Mahasiswa rekomendasikanSebagaiAsdos(Long mahasiswaId, Long dosenId) {
        Mahasiswa mahasiswa = mahasiswaRepository.findById(mahasiswaId)
            .orElseThrow(() -> new NoSuchElementException("Mahasiswa tidak ditemukan."));

        if (!mahasiswa.memenuhiSyaratAsdos()) {
            throw new IllegalStateException(
                "Mahasiswa belum memenuhi syarat Asdos. " +
                "Skor kelayakan: " + mahasiswa.hitungSkorKelayakanAsdos());
        }

        mahasiswa.setKandidatAsdos(true);

        // Update counter rekomendasi di profil dosen
        dosenRepository.findById(dosenId).ifPresent(dosen -> {
            dosen.catatRekomendasiAsdos();
            dosenRepository.save(dosen);
        });

        // Kirim notifikasi ke mahasiswa
        Notifikasi notif = new Notifikasi(
            "Rekomendasi Asisten Dosen",
            "Selamat! Anda telah direkomendasikan sebagai kandidat Asisten Dosen.",
            "ASDOS",
            mahasiswa
        );
        notifikasiRepository.save(notif);

        return mahasiswaRepository.save(mahasiswa);
    }

    /**
     * Mengangkat mahasiswa menjadi Asisten Dosen resmi.
     */
    public Mahasiswa angkatSebagaiAsdos(Long mahasiswaId) {
        Mahasiswa mahasiswa = mahasiswaRepository.findById(mahasiswaId)
            .orElseThrow(() -> new NoSuchElementException("Mahasiswa tidak ditemukan."));

        mahasiswa.setAsdos(true);

        Notifikasi notif = new Notifikasi(
            "Pengangkatan Asisten Dosen",
            "Selamat! Anda resmi diangkat sebagai Asisten Dosen.",
            "ASDOS",
            mahasiswa
        );
        notifikasiRepository.save(notif);

        return mahasiswaRepository.save(mahasiswa);
    }

    /**
     * Mendapatkan daftar kandidat Asdos yang sudah direkomendasikan.
     */
    public List<Mahasiswa> getKandidatAsdos() {
        return mahasiswaRepository.findByKandidatAsdosTrue();
    }

    /**
     * Mendapatkan daftar Asdos aktif.
     */
    public List<Mahasiswa> getAsdosAktif() {
        return mahasiswaRepository.findByIsAsdosTrue();
    }

    // ============================================================
    // STATISTIK DAN RANKING
    // ============================================================

    /**
     * Mendapatkan statistik ringkas untuk dashboard supervisor.
     * Mengembalikan map berisi berbagai metrik statistik.
     */
    public Map<String, Object> getStatistikDashboard(Long supervisorId) {
        Map<String, Object> statistik = new LinkedHashMap<>();

        List<SesiMentoring> sesiDiawasi = sesiRepository.findBySupervisorId(supervisorId);
        long sesiMenunggu = sesiDiawasi.stream()
            .filter(SesiMentoring::menungguPeninjauan).count();
        long sesiDivalidasi = sesiDiawasi.stream()
            .filter(SesiMentoring::sudahDivalidasi).count();
        long sesiDitolak = sesiDiawasi.stream()
            .filter(s -> s.getStatusValidasi() == StatusValidasi.DITOLAK).count();

        statistik.put("totalSesiDiawasi", sesiDiawasi.size());
        statistik.put("sesiMenungguValidasi", sesiMenunggu);
        statistik.put("sesiDivalidasi", sesiDivalidasi);
        statistik.put("sesiDitolak", sesiDitolak);
        statistik.put("totalMentorMahasiswa", mahasiswaRepository.findByIsMentorTrue().size());
        statistik.put("totalMentorSiswa", siswaRepository.findByIsMentorTrue().size());
        statistik.put("totalKandidatAsdos", mahasiswaRepository.countByKandidatAsdosTrue());

        return statistik;
    }

    /**
     * Mendapatkan daftar siswa berprestasi untuk dashboard Guru.
     */
    public List<Siswa> getSiswaBerprestasi() {
        return siswaRepository.findByAktifTrueOrderByTotalPoinProgresDesc();
    }
}
