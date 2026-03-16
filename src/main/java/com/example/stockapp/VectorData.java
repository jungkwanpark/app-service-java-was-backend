package com.example.stockapp;

import jakarta.persistence.*;

@Entity
@Table(name = "vectordbtable")
public class VectorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @Column(name = "stockticker", nullable = false, length = 10)
    private String stockTicker;

    // VectorAnalysis 컬럼은 Java에서 처리하지 않으므로 필드를 작성하지 않습니다.
    // DB의 나머지 컬럼(IDX, StockTicker)만 매핑합니다.

    // Getters and Setters
    public Integer getIdx() { return idx; }
    public void setIdx(Integer idx) { this.idx = idx; }

    public String getStockTicker() { return stockTicker; }
    public void setStockTicker(String stockTicker) { this.stockTicker = stockTicker; }
}

