package com.example.projectv3.dto.unified;

import java.util.List;

public class TestSubmissionDTO {
    private List<AnswerDTO> answers;

    public static class AnswerDTO {
        private Long questionId;
        private Long optionId;
        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public Long getOptionId() { return optionId; }
        public void setOptionId(Long optionId) { this.optionId = optionId; }
    }

    public List<AnswerDTO> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDTO> answers) { this.answers = answers; }
}

