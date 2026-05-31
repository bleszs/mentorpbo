package com.mentorpbo.model;

import com.mentorpbo.model.enums.JenisSesi;
import jakarta.persistence.*;

/**
 * Entity SesiOnline - Sesi mentoring yang dilaksanakan secara daring.
 *
 * Menerapkan POLYMORPHISM: Method lakukanSesi(), getInstruksiPersiapan(),
 * dan getRingkasanSesi() diimplementasikan sesuai karakteristik sesi online.
 *
 * Sesi online menggunakan platform video conference dan memerlukan
 * tautan meeting serta kode akses untuk bergabung.
 */
@Entity
@Table(name = "sesi_online")
@DiscriminatorValue("ONLINE")
public class SesiOnline extends SesiMentoring {

    /** Tautan/URL meeting room (Zoom, Google Meet, dll.) */
    @Column(length = 500)
    private String tautanMeeting;

    /** Kode akses atau password untuk masuk ke ruang meeting */
    @Column(length = 50)
    private String kodeAkses;

    /** Platform yang digunakan (Zoom, Google Meet, Microsoft Teams, dll.) */
    @Column(length = 50)
    private String platformDaring;

    /** Catatan teknis seperti kebutuhan bandwidth, perangkat, dll. */
    @Column(length = 500)
    private String catatanTeknis;

    // === Konstruktor ===

    public SesiOnline() {
        super();
    }

    public SesiOnline(String topikPembahasan, int durasiMenit,
                      Pengguna mentor, Pengguna mentee,
                      String tautanMeeting, String platformDaring) {
        super(topikPembahasan, durasiMenit, JenisSesi.ONLINE, mentor, mentee);
        this.tautanMeeting = tautanMeeting;
        this.platformDaring = platformDaring;
    }

    // === Implementasi Method Abstrak (Polymorphism) ===

    /**
     * Melaksanakan sesi secara online melalui platform video conference.
     * Proses: Aktivasi ruang meeting → Verifikasi peserta → Mulai sesi daring.
     */
    @Override
    public String lakukanSesi() {
        mulaiSesi();
        StringBuilder detail = new StringBuilder();
        detail.append(">>> Sesi Online Dimulai <<<\n");
        detail.append(String.format("Platform: %s\n", platformDaring));
        detail.append(String.format("Tautan: %s\n", tautanMeeting));
        detail.append(String.format("Topik: %s\n", getTopikPembahasan()));
        detail.append(String.format("Durasi: %d menit\n", getDurasiMenit()));
        detail.append("Status: Ruang meeting aktif, menunggu peserta bergabung...\n");
        return detail.toString();
    }

    /**
     * Instruksi persiapan khusus untuk sesi online.
     */
    @Override
    public String getInstruksiPersiapan() {
        return String.format(
            "Persiapan Sesi Online:\n"
            + "1. Pastikan koneksi internet stabil (minimal 5 Mbps)\n"
            + "2. Instal atau buka aplikasi %s\n"
            + "3. Gunakan tautan berikut untuk bergabung: %s\n"
            + "4. Masukkan kode akses: %s\n"
            + "5. Siapkan headset/earphone dan kamera\n"
            + "6. Bergabung 5 menit sebelum jadwal dimulai",
            platformDaring, tautanMeeting, kodeAkses != null ? kodeAkses : "Tidak diperlukan"
        );
    }

    /**
     * Ringkasan sesi online yang mencakup informasi platform dan akses.
     */
    @Override
    public String getRingkasanSesi() {
        return String.format(
            "[ONLINE] %s | Platform: %s | Durasi: %d menit | Status: %s",
            getTopikPembahasan(), platformDaring, getDurasiMenit(),
            getStatusSesi().getLabel()
        );
    }

    // === Getter dan Setter ===

    public String getTautanMeeting() {
        return tautanMeeting;
    }

    public void setTautanMeeting(String tautanMeeting) {
        this.tautanMeeting = tautanMeeting;
    }

    public String getKodeAkses() {
        return kodeAkses;
    }

    public void setKodeAkses(String kodeAkses) {
        this.kodeAkses = kodeAkses;
    }

    public String getPlatformDaring() {
        return platformDaring;
    }

    public void setPlatformDaring(String platformDaring) {
        this.platformDaring = platformDaring;
    }

    public String getCatatanTeknis() {
        return catatanTeknis;
    }

    public void setCatatanTeknis(String catatanTeknis) {
        this.catatanTeknis = catatanTeknis;
    }
}
