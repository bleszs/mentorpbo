package com.mentorpbo.model.enums;

/**
 * Enum StatusSesi - Merepresentasikan siklus hidup (lifecycle) sesi mentoring.
 *
 * Alur status sesi:
 * MENUNGGU_KONFIRMASI → DIJADWALKAN → BERLANGSUNG → SELESAI
 *                    ↘ DIBATALKAN
 */
public enum StatusSesi {

    /** Sesi baru dibuat, menunggu konfirmasi dari mentor */
    MENUNGGU_KONFIRMASI("Menunggu Konfirmasi"),

    /** Sesi telah dikonfirmasi dan dijadwalkan */
    DIJADWALKAN("Dijadwalkan"),

    /** Sesi sedang berlangsung saat ini */
    BERLANGSUNG("Sedang Berlangsung"),

    /** Sesi telah selesai dilaksanakan */
    SELESAI("Selesai"),

    /** Sesi dibatalkan oleh salah satu pihak */
    DIBATALKAN("Dibatalkan");

    private final String label;

    StatusSesi(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Mengecek apakah sesi masih dalam status aktif (belum selesai/dibatalkan).
     */
    public boolean isAktif() {
        return this == MENUNGGU_KONFIRMASI || this == DIJADWALKAN || this == BERLANGSUNG;
    }

    /**
     * Mengecek apakah sesi sudah berakhir (selesai atau dibatalkan).
     */
    public boolean isBerakhir() {
        return this == SELESAI || this == DIBATALKAN;
    }
}
