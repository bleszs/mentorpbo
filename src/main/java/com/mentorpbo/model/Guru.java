package com.mentorpbo.model;

import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Guru - Merepresentasikan supervisor di lingkungan sekolah.
 *
 * Menerapkan prinsip OOP:
 * - INHERITANCE: Mewarisi seluruh atribut dan perilaku dari Pengguna.
 * - POLYMORPHISM: Mengimplementasikan method abstrak dengan perilaku spesifik Guru.
 *
 * Guru berperan sebagai supervisor yang memantau, memvalidasi kegiatan mentoring,
 * dan mengidentifikasi siswa aktif berprestasi di lingkungan sekolah.
 */
@Entity
@DiscriminatorValue("GURU")
public class Guru extends Pengguna {

    @Column(length = 30)
    private String nip;

    @Column(length = 100)
    private String namaSekolah;

    /** Mata pelajaran yang diampu oleh guru ini */
    @Column(length = 200)
    private String mataPelajaranDiampu;

    /** Bidang keahlian spesifik guru */
    @Column(length = 200)
    private String bidangKeahlian;

    /** Jumlah tahun pengalaman mengajar */
    @Column
    private int tahunPengalaman;

    /** Jumlah total kegiatan yang telah divalidasi */
    @Column(name = "total_validasi")
    private int totalValidasi = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private boolean isMentor = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private boolean isAsdos = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private boolean kandidatAsdos = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private int jumlahPenilaian = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private int sesiDiselesaikan = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private int totalPoinProgres = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Guru */
    @Column(nullable = false)
    private double totalRating = 0.0;

    /**
     * Daftar sesi mentoring yang diawasi oleh guru ini.
     * Relasi OneToMany: Satu guru mengawasi banyak sesi.
     */
    @OneToMany(mappedBy = "supervisor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SesiMentoring> sesiDiawasi = new ArrayList<>();

    // === Konstruktor ===

    public Guru() {
        super();
    }

    public Guru(String namaLengkap, String email, String kataSandi,
                String nip, String namaSekolah, String mataPelajaranDiampu) {
        super(namaLengkap, email, kataSandi, RolePengguna.GURU, LingkunganBelajar.SEKOLAH);
        this.nip = nip;
        this.namaSekolah = namaSekolah;
        this.mataPelajaranDiampu = mataPelajaranDiampu;
    }

    // === Implementasi Method Abstrak dari Pengguna (Polymorphism) ===

    @Override
    public String tampilkanProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append(String.format("=== Profil Guru (Supervisor) ===\n"));
        profil.append(String.format("Nama: %s\n", getNamaLengkap()));
        profil.append(String.format("NIP: %s\n", nip));
        profil.append(String.format("Sekolah: %s\n", namaSekolah));
        profil.append(String.format("Mata Pelajaran: %s\n", mataPelajaranDiampu));
        profil.append(String.format("Bidang Keahlian: %s\n", bidangKeahlian));
        profil.append(String.format("Pengalaman: %d tahun\n", tahunPengalaman));
        profil.append(String.format("Total Validasi: %d kegiatan\n", totalValidasi));
        return profil.toString();
    }

    @Override
    public String getDashboardView() {
        return "dashboard/dashboard-guru";
    }

    // === Method Spesifik Guru ===

    /**
     * Menambahkan hitungan validasi setelah guru memvalidasi kegiatan.
     */
    public void catatValidasi() {
        this.totalValidasi++;
    }

    // === Getter dan Setter ===

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getNamaSekolah() {
        return namaSekolah;
    }

    public void setNamaSekolah(String namaSekolah) {
        this.namaSekolah = namaSekolah;
    }

    public String getMataPelajaranDiampu() {
        return mataPelajaranDiampu;
    }

    public void setMataPelajaranDiampu(String mataPelajaranDiampu) {
        this.mataPelajaranDiampu = mataPelajaranDiampu;
    }

    public String getBidangKeahlian() {
        return bidangKeahlian;
    }

    public void setBidangKeahlian(String bidangKeahlian) {
        this.bidangKeahlian = bidangKeahlian;
    }

    public int getTahunPengalaman() {
        return tahunPengalaman;
    }

    public void setTahunPengalaman(int tahunPengalaman) {
        this.tahunPengalaman = tahunPengalaman;
    }

    public int getTotalValidasi() {
        return totalValidasi;
    }

    public void setTotalValidasi(int totalValidasi) {
        this.totalValidasi = totalValidasi;
    }

    public List<SesiMentoring> getSesiDiawasi() {
        return sesiDiawasi;
    }

    public void setSesiDiawasi(List<SesiMentoring> sesiDiawasi) {
        this.sesiDiawasi = sesiDiawasi;
    }
}
