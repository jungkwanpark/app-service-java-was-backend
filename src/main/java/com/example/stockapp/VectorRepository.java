package com.example.stockapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VectorRepository extends JpaRepository<VectorData, Integer> {
    // 특정 티커가 이미 존재하는지 확인하는 메소드 추가
    boolean existsByStockTicker(String stockTicker);
}
