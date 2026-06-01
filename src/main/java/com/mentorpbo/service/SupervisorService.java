package com.mentorpbo.service;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.StatusSesi;
import com.mentorpbo.model.enums.StatusValidasi;
import com.mentorpbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SupervisorService — Logika bisnis khusus untuk peran Guru dan Dosen.
 *
 * Fitur utama:
 * 1. Validasi laporan akademik (interface Verifiable)
 * 2. Pemfilteran data berdasarkan scope institusi — PENTING:
 *    - Guru   hanya melihat sesi dari Siswa di sekolahnya
 *    - Dosen  hanya melihat sesi dari Mahasiswa di universitasnya (dan program studinya)
 * 3. Pencarian kandidat Asisten Dosen (khusus Dosen)
 * 4. Statistik dashboard yang sudah difilter berdasarkan scope
 */
@Service
@Transactional
public class SupervisorService {

    private final SesiMentoringRepository sesiRepository;
    private final MahasiswaRepository     mahasiswaRepository;
    private final SiswaRepository         siswaRepository;
    private final GuruRepository          guruRepository;
    private final DosenRepository         dosenRepository;
    private final NotifikasiRepository    notifikasiRepository;
    private final PenggunaRepository      penggunaRepository;

    @Autowired
    public SupervisorService(SesiMentoringRepository sesiRepository,
                             MahasiswaRepository mahasiswaRepository,
                             SiswaRepository siswaRepository,
                             GuruRepository guruRepository,
                             DosenRepository dosenRepository,
                             NotifikasiRepository notifikasiRepository,
                             PenggunaRepository penggunaRepository) {
        this.sesiRepository       = sesiRepository;
        this.mahasiswaRepository  = mahasiswaRepository;
        this.siswaRepository      = siswaRepository;
        this.guruRepository       = guruRepository;
        this.dosenRepository      = dosenRepository;
        this.notifikasiRepository = notifikasiRepository;
        this.penggunaRepository   = penggunaRepository;
    }

    // ============================================================
    // SCOPE FILTERING — PEMISAHAN DATA BERDASARKAN INSTITUSI
    // ============================================================

    /**
     * Mendapatkan semua sesi dalam scope institusi supervisor.
     *
     * Aturan pemisahan data (Scope Filtering):
     * - Guru  → hanya sesi di mana mentor adalah Siswa dari sekolah yang sama
     * - Dosen → hanya sesi di mana mentor adalah Mahasiswa dari universitas yang sama
     *            (dipersempit lagi ke program studi jika memungkinkan)
     *
     * Ini mencegah Guru/Dosen mengintip aktivitas di institusi lain.
     *
     * @param supervisorId ID pengguna supervisor (Guru atau Dosen)
     * @return daftar sesi yang berada dalam scope institusi supervisor
     */
    public List<SesiMentoring> getSesiDalamScopeInstitusi(Long supervisorId) {
        Pengguna supervisor = penggunaRepository.findById(supervisorId).orElse(null);
        if (supervisor == null) return List.of();

        // Ambil semua sesi yang punya supervisor ini (sudah di-assign saat pembuatan)
        List<SesiMentoring> sesiLangsung = sesiRepository.findBySupervisorId(supervisorId);

        // Gabungkan dengan sesi yang cocok berdasarkan institusi (supervisor belum diset)
        List<SesiMentoring> sesiByInstitusi = getSesiByInstitusiSupervisor(supervisor);

        // Gabungkan, hilangkan duplikat berdasarkan ID
        Set<Long> sudahAda = new HashSet<>();
        List<SesiMentoring> hasil = new ArrayList<>();
        for (SesiMentoring s : sesiLangsung)    { if (sudahAda.add(s.getId())) hasil.add(s); }
        for (SesiMentoring s : sesiByInstitusi) { if (sudahAda.add(s.getId())) hasil.add(s); }

        return hasil;
    }

