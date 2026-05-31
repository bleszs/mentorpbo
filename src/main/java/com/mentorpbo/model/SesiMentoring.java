package com.mentorpbo.model;

import com.mentorpbo.model.enums.JenisSesi;
import com.mentorpbo.model.enums.StatusSesi;
import com.mentorpbo.model.enums.StatusValidasi;
import com.mentorpbo.model.interfaces.Schedulable;
import com.mentorpbo.model.interfaces.Verifiable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Class SesiMentoring - Kelas dasar untuk seluruh jenis sesi mentoring.
 *
 * Menerapkan prinsip OOP:
 * - ABSTRAKSI: Mendefinisikan kerangka sesi mentoring tanpa detail pelaksanaan.
 * - ENCAPSULATION: Semua field private dengan akses terkontrol.
 * - INHERITANCE: Diturunkan oleh SesiOnline, SesiOffline, dan SesiVideo.
 * - POLYMORPHISM: Method abstrak lakukanSesi() diimplementasikan berbeda per subclass.
 *
 * Mengimplementasikan Interface:
 * - Schedulable: Kemampuan penjadwalan sesi.
 * - Verifiable: Kemampuan validasi oleh supervisor.
 *
 * Strategi Inheritance JPA: JOINED
 * Setiap subclass memiliki tabel tersendiri yang di-JOIN dengan tabel induk.
 * Dipilih agar setiap jenis sesi bisa memiliki kolom spesifik tanpa
 * membuat tabel induk terlalu lebar (banyak kolom NULL).
 */
