package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity MateriBelajar - Merepresentasikan materi pembelajaran yang dibagikan.
 *
 * Mentor dapat mengunggah dan membagikan materi belajar kepada mentee
 * dalam konteks sesi mentoring. Materi dapat berupa file dokumen,
 * tautan referensi, atau catatan tertulis.
 */
@Entity
@Table(name = "materi_belajar")
public class MateriBelajar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Judul materi belajar */
    @Column(nullable = false, length = 200)
    private String judul;

    /** Deskripsi singkat mengenai isi materi */
    @Column(length = 1000)
    private String deskripsi;

    /** Jenis materi: DOKUMEN, TAUTAN, CATATAN, PRESENTASI */
    @Column(length = 30)
    private String jenisMateri;

    /** Tautan atau path ke file materi */
    @Column(length = 500)
    private String tautanMateri;

    /** Nama file asli (jika berupa file) */
    @Column(length = 200)
    private String namaFile;

    /** Ukuran file dalam bytes (jika berupa file) */
    @Column
    private long ukuranFile;

    /** Mata pelajaran/mata kuliah terkait */
    @Column(length = 100)
    private String mataPelajaran;

    /** Jumlah unduhan materi */
    @Column(nullable = false)
    private int jumlahUnduhan = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalUnggah;

    /**
     * Relasi ManyToOne ke Pengguna yang mengunggah materi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengunggah_id", nullable = false)
    private Pengguna pengunggah;

    /**
     * Relasi ManyToOne ke sesi mentoring terkait (opsional).
     * Materi bisa dibagikan di luar konteks sesi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesi_id")
    private SesiMentoring sesiTerkait;

    // === Konstruktor ===

    public MateriBelajar() {
        this.tanggalUnggah = LocalDateTime.now();
    }

    public MateriBelajar(String judul, String deskripsi, String jenisMateri,
                         String tautanMateri, Pengguna pengunggah) {
        this();
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.jenisMateri = jenisMateri;
        this.tautanMateri = tautanMateri;
        this.pengunggah = pengunggah;
    }

    // === Method ===

    /**
     * Menambahkan hitungan unduhan saat materi diunduh.
     */
    public void tambahUnduhan() {
        this.jumlahUnduhan++;
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

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getJenisMateri() {
        return jenisMateri;
    }

    public void setJenisMateri(String jenisMateri) {
        this.jenisMateri = jenisMateri;
    }

    public String getTautanMateri() {
        return tautanMateri;
    }

    public void setTautanMateri(String tautanMateri) {
        this.tautanMateri = tautanMateri;
    }

    public String getNamaFile() {
        return namaFile;
    }

    public void setNamaFile(String namaFile) {
        this.namaFile = namaFile;
    }

    public long getUkuranFile() {
        return ukuranFile;
    }

    public void setUkuranFile(long ukuranFile) {
        this.ukuranFile = ukuranFile;
    }

    public String getMataPelajaran() {
        return mataPelajaran;
    }

    public void setMataPelajaran(String mataPelajaran) {
        this.mataPelajaran = mataPelajaran;
    }

    public int getJumlahUnduhan() {
        return jumlahUnduhan;
    }

    public void setJumlahUnduhan(int jumlahUnduhan) {
        this.jumlahUnduhan = jumlahUnduhan;
    }

    public LocalDateTime getTanggalUnggah() {
        return tanggalUnggah;
    }

    public void setTanggalUnggah(LocalDateTime tanggalUnggah) {
        this.tanggalUnggah = tanggalUnggah;
    }

    public Pengguna getPengunggah() {
        return pengunggah;
    }

    public void setPengunggah(Pengguna pengunggah) {
        this.pengunggah = pengunggah;
    }

    public SesiMentoring getSesiTerkait() {
        return sesiTerkait;
    }

    public void setSesiTerkait(SesiMentoring sesiTerkait) {
        this.sesiTerkait = sesiTerkait;
    }
}
