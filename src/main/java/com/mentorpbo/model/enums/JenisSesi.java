package com.mentorpbo.model.enums;

/**
 * Enum JenisSesi - Menentukan jenis/format pelaksanaan sesi mentoring.
 *
 * Setiap sesi mentoring dapat dilaksanakan dalam tiga format berbeda,
 * masing-masing dengan karakteristik dan kebutuhan yang unik.
 */
public enum JenisSesi {

    /** Sesi dilaksanakan secara daring melalui platform video conference */
    ONLINE("Online", "Sesi daring melalui video conference"),

    /** Sesi dilaksanakan secara tatap muka langsung di lokasi fisik */
    OFFLINE("Offline", "Sesi tatap muka di lokasi fisik"),

    /** Sesi berupa rekaman video yang dapat diakses kapan saja (asinkron) */
    VIDEO("Rekaman Video", "Sesi asinkron melalui video rekaman");

    private final String label;
    private final String deskripsi;

    JenisSesi(String label, String deskripsi) {
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
