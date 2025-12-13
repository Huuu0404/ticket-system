// src/main/java/com/ticket/service/TicketMQService.java
package com.ticket.service;

import com.ticket.dto.TicketPurchaseMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class TicketMQService {
    
    private final RedisService redisService;
    private final RabbitTemplate rabbitTemplate;
    
    public TicketMQService(RedisService redisService, RabbitTemplate rabbitTemplate) {
        this.redisService = redisService;
        this.rabbitTemplate = rabbitTemplate;
    }
    
    /**
     * 純異步搶票
     */
    public Map<String, Object> purchaseTicketAsync(Long ticketId, Long userId, Integer quantity) {
        // 1. 先檢查 Redis 庫存（不直接減）
        Integer currentStock = redisService.getStock(ticketId);
        if (currentStock == null || currentStock < quantity) {
            throw new RuntimeException("庫存不足");
        }
        
        // 2. Redis 原子減庫存
        Long remainingStock = redisService.decrementStock(ticketId, quantity);
        
        if (remainingStock < 0) {
            // 立即恢復庫存
            redisService.incrementStock(ticketId, quantity);
            throw new RuntimeException("庫存不足");
        }
        
        // 3. 生成唯一訂單號（不存DB）
        String orderSn = generateOrderSn();
        
        // 4. 發送消息到隊列
        TicketPurchaseMessage message = new TicketPurchaseMessage();
        message.setOrderSn(orderSn);
        message.setTicketId(ticketId);
        message.setUserId(userId);
        message.setQuantity(quantity);
        
        rabbitTemplate.convertAndSend("ticket.purchase.exchange", "ticket.purchase", message);
        
        System.out.println("✅ 搶票請求已發送MQ: " + orderSn);
        
        // 5. 立即返回！
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "搶票成功");
        result.put("orderSn", orderSn);
        result.put("note", "訂單正在處理中，請稍後查看");
        
        return result;
    }
    
    /**
     * 生成唯一訂單號
     */
    private String generateOrderSn() {
        return "T" + UUID.randomUUID().toString().replace("-", "");
    }
}