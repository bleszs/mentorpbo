package com.mentorpbo.repository;

import com.mentorpbo.model.SesiMentoring;
import com.mentorpbo.model.enums.StatusSesi;
import com.mentorpbo.model.enums.StatusValidasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository SesiMentoring - Antarmuka akses data untuk sesi mentoring dan turunannya.
 *
 * Menyediakan query untuk pencarian sesi berdasarkan berbagai kriteria:
 * mentor, mentee, supervisor, status, dan jadwal.
 */
@Repository
public interface SesiMentoringRepository extends JpaRepository<SesiMentoring, Long> {

    /**
     * Mendapatkan semua sesi dimana pengguna tertentu adalah mentor.
     */
    List<SesiMentoring> findByMentorId(Long mentorId);

    /**
     * Mendapatkan semua sesi dimana pengguna tertentu adalah mentee.
     */
    List<SesiMentoring> findByMenteeId(Long menteeId);

    /**
     * Mendapatkan semua sesi yang diawasi oleh supervisor tertentu.
     */
    List<SesiMentoring> findBySupervisorId(Long supervisorId);

    /**
     * Mendapatkan sesi berdasarkan status tertentu.
     */
    List<SesiMentoring> findByStatusSesi(StatusSesi statusSesi);

    /**
     * Mendapatkan sesi berdasarkan status validasi.
     */
    List<SesiMentoring> findByStatusValidasi(StatusValidasi statusValidasi);

    /**
     * Mendapatkan sesi yang dijadwalkan dalam rentang waktu tertentu.
     *
     * @param mulai batas awal waktu
     * @param akhir batas akhir waktu
     * @return daftar sesi dalam rentang waktu
     */
    List<SesiMentoring> findByWaktuMulaiBetween(LocalDateTime mulai, LocalDateTime akhir);

    /**
     * Mendapatkan sesi mendatang untuk mentor tertentu yang sudah dijadwalkan.
     */
    @Query("SELECT s FROM SesiMentoring s WHERE s.mentor.id = :mentorId " +
           "AND s.statusSesi = 'DIJADWALKAN' AND s.waktuMulai > :sekarang " +
           "ORDER BY s.waktuMulai ASC")
    List<SesiMentoring> getSesiMendatangMentor(
        @Param("mentorId") Long mentorId,
        @Param("sekarang") LocalDateTime sekarang
    );

    /**
     * Mendapatkan sesi mendatang untuk mentee tertentu.
     */
    @Query("SELECT s FROM SesiMentoring s WHERE s.mentee.id = :menteeId " +
           "AND s.statusSesi = 'DIJADWALKAN' AND s.waktuMulai > :sekarang " +
           "ORDER BY s.waktuMulai ASC")
    List<SesiMentoring> getSesiMendatangMentee(
        @Param("menteeId") Long menteeId,
        @Param("sekarang") LocalDateTime sekarang
    );

    /**
     * Mendapatkan sesi yang menunggu validasi oleh supervisor.
     */
    @Query("SELECT s FROM SesiMentoring s WHERE s.supervisor.id = :supervisorId " +
           "AND s.statusValidasi = 'BELUM_DITINJAU' ORDER BY s.tanggalDibuat DESC")
    List<SesiMentoring> getSesiMenungguValidasi(@Param("supervisorId") Long supervisorId);

    /**
     * Menghitung jumlah sesi selesai untuk pengguna tertentu sebagai mentor.
     */
    long countByMentorIdAndStatusSesi(Long mentorId, StatusSesi statusSesi);

    /**
     * Menghitung jumlah sesi berdasarkan status tertentu.
     */
    long countByStatusSesi(StatusSesi statusSesi);

    /**
     * Mendapatkan semua sesi yang melibatkan pengguna tertentu (sebagai mentor atau mentee).
     */
    @Query("SELECT s FROM SesiMentoring s WHERE s.mentor.id = :penggunaId " +
           "OR s.mentee.id = :penggunaId ORDER BY s.tanggalDibuat DESC")
    List<SesiMentoring> findSemuaSesiPengguna(@Param("penggunaId") Long penggunaId);
}
