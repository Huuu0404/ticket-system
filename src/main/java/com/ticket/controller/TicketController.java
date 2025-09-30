package com.ticket.controller;

import com.ticket.entity.Ticket;
import com.ticket.entity.Order;
import com.ticket.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    
    private final TicketService ticketService;
    
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
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
     * Redis 搶票
     */
    @PostMapping("/{id}/purchase-redis")
    public ResponseEntity<?> purchaseWithRedis(@PathVariable Long id, @RequestParam Integer quantity, @RequestHeader("Authorization") String token) {
        try {
            Long userId = 1L; // 暫時寫死
            
            Order order = ticketService.purchaseTicketWithRedis(id, userId, quantity);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}