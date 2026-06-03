package com.mentorpbo.model;

import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import com.mentorpbo.model.enums.StatusValidasi;
import com.mentorpbo.model.interfaces.Ratable;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Siswa - Merepresentasikan peserta didik di lingkungan sekolah.
 *
 * Menerapkan prinsip OOP:
 * - INHERITANCE: Mewarisi seluruh atribut dan perilaku dari Pengguna.
 * - POLYMORPHISM: Mengimplementasikan method abstrak tampilkanProfil() dan
 *                  getDashboardView() dengan perilaku spesifik Siswa.
 * - ABSTRAKSI: Mengimplementasikan interface Ratable untuk sistem penilaian.
 *
 * Siswa dapat berperan sebagai Mentor (jika berprestasi) atau Mentee
 * (jika membutuhkan bantuan belajar).
 */
@Entity
@DiscriminatorValue("SISWA")
public class Siswa extends Pengguna implements Ratable {

    @Column(length = 50)
    private String kelas;

    @Column(length = 100)
    private String namaSekolah;

    @Column(length = 20)
    private String nisn;

    /** Menandakan apakah siswa ini terdaftar sebagai mentor aktif */
    @Column(nullable = false)
    private boolean isMentor = false;

    /**
     * Status validasi pendaftaran sebagai mentor oleh supervisor (Guru).
     * null   = bukan mentor / data lama (legacy, dianggap sudah disetujui)
     * BELUM_DITINJAU = menunggu persetujuan supervisor (terkunci)
     * DIVALIDASI     = disetujui, boleh aktif & muncul di pencarian
     * DITOLAK        = pendaftaran mentor ditolak
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusValidasi statusValidasiMentor;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Siswa */
    @Column(nullable = false)
    private boolean isAsdos = false;

    /** Required field untuk SINGLE_TABLE inheritance - tidak digunakan untuk Siswa */
    @Column(nullable = false)
    private boolean kandidatAsdos = false;

    /** Mata pelajaran yang dikuasai (jika menjadi mentor) */
    @Column(length = 500)
    private String mataPelajaranKeahlian;

    /** Total poin progres yang dikumpulkan dari aktivitas mentoring */
    @Column(nullable = false)
    private int totalPoinProgres = 0;

    /** Akumulasi total rating yang diterima */
    @Column(nullable = false)
    private double totalRating = 0.0;

    /** Jumlah total penilaian yang sudah diterima */
    @Column(nullable = false)
    private int jumlahPenilaian = 0;

    /** Jumlah sesi mentoring yang sudah diselesaikan */
    @Column(nullable = false)
    private int sesiDiselesaikan = 0;

