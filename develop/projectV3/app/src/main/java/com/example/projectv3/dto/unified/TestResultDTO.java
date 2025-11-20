package com.example.projectv3.dto.unified;

import java.math.BigDecimal;

public class TestResultDTO {
    private Long sessionId;
    private BigDecimal totalScore;
    private String level;
    private String resultJson;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
}

