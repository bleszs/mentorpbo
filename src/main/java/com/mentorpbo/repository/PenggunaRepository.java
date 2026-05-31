package com.mentorpbo.repository;

import com.mentorpbo.model.Pengguna;
import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Pengguna - Antarmuka akses data untuk entitas Pengguna dan turunannya.
 *
 * Menggunakan Spring Data JPA yang secara otomatis menghasilkan implementasi
 * query berdasarkan konvensi penamaan method (derived query methods).
 */
@Repository
public interface PenggunaRepository extends JpaRepository<Pengguna, Long> {

    /**
     * Mencari pengguna berdasarkan alamat email (untuk proses login).
     *
     * @param email alamat email pengguna
     * @return Optional berisi Pengguna jika ditemukan
     */
    Optional<Pengguna> findByEmail(String email);

    /**
     * Mencari pengguna berdasarkan email dan kata sandi (autentikasi sederhana).
     *
     * @param email alamat email
     * @param kataSandi kata sandi
     * @return Optional berisi Pengguna jika kredensial valid
     */
    Optional<Pengguna> findByEmailAndKataSandi(String email, String kataSandi);

    /**
     * Mendapatkan daftar pengguna berdasarkan role tertentu.
     *
     * @param role peran pengguna
     * @return daftar pengguna dengan role tersebut
     */
    List<Pengguna> findByRole(RolePengguna role);

    /**
     * Mendapatkan daftar pengguna berdasarkan lingkungan belajar.
     *
     * @param lingkungan lingkungan belajar (SEKOLAH/KAMPUS)
     * @return daftar pengguna di lingkungan tersebut
     */
    List<Pengguna> findByLingkungan(LingkunganBelajar lingkungan);

    /**
     * Mendapatkan daftar pengguna aktif berdasarkan role.
     *
     * @param role peran pengguna
     * @param aktif status aktif
     * @return daftar pengguna aktif
     */
    List<Pengguna> findByRoleAndAktif(RolePengguna role, boolean aktif);

    /**
     * Mencari pengguna berdasarkan nama (pencarian parsial, case insensitive).
     *
     * @param nama kata kunci pencarian nama
     * @return daftar pengguna yang cocok
     */
    List<Pengguna> findByNamaLengkapContainingIgnoreCase(String nama);

    /**
     * Mengecek apakah email sudah terdaftar di sistem.
     *
     * @param email alamat email yang diperiksa
     * @return true jika email sudah ada
     */
    boolean existsByEmail(String email);

    /**
     * Menghitung jumlah pengguna berdasarkan role.
     *
     * @param role peran pengguna
     * @return jumlah pengguna
     */
    long countByRole(RolePengguna role);

    /**
     * Menghitung jumlah pengguna aktif berdasarkan lingkungan.
     *
     * @param lingkungan lingkungan belajar
     * @param aktif status aktif
     * @return jumlah pengguna aktif
     */
    long countByLingkunganAndAktif(LingkunganBelajar lingkungan, boolean aktif);
}
