package com.mentorpbo.repository;

import com.mentorpbo.model.Pesan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PesanRepository extends JpaRepository<Pesan, Long> {

    /** Semua pesan antara dua pengguna (percakapan), diurutkan waktu */
    @Query("SELECT p FROM Pesan p WHERE " +
           "(p.pengirim.id = :idA AND p.penerima.id = :idB) OR " +
           "(p.pengirim.id = :idB AND p.penerima.id = :idA) " +
           "ORDER BY p.waktuKirim ASC")
    List<Pesan> findPercakapan(@Param("idA") Long idA, @Param("idB") Long idB);

    /** Hitung pesan belum dibaca untuk penerima tertentu */
    long countByPenerimaIdAndSudahDibacaFalse(Long penerimaId);

    /** Tandai semua pesan dari pengirim ke penerima sebagai sudah dibaca */
    @Query("SELECT p FROM Pesan p WHERE p.pengirim.id = :pengirimId AND p.penerima.id = :penerimaId AND p.sudahDibaca = false")
    List<Pesan> findBelumDibaca(@Param("pengirimId") Long pengirimId, @Param("penerimaId") Long penerimaId);

    /** Partner percakapan unik untuk pengguna tertentu (berdasarkan pesan terakhir) */
    @Query("SELECT p FROM Pesan p WHERE p.pengirim.id = :id OR p.penerima.id = :id ORDER BY p.waktuKirim DESC")
    List<Pesan> findSemuaPesanPengguna(@Param("id") Long id);
}
