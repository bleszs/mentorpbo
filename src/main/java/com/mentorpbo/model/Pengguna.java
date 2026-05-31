package com.mentorpbo.model;

import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Abstract Class Pengguna - Kelas dasar (superclass) untuk seluruh pengguna sistem.
 *
 * Menerapkan prinsip OOP:
 * - ABSTRAKSI: Mendefinisikan kerangka umum pengguna tanpa implementasi spesifik.
 * - ENCAPSULATION: Field bersifat private dengan akses melalui getter/setter.
 * - INHERITANCE: Diturunkan oleh Siswa, Mahasiswa, Guru, dan Dosen.
 *
 * Strategi Inheritance JPA: SINGLE_TABLE
 * Semua turunan Pengguna disimpan dalam satu tabel 'pengguna' dengan kolom
 * diskriminator 'tipe_pengguna' untuk membedakan jenis pengguna.
 * Dipilih karena query lebih efisien dan tidak memerlukan JOIN antar tabel.
 */
@Entity
@Table(name = "pengguna")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "tipe_pengguna",
    discriminatorType = DiscriminatorType.STRING,
    length = 20
)
public abstract class Pengguna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String namaLengkap;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String kataSandi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolePengguna role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LingkunganBelajar lingkungan;

    @Column(length = 255)
    private String fotoProfil;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private boolean aktif = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalDaftar;

    @Column
    private LocalDateTime terakhirLogin;

    // === Konstruktor ===

    /** Konstruktor default (diperlukan oleh JPA) */
    protected Pengguna() {
        this.tanggalDaftar = LocalDateTime.now();
        this.aktif = true;
    }

    /**
     * Konstruktor dengan parameter dasar.
     *
     * @param namaLengkap nama lengkap pengguna
     * @param email alamat email (unik)
     * @param kataSandi kata sandi terenkripsi
     * @param role peran pengguna dalam sistem
     * @param lingkungan lingkungan pendidikan (Sekolah/Kampus)
     */
    protected Pengguna(String namaLengkap, String email, String kataSandi,
                       RolePengguna role, LingkunganBelajar lingkungan) {
        this();
        this.namaLengkap = namaLengkap;
        this.email = email;
        this.kataSandi = kataSandi;
        this.role = role;
        this.lingkungan = lingkungan;
    }

    // === Method Abstrak (Wajib diimplementasikan oleh subclass) ===

    /**
     * Mengembalikan deskripsi profil yang spesifik sesuai jenis pengguna.
     * Implementasi berbeda untuk Siswa, Mahasiswa, Guru, dan Dosen.
     * Ini adalah contoh penerapan POLIMORFISME.
     *
     * @return deskripsi profil dalam format String
     */
    public abstract String tampilkanProfil();

    /**
     * Menentukan halaman dashboard yang sesuai berdasarkan role pengguna.
     * Setiap jenis pengguna memiliki dashboard berbeda.
     *
     * @return nama view/template dashboard
     */
    public abstract String getDashboardView();

    // === Getter dan Setter (Encapsulation) ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getKataSandi() {
        return kataSandi;
    }

    public void setKataSandi(String kataSandi) {
        this.kataSandi = kataSandi;
    }

    public RolePengguna getRole() {
        return role;
    }

    public void setRole(RolePengguna role) {
        this.role = role;
    }

    public LingkunganBelajar getLingkungan() {
        return lingkungan;
    }

    public void setLingkungan(LingkunganBelajar lingkungan) {
        this.lingkungan = lingkungan;
    }

    public String getFotoProfil() {
        return fotoProfil;
    }

    public void setFotoProfil(String fotoProfil) {
        this.fotoProfil = fotoProfil;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public LocalDateTime getTanggalDaftar() {
        return tanggalDaftar;
    }

    public void setTanggalDaftar(LocalDateTime tanggalDaftar) {
        this.tanggalDaftar = tanggalDaftar;
    }

    public LocalDateTime getTerakhirLogin() {
        return terakhirLogin;
    }

    public void setTerakhirLogin(LocalDateTime terakhirLogin) {
        this.terakhirLogin = terakhirLogin;
    }

    // === Override dari Object ===

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - %s",
            role.getLabel(), namaLengkap, email, lingkungan.getLabel());
    }
}
