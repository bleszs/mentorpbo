package com.mentorpbo.repository;

import com.mentorpbo.model.Guru;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Guru - Antarmuka akses data khusus untuk entitas Guru.
 */
@Repository
public interface GuruRepository extends JpaRepository<Guru, Long> {

    Optional<Guru> findByNip(String nip);

    List<Guru> findByNamaSekolah(String namaSekolah);

    List<Guru> findByMataPelajaranDiampuContainingIgnoreCase(String mataPelajaran);
}