    /** Relasi ke sesi mentoring sebagai mentor */
    @OneToMany(mappedBy = "mentor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SesiMentoring> sesiSebagaiMentor = new ArrayList<>();

    /** Relasi ke sesi mentoring sebagai mentee */
    @OneToMany(mappedBy = "mentee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SesiMentoring> sesiSebagaiMentee = new ArrayList<>();

    /** Relasi ke materi belajar yang diunggah */
    @OneToMany(mappedBy = "pengunggah", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MateriBelajar> materiDiunggah = new ArrayList<>();

    // === Konstruktor ===

    public Siswa() {
        super();
    }

    public Siswa(String namaLengkap, String email, String kataSandi,
                 String kelas, String namaSekolah, String nisn) {
        super(namaLengkap, email, kataSandi, RolePengguna.SISWA, LingkunganBelajar.SEKOLAH);
        this.kelas = kelas;
        this.namaSekolah = namaSekolah;
        this.nisn = nisn;
    }

    // === Implementasi Method Abstrak dari Pengguna (Polymorphism) ===

    @Override
    public String tampilkanProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append(String.format("=== Profil Siswa ===\n"));
        profil.append(String.format("Nama: %s\n", getNamaLengkap()));
        profil.append(String.format("Sekolah: %s | Kelas: %s\n", namaSekolah, kelas));
        profil.append(String.format("NISN: %s\n", nisn));
        profil.append(String.format("Status Mentor: %s\n", isMentor ? "Aktif" : "Tidak Aktif"));
        if (isMentor) {
            profil.append(String.format("Keahlian: %s\n", mataPelajaranKeahlian));
            profil.append(String.format("Rating: %.1f/5.0 (%d ulasan)\n",
                hitungRataRataRating(), jumlahPenilaian));
        }
        profil.append(String.format("Poin Progres: %d\n", totalPoinProgres));
        return profil.toString();
    }

    @Override
    public String getDashboardView() {
        return "dashboard/dashboard-siswa";
    }

    // === Implementasi Interface Ratable ===

    @Override
    public void beriRating(int nilaiRating, String ulasan) {
        if (nilaiRating < 1 || nilaiRating > 5) {
            throw new IllegalArgumentException(
                "Nilai rating harus antara 1-5, diberikan: " + nilaiRating);
        }
        this.totalRating += nilaiRating;
        this.jumlahPenilaian++;
    }

    @Override
    public double hitungRataRataRating() {
        if (jumlahPenilaian == 0) return 0.0;
        return Math.round((totalRating / jumlahPenilaian) * 10.0) / 10.0;
    }

    @Override
    public int getTotalPenilaian() {
        return jumlahPenilaian;
    }

    @Override
    public boolean memenuhinSyaratRating(double minimumRating) {
        return hitungRataRataRating() >= minimumRating && jumlahPenilaian >= 3;
    }

    // === Method Spesifik Siswa ===

    /**
     * Menambahkan poin progres dari aktivitas mentoring.
     *
     * @param poin jumlah poin yang ditambahkan
     */
    public void tambahPoinProgres(int poin) {
        if (poin > 0) {
            this.totalPoinProgres += poin;
        }
    }

    /**
     * Mengaktifkan status mentor untuk siswa ini.
     *
     * @param mataPelajaran daftar mata pelajaran yang dikuasai
     */
    public void aktifkanSebagaiMentor(String mataPelajaran) {
        this.isMentor = true;
        this.mataPelajaranKeahlian = mataPelajaran;
    }

    // === Getter dan Setter ===

    public String getKelas() {
        return kelas;
    }

    public void setKelas(String kelas) {
        this.kelas = kelas;
    }

    public String getNamaSekolah() {
        return namaSekolah;
    }

    public void setNamaSekolah(String namaSekolah) {
        this.namaSekolah = namaSekolah;
    }

    public String getNisn() {
        return nisn;
    }

    public void setNisn(String nisn) {
        this.nisn = nisn;
    }

    public boolean isMentor() {
        return isMentor;
    }

    public void setMentor(boolean mentor) {
        isMentor = mentor;
    }

    public StatusValidasi getStatusValidasiMentor() {
        return statusValidasiMentor;
    }

    public void setStatusValidasiMentor(StatusValidasi statusValidasiMentor) {
        this.statusValidasiMentor = statusValidasiMentor;
    }

    /** Mentor yang sudah disetujui supervisor (atau data legacy tanpa status). */
    public boolean isMentorTervalidasi() {
        return isMentor && (statusValidasiMentor == null || statusValidasiMentor == StatusValidasi.DIVALIDASI);
    }

    /** Mentor yang masih menunggu persetujuan supervisor. */
    public boolean isMentorMenungguPersetujuan() {
        return isMentor && statusValidasiMentor == StatusValidasi.BELUM_DITINJAU;
    }

    public String getMataPelajaranKeahlian() {
        return mataPelajaranKeahlian;
    }

    public void setMataPelajaranKeahlian(String mataPelajaranKeahlian) {
        this.mataPelajaranKeahlian = mataPelajaranKeahlian;
    }

    public int getTotalPoinProgres() {
        return totalPoinProgres;
    }

    public void setTotalPoinProgres(int totalPoinProgres) {
        this.totalPoinProgres = totalPoinProgres;
    }

    public double getTotalRating() {
        return totalRating;
    }

    public void setTotalRating(double totalRating) {
        this.totalRating = totalRating;
    }

    public int getJumlahPenilaian() {
        return jumlahPenilaian;
    }

    public void setJumlahPenilaian(int jumlahPenilaian) {
        this.jumlahPenilaian = jumlahPenilaian;
    }

    public int getSesiDiselesaikan() {
        return sesiDiselesaikan;
    }

    public void setSesiDiselesaikan(int sesiDiselesaikan) {
        this.sesiDiselesaikan = sesiDiselesaikan;
    }

    public List<SesiMentoring> getSesiSebagaiMentor() {
        return sesiSebagaiMentor;
    }

    public void setSesiSebagaiMentor(List<SesiMentoring> sesiSebagaiMentor) {
        this.sesiSebagaiMentor = sesiSebagaiMentor;
    }

    public List<SesiMentoring> getSesiSebagaiMentee() {
        return sesiSebagaiMentee;
    }

    public void setSesiSebagaiMentee(List<SesiMentoring> sesiSebagaiMentee) {
        this.sesiSebagaiMentee = sesiSebagaiMentee;
    }

    public List<MateriBelajar> getMateriDiunggah() {
        return materiDiunggah;
    }

    public void setMateriDiunggah(List<MateriBelajar> materiDiunggah) {
        this.materiDiunggah = materiDiunggah;
    }
}
