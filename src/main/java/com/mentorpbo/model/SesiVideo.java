package com.mentorpbo.model;

import com.mentorpbo.model.enums.JenisSesi;
import jakarta.persistence.*;

/**
 * Entity SesiVideo - Sesi mentoring berupa rekaman video (asinkron).
 *
 * Menerapkan POLYMORPHISM: Method lakukanSesi(), getInstruksiPersiapan(),
 * dan getRingkasanSesi() diimplementasikan sesuai karakteristik sesi rekaman video.
 *
 * Sesi video bersifat asinkron - mentor merekam materi pembelajaran
 * yang kemudian dapat diakses oleh mentee kapan saja. Format ini
 * sangat cocok untuk materi yang perlu dipelajari berulang kali.
 */
@Entity
@Table(name = "sesi_video")
@DiscriminatorValue("VIDEO")
public class SesiVideo extends SesiMentoring {

    /** Tautan/URL video rekaman yang sudah diunggah */
    @Column(length = 500)
    private String tautanVideo;

    /** Platform penyimpanan video (YouTube, Google Drive, dll.) */
    @Column(length = 50)
    private String platformVideo;

    /** Resolusi/kualitas video (480p, 720p, 1080p) */
    @Column(length = 20)
    private String kualitasVideo;

    /** Ukuran file video dalam MB */
    @Column
    private double ukuranFileMB;

    /** Apakah video memiliki subtitle/teks berjalan */
    @Column(nullable = false)
    private boolean memilikiSubtitle = false;

    /** Jumlah total view/tontonan video */
    @Column(nullable = false)
    private int jumlahTontonan = 0;

    // === Konstruktor ===

    public SesiVideo() {
        super();
    }

    public SesiVideo(String topikPembahasan, int durasiMenit,
                     Pengguna mentor, Pengguna mentee,
                     String tautanVideo, String platformVideo) {
        super(topikPembahasan, durasiMenit, JenisSesi.VIDEO, mentor, mentee);
        this.tautanVideo = tautanVideo;
        this.platformVideo = platformVideo;
    }

    // === Implementasi Method Abstrak (Polymorphism) ===

    /**
     * Melaksanakan sesi berupa rekaman video yang dapat diakses secara asinkron.
     * Proses: Unggah video → Verifikasi kualitas → Publikasi ke mentee.
     */
    @Override
    public String lakukanSesi() {
        mulaiSesi();
        StringBuilder detail = new StringBuilder();
        detail.append(">>> Sesi Video Rekaman Dipublikasikan <<<\n");
        detail.append(String.format("Platform: %s\n", platformVideo));
        detail.append(String.format("Tautan Video: %s\n", tautanVideo));
        detail.append(String.format("Topik: %s\n", getTopikPembahasan()));
        detail.append(String.format("Durasi: %d menit\n", getDurasiMenit()));
        detail.append(String.format("Kualitas: %s | Subtitle: %s\n",
            kualitasVideo != null ? kualitasVideo : "Standar",
            memilikiSubtitle ? "Ya" : "Tidak"));
        detail.append("Status: Video tersedia untuk ditonton kapan saja.\n");
        return detail.toString();
    }

    /**
     * Instruksi persiapan khusus untuk sesi video rekaman.
     */
    @Override
    public String getInstruksiPersiapan() {
        return String.format(
            "Persiapan Sesi Video Rekaman:\n"
            + "1. Buka tautan video: %s\n"
            + "2. Platform: %s\n"
            + "3. Pastikan koneksi internet stabil untuk streaming\n"
            + "4. Siapkan catatan untuk mencatat poin-poin penting\n"
            + "5. Video dapat ditonton berulang kali sesuai kebutuhan\n"
            + "6. Subtitle tersedia: %s",
            tautanVideo, platformVideo,
            memilikiSubtitle ? "Ya (aktifkan di pengaturan)" : "Tidak tersedia"
        );
    }

    /**
     * Ringkasan sesi video yang mencakup informasi platform dan statistik tontonan.
     */
    @Override
    public String getRingkasanSesi() {
        return String.format(
            "[VIDEO] %s | Platform: %s | Durasi: %d menit | Tontonan: %d | Status: %s",
            getTopikPembahasan(), platformVideo, getDurasiMenit(),
            jumlahTontonan, getStatusSesi().getLabel()
        );
    }

    // === Method Spesifik SesiVideo ===

    /**
     * Menambahkan hitungan tontonan saat video ditonton.
     */
    public void tambahTontonan() {
        this.jumlahTontonan++;
    }

    // === Getter dan Setter ===

    public String getTautanVideo() {
        return tautanVideo;
    }

    public void setTautanVideo(String tautanVideo) {
        this.tautanVideo = tautanVideo;
    }

    public String getPlatformVideo() {
        return platformVideo;
    }

    public void setPlatformVideo(String platformVideo) {
        this.platformVideo = platformVideo;
    }

    public String getKualitasVideo() {
        return kualitasVideo;
    }

    public void setKualitasVideo(String kualitasVideo) {
        this.kualitasVideo = kualitasVideo;
    }

    public double getUkuranFileMB() {
        return ukuranFileMB;
    }

    public void setUkuranFileMB(double ukuranFileMB) {
        this.ukuranFileMB = ukuranFileMB;
    }

    public boolean isMemilikiSubtitle() {
        return memilikiSubtitle;
    }

    public void setMemilikiSubtitle(boolean memilikiSubtitle) {
        this.memilikiSubtitle = memilikiSubtitle;
    }

    public int getJumlahTontonan() {
        return jumlahTontonan;
    }

    public void setJumlahTontonan(int jumlahTontonan) {
        this.jumlahTontonan = jumlahTontonan;
    }
}
