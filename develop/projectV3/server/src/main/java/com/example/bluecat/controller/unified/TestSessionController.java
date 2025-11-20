package com.example.bluecat.controller.unified;

import com.example.bluecat.dto.unified.TestResultDTO;
import com.example.bluecat.dto.unified.TestSubmissionDTO;
import com.example.bluecat.service.unified.UnifiedTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 针对会话级接口提供 /api/sessions/... 映射，便于移动端直接调用。
 */
@RestController
@RequestMapping("/api/sessions")
public class TestSessionController {

    private final UnifiedTestService unifiedTestService;

    public TestSessionController(UnifiedTestService unifiedTestService) {
        this.unifiedTestService = unifiedTestService;
    }

    @PostMapping("/{sessionId}/responses")
    public ResponseEntity<TestResultDTO> submitAnswers(@PathVariable Long sessionId,
                                                       @RequestBody TestSubmissionDTO submissionDTO) {
        try {
            TestResultDTO result = unifiedTestService.submitAnswers(sessionId, submissionDTO);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{sessionId}/result")
    public ResponseEntity<TestResultDTO> getTestResult(@PathVariable Long sessionId) {
        TestResultDTO result = unifiedTestService.getTestResult(sessionId);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }
}

