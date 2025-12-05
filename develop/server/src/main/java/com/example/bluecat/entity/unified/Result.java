package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("results")
public class Result {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private BigDecimal totalScore;
    private String level;
    private String resultJson;
    private LocalDateTime createdAt;
}

