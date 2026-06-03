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
     * Supervisor mulai meninjau laporan sesi: BELUM_DITINJAU → SEDANG_DITINJAU.
     * Mengaktifkan state yang sebelumnya dead (tidak pernah di-set).
     * Memberi sinyal kepada mentor/mentee bahwa laporan sedang diperiksa.
     */
    public SesiMentoring mulaiTinjauSesi(Long sesiId, Long supervisorId) {
        SesiMentoring sesi = getSesiAtauLempar(sesiId);
        verifikasiScopeAtauLempar(sesiId, supervisorId);

        if (sesi.getStatusSesi() != StatusSesi.SELESAI) {
            throw new IllegalStateException("Hanya sesi SELESAI yang bisa ditinjau.");
        }
        if (sesi.getStatusValidasi() != StatusValidasi.BELUM_DITINJAU) {
            throw new IllegalStateException(
                "Sesi sudah dalam status: " + sesi.getStatusValidasi().getLabel());
        }

        sesi.setStatusValidasi(StatusValidasi.SEDANG_DITINJAU);

        kirimNotifikasi("Laporan Sedang Ditinjau",
            "Supervisor sedang meninjau laporan sesi \"" + sesi.getTopikPembahasan() + "\".",
            "VALIDASI", sesi.getMentor());

        return sesiRepository.save(sesi);
    }

    /**
     * Supervisor memvalidasi laporan sesi: BELUM_DITINJAU / SEDANG_DITINJAU → DIVALIDASI.
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
            stat.put("totalKandidatAsdos", 0L); // tidak relevan untuk Guru
        } else if (supervisor instanceof Dosen dosen) {
            List<Mahasiswa> mentorProdi = mahasiswaRepository
                .findByIsMentorTrueAndProgramStudi(dosen.getProgramStudi());
            stat.put("totalMentorDiScope", mentorProdi.size());
            stat.put("labelScope",         "Mentor Mahasiswa di " + dosen.getProgramStudi());
            // Kandidat asdos hanya dari prodi Dosen ini (scoped)
            long kandidatProdi = mahasiswaRepository.findByKandidatAsdosTrue().stream()
                .filter(m -> dosenScopeCocok(dosen, m)).count();
            stat.put("totalKandidatAsdos", kandidatProdi);
        }

        return stat;
    }

    /**
     * Siswa berprestasi berdasarkan scope Guru: hanya dari sekolah Guru tersebut.
     * Supervisor yang bukan Guru mendapat list kosong (tidak relevan).
     *
     * @param supervisorId id Guru yang sedang login
     */
    public List<Siswa> getSiswaBerprestasi(Long supervisorId) {
        Pengguna supervisor = penggunaRepository.findById(supervisorId).orElse(null);
        if (supervisor instanceof Guru guru && guru.getNamaSekolah() != null) {
            return siswaRepository.findByAktifTrueAndNamaSekolahOrderByTotalPoinProgresDesc(
                guru.getNamaSekolah());
        }
        return List.of();
    }

    // ============================================================
    // KANDIDAT ASDOS (KHUSUS DOSEN)
    // ============================================================

    /**
     * Mencari mahasiswa kandidat Asisten Dosen, difilter ke program studi Dosen yang login.
     * Kriteria default: IPK >= 3.0, Rating >= 4.0, Min 5 sesi, Min 5 penilaian.
     */
    public List<Mahasiswa> cariKandidatAsdos(Long dosenId) {
        return cariKandidatAsdos(dosenId, 3.0, 5, 5, 4.0);
    }

    public List<Mahasiswa> cariKandidatAsdos(Long dosenId, double minIpk, int minPenilaian,
                                              int minSesi, double minRating) {
        List<Mahasiswa> semua = mahasiswaRepository.cariKandidatAsdos(minIpk, minPenilaian, minSesi, minRating);
        Pengguna supervisor = penggunaRepository.findById(dosenId).orElse(null);
        if (supervisor instanceof Dosen dosen) {
            return semua.stream().filter(m -> dosenScopeCocok(dosen, m)).toList();
        }
        return semua;
    }

    /**
     * Dosen merekomendasikan mahasiswa sebagai kandidat Asdos.
     * Memverifikasi mahasiswa berada dalam scope program studi Dosen
     * dan memenuhi syarat kelayakan.
     */
    public Mahasiswa rekomendasikanSebagaiAsdos(Long mahasiswaId, Long dosenId) {
        Mahasiswa mhs = mahasiswaRepository.findById(mahasiswaId)
            .orElseThrow(() -> new NoSuchElementException("Mahasiswa tidak ditemukan."));

        // Verifikasi scope: Dosen hanya merekomendasikan mahasiswa di prodinya
        Pengguna supervisorRaw = penggunaRepository.findById(dosenId).orElse(null);
        if (supervisorRaw instanceof Dosen dosen && !dosenScopeCocok(dosen, mhs)) {
            throw new IllegalStateException(
                "Mahasiswa ini bukan dari program studi Anda (" +
                dosen.getProgramStudi() + "). Tidak bisa direkomendasikan.");
        }

        if (!mhs.memenuhiSyaratAsdos()) {
            double skor = mhs.hitungSkorKelayakanAsdos();
            throw new IllegalStateException(
                "Mahasiswa belum memenuhi syarat Asdos. Skor: " + skor +
                "/100. Syarat: IPK >= 3.0, Rating >= 4.0, Min 5 sesi, Min 5 ulasan.");
        }

        mhs.setKandidatAsdos(true);

        dosenRepository.findById(dosenId).ifPresent(dosen -> {
            dosen.catatRekomendasiAsdos();
            dosenRepository.save(dosen);
        });

        kirimNotifikasi(
            "Rekomendasi Asisten Dosen",
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
            "Pengangkatan Resmi Asisten Dosen",
            "Selamat! Anda resmi diangkat sebagai Asisten Dosen. " +
                "Status Anda telah diperbarui di sistem.",
            "ASDOS", mhs);

        return mahasiswaRepository.save(mhs);
    }

    /** Kandidat asdos scoped ke program studi Dosen yang login. */
    public List<Mahasiswa> getKandidatAsdos(Long dosenId) {
        Pengguna supervisor = penggunaRepository.findById(dosenId).orElse(null);
        List<Mahasiswa> semua = mahasiswaRepository.findByKandidatAsdosTrue();
        if (supervisor instanceof Dosen dosen) {
            return semua.stream().filter(m -> dosenScopeCocok(dosen, m)).toList();
        }
        return semua;
    }

    /** Asdos aktif scoped ke program studi Dosen yang login. */
    public List<Mahasiswa> getAsdosAktif(Long dosenId) {
        Pengguna supervisor = penggunaRepository.findById(dosenId).orElse(null);
        List<Mahasiswa> semua = mahasiswaRepository.findByIsAsdosTrue();
        if (supervisor instanceof Dosen dosen) {
            return semua.stream().filter(m -> dosenScopeCocok(dosen, m)).toList();
        }
        return semua;
    }

    /** Tanpa scope - backward-compat. */
    public List<Mahasiswa> getKandidatAsdos() { return mahasiswaRepository.findByKandidatAsdosTrue(); }
    public List<Mahasiswa> getAsdosAktif()    { return mahasiswaRepository.findByIsAsdosTrue(); }

    // ============================================================
    // PERSETUJUAN PENDAFTARAN MENTOR (Verifiable untuk akun mentor)
    // ============================================================

    /**
     * Daftar mentor yang menunggu persetujuan, DIFILTER berdasarkan scope supervisor:
     * - Guru  → Siswa mentor PENDING dari sekolah yang sama
     * - Dosen → Mahasiswa mentor PENDING dari program studi (atau universitas) yang sama
     *
     * Hasil dinormalisasi ke Map agar template tidak perlu tahu tipe konkret
     * (Siswa vs Mahasiswa) dan terhindar dari error akses getter yang berbeda.
     */
    public List<Map<String, Object>> getMentorPendingDalamScope(Long supervisorId) {
        Pengguna supervisor = penggunaRepository.findById(supervisorId).orElse(null);
        if (supervisor == null) return List.of();

        List<Map<String, Object>> hasil = new ArrayList<>();
        if (supervisor instanceof Guru guru) {
            for (Siswa s : siswaRepository.findByIsMentorTrueAndStatusValidasiMentor(StatusValidasi.BELUM_DITINJAU)) {
                if (guru.getNamaSekolah() != null && guru.getNamaSekolah().equals(s.getNamaSekolah())) {
                    hasil.add(mentorRingkas(s.getId(), s.getNamaLengkap(), s.getEmail(),
                        s.getNamaSekolah(), s.getMataPelajaranKeahlian(), s.getBio()));
                }
            }
        } else if (supervisor instanceof Dosen dosen) {
            for (Mahasiswa m : mahasiswaRepository.findByIsMentorTrueAndStatusValidasiMentor(StatusValidasi.BELUM_DITINJAU)) {
                if (dosenScopeCocok(dosen, m)) {
                    hasil.add(mentorRingkas(m.getId(), m.getNamaLengkap(), m.getEmail(),
                        m.getProgramStudi(), m.getMataKuliahKeahlian(), m.getBio()));
                }
            }
        }
        return hasil;
    }

    /**
     * Kecocokan scope Dosen → Mahasiswa.
     * Jika Dosen memiliki program studi, gunakan kesamaan program studi (ketat).
     * Hanya jika program studi Dosen kosong, fallback ke kesamaan universitas.
     */
    private boolean dosenScopeCocok(Dosen dosen, Mahasiswa m) {
        if (dosen.getProgramStudi() != null && !dosen.getProgramStudi().isBlank()) {
            return dosen.getProgramStudi().equals(m.getProgramStudi());
        }
        return Objects.equals(dosen.getUniversitas(), m.getUniversitas());
    }

    private Map<String, Object> mentorRingkas(Long id, String nama, String email,
                                              String institusi, String keahlian, String bio) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nama", nama);
        m.put("email", email);
        m.put("institusi", institusi);
        m.put("keahlian", keahlian);
        m.put("bio", bio);
        return m;
    }

    /**
     * Supervisor menyetujui pendaftaran mentor: statusValidasiMentor → DIVALIDASI.
     * Hanya boleh untuk mentor di dalam scope institusi supervisor.
     */
    public void setujuiMentor(Long mentorId, Long supervisorId) {
        Pengguna supervisor = penggunaRepository.findById(supervisorId)
            .orElseThrow(() -> new IllegalStateException("Supervisor tidak ditemukan."));

        if (supervisor instanceof Guru guru) {
            Siswa s = siswaRepository.findById(mentorId)
                .orElseThrow(() -> new NoSuchElementException("Mentor siswa tidak ditemukan."));
            if (!Objects.equals(guru.getNamaSekolah(), s.getNamaSekolah())) {
                throw new IllegalStateException("Mentor ini berada di luar scope sekolah Anda.");
            }
            s.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
            siswaRepository.save(s);
            kirimNotifikasi("Pendaftaran Mentor Disetujui ✓",
                "Selamat! Pendaftaran Anda sebagai mentor telah disetujui. Anda kini bisa menerima mentee.",
                "MENTOR", s);
        } else if (supervisor instanceof Dosen dosen) {
            Mahasiswa m = mahasiswaRepository.findById(mentorId)
                .orElseThrow(() -> new NoSuchElementException("Mentor mahasiswa tidak ditemukan."));
            if (!dosenScopeCocok(dosen, m)) {
                throw new IllegalStateException("Mentor ini berada di luar scope program studi Anda.");
            }
            m.setStatusValidasiMentor(StatusValidasi.DIVALIDASI);
            mahasiswaRepository.save(m);
            kirimNotifikasi("Pendaftaran Mentor Disetujui ✓",
                "Selamat! Pendaftaran Anda sebagai mentor telah disetujui. Anda kini bisa menerima mentee.",
                "MENTOR", m);
        } else {
            throw new IllegalStateException("Hanya Guru/Dosen yang dapat menyetujui pendaftaran mentor.");
        }
        updateKonterValidasi(supervisorId);
    }

    /**
     * Supervisor menolak pendaftaran mentor: statusValidasiMentor → DITOLAK.
     * Hanya boleh untuk mentor di dalam scope institusi supervisor.
     */
    public void tolakMentor(Long mentorId, Long supervisorId, String alasan) {
        if (alasan == null || alasan.isBlank()) {
            throw new IllegalArgumentException("Alasan penolakan wajib diisi.");
        }
        Pengguna supervisor = penggunaRepository.findById(supervisorId)
            .orElseThrow(() -> new IllegalStateException("Supervisor tidak ditemukan."));

        if (supervisor instanceof Guru guru) {
            Siswa s = siswaRepository.findById(mentorId)
                .orElseThrow(() -> new NoSuchElementException("Mentor siswa tidak ditemukan."));
            if (!Objects.equals(guru.getNamaSekolah(), s.getNamaSekolah())) {
                throw new IllegalStateException("Mentor ini berada di luar scope sekolah Anda.");
            }
            s.setStatusValidasiMentor(StatusValidasi.DITOLAK);
            siswaRepository.save(s);
            kirimNotifikasi("Pendaftaran Mentor Ditolak",
                "Maaf, pendaftaran Anda sebagai mentor ditolak. Alasan: " + alasan,
                "MENTOR", s);
        } else if (supervisor instanceof Dosen dosen) {
            Mahasiswa m = mahasiswaRepository.findById(mentorId)
                .orElseThrow(() -> new NoSuchElementException("Mentor mahasiswa tidak ditemukan."));
            if (!dosenScopeCocok(dosen, m)) {
                throw new IllegalStateException("Mentor ini berada di luar scope program studi Anda.");
            }
            m.setStatusValidasiMentor(StatusValidasi.DITOLAK);
            mahasiswaRepository.save(m);
            kirimNotifikasi("Pendaftaran Mentor Ditolak",
                "Maaf, pendaftaran Anda sebagai mentor ditolak. Alasan: " + alasan,
                "MENTOR", m);
        } else {
            throw new IllegalStateException("Hanya Guru/Dosen yang dapat menolak pendaftaran mentor.");
        }
    }

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
