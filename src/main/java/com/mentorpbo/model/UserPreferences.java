package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * UserPreferences — menyimpan preferensi dan pengaturan pengguna secara persisten di H2.
 *
 * Setiap pengguna memiliki SATU baris di tabel ini (One-to-One dengan Pengguna).
 * Jika belum ada, DashboardController membuatnya dengan nilai default.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengguna_id", nullable = false, unique = true)
    private Pengguna pengguna;

    // === Profil ===
    @Column(length = 500)
    private String bio;

    @Column(length = 200)
    private String fotoProfilUrl;

    @Column(length = 200)
    private String portofolioUrl;

    @Column(length = 200)
    private String namaLengkap;

    @Column(length = 100)
    private String programStudi;

    @Column(length = 100)
    private String universitas;

    @Column(length = 100)
    private String namaSekolah;

    @Column(length = 200)
    private String keahlian;

    @Column(length = 200)
    private String topikKeahlian;

    // === Notifikasi ===
    private boolean notifEmailAktif = true;
    private boolean notifPengingatSesi = true;
    private boolean notifPesanBaru = true;
    private boolean notifValidasiSesi = true;
    private boolean notifRating = true;

    // === Privasi ===
    private boolean profilPublik = true;
    private boolean tampilDiRanking = true;
    private boolean bagikanStatistik = true;

    // === Tampilan ===
    @Column(length = 20)
    private String bahasa = "id";

    @Column(length = 20)
    private String tema = "light";

    @Column(nullable = false, updatable = false)
    private LocalDateTime dibuatPada;

    private LocalDateTime diubahPada;

    @PrePersist
    protected void onCreate() {
        dibuatPada = LocalDateTime.now();
        diubahPada = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        diubahPada = LocalDateTime.now();
    }

    public UserPreferences() {}

    public UserPreferences(Pengguna pengguna) {
        this.pengguna = pengguna;
        // Sinkronisasi dari data pengguna yang sudah ada
        if (pengguna.getBio() != null)        this.bio = pengguna.getBio();
        if (pengguna.getFotoProfil() != null) this.fotoProfilUrl = pengguna.getFotoProfil();
        this.namaLengkap = pengguna.getNamaLengkap();
    }

    // ============================================================
    // Getters & Setters
    // ============================================================

    public Long getId() { return id; }
    public Pengguna getPengguna() { return pengguna; }
    public void setPengguna(Pengguna pengguna) { this.pengguna = pengguna; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getFotoProfilUrl() { return fotoProfilUrl; }
    public void setFotoProfilUrl(String fotoProfilUrl) { this.fotoProfilUrl = fotoProfilUrl; }

    public String getPortofolioUrl() { return portofolioUrl; }
    public void setPortofolioUrl(String portofolioUrl) { this.portofolioUrl = portofolioUrl; }

    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }

    public String getProgramStudi() { return programStudi; }
    public void setProgramStudi(String programStudi) { this.programStudi = programStudi; }

    public String getUniversitas() { return universitas; }
    public void setUniversitas(String universitas) { this.universitas = universitas; }

    public String getNamaSekolah() { return namaSekolah; }
    public void setNamaSekolah(String namaSekolah) { this.namaSekolah = namaSekolah; }

    public String getKeahlian() { return keahlian; }
    public void setKeahlian(String keahlian) { this.keahlian = keahlian; }

    public String getTopikKeahlian() { return topikKeahlian; }
    public void setTopikKeahlian(String topikKeahlian) { this.topikKeahlian = topikKeahlian; }

    public boolean isNotifEmailAktif() { return notifEmailAktif; }
    public void setNotifEmailAktif(boolean notifEmailAktif) { this.notifEmailAktif = notifEmailAktif; }

    public boolean isNotifPengingatSesi() { return notifPengingatSesi; }
    public void setNotifPengingatSesi(boolean notifPengingatSesi) { this.notifPengingatSesi = notifPengingatSesi; }

    public boolean isNotifPesanBaru() { return notifPesanBaru; }
    public void setNotifPesanBaru(boolean notifPesanBaru) { this.notifPesanBaru = notifPesanBaru; }

    public boolean isNotifValidasiSesi() { return notifValidasiSesi; }
    public void setNotifValidasiSesi(boolean notifValidasiSesi) { this.notifValidasiSesi = notifValidasiSesi; }

    public boolean isNotifRating() { return notifRating; }
    public void setNotifRating(boolean notifRating) { this.notifRating = notifRating; }

    public boolean isProfilPublik() { return profilPublik; }
    public void setProfilPublik(boolean profilPublik) { this.profilPublik = profilPublik; }

    public boolean isTampilDiRanking() { return tampilDiRanking; }
    public void setTampilDiRanking(boolean tampilDiRanking) { this.tampilDiRanking = tampilDiRanking; }

    public boolean isBagikanStatistik() { return bagikanStatistik; }
    public void setBagikanStatistik(boolean bagikanStatistik) { this.bagikanStatistik = bagikanStatistik; }

    public String getBahasa() { return bahasa; }
    public void setBahasa(String bahasa) { this.bahasa = bahasa; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public LocalDateTime getDibuatPada() { return dibuatPada; }
    public LocalDateTime getDiubahPada() { return diubahPada; }
}
