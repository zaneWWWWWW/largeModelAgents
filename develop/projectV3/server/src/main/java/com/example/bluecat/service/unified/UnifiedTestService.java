package com.example.bluecat.service.unified;

import com.example.bluecat.dto.unified.TestCreateRequest;
import com.example.bluecat.dto.unified.TestDetailDTO;
import com.example.bluecat.dto.unified.TestListDTO;
import com.example.bluecat.dto.unified.TestResultDTO;
import com.example.bluecat.dto.unified.TestSubmissionDTO;
import com.example.bluecat.entity.unified.TestSession;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UnifiedTestService {

    List<TestListDTO> getActiveTests();

    TestDetailDTO getTestDetails(Long testId);

    TestSession createTestSession(Long testId, Long userId);

    TestResultDTO submitAnswers(Long sessionId, TestSubmissionDTO submissionDTO);

    TestResultDTO getTestResult(Long sessionId);

    com.example.bluecat.entity.unified.Test createTest(TestCreateRequest request);

    void importQuestions(Long testId, MultipartFile file);

    byte[] exportResponses(Long testId);

    void offlineTest(Long testId);
}
