package com.ticket.service;

import com.ticket.entity.Ticket;
import com.ticket.entity.Order;
import com.ticket.repository.TicketRepository;
import com.ticket.repository.OrderRepository;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


import java.math.BigDecimal;

@Service
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final RedisService redisService;
    
    public TicketService(TicketRepository ticketRepository, OrderRepository orderRepository, RedisService redisService) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.redisService = redisService;
    }
    
    /**
     * 獲取所有票券
     */
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }
    
    /**
     * 根據ID獲取票券
     */
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("票券不存在"));
    }
    
    /**
     * 創建新票券
     */
    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }


    /**
     * Redis 搶票 
     * 先在 Redis 中原子減庫存，成功後再寫數據庫
     */
    @Transactional
    public Order purchaseTicketWithRedis(Long ticketId, Long userId, Integer quantity) {
        // 1. 先在 Redis 中原子減庫存
        Long remainingStock = redisService.decrementStock(ticketId, quantity);
        
        if (remainingStock == null) {
            throw new RuntimeException("票券不存在或未初始化庫存");
        }
        
        if (remainingStock < 0) {
            // 庫存不足，恢復 Redis 庫存
            redisService.decrementStock(ticketId, -quantity);
            throw new RuntimeException("庫存不足");
        }
        
        try {
            // 2. Redis 減庫存成功，再操作數據庫
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("票券不存在"));
            
            // 再次檢查庫存（雙重保障）
            if (ticket.getAvailableStock() < quantity) {
                // 恢復 Redis 庫存
                redisService.decrementStock(ticketId, -quantity);
                throw new RuntimeException("庫存不足");
            }
            
            // 扣減數據庫庫存
            ticket.setAvailableStock(ticket.getAvailableStock() - quantity);
            ticketRepository.save(ticket);
            
            // 創建訂單
            Order order = new Order();
            order.setUserId(userId);
            order.setTicketId(ticketId);
            order.setQuantity(quantity);
            order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(quantity)));
            
            return orderRepository.save(order);
            
        } catch (Exception e) {
            // 發生異常，恢復 Redis 庫存
            redisService.decrementStock(ticketId, -quantity);
            throw new RuntimeException("搶票失敗: " + e.getMessage());
        }
    }
    
    
    /**
     * 初始化 Redis 庫存（管理員用）
     */
    public void initRedisStock(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("票券不存在"));
        redisService.setStock(ticketId, ticket.getAvailableStock());
    }


    /**
     * 純數據庫樂觀鎖搶票（對比方案）
     */
    @Transactional
    public Order purchaseTicketDBOnly(Long ticketId, Long userId, Integer quantity) {
        System.out.println("🗃️ 純DB方案 - 開始處理: ticket=" + ticketId + ", user=" + userId);
        
        // 使用悲觀鎖或樂觀鎖重試
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 查找票券（帶版本號）
                Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("票券不存在"));
                
                System.out.println("🗃️ DB方案 - 嘗試 " + attempt + ": 庫存=" + ticket.getAvailableStock() + ", 版本=" + ticket.getVersion());
                
                // 檢查庫存
                if (ticket.getAvailableStock() < quantity) {
                    throw new RuntimeException("庫存不足");
                }
                
                // 扣減庫存
                ticket.setAvailableStock(ticket.getAvailableStock() - quantity);
                Ticket savedTicket = ticketRepository.save(ticket); // 這裡會檢查版本號
                
                System.out.println("🗃️ DB方案 - 更新成功: 新庫存=" + savedTicket.getAvailableStock() + ", 新版本=" + savedTicket.getVersion());
                
                // 創建訂單
                Order order = new Order();
                order.setUserId(userId);
                order.setTicketId(ticketId);
                order.setQuantity(quantity);
                order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(quantity)));
                order.setStatus(Order.OrderStatus.PAID);
                
                return orderRepository.save(order);
                
            } catch (ObjectOptimisticLockingFailureException e) {
                System.out.println("🗃️ DB方案 - 樂觀鎖衝突，重試 " + attempt);
                if (attempt == maxRetries) {
                    throw new RuntimeException("搶票失敗，請重試");
                }
                try {
                    Thread.sleep(100 * attempt); // 遞增等待
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("搶票被中斷");
                }
            }
        }
        
        throw new RuntimeException("搶票失敗");
    }
}