    /**
     * Mendapatkan laporan akademik yang menunggu validasi, dalam scope supervisor.
     * Hanya menampilkan sesi yang sudah SELESAI dan belum/sedang ditinjau.
     */
    public List<SesiMentoring> getLaporanMenungguValidasi(Long supervisorId) {
        return getSesiDalamScopeInstitusi(supervisorId).stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
            .filter(s -> s.getStatusValidasi() == StatusValidasi.BELUM_DITINJAU
                      || s.getStatusValidasi() == StatusValidasi.SEDANG_DITINJAU)
            .sorted(Comparator.comparing(SesiMentoring::getTanggalDibuat).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Mendapatkan sesi yang menunggu validasi (alias dari getLaporanMenungguValidasi).
     * Digunakan oleh controller sebagai nama alternatif yang lebih deskriptif.
     */
    public List<SesiMentoring> getSesiMenungguValidasi(Long supervisorId) {
        return getLaporanMenungguValidasi(supervisorId);
    }

    /**
     * Mendapatkan semua sesi yang diawasi (termasuk yang sudah divalidasi/ditolak).
     */
    public List<SesiMentoring> getSesiDiawasi(Long supervisorId) {
        return getSesiDalamScopeInstitusi(supervisorId);
    }

    // ============================================================
    // VALIDASI LAPORAN (Interface Verifiable)
    // ============================================================

    /**
     * Supervisor memvalidasi laporan sesi: BELUM_DITINJAU → DIVALIDASI.
     *
     * Demonstrasi POLIMORFISME via interface Verifiable:
     * sesi.validasi() dipanggil pada tipe abstrak SesiMentoring,
     * implementasi konkret ada di class turunan.
     *
     * Validasi: supervisor harus berada dalam scope yang sama dengan sesi.
     */
    public SesiMentoring validasiSesi(Long sesiId, Long supervisorId, String catatan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);
        verifikasiScopeAtauLempar(sesiId, supervisorId);

        if (sesi.getStatusSesi() != StatusSesi.SELESAI) {
            throw new IllegalStateException(
                "Hanya sesi yang sudah SELESAI yang bisa divalidasi.");
        }
        if (sesi.getStatusValidasi() == StatusValidasi.DIVALIDASI) {
            throw new IllegalStateException("Sesi ini sudah divalidasi sebelumnya.");
        }

        // Memanggil method dari interface Verifiable — polimorfis
        sesi.validasi(supervisorId, catatan);

        // Catat di profil supervisor
        updateKonterValidasi(supervisorId);

        // Notifikasi ke mentor dan mentee
        kirimNotifikasi("Laporan Divalidasi ✓",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah divalidasi oleh supervisor. "
                + (catatan != null && !catatan.isBlank() ? "Catatan: " + catatan : ""),
            "VALIDASI", sesi.getMentor());
        kirimNotifikasi("Laporan Sesi Anda Divalidasi",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" telah resmi divalidasi.",
            "VALIDASI", sesi.getMentee());

        return sesiRepository.save(sesi);
    }

    /**
     * Supervisor menolak laporan sesi: status → DITOLAK.
     * Mentor mendapat notifikasi dengan alasan penolakan.
     */
    public SesiMentoring tolakValidasiSesi(Long sesiId, Long supervisorId, String alasan) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);
        verifikasiScopeAtauLempar(sesiId, supervisorId);

        if (alasan == null || alasan.isBlank()) {
            throw new IllegalArgumentException("Alasan penolakan wajib diisi.");
        }

        // Delegasi ke interface Verifiable
        sesi.tolakValidasi(supervisorId, alasan);

        kirimNotifikasi("Laporan Ditolak — Perlu Perbaikan",
            "Sesi \"" + sesi.getTopikPembahasan() + "\" ditolak oleh supervisor. " +
                "Alasan: " + alasan,
            "VALIDASI", sesi.getMentor());

