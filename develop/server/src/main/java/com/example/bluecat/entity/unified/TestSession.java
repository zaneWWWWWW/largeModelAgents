package com.example.bluecat.entity.unified;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_sessions")
public class TestSession {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long testId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
}

