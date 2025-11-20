package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("options")
public class Option {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private String label;
    private String valueStr;
    private BigDecimal score;
    private Integer orderNo;
}