        return sesiRepository.save(sesi);
    }

    // ============================================================
    // STATISTIK DASHBOARD (SCOPE-FILTERED)
    // ============================================================

    /**
     * Statistik dashboard supervisor, difilter berdasarkan scope institusinya.
     * Guru tidak akan melihat angka dari sekolah lain, begitu pula Dosen.
     */
    public Map<String, Object> getStatistikDashboard(Long supervisorId) {
        List<SesiMentoring> scopeSesi = getSesiDalamScopeInstitusi(supervisorId);
        Pengguna supervisor = penggunaRepository.findById(supervisorId).orElse(null);

        long menungguValidasi = scopeSesi.stream()
            .filter(s -> s.getStatusSesi() == StatusSesi.SELESAI)
            .filter(s -> s.getStatusValidasi() == StatusValidasi.BELUM_DITINJAU
                      || s.getStatusValidasi() == StatusValidasi.SEDANG_DITINJAU)
            .count();

        long sudahDivalidasi = scopeSesi.stream()
            .filter(SesiMentoring::sudahDivalidasi).count();

        long ditolak = scopeSesi.stream()
            .filter(s -> s.getStatusValidasi() == StatusValidasi.DITOLAK).count();

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("totalSesiDiawasi",    scopeSesi.size());
        stat.put("sesiMenungguValidasi", menungguValidasi);
        stat.put("sesiDivalidasi",       sudahDivalidasi);
        stat.put("sesiDitolak",          ditolak);

        // Statistik mentor berdasarkan scope
        if (supervisor instanceof Guru guru) {
            List<Siswa> mentorSekolah = siswaRepository
                .findByIsMentorTrueAndNamaSekolah(guru.getNamaSekolah());
            stat.put("totalMentorDiScope", mentorSekolah.size());
            stat.put("labelScope",         "Mentor Siswa di " + guru.getNamaSekolah());
            stat.put("totalKandidatAsdos", 0L);
        } else if (supervisor instanceof Dosen dosen) {
            List<Mahasiswa> mentorProdi = mahasiswaRepository
                .findByIsMentorTrueAndProgramStudi(dosen.getProgramStudi());
            stat.put("totalMentorDiScope", mentorProdi.size());
            stat.put("labelScope",         "Mentor Mahasiswa di " + dosen.getProgramStudi());
            stat.put("totalKandidatAsdos", mahasiswaRepository.countByKandidatAsdosTrue());
        }

        return stat;
    }

    /**
     * Siswa/mahasiswa berprestasi dalam scope supervisor.
     */
    public List<Siswa> getSiswaBerprestasi() {
        return siswaRepository.findByAktifTrueOrderByTotalPoinProgresDesc();
    }

    // ============================================================
    // KANDIDAT ASDOS (KHUSUS DOSEN)
    // ============================================================

    /**
     * Mencari mahasiswa "bibit unggul" yang layak menjadi Asisten Dosen.
     * Kriteria default: IPK ≥ 3.0, Rating ≥ 4.0, Min 5 sesi, Min 5 penilaian.
     */
    public List<Mahasiswa> cariKandidatAsdos() {
        return cariKandidatAsdos(3.0, 5, 5, 4.0);
    }

    public List<Mahasiswa> cariKandidatAsdos(double minIpk, int minPenilaian,
                                              int minSesi, double minRating) {
        return mahasiswaRepository.cariKandidatAsdos(minIpk, minPenilaian, minSesi, minRating);
    }

    /**
     * Dosen merekomendasikan mahasiswa sebagai kandidat Asdos.
     * Sistem mengecek kelayakan via memenuhiSyaratAsdos() di entity Mahasiswa.
     */
    public Mahasiswa rekomendasikanSebagaiAsdos(Long mahasiswaId, Long dosenId) {
        Mahasiswa mhs = mahasiswaRepository.findById(mahasiswaId)
            .orElseThrow(() -> new NoSuchElementException("Mahasiswa tidak ditemukan."));

        if (!mhs.memenuhiSyaratAsdos()) {
            double skor = mhs.hitungSkorKelayakanAsdos();
            throw new IllegalStateException(
                "Mahasiswa belum memenuhi syarat Asdos. Skor kelayakan saat ini: " + skor +
                "/100. Syarat: IPK ≥ 3.0, Rating ≥ 4.0, Min 5 sesi, Min 5 ulasan.");
        }

        mhs.setKandidatAsdos(true);

        dosenRepository.findById(dosenId).ifPresent(dosen -> {
            dosen.catatRekomendasiAsdos();
            dosenRepository.save(dosen);
        });

        kirimNotifikasi(
            "Rekomendasi Asisten Dosen 🎓",
            "Selamat! Anda telah direkomendasikan sebagai kandidat Asisten Dosen " +
                "berdasarkan prestasi akademik Anda.",
            "ASDOS", mhs);

        return mahasiswaRepository.save(mhs);
    }

    /** Mengangkat mahasiswa menjadi Asisten Dosen resmi. */
    public Mahasiswa angkatSebagaiAsdos(Long mahasiswaId) {
        Mahasiswa mhs = mahasiswaRepository.findById(mahasiswaId)
            .orElseThrow(() -> new NoSuchElementException("Mahasiswa tidak ditemukan."));

        if (!mhs.isKandidatAsdos()) {
            throw new IllegalStateException(
                "Mahasiswa harus direkomendasikan terlebih dahulu sebelum diangkat.");
        }

        mhs.setAsdos(true);
        kirimNotifikasi(
            "Pengangkatan Resmi Asisten Dosen 🎉",
            "Selamat! Anda resmi diangkat sebagai Asisten Dosen. " +
                "Status Anda telah diperbarui di sistem.",
            "ASDOS", mhs);

        return mahasiswaRepository.save(mhs);
    }

    public List<Mahasiswa> getKandidatAsdos() { return mahasiswaRepository.findByKandidatAsdosTrue(); }
    public List<Mahasiswa> getAsdosAktif()    { return mahasiswaRepository.findByIsAsdosTrue(); }

    // ============================================================
    // HELPER PRIVATE
    // ============================================================

    /**
     * Mendapatkan sesi berdasarkan kecocokan institusi supervisor, terlepas dari
     * apakah supervisor sudah di-assign di field supervisor sesi.
     *
     * Logika:
     * - Guru   → sesi di mana mentor adalah Siswa dari sekolah yang sama
     * - Dosen  → sesi di mana mentor adalah Mahasiswa dari universitas yang sama
     */
    private List<SesiMentoring> getSesiByInstitusiSupervisor(Pengguna supervisor) {
        if (supervisor instanceof Guru guru) {
            String namaSekolah = guru.getNamaSekolah();
            // Ambil semua Siswa dari sekolah ini, lalu kumpulkan sesi mereka sebagai mentor
            return siswaRepository.findByNamaSekolah(namaSekolah).stream()
                .flatMap(siswa -> sesiRepository.findByMentorId(siswa.getId()).stream())
                .collect(Collectors.toList());

        } else if (supervisor instanceof Dosen dosen) {
            String universitas  = dosen.getUniversitas();
            String programStudi = dosen.getProgramStudi();
            // Prioritaskan filter berdasarkan program studi, fallback ke universitas
            List<Mahasiswa> mahasiswaList = mahasiswaRepository.findByProgramStudi(programStudi);
            if (mahasiswaList.isEmpty()) {
                mahasiswaList = mahasiswaRepository.findByUniversitas(universitas);
            }
            return mahasiswaList.stream()
                .flatMap(mhs -> sesiRepository.findByMentorId(mhs.getId()).stream())
                .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Memverifikasi bahwa supervisor berwenang melihat/memvalidasi sesi ini.
     * Lempar exception jika sesi berada di luar scope institusi supervisor.
     */
    private void verifikasiScopeAtauLempar(Long sesiId, Long supervisorId) {
        Pengguna supervisor = penggunaRepository.findById(supervisorId).orElse(null);
        if (supervisor == null) {
            throw new IllegalStateException("Supervisor tidak ditemukan.");
        }

        // Supervisor yang sudah di-assign langsung boleh validasi tanpa cek scope lanjut
        SesiMentoring sesi = getSesiAtauLempar(sesiId);
        if (sesi.getSupervisor() != null && sesi.getSupervisor().getId().equals(supervisorId)) {
            return;
        }

        // Cek berdasarkan kecocokan institusi
        boolean dalamScope = getSesiByInstitusiSupervisor(supervisor).stream()
            .anyMatch(s -> s.getId().equals(sesiId));

        if (!dalamScope) {
            throw new IllegalStateException(
                "Anda tidak berwenang memvalidasi sesi ini karena berada di luar scope institusi Anda.");
        }
    }

    /** Menambah counter validasi di profil Guru atau Dosen. */
    private void updateKonterValidasi(Long supervisorId) {
        penggunaRepository.findById(supervisorId).ifPresent(s -> {
            if (s instanceof Guru guru) {
                guru.catatValidasi();
                guruRepository.save(guru);
            } else if (s instanceof Dosen dosen) {
                dosen.catatValidasi();
                dosenRepository.save(dosen);
            }
        });
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
