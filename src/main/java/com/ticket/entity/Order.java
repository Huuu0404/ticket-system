package com.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_sn", unique = true, nullable = false)
    private String orderSn;  // 新增：訂單號
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long ticketId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(length = 500)
    private String remarks;  // 新增：備註（用於存儲失敗原因等）
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum OrderStatus {
        PENDING,    // 待支付
        PAID,       // 已支付
        CANCELLED,  // 已取消
        EXPIRED,    // 已過期
        FAILED      // 新增：處理失敗
    }
    
    // 無參構造器 - 生成默認訂單號
    public Order() {
        this.orderSn = "TEMP" + System.currentTimeMillis();
    }
    
    // 全參構造器（可選）
    public Order(String orderSn, Long userId, Long ticketId, Integer quantity, 
                 BigDecimal totalAmount, OrderStatus status) {
        this.orderSn = orderSn;
        this.userId = userId;
        this.ticketId = ticketId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        // 確保 orderSn 不為空
        if (orderSn == null || orderSn.startsWith("TEMP")) {
            orderSn = "T" + System.currentTimeMillis() + String.format("%03d", (int)(Math.random() * 1000));
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}