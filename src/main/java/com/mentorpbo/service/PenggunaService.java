package com.mentorpbo.service;

import com.mentorpbo.model.*;
import com.mentorpbo.model.enums.RolePengguna;
import com.mentorpbo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    public PenggunaService(PenggunaRepository penggunaRepository,
                           SiswaRepository siswaRepository,
                           MahasiswaRepository mahasiswaRepository,
                           GuruRepository guruRepository,
                           DosenRepository dosenRepository,
                           NotifikasiRepository notifikasiRepository) {
        this.penggunaRepository = penggunaRepository;
        this.siswaRepository = siswaRepository;
        this.mahasiswaRepository = mahasiswaRepository;
        this.guruRepository = guruRepository;
        this.dosenRepository = dosenRepository;
        this.notifikasiRepository = notifikasiRepository;
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
}
