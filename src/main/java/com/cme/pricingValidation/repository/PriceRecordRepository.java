package com.cme.pricingValidation.repository;

import com.cme.pricingValidation.entity.PriceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecordEntity,Long> {
    List<PriceRecordEntity> findByValidFalse();
    List<PriceRecordEntity> findByValidTrue();
}
