package com.example.projectv3.model.unified;

import java.util.List;

public class Question {
    private Long id;
    private String stem;
    private String type;
    private Integer orderNo;
    private List<Option> options;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }
}

