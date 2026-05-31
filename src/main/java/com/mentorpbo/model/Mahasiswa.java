package com.mentorpbo.model;

import com.mentorpbo.model.enums.LingkunganBelajar;
import com.mentorpbo.model.enums.RolePengguna;
import com.mentorpbo.model.interfaces.Ratable;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Mahasiswa - Merepresentasikan peserta didik di lingkungan kampus.
 *
 * Menerapkan prinsip OOP:
 * - INHERITANCE: Mewarisi seluruh atribut dan perilaku dari Pengguna.
 * - POLYMORPHISM: Mengimplementasikan method abstrak dengan perilaku spesifik Mahasiswa.
 * - ABSTRAKSI: Mengimplementasikan interface Ratable untuk sistem penilaian.
 *
 * Mahasiswa berprestasi dapat menjadi Mentor dan berpotensi diangkat sebagai
 * Asisten Dosen (Asdos) berdasarkan performa, rating, dan keaktifan.
 */
@Entity
@DiscriminatorValue("MAHASISWA")
public class Mahasiswa extends Pengguna implements Ratable {

    @Column(length = 20)
    private String nim;

    @Column(length = 100)
    private String programStudi;

    @Column(length = 100)
    private String fakultas;

    @Column(length = 100)
    private String universitas;

    /** Semester aktif mahasiswa saat ini (1-14) */
    @Column
    private int semester;

    /** Indeks Prestasi Kumulatif mahasiswa */
    @Column
    private double ipk;

    /** Menandakan apakah mahasiswa ini terdaftar sebagai mentor aktif */
    @Column(nullable = false)
    private boolean isMentor = false;

    /** Mata kuliah yang dikuasai (jika menjadi mentor) */
    @Column(length = 500)
    private String mataKuliahKeahlian;

    /** Topik pembelajaran spesifik yang dikuasai */
    @Column(length = 500)
    private String topikKeahlian;

    /** Menandakan apakah mahasiswa sudah direkomendasikan sebagai Asdos */
    @Column(nullable = false)
    private boolean kandidatAsdos = false;

    /** Menandakan apakah mahasiswa sudah resmi menjadi Asdos */
    @Column(nullable = false)
    private boolean isAsdos = false;

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

    public Mahasiswa() {
        super();
    }

    public Mahasiswa(String namaLengkap, String email, String kataSandi,
                     String nim, String programStudi, String fakultas,
                     String universitas, int semester) {
        super(namaLengkap, email, kataSandi, RolePengguna.MAHASISWA, LingkunganBelajar.KAMPUS);
        this.nim = nim;
        this.programStudi = programStudi;
        this.fakultas = fakultas;
        this.universitas = universitas;
        this.semester = semester;
    }

    // === Implementasi Method Abstrak dari Pengguna (Polymorphism) ===

    @Override
    public String tampilkanProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append(String.format("=== Profil Mahasiswa ===\n"));
        profil.append(String.format("Nama: %s\n", getNamaLengkap()));
        profil.append(String.format("NIM: %s\n", nim));
        profil.append(String.format("Program Studi: %s | Fakultas: %s\n", programStudi, fakultas));
        profil.append(String.format("Universitas: %s | Semester: %d\n", universitas, semester));
        profil.append(String.format("IPK: %.2f\n", ipk));
        profil.append(String.format("Status Mentor: %s\n", isMentor ? "Aktif" : "Tidak Aktif"));
        if (isMentor) {
            profil.append(String.format("Keahlian MK: %s\n", mataKuliahKeahlian));
            profil.append(String.format("Rating: %.1f/5.0 (%d ulasan)\n",
                hitungRataRataRating(), jumlahPenilaian));
        }
        if (isAsdos) {
            profil.append(">>> Status Asdos: AKTIF <<<\n");
        } else if (kandidatAsdos) {
            profil.append(">>> Kandidat Asdos: DIREKOMENDASIKAN <<<\n");
        }
        profil.append(String.format("Poin Progres: %d\n", totalPoinProgres));
        return profil.toString();
    }

    @Override
    public String getDashboardView() {
        return isMentor ? "dashboard/dashboard-mentor" : "dashboard/dashboard-mahasiswa";
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
        return hitungRataRataRating() >= minimumRating && jumlahPenilaian >= 5;
    }

    // === Method Spesifik Mahasiswa ===

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
     * Mengaktifkan status mentor untuk mahasiswa ini.
     *
     * @param mataKuliah daftar mata kuliah yang dikuasai
     * @param topik topik pembelajaran spesifik
     */
    public void aktifkanSebagaiMentor(String mataKuliah, String topik) {
        this.isMentor = true;
        this.mataKuliahKeahlian = mataKuliah;
        this.topikKeahlian = topik;
    }

    /**
     * Menghitung skor kelayakan untuk menjadi Asisten Dosen (Asdos).
     * Skor dihitung berdasarkan kombinasi:
     * - Rata-rata rating (bobot 40%)
     * - IPK (bobot 30%)
     * - Jumlah sesi diselesaikan (bobot 20%)
     * - Total poin progres (bobot 10%)
     *
     * @return skor kelayakan Asdos (0.0 - 100.0)
     */
    public double hitungSkorKelayakanAsdos() {
        double skorRating = (hitungRataRataRating() / 5.0) * 40;
        double skorIpk = (ipk / 4.0) * 30;
        double skorSesi = Math.min((sesiDiselesaikan / 20.0) * 20, 20);
        double skorPoin = Math.min((totalPoinProgres / 500.0) * 10, 10);
        return Math.round((skorRating + skorIpk + skorSesi + skorPoin) * 100.0) / 100.0;
    }

    /**
     * Mengecek apakah mahasiswa memenuhi syarat untuk menjadi kandidat Asdos.
     * Syarat: IPK >= 3.0, rating >= 4.0, minimal 5 sesi selesai, dan minimal 5 penilaian.
     *
     * @return true jika memenuhi semua syarat
     */
    public boolean memenuhiSyaratAsdos() {
        return ipk >= 3.0
            && hitungRataRataRating() >= 4.0
            && sesiDiselesaikan >= 5
            && jumlahPenilaian >= 5
            && isMentor;
    }

    // === Getter dan Setter ===

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
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

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public double getIpk() {
        return ipk;
    }

    public void setIpk(double ipk) {
        this.ipk = ipk;
    }

    public boolean isMentor() {
        return isMentor;
    }

    public void setMentor(boolean mentor) {
        isMentor = mentor;
    }

    public String getMataKuliahKeahlian() {
        return mataKuliahKeahlian;
    }

    public void setMataKuliahKeahlian(String mataKuliahKeahlian) {
        this.mataKuliahKeahlian = mataKuliahKeahlian;
    }

    public String getTopikKeahlian() {
        return topikKeahlian;
    }

    public void setTopikKeahlian(String topikKeahlian) {
        this.topikKeahlian = topikKeahlian;
    }

    public boolean isKandidatAsdos() {
        return kandidatAsdos;
    }

    public void setKandidatAsdos(boolean kandidatAsdos) {
        this.kandidatAsdos = kandidatAsdos;
    }

    public boolean isAsdos() {
        return isAsdos;
    }

    public void setAsdos(boolean asdos) {
        isAsdos = asdos;
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
