package com.gozero.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformListingRepository extends JpaRepository<PlatformListing, Long> {

    List<PlatformListing> findByProductIdOrderByPlatformAsc(Long productId);

    List<PlatformListing> findAllByOrderBySkuAscPlatformAsc();
}
