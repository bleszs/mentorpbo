package com.mentorpbo.model.enums;

/**
 * Enum LingkunganBelajar - Membedakan konteks lingkungan pendidikan.
 *
 * Platform mentoring ini mengakomodasi dua ekosistem pendidikan
 * yang terpisah namun terintegrasi dalam satu sistem:
 * - SEKOLAH: Interaksi antara Guru dan Siswa
 * - KAMPUS: Interaksi antara Dosen dan Mahasiswa
 */
public enum LingkunganBelajar {

    /** Lingkungan pendidikan tingkat sekolah (SMP/SMA/SMK) */
    SEKOLAH("Sekolah", "Lingkungan pendidikan menengah"),

    /** Lingkungan pendidikan tingkat perguruan tinggi */
    KAMPUS("Kampus", "Lingkungan pendidikan tinggi");

    private final String label;
    private final String deskripsi;

    LingkunganBelajar(String label, String deskripsi) {
        this.label = label;
        this.deskripsi = deskripsi;
    }

    public String getLabel() {
        return label;
    }

    public String getDeskripsi() {
        return deskripsi;
    }
}
