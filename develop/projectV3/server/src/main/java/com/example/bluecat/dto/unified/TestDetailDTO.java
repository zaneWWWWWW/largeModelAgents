package com.example.bluecat.dto.unified;

import lombok.Data;
import java.util.List;

@Data
public class TestDetailDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private List<QuestionDTO> questions;

    @Data
    public static class QuestionDTO {
        private Long id;
        private String stem;
        private String type;
        private Integer orderNo;
        private List<OptionDTO> options;
    }

    @Data
    public static class OptionDTO {
        private Long id;
        private String label;
        private Integer orderNo;
    }
}

