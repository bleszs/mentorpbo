package com.mentorpbo.repository;

import com.mentorpbo.model.Mahasiswa;
import com.mentorpbo.model.enums.StatusValidasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Mahasiswa - Antarmuka akses data khusus untuk entitas Mahasiswa.
 *
 * Menyediakan query khusus untuk fitur pencarian mentor kampus,
 * ranking mahasiswa, dan pencarian kandidat Asisten Dosen (Asdos).
 */
@Repository
public interface MahasiswaRepository extends JpaRepository<Mahasiswa, Long> {

    /**
     * Mencari mahasiswa berdasarkan NIM.
     */
    Optional<Mahasiswa> findByNim(String nim);

    /**
     * Mendapatkan daftar mahasiswa yang aktif sebagai mentor.
     */
    List<Mahasiswa> findByIsMentorTrue();

    /**
     * Mendapatkan daftar mahasiswa mentor di program studi tertentu.
     */
    List<Mahasiswa> findByIsMentorTrueAndProgramStudi(String programStudi);

    /**
     * Mendapatkan daftar mahasiswa mentor berdasarkan status validasi mentor
     * (mis. BELUM_DITINJAU untuk antrean persetujuan supervisor).
     */
    List<Mahasiswa> findByIsMentorTrueAndStatusValidasiMentor(StatusValidasi statusValidasiMentor);

    /**
     * Mencari mentor mahasiswa berdasarkan mata kuliah keahlian (pencarian parsial).
     *
     * @param mataKuliah kata kunci mata kuliah
     * @return daftar mahasiswa mentor yang cocok
     */
    @Query("SELECT m FROM Mahasiswa m WHERE m.isMentor = true " +
           "AND LOWER(m.mataKuliahKeahlian) LIKE LOWER(CONCAT('%', :mataKuliah, '%'))")
    List<Mahasiswa> cariMentorBerdasarkanMataKuliah(@Param("mataKuliah") String mataKuliah);

    /**
     * Mencari mentor mahasiswa berdasarkan topik keahlian (pencarian parsial).
     *
     * @param topik kata kunci topik
     * @return daftar mahasiswa mentor yang cocok
     */
    @Query("SELECT m FROM Mahasiswa m WHERE m.isMentor = true " +
           "AND LOWER(m.topikKeahlian) LIKE LOWER(CONCAT('%', :topik, '%'))")
    List<Mahasiswa> cariMentorBerdasarkanTopik(@Param("topik") String topik);

    /**
     * Mendapatkan ranking mentor mahasiswa berdasarkan rata-rata rating tertinggi.
     * Hanya menampilkan mentor dengan minimal 5 penilaian.
     */
    @Query("SELECT m FROM Mahasiswa m WHERE m.isMentor = true AND m.jumlahPenilaian >= 5 " +
           "ORDER BY (m.totalRating / m.jumlahPenilaian) DESC")
    List<Mahasiswa> getRankingMentorMahasiswa();

    /**
     * Mencari kandidat potensial Asisten Dosen berdasarkan kriteria kelayakan:
     * - IPK >= 3.0
     * - Rating rata-rata >= 4.0 (dihitung dari totalRating/jumlahPenilaian)
     * - Minimal 5 sesi diselesaikan
     * - Minimal 5 penilaian
     * - Sudah menjadi mentor aktif
     *
     * Query ini digunakan oleh Dosen untuk menemukan "bibit unggul".
     */
    @Query("SELECT m FROM Mahasiswa m WHERE m.isMentor = true " +
           "AND m.ipk >= :minIpk " +
           "AND m.jumlahPenilaian >= :minPenilaian " +
           "AND m.sesiDiselesaikan >= :minSesi " +
           "AND (m.totalRating / m.jumlahPenilaian) >= :minRating " +
           "ORDER BY (m.totalRating / m.jumlahPenilaian) DESC, m.ipk DESC")
    List<Mahasiswa> cariKandidatAsdos(
        @Param("minIpk") double minIpk,
        @Param("minPenilaian") int minPenilaian,
        @Param("minSesi") int minSesi,
        @Param("minRating") double minRating
    );

    /**
     * Mendapatkan daftar mahasiswa yang sudah menjadi kandidat Asdos.
     */
    List<Mahasiswa> findByKandidatAsdosTrue();

    /**
     * Mendapatkan daftar mahasiswa yang sudah resmi menjadi Asdos.
     */
    List<Mahasiswa> findByIsAsdosTrue();

    /**
     * Mendapatkan daftar mahasiswa berdasarkan program studi tertentu.
     */
    List<Mahasiswa> findByProgramStudi(String programStudi);

    /**
     * Mendapatkan daftar mahasiswa berdasarkan universitas.
     */
    List<Mahasiswa> findByUniversitas(String universitas);

    /**
     * Menghitung jumlah mentor aktif di program studi tertentu.
     */
    long countByIsMentorTrueAndProgramStudi(String programStudi);

    /**
     * Menghitung jumlah kandidat asdos.
     */
    long countByKandidatAsdosTrue();
}
