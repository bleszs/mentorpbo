package com.mentorpbo.repository;

import com.mentorpbo.model.ReviewRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository ReviewRating - Antarmuka akses data untuk ulasan dan penilaian.
 */
@Repository
public interface ReviewRatingRepository extends JpaRepository<ReviewRating, Long> {

    List<ReviewRating> findBySesiMentoringId(Long sesiId);

    List<ReviewRating> findByPenerimaReviewId(Long penerimaId);

    List<ReviewRating> findByPemberiReviewId(Long pemberiId);

    @Query("SELECT AVG(r.nilaiRating) FROM ReviewRating r WHERE r.penerimaReview.id = :id")
    Double hitungRataRataRating(@Param("id") Long penggunaId);

    long countByPenerimaReviewId(Long penerimaId);
}
