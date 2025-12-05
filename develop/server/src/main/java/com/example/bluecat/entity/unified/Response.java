package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("responses")
public class Response {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long questionId;
    private Long optionId;
    private BigDecimal valueNumeric;
    private String valueText;
}

