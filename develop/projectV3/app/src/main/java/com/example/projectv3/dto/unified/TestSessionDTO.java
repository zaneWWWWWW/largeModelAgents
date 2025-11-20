package com.example.projectv3.dto.unified;

/**
 * 只关心ID等基础信息，时间字段保持字符串避免解析LocalDateTime失败。
 */
public class TestSessionDTO {
    private Long id;
    private Long userId;
    private Long testId;
    private String startedAt;
    private String finishedAt;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

