package com.mentorpbo.repository;

import com.mentorpbo.model.Dosen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Dosen - Antarmuka akses data khusus untuk entitas Dosen.
 */
@Repository
public interface DosenRepository extends JpaRepository<Dosen, Long> {

    Optional<Dosen> findByNidn(String nidn);

    List<Dosen> findByProgramStudi(String programStudi);

    List<Dosen> findByFakultas(String fakultas);

    List<Dosen> findByUniversitas(String universitas);

    List<Dosen> findByMataKuliahDiampuContainingIgnoreCase(String mataKuliah);
}
