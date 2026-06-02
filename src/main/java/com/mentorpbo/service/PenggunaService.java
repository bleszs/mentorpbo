package com.mentorpbo.service;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.RolePengguna;
import com.mentorpbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

/**
 * Service PenggunaService - Lapisan logika bisnis untuk manajemen pengguna.
 *
 * Menangani:
 * - Autentikasi pengguna (login sederhana tanpa Spring Security)
 * - Registrasi pengguna baru
 * - Pencarian dan manajemen profil pengguna
 * - Statistik pengguna per role dan lingkungan
 */
@Service
@Transactional
public class PenggunaService {

    private final PenggunaRepository penggunaRepository;
    private final SiswaRepository siswaRepository;
    private final MahasiswaRepository mahasiswaRepository;
    private final GuruRepository guruRepository;
    private final DosenRepository dosenRepository;
    private final NotifikasiRepository notifikasiRepository;
    private final UserPreferencesRepository preferencesRepository;

    @Autowired
    public PenggunaService(PenggunaRepository penggunaRepository,
                           SiswaRepository siswaRepository,
                           MahasiswaRepository mahasiswaRepository,
                           GuruRepository guruRepository,
                           DosenRepository dosenRepository,
                           NotifikasiRepository notifikasiRepository,
                           UserPreferencesRepository preferencesRepository) {
        this.penggunaRepository = penggunaRepository;
        this.siswaRepository = siswaRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.guruRepository = guruRepository;
        this.dosenRepository = dosenRepository;
        this.notifikasiRepository = notifikasiRepository;
        this.preferencesRepository = preferencesRepository;
    }

    // === Autentikasi ===

    /**
     * Melakukan proses login sederhana berdasarkan email dan kata sandi.
     * Jika berhasil, memperbarui waktu terakhir login.
     *
     * @param email alamat email pengguna
     * @param kataSandi kata sandi pengguna
     * @return Optional berisi Pengguna jika autentikasi berhasil
     */
    public Optional<Pengguna> login(String email, String kataSandi) {
        Optional<Pengguna> pengguna = penggunaRepository.findByEmailAndKataSandi(email, kataSandi);
        pengguna.ifPresent(p -> {
            p.setTerakhirLogin(LocalDateTime.now());
            penggunaRepository.save(p);
        });
        return pengguna;
    }

    /**
     * Mendapatkan pengguna berdasarkan ID.
     */
    public Optional<Pengguna> getPenggunaById(Long id) {
        return penggunaRepository.findById(id);
    }

    /**
     * Mendapatkan pengguna berdasarkan email.
     */
    public Optional<Pengguna> getPenggunaByEmail(String email) {
        return penggunaRepository.findByEmail(email);
    }

    // === Registrasi ===

