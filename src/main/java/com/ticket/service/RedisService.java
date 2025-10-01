// src/main/java/com/ticket/service/RedisService.java
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

    public void setStock(Long ticketId, Integer stock) {
        String key = "ticket:stock:" + ticketId;
        redisTemplate.opsForValue().set(key, stock, 1, TimeUnit.HOURS);
    }

    public Integer getStock(Long ticketId) {
        String key = "ticket:stock:" + ticketId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? (Integer) value : null;
    }

    public Long decrementStock(Long ticketId, Integer quantity) {
        String key = "ticket:stock:" + ticketId;
        Long result = redisTemplate.opsForValue().decrement(key, quantity);
        System.out.println("🔴 Redis 操作: DECR ticket=" + ticketId + ", quantity=" + quantity + ", 結果=" + result);
        return result;
    }

    public Long incrementStock(Long ticketId, Integer quantity) {
        String key = "ticket:stock:" + ticketId;
        Long result = redisTemplate.opsForValue().increment(key, quantity);
        System.out.println("🟢 Redis 操作: INCR ticket=" + ticketId + ", quantity=" + quantity + ", 結果=" + result);
        return result;
    }

    public void deleteStock(Long ticketId) {
        String key = "ticket:stock:" + ticketId;
        redisTemplate.delete(key);
    }
}