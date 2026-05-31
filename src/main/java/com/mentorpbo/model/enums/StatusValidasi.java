package com.mentorpbo.model.enums;

/**
 * Enum StatusValidasi - Status validasi kegiatan mentoring oleh supervisor.
 *
 * Guru dan Dosen memiliki wewenang untuk memvalidasi kegiatan mentoring
 * yang dilakukan oleh siswa/mahasiswa di bawah pengawasannya.
 */
public enum StatusValidasi {

    /** Kegiatan belum ditinjau oleh supervisor */
    BELUM_DITINJAU("Belum Ditinjau"),

    /** Kegiatan sedang dalam proses peninjauan */
    SEDANG_DITINJAU("Sedang Ditinjau"),

    /** Kegiatan telah divalidasi dan disetujui oleh supervisor */
    DIVALIDASI("Divalidasi"),

    /** Kegiatan ditolak oleh supervisor dengan alasan tertentu */
    DITOLAK("Ditolak");

    private final String label;

    StatusValidasi(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Mengecek apakah validasi sudah final (divalidasi atau ditolak).
     */
    public boolean isFinal() {
        return this == DIVALIDASI || this == DITOLAK;
    }
}
