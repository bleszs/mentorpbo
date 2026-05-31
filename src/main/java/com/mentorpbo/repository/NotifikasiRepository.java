package com.mentorpbo.repository;

import com.mentorpbo.model.Notifikasi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Notifikasi - Antarmuka akses data untuk notifikasi pengguna.
 */
@Repository
public interface NotifikasiRepository extends JpaRepository<Notifikasi, Long> {

    /** Mendapatkan semua notifikasi untuk penerima tertentu, diurutkan terbaru. */
    List<Notifikasi> findByPenerimaIdOrderByTanggalDibuatDesc(Long penerimaId);

    /** Mendapatkan notifikasi yang belum dibaca oleh penerima. */
    List<Notifikasi> findByPenerimaIdAndSudahDibacaFalseOrderByTanggalDibuatDesc(Long penerimaId);

    /** Menghitung jumlah notifikasi yang belum dibaca. */
    long countByPenerimaIdAndSudahDibacaFalse(Long penerimaId);

    /** Mendapatkan notifikasi berdasarkan kategori untuk penerima tertentu. */
    List<Notifikasi> findByPenerimaIdAndKategori(Long penerimaId, String kategori);
}
