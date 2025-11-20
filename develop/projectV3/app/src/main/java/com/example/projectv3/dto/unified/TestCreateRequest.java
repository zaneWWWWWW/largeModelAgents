package com.example.projectv3.dto.unified;

public class TestCreateRequest {
    private String code;
    private String name;
    private String description;
    private String category;
    private String totalScoreThresholds;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTotalScoreThresholds() { return totalScoreThresholds; }
    public void setTotalScoreThresholds(String totalScoreThresholds) { this.totalScoreThresholds = totalScoreThresholds; }
}

