package com.mentorpbo.repository;

import com.mentorpbo.model.Siswa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Siswa - Antarmuka akses data khusus untuk entitas Siswa.
 *
 * Menyediakan query khusus yang relevan dengan fitur pencarian mentor,
 * ranking siswa, dan filtering berdasarkan sekolah/kelas.
 */
@Repository
public interface SiswaRepository extends JpaRepository<Siswa, Long> {

    /**
     * Mencari siswa berdasarkan NISN.
     */
    Optional<Siswa> findByNisn(String nisn);

    /**
     * Mendapatkan daftar siswa yang aktif sebagai mentor.
     */
    List<Siswa> findByIsMentorTrue();

    /**
     * Mendapatkan daftar siswa mentor berdasarkan sekolah.
     */
    List<Siswa> findByIsMentorTrueAndNamaSekolah(String namaSekolah);

    /**
     * Mencari mentor siswa berdasarkan mata pelajaran keahlian (pencarian parsial).
     *
     * @param mataPelajaran kata kunci mata pelajaran
     * @return daftar siswa mentor yang cocok
     */
    @Query("SELECT s FROM Siswa s WHERE s.isMentor = true " +
           "AND LOWER(s.mataPelajaranKeahlian) LIKE LOWER(CONCAT('%', :mataPelajaran, '%'))")
    List<Siswa> cariMentorBerdasarkanMataPelajaran(@Param("mataPelajaran") String mataPelajaran);

    /**
     * Mendapatkan ranking mentor siswa berdasarkan rata-rata rating tertinggi.
     * Hanya menampilkan mentor dengan minimal 3 penilaian.
     */
    @Query("SELECT s FROM Siswa s WHERE s.isMentor = true AND s.jumlahPenilaian >= 3 " +
           "ORDER BY (s.totalRating / s.jumlahPenilaian) DESC")
    List<Siswa> getRankingMentorSiswa();

    /**
     * Mendapatkan daftar siswa berprestasi berdasarkan total poin progres.
     */
    List<Siswa> findByAktifTrueOrderByTotalPoinProgresDesc();

    /**
     * Mendapatkan daftar siswa berdasarkan sekolah tertentu.
     */
    List<Siswa> findByNamaSekolah(String namaSekolah);

    /**
     * Menghitung jumlah mentor aktif di sekolah tertentu.
     */
    long countByIsMentorTrueAndNamaSekolah(String namaSekolah);
}
