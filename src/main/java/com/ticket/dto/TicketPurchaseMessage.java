package com.ticket.dto;

import java.io.Serializable;

public class TicketPurchaseMessage implements Serializable {
    private String orderSn;
    private Long ticketId;
    private Long userId;
    private Integer quantity;
    
    public TicketPurchaseMessage() {
    }
    
    public TicketPurchaseMessage(String orderSn, Long ticketId, Long userId, Integer quantity) {
        this.orderSn = orderSn;
        this.ticketId = ticketId;
        this.userId = userId;
        this.quantity = quantity;
    }
    
    public String getOrderSn() { return orderSn; }
    public void setOrderSn(String orderSn) { this.orderSn = orderSn; }
    
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    @Override
    public String toString() {
        return "TicketPurchaseMessage{" +
                "orderSn='" + orderSn + '\'' +
                ", ticketId=" + ticketId +
                ", userId=" + userId +
                ", quantity=" + quantity +
                '}';
    }
}