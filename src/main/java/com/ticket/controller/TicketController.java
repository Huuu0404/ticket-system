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
     * 基礎搶票（有超賣風險 - 用於測試對比）
     */
    @PostMapping("/{id}/purchase-basic")
    public ResponseEntity<?> purchaseBasic(@PathVariable Long id, 
                                          @RequestParam Integer quantity,
                                          @RequestHeader("Authorization") String token) {
        try {
            // 從 token 中提取用戶ID（暫時寫死，後面會實現）
            Long userId = 1L; 
            
            Order order = ticketService.purchaseTicketBasic(id, userId, quantity);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * 樂觀鎖搶票（推薦）
     */
    @PostMapping("/{id}/purchase-optimistic")
    public ResponseEntity<?> purchaseOptimistic(@PathVariable Long id,
                                               @RequestParam Integer quantity,
                                               @RequestHeader("Authorization") String token) {
        try {
            Long userId = 1L; // 暫時寫死
            
            Order order = ticketService.purchaseTicketWithOptimisticLock(id, userId, quantity);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * 樂觀鎖搶票 + 重試機制（生產環境）
     */
    @PostMapping("/{id}/purchase-retry")
    public ResponseEntity<?> purchaseWithRetry(@PathVariable Long id,
                                              @RequestParam Integer quantity,
                                              @RequestHeader("Authorization") String token) {
        try {
            Long userId = 1L; // 暫時寫死
            
            Order order = ticketService.purchaseTicketWithRetry(id, userId, quantity);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}