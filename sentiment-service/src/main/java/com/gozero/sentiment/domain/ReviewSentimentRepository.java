package com.gozero.sentiment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewSentimentRepository extends JpaRepository<ReviewSentiment, Long> {

    List<ReviewSentiment> findByLabelOrderByNormalizedScoreAsc(String label);
}
