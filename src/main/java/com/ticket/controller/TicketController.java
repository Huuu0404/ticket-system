package com.ticket.controller;

import com.ticket.entity.Ticket;
import com.ticket.entity.Order;
import com.ticket.service.TicketService;
import com.ticket.service.TicketMQService;
import com.ticket.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    
    private final TicketService ticketService;
    private final TicketMQService ticketMQService;
    private final OrderRepository orderRepository;
    
    public TicketController(TicketService ticketService, TicketMQService ticketMQService, OrderRepository orderRepository) {
        this.ticketService = ticketService;
        this.ticketMQService = ticketMQService;
        this.orderRepository = orderRepository;
    }
    
    /**
     * 獲取所有票券
     */
    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * 獲取單個票券詳情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }
    
    /**
     * Redis + MQ 異步搶票（終極方案）
     */
    @PostMapping("/{id}/purchase-async")
    public ResponseEntity<?> purchaseAsync(@PathVariable Long id, @RequestParam Integer quantity, @RequestHeader("Authorization") String token) {
        try {
            Long userId = 1L; // 暫時寫死
            
            Map<String, Object> result = ticketMQService.purchaseTicketAsync(id, userId, quantity);
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 查詢訂單狀態
     */
    @GetMapping("/orders/{orderSn}")
    public ResponseEntity<?> getOrder(@PathVariable String orderSn) {
        Optional<Order> orderOpt = orderRepository.findByOrderSn(orderSn);
        
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "status", "processing",
                "message", "訂單正在處理中，請稍後刷新"
            ));
        }
        
        Order order = orderOpt.get();
        return ResponseEntity.ok(Map.of(
            "status", order.getStatus().toString().toLowerCase(),
            "order", order
        ));
    }
    
    /**
     * 初始化 Redis 庫存
     */
    @PostMapping("/{id}/init-redis")
    public ResponseEntity<?> initRedisStock(@PathVariable Long id) {
        try {
            ticketService.initRedisStock(id);
            return ResponseEntity.ok("Redis 庫存初始化成功");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}