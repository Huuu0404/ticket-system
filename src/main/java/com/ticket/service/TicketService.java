package com.ticket.service;

import com.ticket.entity.Ticket;
import com.ticket.entity.Order;
import com.ticket.repository.TicketRepository;
import com.ticket.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


import java.math.BigDecimal;

@Service
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    
    public TicketService(TicketRepository ticketRepository, OrderRepository orderRepository) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
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
     * 基礎搶票 - 有超賣風險（用來對比）
     * 問題：高併發時會超賣，多個請求同時讀到相同庫存
     */
    @Transactional
    public Order purchaseTicketBasic(Long ticketId, Long userId, Integer quantity) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("票券不存在"));
        
        // 檢查庫存
        if (ticket.getAvailableStock() < quantity) {
            throw new RuntimeException("庫存不足");
        }
        
        // 扣減庫存
        ticket.setAvailableStock(ticket.getAvailableStock() - quantity);
        ticketRepository.save(ticket);
        
        // 創建訂單
        Order order = new Order();
        order.setUserId(userId);
        order.setTicketId(ticketId);
        order.setQuantity(quantity);
        order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(quantity)));
        
        return orderRepository.save(order);
    }


    /**
     * 樂觀鎖搶票 - 解決超賣問題
     * 原理：每次更新時檢查版本號，如果版本號不匹配則拋出異常
     */
    @Transactional
    public Order purchaseTicketWithOptimisticLock(Long ticketId, Long userId, Integer quantity) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("票券不存在"));
        
        // 檢查庫存
        if (ticket.getAvailableStock() < quantity) {
            throw new RuntimeException("庫存不足");
        }
        
        // 扣減庫存（這裡會自動檢查版本號）
        ticket.setAvailableStock(ticket.getAvailableStock() - quantity);
        try {
            ticketRepository.save(ticket); // 這裡會更新 version + 1
        } catch (Exception e) {
            // 如果版本號衝突，會拋出異常
            throw new RuntimeException("搶票失敗，請重試");
        }
        
        // 創建訂單
        Order order = new Order();
        order.setUserId(userId);
        order.setTicketId(ticketId);
        order.setQuantity(quantity);
        order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(quantity)));
        
        return orderRepository.save(order);
    }
    
    /**
     * 樂觀鎖搶票 + 重試機制（生產環境推薦）
     * 原理：如果版本衝突，自動重試指定的次數
     */
    @Transactional
    public Order purchaseTicketWithRetry(Long ticketId, Long userId, Integer quantity) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new RuntimeException("票券不存在"));
                
                if (ticket.getAvailableStock() < quantity) {
                    throw new RuntimeException("庫存不足");
                }
                
                // 扣減庫存
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
                retryCount++;
                if (retryCount == maxRetries) {
                    throw new RuntimeException("搶票失敗，請稍後再試");
                }
                // 等待一小段時間後重試
                try {
                    Thread.sleep(100); // 100ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("搶票被中斷");
                }
            }
        }
        throw new RuntimeException("搶票失敗");
    }
}