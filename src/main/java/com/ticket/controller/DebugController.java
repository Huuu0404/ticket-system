package com.ticket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/redis-test")
    public ResponseEntity<?> testRedis() {
        try {
            // 測試 Redis 連接
            redisTemplate.opsForValue().set("test-key", "hello-redis");
            String value = (String) redisTemplate.opsForValue().get("test-key");
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("redisValue", value);
            result.put("message", "Redis 連接正常！");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "Redis 連接失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}