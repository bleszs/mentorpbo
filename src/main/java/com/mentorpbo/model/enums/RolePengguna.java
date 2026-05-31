package com.mentorpbo.model.enums;

/**
 * Enum RolePengguna - Mendefinisikan peran pengguna dalam sistem mentoring.
 *
 * Setiap pengguna dalam platform memiliki tepat satu peran utama
 * yang menentukan akses, dashboard, dan fungsionalitas yang tersedia.
 *
 * Peran dibagi menjadi dua lingkungan:
 * - Lingkungan Sekolah: SISWA, GURU
 * - Lingkungan Kampus: MAHASISWA, DOSEN
 */
public enum RolePengguna {

    /** Siswa di lingkungan sekolah (dapat menjadi Mentor atau Mentee) */
    SISWA("Siswa", "Pelajar di lingkungan sekolah"),

    /** Mahasiswa di lingkungan kampus (dapat menjadi Mentor atau Mentee) */
    MAHASISWA("Mahasiswa", "Pelajar di lingkungan perguruan tinggi"),

    /** Guru sebagai Supervisor di lingkungan sekolah */
    GURU("Guru", "Supervisor di lingkungan sekolah"),

    /** Dosen sebagai Supervisor di lingkungan kampus */
    DOSEN("Dosen", "Supervisor di lingkungan perguruan tinggi");

    private final String label;
    private final String deskripsi;

    RolePengguna(String label, String deskripsi) {
        this.label = label;
        this.deskripsi = deskripsi;
    }

    public String getLabel() {
        return label;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    /**
     * Mengecek apakah role ini termasuk dalam lingkungan sekolah.
     */
    public boolean isLingkunganSekolah() {
        return this == SISWA || this == GURU;
    }

    /**
     * Mengecek apakah role ini termasuk dalam lingkungan kampus.
     */
    public boolean isLingkunganKampus() {
        return this == MAHASISWA || this == DOSEN;
    }

    /**
     * Mengecek apakah role ini adalah supervisor (Guru atau Dosen).
     */
    public boolean isSupervisor() {
        return this == GURU || this == DOSEN;
    }

    /**
     * Mengecek apakah role ini adalah peserta didik (Siswa atau Mahasiswa).
     */
    public boolean isPesertaDidik() {
        return this == SISWA || this == MAHASISWA;
    }
}
