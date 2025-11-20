package com.example.projectv3.dto;

public class PsychStatusDTO {
    public Long userId;
    public Integer depressionLevel;
    public Integer anxietyLevel;
    public String riskFlag;
    public Integer distressScore;
    public Long timestamp;
    public String source; // LOCAL_AGENT / ADVANCED_AGENT / OFFLINE

    public PsychStatusDTO() {}

    public PsychStatusDTO(Long userId, Integer depressionLevel, Integer anxietyLevel,
                          String riskFlag, Integer distressScore, Long timestamp, String source) {
        this.userId = userId;
        this.depressionLevel = depressionLevel;
        this.anxietyLevel = anxietyLevel;
        this.riskFlag = riskFlag;
        this.distressScore = distressScore;
        this.timestamp = timestamp;
        this.source = source;
    }
}
