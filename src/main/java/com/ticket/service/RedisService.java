package com.ticket.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 設置庫存到 Redis
     */
    public void setStock(Long ticketId, Integer stock) {
        String key = "ticket:stock:" + ticketId;
        redisTemplate.opsForValue().set(key, stock, 1, TimeUnit.HOURS); // 1小時過期
    }

    /**
     * 從 Redis 獲取庫存
     */
    public Integer getStock(Long ticketId) {
        String key = "ticket:stock:" + ticketId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? (Integer) value : null;
    }

    /**
     * Redis 原子減庫存
     * 返回減庫存後的結果
     */
    public Long decrementStock(Long ticketId, Integer quantity) {
        String key = "ticket:stock:" + ticketId;
        return redisTemplate.opsForValue().decrement(key, quantity);
    }

    /**
     * 刪除庫存緩存
     */
    public void deleteStock(Long ticketId) {
        String key = "ticket:stock:" + ticketId;
        redisTemplate.delete(key);
    }
}