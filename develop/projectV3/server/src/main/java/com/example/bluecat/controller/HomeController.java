package com.example.bluecat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("message", "Bluecat server is running");
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(body);
    }
}