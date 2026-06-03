package com.mentorpbo.repository;

import com.mentorpbo.model.GrupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupChatRepository extends JpaRepository<GrupChat, Long> {

    @Query("SELECT g FROM GrupChat g JOIN g.anggota a WHERE a.id = :penggunaId ORDER BY g.waktuDibuat DESC")
    List<GrupChat> findByAnggotaId(@Param("penggunaId") Long penggunaId);
}
