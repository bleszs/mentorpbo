package com.mentorpbo.model;

import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Dosen - Merepresentasikan supervisor di lingkungan kampus.
 *
 * Menerapkan prinsip OOP:
 * - INHERITANCE: Mewarisi seluruh atribut dan perilaku dari Pengguna.
 * - POLYMORPHISM: Mengimplementasikan method abstrak dengan perilaku spesifik Dosen.
 *
 * Dosen berperan sebagai supervisor yang memantau kegiatan mentoring mahasiswa,
 * memvalidasi program, melihat statistik dan ranking mentor, serta secara khusus
 * mencari "bibit unggul" mahasiswa untuk diangkat menjadi Asisten Dosen (Asdos).
 */
@Entity
@DiscriminatorValue("DOSEN")
public class Dosen extends Pengguna {

    @Column(length = 30)
    private String nidn;

    @Column(length = 100)
    private String programStudi;

    @Column(length = 100)
    private String fakultas;

    @Column(length = 100)
    private String universitas;

    /** Mata kuliah yang diampu oleh dosen ini */
    @Column(length = 300)
    private String mataKuliahDiampu;

    /** Jabatan fungsional dosen (Asisten Ahli, Lektor, Lektor Kepala, Guru Besar) */
    @Column(length = 50)
    private String jabatanFungsional;

    /** Bidang riset/keahlian spesifik dosen */
    @Column(length = 300)
    private String bidangRiset;

    /** Jumlah total kegiatan yang telah divalidasi */
    @Column(name = "total_validasi")
    private int totalValidasi = 0;

    /** Jumlah mahasiswa yang telah direkomendasikan sebagai Asdos */
    @Column(name = "total_rekomendasi_asdos")
    private int totalRekomendasiAsdos = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private boolean isMentor = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private boolean isAsdos = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private boolean kandidatAsdos = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private int jumlahPenilaian = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private int sesiDiselesaikan = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private int totalPoinProgres = 0;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Dosen */
    @Column(nullable = false)
    private double totalRating = 0.0;

    /**
     * Daftar sesi mentoring yang diawasi oleh dosen ini.
     * Relasi OneToMany: Satu dosen mengawasi banyak sesi.
     */
    @OneToMany(mappedBy = "supervisor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SesiMentoring> sesiDiawasi = new ArrayList<>();

    // === Konstruktor ===

    public Dosen() {
        super();
    }

    public Dosen(String namaLengkap, String email, String kataSandi,
                 String nidn, String programStudi, String fakultas, String universitas) {
        super(namaLengkap, email, kataSandi, RolePengguna.DOSEN, LingkunganBelajar.KAMPUS);
        this.nidn = nidn;
        this.programStudi = programStudi;
        this.fakultas = fakultas;
        this.universitas = universitas;
    }

    // === Implementasi Method Abstrak dari Pengguna (Polymorphism) ===

    @Override
    public String tampilkanProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append(String.format("=== Profil Dosen (Supervisor) ===\n"));
        profil.append(String.format("Nama: %s\n", getNamaLengkap()));
        profil.append(String.format("NIDN: %s\n", nidn));
        profil.append(String.format("Prodi: %s | Fakultas: %s\n", programStudi, fakultas));
        profil.append(String.format("Universitas: %s\n", universitas));
        profil.append(String.format("Jabatan: %s\n", jabatanFungsional));
        profil.append(String.format("Mata Kuliah: %s\n", mataKuliahDiampu));
        profil.append(String.format("Bidang Riset: %s\n", bidangRiset));
        profil.append(String.format("Total Validasi: %d | Rekomendasi Asdos: %d\n",
            totalValidasi, totalRekomendasiAsdos));
        return profil.toString();
    }

    @Override
    public String getDashboardView() {
        return "dashboard/dashboard-dosen";
    }

    // === Method Spesifik Dosen ===

    /**
     * Menambahkan hitungan validasi setelah dosen memvalidasi kegiatan.
     */
    public void catatValidasi() {
        this.totalValidasi++;
    }

    /**
     * Mencatat rekomendasi Asdos baru yang diberikan oleh dosen ini.
     */
    public void catatRekomendasiAsdos() {
        this.totalRekomendasiAsdos++;
    }

    // === Getter dan Setter ===

    public String getNidn() {
        return nidn;
    }

    public void setNidn(String nidn) {
        this.nidn = nidn;
    }

    public String getProgramStudi() {
        return programStudi;
    }

    public void setProgramStudi(String programStudi) {
        this.programStudi = programStudi;
    }

    public String getFakultas() {
        return fakultas;
    }

    public void setFakultas(String fakultas) {
        this.fakultas = fakultas;
    }

    public String getUniversitas() {
        return universitas;
    }

    public void setUniversitas(String universitas) {
        this.universitas = universitas;
    }

    public String getMataKuliahDiampu() {
        return mataKuliahDiampu;
    }

    public void setMataKuliahDiampu(String mataKuliahDiampu) {
        this.mataKuliahDiampu = mataKuliahDiampu;
    }

    public String getJabatanFungsional() {
        return jabatanFungsional;
    }

    public void setJabatanFungsional(String jabatanFungsional) {
        this.jabatanFungsional = jabatanFungsional;
    }

    public String getBidangRiset() {
        return bidangRiset;
    }

    public void setBidangRiset(String bidangRiset) {
        this.bidangRiset = bidangRiset;
    }

    public int getTotalValidasi() {
        return totalValidasi;
    }

    public void setTotalValidasi(int totalValidasi) {
        this.totalValidasi = totalValidasi;
    }

    public int getTotalRekomendasiAsdos() {
        return totalRekomendasiAsdos;
    }

    public void setTotalRekomendasiAsdos(int totalRekomendasiAsdos) {
        this.totalRekomendasiAsdos = totalRekomendasiAsdos;
    }

    public List<SesiMentoring> getSesiDiawasi() {
        return sesiDiawasi;
    }

    public void setSesiDiawasi(List<SesiMentoring> sesiDiawasi) {
        this.sesiDiawasi = sesiDiawasi;
    }
}
