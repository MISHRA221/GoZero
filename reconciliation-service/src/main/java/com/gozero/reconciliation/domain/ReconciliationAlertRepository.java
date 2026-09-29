package com.gozero.reconciliation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationAlertRepository extends JpaRepository<ReconciliationAlert, Long> {

    List<ReconciliationAlert> findByProductId(Long productId);
}
