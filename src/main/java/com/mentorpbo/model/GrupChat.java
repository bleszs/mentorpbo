package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grup_chat")
public class GrupChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibuat_oleh_id", nullable = false)
    private Pengguna dibuatOleh;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "grup_chat_anggota",
        joinColumns = @JoinColumn(name = "grup_id"),
        inverseJoinColumns = @JoinColumn(name = "pengguna_id")
    )
    private List<Pengguna> anggota = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime waktuDibuat = LocalDateTime.now();

    public GrupChat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public Pengguna getDibuatOleh() { return dibuatOleh; }
    public void setDibuatOleh(Pengguna dibuatOleh) { this.dibuatOleh = dibuatOleh; }
    public List<Pengguna> getAnggota() { return anggota; }
    public void setAnggota(List<Pengguna> anggota) { this.anggota = anggota; }
    public LocalDateTime getWaktuDibuat() { return waktuDibuat; }
}
