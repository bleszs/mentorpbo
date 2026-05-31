package com.mentorpbo.model.interfaces;

/**
 * Interface Ratable - Kontrak untuk entitas yang dapat diberi penilaian/rating.
 *
 * Interface ini menerapkan prinsip ABSTRAKSI dalam OOP.
 * Mentor dan sesi mentoring dapat dinilai oleh mentee setelah sesi selesai.
 * Sistem rating ini menjadi dasar untuk menentukan kualitas mentor,
 * ranking, dan kelayakan menjadi Asisten Dosen (Asdos).
 *
 * Skala rating: 1 (Sangat Buruk) hingga 5 (Sangat Baik)
 */
public interface Ratable {

    /**
     * Memberikan rating kepada entitas ini.
     *
     * @param nilaiRating nilai rating antara 1-5
     * @param ulasan ulasan/komentar dari pemberi rating
     * @throws IllegalArgumentException jika nilaiRating di luar rentang 1-5
     */
    void beriRating(int nilaiRating, String ulasan);

    /**
     * Menghitung rata-rata rating yang diterima oleh entitas ini.
     *
     * @return rata-rata rating dalam bentuk desimal (misal: 4.5)
     */
    double hitungRataRataRating();

    /**
     * Mendapatkan jumlah total rating yang sudah diterima.
     *
     * @return jumlah total rating
     */
    int getTotalPenilaian();

    /**
     * Mengecek apakah entitas ini sudah memenuhi syarat rating minimum.
     * Digunakan untuk validasi kelayakan mentor atau kandidat Asdos.
     *
     * @param minimumRating batas minimum rating yang harus dipenuhi
     * @return true jika rata-rata rating >= minimumRating
     */
    boolean memenuhinSyaratRating(double minimumRating);
}
