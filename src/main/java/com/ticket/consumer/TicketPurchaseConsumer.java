package com.ticket.consumer;

import com.ticket.dto.TicketPurchaseMessage;
import com.ticket.entity.Ticket;
import com.ticket.entity.Order;
import com.ticket.repository.TicketRepository;
import com.ticket.repository.OrderRepository;
import com.ticket.service.RedisService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TicketPurchaseConsumer {
    
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final RedisService redisService;
    
    public TicketPurchaseConsumer(TicketRepository ticketRepository,  OrderRepository orderRepository, RedisService redisService) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.redisService = redisService;
    }
    
    @RabbitListener(queues = "ticket.purchase.queue")
    public void processPurchase(TicketPurchaseMessage message) {
        System.out.println("🟡 開始處理訂單: " + message.getOrderSn());
        
        try {
            // 1. 檢查票券是否存在
            Ticket ticket = ticketRepository.findById(message.getTicketId())
                    .orElseThrow(() -> new RuntimeException("票券不存在"));
            
            // 2. 檢查數據庫庫存（雙重保障）
            if (ticket.getAvailableStock() < message.getQuantity()) {
                // 庫存不足，恢復 Redis 並創建失敗訂單
                redisService.incrementStock(message.getTicketId(), message.getQuantity());
                createFailedOrder(message, "庫存不足");
                System.out.println("❌ 訂單處理失敗 - 庫存不足: " + message.getOrderSn());
                return;
            }
            
            // 3. 扣減數據庫庫存
            ticket.setAvailableStock(ticket.getAvailableStock() - message.getQuantity());
            ticketRepository.save(ticket);
            
            // 4. 創建成功訂單
            createSuccessOrder(message, ticket);
            
            System.out.println("✅ 訂單處理成功: " + message.getOrderSn());
            
        } catch (Exception e) {
            // 處理失敗，恢復 Redis 並創建失敗訂單
            redisService.incrementStock(message.getTicketId(), message.getQuantity());
            createFailedOrder(message, "系統錯誤: " + e.getMessage());
            System.out.println("❌ 訂單處理失敗 - 系統錯誤: " + message.getOrderSn() + ", 錯誤: " + e.getMessage());
        }
    }
    
    /**
     * 創建成功訂單
     */
    private void createSuccessOrder(TicketPurchaseMessage message, Ticket ticket) {
        Order order = new Order();
        order.setOrderSn(message.getOrderSn());
        order.setUserId(message.getUserId());
        order.setTicketId(message.getTicketId());
        order.setQuantity(message.getQuantity());
        order.setTotalAmount(ticket.getPrice().multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setStatus(Order.OrderStatus.PAID);
        
        orderRepository.save(order);
    }
    
    /**
     * 創建失敗訂單
     */
    private void createFailedOrder(TicketPurchaseMessage message, String errorReason) {
        try {
            // 查找票券信息來計算金額
            Ticket ticket = ticketRepository.findById(message.getTicketId()).orElse(null);
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            if (ticket != null) {
                totalAmount = ticket.getPrice().multiply(BigDecimal.valueOf(message.getQuantity()));
            }
            
            Order order = new Order();
            order.setOrderSn(message.getOrderSn());
            order.setUserId(message.getUserId());
            order.setTicketId(message.getTicketId());
            order.setQuantity(message.getQuantity());
            order.setTotalAmount(totalAmount);
            order.setStatus(Order.OrderStatus.FAILED);
            order.setRemarks(errorReason);
            
            orderRepository.save(order);
            System.out.println("📝 創建失敗訂單完成: " + order.getId() + " - " + errorReason);
            
        } catch (Exception e) {
            System.err.println("💥 創建失敗訂單異常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}