package com.example.bluecat.dto.unified;

import lombok.Data;

@Data
public class TestCreateRequest {
    private String code;
    private String name;
    private String description;
    private String category;
    private String totalScoreThresholds;
}

