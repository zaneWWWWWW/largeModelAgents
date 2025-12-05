package com.example.bluecat.dto.unified;

import lombok.Data;
import java.util.List;

@Data
public class TestSubmissionDTO {
    private List<AnswerDTO> answers;

    @Data
    public static class AnswerDTO {
        private Long questionId;
        private Long optionId;
    }
}

