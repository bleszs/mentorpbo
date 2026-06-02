package com.mentorpbo.repository;

import com.mentorpbo.model.MateriBelajar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository MateriBelajar - Antarmuka akses data untuk materi pembelajaran.
 */
@Repository
public interface MateriBelajarRepository extends JpaRepository<MateriBelajar, Long> {

    /**
     * Mendapatkan materi yang diunggah oleh pengguna tertentu.
     */
    List<MateriBelajar> findByPengunggahId(Long pengunggahId);

    /**
     * Mendapatkan materi terkait sesi mentoring tertentu.
     */
    List<MateriBelajar> findBySesiTerkaitId(Long sesiId);

    /**
     * Mencari materi berdasarkan mata pelajaran (pencarian parsial).
     */
    List<MateriBelajar> findByMataPelajaranContainingIgnoreCase(String mataPelajaran);

    /**
     * Mencari materi berdasarkan judul (pencarian parsial).
     */
    List<MateriBelajar> findByJudulContainingIgnoreCase(String judul);

    /**
     * Mendapatkan materi diurutkan berdasarkan jumlah unduhan terbanyak.
     */
    List<MateriBelajar> findAllByOrderByJumlahUnduhanDesc();

    /** Materi milik pengguna tertentu berdasarkan tipeKonten */
    List<MateriBelajar> findByPengunggahIdAndTipeKonten(Long pengunggahId, String tipeKonten);

    /** Semua materi berdasarkan tipeKonten */
    List<MateriBelajar> findByTipeKontenOrderByTanggalUnggahDesc(String tipeKonten);
}
