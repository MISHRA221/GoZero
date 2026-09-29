package com.gozero.ingestion.repository;

import com.gozero.ingestion.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByIdAsc(Long productId);
}
