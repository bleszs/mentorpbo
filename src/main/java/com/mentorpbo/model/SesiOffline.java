package com.mentorpbo.model;

import com.mentorpbo.model.enums.JenisSesi;
import jakarta.persistence.*;

/**
 * Entity SesiOffline - Sesi mentoring yang dilaksanakan secara tatap muka.
 *
 * Menerapkan POLYMORPHISM: Method lakukanSesi(), getInstruksiPersiapan(),
 * dan getRingkasanSesi() diimplementasikan sesuai karakteristik sesi tatap muka.
 *
 * Sesi offline memerlukan lokasi fisik pertemuan dan informasi
 * ruangan yang jelas agar mentor dan mentee dapat bertemu.
 */
@Entity
@Table(name = "sesi_offline")
@DiscriminatorValue("OFFLINE")
public class SesiOffline extends SesiMentoring {

    /** Nama lokasi pertemuan (gedung, kampus, perpustakaan, dll.) */
    @Column(length = 200)
    private String namaLokasi;

    /** Alamat lengkap lokasi pertemuan */
    @Column(length = 500)
    private String alamatLengkap;

    /** Nomor ruangan atau nama ruang khusus */
    @Column(length = 50)
    private String nomorRuangan;

    /** Kapasitas maksimal ruangan (jumlah peserta) */
    @Column
    private int kapasitasRuangan;

    /** Fasilitas yang tersedia di lokasi (WiFi, proyektor, whiteboard, dll.) */
    @Column(length = 500)
    private String fasilitasTersedia;

    // === Konstruktor ===

    public SesiOffline() {
        super();
    }

    public SesiOffline(String topikPembahasan, int durasiMenit,
                       Pengguna mentor, Pengguna mentee,
                       String namaLokasi, String nomorRuangan) {
        super(topikPembahasan, durasiMenit, JenisSesi.OFFLINE, mentor, mentee);
        this.namaLokasi = namaLokasi;
        this.nomorRuangan = nomorRuangan;
    }

    // === Implementasi Method Abstrak (Polymorphism) ===

    /**
     * Melaksanakan sesi secara tatap muka di lokasi fisik.
     * Proses: Verifikasi kehadiran → Pembukaan sesi → Diskusi tatap muka.
     */
    @Override
    public String lakukanSesi() {
        mulaiSesi();
        StringBuilder detail = new StringBuilder();
        detail.append(">>> Sesi Tatap Muka Dimulai <<<\n");
        detail.append(String.format("Lokasi: %s - Ruang %s\n", namaLokasi, nomorRuangan));
        detail.append(String.format("Alamat: %s\n",
            alamatLengkap != null ? alamatLengkap : "Lihat peta lokasi"));
        detail.append(String.format("Topik: %s\n", getTopikPembahasan()));
        detail.append(String.format("Durasi: %d menit\n", getDurasiMenit()));
        detail.append("Status: Menunggu kehadiran semua peserta di lokasi...\n");
        return detail.toString();
    }

    /**
     * Instruksi persiapan khusus untuk sesi tatap muka.
     */
    @Override
    public String getInstruksiPersiapan() {
        return String.format(
            "Persiapan Sesi Tatap Muka:\n"
            + "1. Datang ke lokasi: %s\n"
            + "2. Cari ruangan: %s\n"
            + "3. Alamat: %s\n"
            + "4. Bawa alat tulis dan materi belajar yang diperlukan\n"
            + "5. Hadir 10 menit sebelum jadwal dimulai\n"
            + "6. Konfirmasi kehadiran kepada mentor/mentee",
            namaLokasi, nomorRuangan,
            alamatLengkap != null ? alamatLengkap : "Hubungi mentor untuk detail lokasi"
        );
    }

    /**
     * Ringkasan sesi tatap muka yang mencakup informasi lokasi.
     */
    @Override
    public String getRingkasanSesi() {
        return String.format(
            "[OFFLINE] %s | Lokasi: %s (Ruang %s) | Durasi: %d menit | Status: %s",
            getTopikPembahasan(), namaLokasi, nomorRuangan,
            getDurasiMenit(), getStatusSesi().getLabel()
        );
    }

    // === Getter dan Setter ===

    public String getNamaLokasi() {
        return namaLokasi;
    }

    public void setNamaLokasi(String namaLokasi) {
        this.namaLokasi = namaLokasi;
    }

    public String getAlamatLengkap() {
        return alamatLengkap;
    }

    public void setAlamatLengkap(String alamatLengkap) {
        this.alamatLengkap = alamatLengkap;
    }

    public String getNomorRuangan() {
        return nomorRuangan;
    }

    public void setNomorRuangan(String nomorRuangan) {
        this.nomorRuangan = nomorRuangan;
    }

    public int getKapasitasRuangan() {
        return kapasitasRuangan;
    }

    public void setKapasitasRuangan(int kapasitasRuangan) {
        this.kapasitasRuangan = kapasitasRuangan;
    }

    public String getFasilitasTersedia() {
        return fasilitasTersedia;
    }

    public void setFasilitasTersedia(String fasilitasTersedia) {
        this.fasilitasTersedia = fasilitasTersedia;
    }
}
