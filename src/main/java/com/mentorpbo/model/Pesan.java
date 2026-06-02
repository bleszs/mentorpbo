package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity Pesan - Pesan chat antara mentor dan mentee.
 */
@Entity
@Table(name = "pesan", indexes = {
    @Index(name = "idx_pesan_pengirim", columnList = "pengirim_id"),
    @Index(name = "idx_pesan_penerima", columnList = "penerima_id")
})
public class Pesan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String isi;

    /** Tipe konten: TEKS, GAMBAR, FILE */
    @Column(nullable = false, length = 10)
    private String tipeKonten = "TEKS";

    /** URL file/gambar lampiran */
    @Column(length = 500)
    private String lampiran;

    /** Nama asli file lampiran */
    @Column(length = 255)
    private String namaFile;

    @Column(nullable = false, updatable = false)
    private LocalDateTime waktuKirim;

    @Column(nullable = false)
    private boolean sudahDibaca = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengirim_id", nullable = false)
    private Pengguna pengirim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penerima_id", nullable = false)
    private Pengguna penerima;

    public Pesan() {
        this.waktuKirim = LocalDateTime.now();
    }

    public Pesan(String isi, Pengguna pengirim, Pengguna penerima) {
        this();
        this.isi = isi;
        this.pengirim = pengirim;
        this.penerima = penerima;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsi() { return isi; }
    public void setIsi(String isi) { this.isi = isi; }
    public String getTipeKonten() { return tipeKonten; }
    public void setTipeKonten(String tipeKonten) { this.tipeKonten = tipeKonten; }
    public String getLampiran() { return lampiran; }
    public void setLampiran(String lampiran) { this.lampiran = lampiran; }
    public String getNamaFile() { return namaFile; }
    public void setNamaFile(String namaFile) { this.namaFile = namaFile; }
    public LocalDateTime getWaktuKirim() { return waktuKirim; }
    public void setWaktuKirim(LocalDateTime waktuKirim) { this.waktuKirim = waktuKirim; }
    public boolean isSudahDibaca() { return sudahDibaca; }
    public void setSudahDibaca(boolean sudahDibaca) { this.sudahDibaca = sudahDibaca; }
    public Pengguna getPengirim() { return pengirim; }
    public void setPengirim(Pengguna pengirim) { this.pengirim = pengirim; }
    public Pengguna getPenerima() { return penerima; }
    public void setPenerima(Pengguna penerima) { this.penerima = penerima; }
}
