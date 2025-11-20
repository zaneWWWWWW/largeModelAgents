package com.example.bluecat.mapper.unified;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bluecat.entity.unified.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}

