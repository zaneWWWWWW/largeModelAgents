package com.example.projectv3.dto.unified;

import java.util.List;

public class TestDetailDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private List<QuestionDTO> questions;

    public static class QuestionDTO {
        private Long id;
        private String stem;
        private String type;
        private Integer orderNo;
        private List<OptionDTO> options;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getStem() { return stem; }
        public void setStem(String stem) { this.stem = stem; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getOrderNo() { return orderNo; }
        public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
        public List<OptionDTO> getOptions() { return options; }
        public void setOptions(List<OptionDTO> options) { this.options = options; }
    }

    public static class OptionDTO {
        private Long id;
        private String label;
        private Integer orderNo;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getOrderNo() { return orderNo; }
        public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }
}

