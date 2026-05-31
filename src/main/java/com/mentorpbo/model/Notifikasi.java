package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity Notifikasi - Merepresentasikan notifikasi/pemberitahuan untuk pengguna.
 *
 * Sistem notifikasi menginformasikan pengguna tentang:
 * - Jadwal sesi mentoring yang akan datang
 * - Perubahan status sesi (dikonfirmasi, dibatalkan, selesai)
 * - Hasil validasi dari supervisor
 * - Review/rating baru yang diterima
 * - Pengangkatan sebagai kandidat Asdos
 */
@Entity
@Table(name = "notifikasi")
public class Notifikasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Judul singkat notifikasi */
    @Column(nullable = false, length = 200)
    private String judul;

    /** Pesan detail notifikasi */
    @Column(nullable = false, length = 1000)
    private String pesan;

    /** Kategori notifikasi: JADWAL, SESI, VALIDASI, RATING, ASDOS, UMUM */
    @Column(nullable = false, length = 20)
    private String kategori;

    /** Apakah notifikasi sudah dibaca oleh pengguna */
    @Column(nullable = false)
    private boolean sudahDibaca = false;

    /** Tautan atau referensi terkait notifikasi (opsional) */
    @Column(length = 300)
    private String tautanReferensi;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalDibuat;

    @Column
    private LocalDateTime tanggalDibaca;

    /**
     * Relasi ManyToOne ke Pengguna penerima notifikasi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penerima_id", nullable = false)
    private Pengguna penerima;

    // === Konstruktor ===

    public Notifikasi() {
        this.tanggalDibuat = LocalDateTime.now();
        this.sudahDibaca = false;
    }

    public Notifikasi(String judul, String pesan, String kategori, Pengguna penerima) {
        this();
        this.judul = judul;
        this.pesan = pesan;
        this.kategori = kategori;
        this.penerima = penerima;
    }

    // === Method ===

    /**
     * Menandai notifikasi sebagai sudah dibaca.
     */
    public void tandaiSudahDibaca() {
        this.sudahDibaca = true;
        this.tanggalDibaca = LocalDateTime.now();
    }

    // === Getter dan Setter ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJudul() {
        return judul;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public String getPesan() {
        return pesan;
    }

    public void setPesan(String pesan) {
        this.pesan = pesan;
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public boolean isSudahDibaca() {
        return sudahDibaca;
    }

    public void setSudahDibaca(boolean sudahDibaca) {
        this.sudahDibaca = sudahDibaca;
    }

    public String getTautanReferensi() {
        return tautanReferensi;
    }

    public void setTautanReferensi(String tautanReferensi) {
        this.tautanReferensi = tautanReferensi;
    }

    public LocalDateTime getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(LocalDateTime tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    public LocalDateTime getTanggalDibaca() {
        return tanggalDibaca;
    }

    public void setTanggalDibaca(LocalDateTime tanggalDibaca) {
        this.tanggalDibaca = tanggalDibaca;
    }

    public Pengguna getPenerima() {
        return penerima;
    }

    public void setPenerima(Pengguna penerima) {
        this.penerima = penerima;
    }
}
