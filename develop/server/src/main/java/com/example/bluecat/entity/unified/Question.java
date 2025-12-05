package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("questions")
public class Question {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long testId;
    private String stem;
    private String type;
    private Integer orderNo;
    private String metaJson;
}

