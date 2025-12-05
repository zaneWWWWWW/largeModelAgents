package com.example.bluecat.controller.unified;

import com.example.bluecat.dto.unified.TestDetailDTO;
import com.example.bluecat.dto.unified.TestListDTO;
import com.example.bluecat.dto.unified.TestResultDTO;
import com.example.bluecat.dto.unified.TestSubmissionDTO;
import com.example.bluecat.entity.User;
import com.example.bluecat.entity.unified.TestSession;
import com.example.bluecat.mapper.UserMapper;
import com.example.bluecat.service.unified.UnifiedTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tests")
public class UnifiedTestController {

    private final UnifiedTestService unifiedTestService;
    private final UserMapper userMapper;

    public UnifiedTestController(UnifiedTestService unifiedTestService, UserMapper userMapper) {
        this.unifiedTestService = unifiedTestService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ResponseEntity<List<TestListDTO>> getActiveTests() {
        return ResponseEntity.ok(unifiedTestService.getActiveTests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestDetailDTO> getTestDetails(@PathVariable Long id) {
        TestDetailDTO testDetails = unifiedTestService.getTestDetails(id);
        return testDetails != null ? ResponseEntity.ok(testDetails) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/sessions")
    public ResponseEntity<TestSession> createTestSession(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("未认证的请求尝试创建测试会话，testId={}", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("根据用户名 {} 未找到用户，拒绝创建测试会话", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TestSession session = unifiedTestService.createTestSession(id, user.getId());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/responses")
    public ResponseEntity<TestResultDTO> submitAnswers(@PathVariable Long sessionId, @RequestBody TestSubmissionDTO submissionDTO) {
        try {
            TestResultDTO result = unifiedTestService.submitAnswers(sessionId, submissionDTO);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<TestResultDTO> getTestResult(@PathVariable Long sessionId) {
        TestResultDTO result = unifiedTestService.getTestResult(sessionId);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }
}

