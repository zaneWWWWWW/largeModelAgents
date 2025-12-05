package com.example.bluecat.controller.unified;

import com.example.bluecat.dto.unified.TestCreateRequest;
import com.example.bluecat.entity.unified.Test;
import com.example.bluecat.mapper.UserMapper;
import com.example.bluecat.service.unified.UnifiedTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/tests")
@RequiredArgsConstructor
public class TestAdminController {

    private final UnifiedTestService unifiedTestService;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<?> createTest(@RequestBody TestCreateRequest request, Authentication authentication) {
        ensureAdmin(authentication);
        try {
            Test test = unifiedTestService.createTest(request);
            return ResponseEntity.ok(test);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建问卷失败", e);
        }
    }

    @PostMapping("/{id}/import")
    public ResponseEntity<Void> importQuestions(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file,
                                                Authentication authentication) {
        ensureAdmin(authentication);
        unifiedTestService.importQuestions(id, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportResponses(@PathVariable Long id, Authentication authentication) {
        ensureAdmin(authentication);
        byte[] data = unifiedTestService.exportResponses(id);
        String filename = "test-" + id + "-responses.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(data.length)
                .body(data);
    }

    @PostMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id, Authentication authentication) {
        ensureAdmin(authentication);
        unifiedTestService.offlineTest(id);
        return ResponseEntity.ok().build();
    }

    private void ensureAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户无权限执行该操作");
        }
        String username = authentication.getName();
        if (!"admin".equalsIgnoreCase(username) || userMapper.findByUsername(username) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可执行该操作");
        }
    }
}
