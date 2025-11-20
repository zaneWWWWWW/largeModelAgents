package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tests")
public class Test {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private Integer version;
    private Boolean isActive;
    // 形如："0:正常;10:轻度;20:中度;30:重度"
    private String totalScoreThresholds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