@Entity
@Table(name = "sesi_mentoring")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
    name = "jenis_sesi",
    discriminatorType = DiscriminatorType.STRING,
    length = 20
)
public abstract class SesiMentoring implements Schedulable, Verifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Topik atau materi yang akan dibahas dalam sesi */
    @Column(nullable = false, length = 200)
    private String topikPembahasan;

    /** Deskripsi detail mengenai sesi mentoring */
    @Column(length = 1000)
    private String deskripsi;

    /** Waktu mulai sesi yang dijadwalkan */
    @Column
    private LocalDateTime waktuMulai;

    /** Waktu selesai sesi (otomatis dihitung dari waktuMulai + durasiMenit) */
    @Column
    private LocalDateTime waktuSelesai;

    /** Durasi sesi dalam satuan menit */
    @Column(nullable = false)
    private int durasiMenit;

    /** Jenis format pelaksanaan sesi */
    @Enumerated(EnumType.STRING)
    @Column(name = "jenis_sesi", nullable = false, length = 20, insertable = false, updatable = false)
    private JenisSesi jenisSesi;

    /** Status siklus hidup sesi saat ini */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSesi statusSesi = StatusSesi.MENUNGGU_KONFIRMASI;

    /** Status validasi dari supervisor */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusValidasi statusValidasi = StatusValidasi.BELUM_DITINJAU;

    /** Catatan dari supervisor saat melakukan validasi */
    @Column(length = 500)
    private String catatanValidasi;

    /** ID supervisor yang melakukan validasi terakhir */
    @Column
    private Long idValidatorSupervisor;

    /** Alasan jika sesi dibatalkan */
    @Column(length = 500)
    private String alasanPembatalan;

    /** Poin yang diberikan kepada mentor setelah sesi selesai */
    @Column(nullable = false)
    private int poinMentor = 0;

    /** Poin yang diberikan kepada mentee setelah sesi selesai */
    @Column(nullable = false)
    private int poinMentee = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalDibuat;

    /**
     * Relasi ManyToOne ke Pengguna sebagai Mentor.
     * Mentor adalah siswa/mahasiswa yang memberikan bimbingan.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Pengguna mentor;

    /**
     * Relasi ManyToOne ke Pengguna sebagai Mentee.
     * Mentee adalah siswa/mahasiswa yang menerima bimbingan.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private Pengguna mentee;

    /**
     * Relasi ManyToOne ke Pengguna sebagai Supervisor (opsional).
     * Supervisor adalah guru/dosen yang mengawasi sesi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Pengguna supervisor;

    /** Daftar review/rating yang diberikan untuk sesi ini */
    @OneToMany(mappedBy = "sesiMentoring", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewRating> daftarReview = new ArrayList<>();

    /** Daftar materi belajar yang dibagikan dalam sesi ini */
    @OneToMany(mappedBy = "sesiTerkait", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MateriBelajar> materiBelajar = new ArrayList<>();

    // === Konstruktor ===

    protected SesiMentoring() {
        this.tanggalDibuat = LocalDateTime.now();
        this.statusSesi = StatusSesi.MENUNGGU_KONFIRMASI;
        this.statusValidasi = StatusValidasi.BELUM_DITINJAU;
    }

    protected SesiMentoring(String topikPembahasan, int durasiMenit,
                            JenisSesi jenisSesi, Pengguna mentor, Pengguna mentee) {
        this();
        this.topikPembahasan = topikPembahasan;
        this.durasiMenit = durasiMenit;
        this.jenisSesi = jenisSesi;
        this.mentor = mentor;
        this.mentee = mentee;
    }

    // === Method Abstrak (Polymorphism - Wajib diimplementasikan subclass) ===

    /**
     * Melaksanakan sesi mentoring sesuai dengan format spesifik.
     * SesiOnline, SesiOffline, dan SesiVideo memiliki implementasi berbeda.
     *
     * @return detail informasi pelaksanaan sesi
     */
    public abstract String lakukanSesi();

    /**
     * Mendapatkan instruksi persiapan khusus sesuai jenis sesi.
     *
     * @return instruksi persiapan dalam format String
     */
    public abstract String getInstruksiPersiapan();

    /**
     * Mengembalikan ringkasan sesi dengan format yang spesifik per jenis.
     *
     * @return ringkasan sesi mentoring
     */
    public abstract String getRingkasanSesi();

    // === Implementasi Interface Schedulable ===

    @Override
    public boolean jadwalkanSesi(LocalDateTime waktuMulai, int durasiMenit) {
        if (waktuMulai == null || waktuMulai.isBefore(LocalDateTime.now())) {
            return false;
        }
        this.waktuMulai = waktuMulai;
        this.durasiMenit = durasiMenit;
        this.waktuSelesai = waktuMulai.plusMinutes(durasiMenit);
        this.statusSesi = StatusSesi.DIJADWALKAN;
        return true;
    }

    @Override
    public boolean ubahJadwal(LocalDateTime waktuBaru) {
        if (!dapatDijadwalkanUlang()) {
            return false;
        }
        if (waktuBaru == null || waktuBaru.isBefore(LocalDateTime.now())) {
            return false;
        }
        this.waktuMulai = waktuBaru;
        this.waktuSelesai = waktuBaru.plusMinutes(this.durasiMenit);
        return true;
    }

    @Override
    public boolean batalkanJadwal(String alasan) {
        if (statusSesi.isBerakhir()) {
            return false;
        }
        this.statusSesi = StatusSesi.DIBATALKAN;
        this.alasanPembatalan = alasan;
        return true;
    }

    @Override
    public boolean dapatDijadwalkanUlang() {
        return statusSesi == StatusSesi.MENUNGGU_KONFIRMASI
            || statusSesi == StatusSesi.DIJADWALKAN;
    }

    // === Implementasi Interface Verifiable ===

    @Override
    public void validasi(Long idSupervisor, String catatan) {
        this.statusValidasi = StatusValidasi.DIVALIDASI;
        this.idValidatorSupervisor = idSupervisor;
        this.catatanValidasi = catatan;
    }

    @Override
    public void tolakValidasi(Long idSupervisor, String alasanPenolakan) {
        this.statusValidasi = StatusValidasi.DITOLAK;
        this.idValidatorSupervisor = idSupervisor;
        this.catatanValidasi = alasanPenolakan;
    }

    @Override
    public StatusValidasi getStatusValidasi() {
        return this.statusValidasi;
    }

    @Override
    public boolean sudahDivalidasi() {
        return this.statusValidasi == StatusValidasi.DIVALIDASI;
    }

    @Override
    public boolean menungguPeninjauan() {
        return this.statusValidasi == StatusValidasi.BELUM_DITINJAU
            || this.statusValidasi == StatusValidasi.SEDANG_DITINJAU;
    }

    // === Method Umum ===

    /**
     * Menandai sesi sebagai sedang berlangsung.
     */
    public void mulaiSesi() {
        this.statusSesi = StatusSesi.BERLANGSUNG;
    }

    /**
     * Menandai sesi sebagai selesai dan mengalokasikan poin progres.
     *
     * @param poinMentor poin yang diberikan kepada mentor
     * @param poinMentee poin yang diberikan kepada mentee
     */
    public void selesaikanSesi(int poinMentor, int poinMentee) {
        this.statusSesi = StatusSesi.SELESAI;
        this.poinMentor = poinMentor;
        this.poinMentee = poinMentee;
        this.waktuSelesai = LocalDateTime.now();
    }

    // === Getter dan Setter ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopikPembahasan() {
        return topikPembahasan;
    }

    public void setTopikPembahasan(String topikPembahasan) {
        this.topikPembahasan = topikPembahasan;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public LocalDateTime getWaktuMulai() {
        return waktuMulai;
    }

    public void setWaktuMulai(LocalDateTime waktuMulai) {
        this.waktuMulai = waktuMulai;
    }

    public LocalDateTime getWaktuSelesai() {
        return waktuSelesai;
    }

    public void setWaktuSelesai(LocalDateTime waktuSelesai) {
        this.waktuSelesai = waktuSelesai;
    }

    public int getDurasiMenit() {
        return durasiMenit;
    }

    public void setDurasiMenit(int durasiMenit) {
        this.durasiMenit = durasiMenit;
    }

    public JenisSesi getJenisSesi() {
        return jenisSesi;
    }

    public void setJenisSesi(JenisSesi jenisSesi) {
        this.jenisSesi = jenisSesi;
    }

    public StatusSesi getStatusSesi() {
        return statusSesi;
    }

    public void setStatusSesi(StatusSesi statusSesi) {
        this.statusSesi = statusSesi;
    }

    public void setStatusValidasi(StatusValidasi statusValidasi) {
        this.statusValidasi = statusValidasi;
    }

    public String getCatatanValidasi() {
        return catatanValidasi;
    }

    public void setCatatanValidasi(String catatanValidasi) {
        this.catatanValidasi = catatanValidasi;
    }

    public Long getIdValidatorSupervisor() {
        return idValidatorSupervisor;
    }

    public void setIdValidatorSupervisor(Long idValidatorSupervisor) {
        this.idValidatorSupervisor = idValidatorSupervisor;
    }

    public String getAlasanPembatalan() {
        return alasanPembatalan;
    }

    public void setAlasanPembatalan(String alasanPembatalan) {
        this.alasanPembatalan = alasanPembatalan;
    }

    public int getPoinMentor() {
        return poinMentor;
    }

    public void setPoinMentor(int poinMentor) {
        this.poinMentor = poinMentor;
    }

    public int getPoinMentee() {
        return poinMentee;
    }

    public void setPoinMentee(int poinMentee) {
        this.poinMentee = poinMentee;
    }

    public LocalDateTime getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(LocalDateTime tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    public Pengguna getMentor() {
        return mentor;
    }

    public void setMentor(Pengguna mentor) {
        this.mentor = mentor;
    }

    public Pengguna getMentee() {
        return mentee;
    }

    public void setMentee(Pengguna mentee) {
        this.mentee = mentee;
    }

    public Pengguna getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Pengguna supervisor) {
        this.supervisor = supervisor;
    }

    public List<ReviewRating> getDaftarReview() {
        return daftarReview;
    }

    public void setDaftarReview(List<ReviewRating> daftarReview) {
        this.daftarReview = daftarReview;
    }

    public List<MateriBelajar> getMateriBelajar() {
        return materiBelajar;
    }

    public void setMateriBelajar(List<MateriBelajar> materiBelajar) {
        this.materiBelajar = materiBelajar;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%s)",
            jenisSesi.getLabel(), topikPembahasan,
            statusSesi.getLabel(), statusValidasi.getLabel());
    }
}