    /**
     * Mendaftarkan siswa baru ke dalam sistem.
     */
    public Siswa daftarSiswa(Siswa siswa) {
        if (penggunaRepository.existsByEmail(siswa.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar: " + siswa.getEmail());
        }
        return siswaRepository.save(siswa);
    }

    /**
     * Mendaftarkan mahasiswa baru ke dalam sistem.
     */
    public Mahasiswa daftarMahasiswa(Mahasiswa mahasiswa) {
        if (penggunaRepository.existsByEmail(mahasiswa.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar: " + mahasiswa.getEmail());
        }
        return mahasiswaRepository.save(mahasiswa);
    }

    /**
     * Mendaftarkan guru baru ke dalam sistem.
     */
    public Guru daftarGuru(Guru guru) {
        if (penggunaRepository.existsByEmail(guru.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar: " + guru.getEmail());
        }
        return guruRepository.save(guru);
    }

    /**
     * Mendaftarkan dosen baru ke dalam sistem.
     */
    public Dosen daftarDosen(Dosen dosen) {
        if (penggunaRepository.existsByEmail(dosen.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar: " + dosen.getEmail());
        }
        return dosenRepository.save(dosen);
    }

    // === Verifikasi Email via OTP ===

    /**
     * Generate kode OTP 6 digit untuk verifikasi email.
     * OTP berlaku selama 15 menit dan disimpan di tokenVerifikasi.
     */
    public String generateTokenVerifikasi(Pengguna pengguna) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        pengguna.setEmailVerified(false);
        pengguna.setTokenVerifikasi(otp);
        pengguna.setTokenVerifikasiExpiry(LocalDateTime.now().plusMinutes(15));
        penggunaRepository.save(pengguna);
        return otp;
    }

    /**
     * Verifikasi OTP yang dimasukkan user secara manual.
     * Mengecek kecocokan OTP dan belum kedaluwarsa (15 menit).
     *
     * @param email email pengguna
     * @param otp   kode 6 digit yang diinput user
     * @return true jika OTP valid dan belum expired
     */
    public boolean verifikasiOtp(String email, String otp) {
        Optional<Pengguna> opt = penggunaRepository.findByEmail(email);
        if (opt.isEmpty()) return false;
        Pengguna p = opt.get();
        if (p.getTokenVerifikasi() == null || !p.getTokenVerifikasi().equals(otp.trim())) return false;
        if (p.getTokenVerifikasiExpiry() == null || p.getTokenVerifikasiExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        p.setEmailVerified(true);
        p.setTokenVerifikasi(null);
        p.setTokenVerifikasiExpiry(null);
        penggunaRepository.save(p);
        return true;
    }

    /**
     * Ambil OTP yang sedang aktif untuk email tertentu (hanya untuk dev mode tampilan di halaman).
     * Return null jika tidak ada OTP aktif atau email sudah diverifikasi.
     */
    public String getOtpForDev(String email) {
        if (email == null || email.isBlank()) return null;
        return penggunaRepository.findByEmail(email)
            .filter(p -> !p.isEmailVerified())
            .filter(p -> p.getTokenVerifikasi() != null)
            .filter(p -> p.getTokenVerifikasiExpiry() != null
                      && p.getTokenVerifikasiExpiry().isAfter(LocalDateTime.now()))
            .map(Pengguna::getTokenVerifikasi)
            .orElse(null);
    }

    /**
     * Kirim ulang OTP baru ke email yang sama (digunakan di halaman verify-otp).
     * Lempar exception jika email tidak ditemukan atau sudah terverifikasi.
     *
     * @param email email pengguna
     * @return OTP baru yang sudah disimpan
     */
    public String kirimUlangOtp(String email) {
        Pengguna p = penggunaRepository.findByEmail(email)
            .orElseThrow(() -> new NoSuchElementException("Email tidak terdaftar: " + email));
        if (p.isEmailVerified()) {
            throw new IllegalStateException("Email ini sudah terverifikasi. Silakan login.");
        }
        return generateTokenVerifikasi(p);
    }

    /**
     * Validasi token lama (UUID) — dipertahankan agar link lama di inbox masih bisa dipakai.
     */
    public boolean verifikasiEmail(String token) {
        Optional<Pengguna> opt = penggunaRepository.findByTokenVerifikasi(token);
        if (opt.isEmpty()) return false;
        Pengguna p = opt.get();
        if (p.getTokenVerifikasiExpiry() == null || p.getTokenVerifikasiExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        p.setEmailVerified(true);
        p.setTokenVerifikasi(null);
        p.setTokenVerifikasiExpiry(null);
        penggunaRepository.save(p);
        return true;
    }

    // === Pencarian ===

    /**
     * Mendapatkan semua pengguna berdasarkan role tertentu.
     */
    public List<Pengguna> getPenggunaByRole(RolePengguna role) {
        return penggunaRepository.findByRole(role);
    }

    /**
     * Mencari pengguna berdasarkan nama (parsial).
     */
    public List<Pengguna> cariPenggunaByNama(String nama) {
        return penggunaRepository.findByNamaLengkapContainingIgnoreCase(nama);
    }

    // === Notifikasi ===

    /**
     * Mengirim notifikasi ke pengguna tertentu.
     */
    public Notifikasi kirimNotifikasi(String judul, String pesan, String kategori, Pengguna penerima) {
        Notifikasi notifikasi = new Notifikasi(judul, pesan, kategori, penerima);
        return notifikasiRepository.save(notifikasi);
    }

    /**
     * Mendapatkan notifikasi yang belum dibaca untuk pengguna tertentu.
     */
    public List<Notifikasi> getNotifikasiBelumDibaca(Long penggunaId) {
        return notifikasiRepository.findByPenerimaIdAndSudahDibacaFalseOrderByTanggalDibuatDesc(penggunaId);
    }

    /**
     * Menghitung jumlah notifikasi belum dibaca.
     */
    public long hitungNotifikasiBelumDibaca(Long penggunaId) {
        return notifikasiRepository.countByPenerimaIdAndSudahDibacaFalse(penggunaId);
    }

    /**
     * Menandai notifikasi sebagai sudah dibaca.
     */
    public void tandaiNotifikasiDibaca(Long notifikasiId) {
        notifikasiRepository.findById(notifikasiId).ifPresent(n -> {
            n.tandaiSudahDibaca();
            notifikasiRepository.save(n);
        });
    }

    // === Statistik ===

    /**
     * Menghitung jumlah pengguna per role.
     */
    public long hitungPenggunaByRole(RolePengguna role) {
        return penggunaRepository.countByRole(role);
    }

    /**
     * Mendapatkan semua pengguna.
     */
    public List<Pengguna> getSemuaPengguna() {
        return penggunaRepository.findAll();
    }

    // ============================================================
    // USER PREFERENCES — Pengaturan tersimpan ke H2
    // ============================================================

    /**
     * Ambil preferences pengguna. Jika belum ada, buat dengan default.
     */
    public UserPreferences getOrCreatePreferences(Long penggunaId) {
        return preferencesRepository.findByPenggunaId(penggunaId)
            .orElseGet(() -> {
                Pengguna p = penggunaRepository.findById(penggunaId)
                    .orElseThrow(() -> new NoSuchElementException("Pengguna tidak ditemukan."));
                return preferencesRepository.save(new UserPreferences(p));
            });
    }

    /**
     * Simpan preferences ke database — dipanggil dari form pengaturan.
     */
    public UserPreferences simpanPreferences(Long penggunaId, UserPreferences form) {
        UserPreferences pref = getOrCreatePreferences(penggunaId);

        if (form.getBio()            != null) pref.setBio(form.getBio());
        if (form.getPortofolioUrl()  != null) pref.setPortofolioUrl(form.getPortofolioUrl());
        if (form.getKeahlian()       != null) pref.setKeahlian(form.getKeahlian());
        if (form.getTopikKeahlian()  != null) pref.setTopikKeahlian(form.getTopikKeahlian());
        if (form.getProgramStudi()   != null) pref.setProgramStudi(form.getProgramStudi());
        if (form.getUniversitas()    != null) pref.setUniversitas(form.getUniversitas());
        if (form.getNamaSekolah()    != null) pref.setNamaSekolah(form.getNamaSekolah());

        pref.setNotifEmailAktif(form.isNotifEmailAktif());
        pref.setNotifPengingatSesi(form.isNotifPengingatSesi());
        pref.setNotifPesanBaru(form.isNotifPesanBaru());
        pref.setNotifValidasiSesi(form.isNotifValidasiSesi());
        pref.setNotifRating(form.isNotifRating());
        pref.setProfilPublik(form.isProfilPublik());
        pref.setTampilDiRanking(form.isTampilDiRanking());
        pref.setBagikanStatistik(form.isBagikanStatistik());

        return preferencesRepository.save(pref);
    }

    /**
     * Update profil pengguna (nama, bio, institusi) — sinkronisasi ke entity Pengguna.
     */
    public Pengguna updateProfil(Long penggunaId, String namaLengkap, String bio,
                                  String portofolioUrl) {
        Pengguna p = penggunaRepository.findById(penggunaId)
            .orElseThrow(() -> new NoSuchElementException("Pengguna tidak ditemukan."));

        if (namaLengkap != null && !namaLengkap.isBlank()) p.setNamaLengkap(namaLengkap.trim());
        if (bio         != null) p.setBio(bio.trim());

        // Sinkronisasi ke UserPreferences juga
        UserPreferences pref = getOrCreatePreferences(penggunaId);
        pref.setNamaLengkap(p.getNamaLengkap());
        pref.setBio(p.getBio());
        if (portofolioUrl != null) pref.setPortofolioUrl(portofolioUrl);
        preferencesRepository.save(pref);

        return penggunaRepository.save(p);
    }

    /**
     * Update data akademik langsung ke entity Mahasiswa/Siswa.
     */
    @Transactional
    public void updateDataAkademik(Long penggunaId, String programStudi, String universitas,
                                    String namaSekolah, String keahlian, String topikKeahlian) {
        Pengguna p = penggunaRepository.findById(penggunaId).orElse(null);
        if (p instanceof com.mentorpbo.model.Mahasiswa mhs) {
            if (programStudi != null && !programStudi.isBlank()) mhs.setProgramStudi(programStudi.trim());
            if (universitas  != null && !universitas.isBlank())  mhs.setUniversitas(universitas.trim());
            if (keahlian     != null) mhs.setMataKuliahKeahlian(keahlian.trim());
            if (topikKeahlian!= null) mhs.setTopikKeahlian(topikKeahlian.trim());
            penggunaRepository.save(mhs);
        } else if (p instanceof com.mentorpbo.model.Siswa siswa) {
            if (namaSekolah  != null && !namaSekolah.isBlank())  siswa.setNamaSekolah(namaSekolah.trim());
            if (keahlian     != null) siswa.setMataPelajaranKeahlian(keahlian.trim());
            penggunaRepository.save(siswa);
        }
    }

    /**
     * Ambil daftar notifikasi (max 10 terbaru) untuk user tertentu.
     */
    public List<Notifikasi> getDaftarNotifikasi(Long penggunaId) {
        return notifikasiRepository
            .findByPenerimaIdOrderByTanggalDibuatDesc(penggunaId)
            .stream().limit(10).toList();
    }

    /**
     * Tandai semua notifikasi belum dibaca sebagai sudah dibaca.
     */
    public void tandaiSemuaNotifikasiDibaca(Long penggunaId) {
        notifikasiRepository
            .findByPenerimaIdAndSudahDibacaFalseOrderByTanggalDibuatDesc(penggunaId)
            .forEach(n -> {
                n.tandaiSudahDibaca();
                notifikasiRepository.save(n);
            });
    }
}
