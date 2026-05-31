package com.mentorpbo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity ReviewRating - Merepresentasikan ulasan dan penilaian setelah sesi mentoring.
 *
 * Setiap mentee dapat memberikan satu review dan rating setelah sesi selesai.
 * Data ini digunakan untuk menghitung performa mentor dan menentukan
 * kelayakan menjadi Asisten Dosen (Asdos).
 *
 * Skala rating: 1 (Sangat Buruk) - 5 (Sangat Baik)
 */
@Entity
@Table(name = "review_rating")
public class ReviewRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nilai rating yang diberikan (1-5) */
    @Column(nullable = false)
    private int nilaiRating;

    /** Ulasan tertulis dari mentee */
    @Column(length = 1000)
    private String ulasan;

    /** Aspek positif yang dirasakan mentee dari sesi */
    @Column(length = 500)
    private String aspekPositif;

    /** Aspek yang perlu diperbaiki dari sesi */
    @Column(length = 500)
    private String aspekPerbaikan;

    /** Apakah mentee merekomendasikan mentor ini ke orang lain */
    @Column(nullable = false)
    private boolean merekomendasikan = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime tanggalReview;

    /**
     * Relasi ManyToOne ke sesi mentoring yang di-review.
     * Satu sesi dapat memiliki satu review dari mentee.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesi_id", nullable = false)
    private SesiMentoring sesiMentoring;

    /**
     * Relasi ManyToOne ke Pengguna yang memberikan review (mentee).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pemberi_review_id", nullable = false)
    private Pengguna pemberiReview;

    /**
     * Relasi ManyToOne ke Pengguna yang menerima review (mentor).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penerima_review_id", nullable = false)
    private Pengguna penerimaReview;

    // === Konstruktor ===

    public ReviewRating() {
        this.tanggalReview = LocalDateTime.now();
    }

    public ReviewRating(int nilaiRating, String ulasan,
                        SesiMentoring sesiMentoring,
                        Pengguna pemberiReview, Pengguna penerimaReview) {
        this();
        if (nilaiRating < 1 || nilaiRating > 5) {
            throw new IllegalArgumentException("Nilai rating harus antara 1 sampai 5.");
        }
        this.nilaiRating = nilaiRating;
        this.ulasan = ulasan;
        this.sesiMentoring = sesiMentoring;
        this.pemberiReview = pemberiReview;
        this.penerimaReview = penerimaReview;
    }

    // === Getter dan Setter ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNilaiRating() {
        return nilaiRating;
    }

    public void setNilaiRating(int nilaiRating) {
        this.nilaiRating = nilaiRating;
    }

    public String getUlasan() {
        return ulasan;
    }

    public void setUlasan(String ulasan) {
        this.ulasan = ulasan;
    }

    public String getAspekPositif() {
        return aspekPositif;
    }

    public void setAspekPositif(String aspekPositif) {
        this.aspekPositif = aspekPositif;
    }

    public String getAspekPerbaikan() {
        return aspekPerbaikan;
    }

    public void setAspekPerbaikan(String aspekPerbaikan) {
        this.aspekPerbaikan = aspekPerbaikan;
    }

    public boolean isMerekomendasikan() {
        return merekomendasikan;
    }

    public void setMerekomendasikan(boolean merekomendasikan) {
        this.merekomendasikan = merekomendasikan;
    }

    public LocalDateTime getTanggalReview() {
        return tanggalReview;
    }

    public void setTanggalReview(LocalDateTime tanggalReview) {
        this.tanggalReview = tanggalReview;
    }

    public SesiMentoring getSesiMentoring() {
        return sesiMentoring;
    }

    public void setSesiMentoring(SesiMentoring sesiMentoring) {
        this.sesiMentoring = sesiMentoring;
    }

    public Pengguna getPemberiReview() {
        return pemberiReview;
    }

    public void setPemberiReview(Pengguna pemberiReview) {
        this.pemberiReview = pemberiReview;
    }

    public Pengguna getPenerimaReview() {
        return penerimaReview;
    }

    public void setPenerimaReview(Pengguna penerimaReview) {
        this.penerimaReview = penerimaReview;
    }
}
