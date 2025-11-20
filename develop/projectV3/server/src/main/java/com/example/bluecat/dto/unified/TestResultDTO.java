package com.example.bluecat.dto.unified;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestResultDTO {
    private Long sessionId;
    private BigDecimal totalScore;
    private String level;
    private String resultJson;
}

