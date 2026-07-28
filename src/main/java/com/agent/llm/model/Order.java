package com.agent.llm.model;

public class Order {
    private String orderId;
    private String customerName;
    private Double amount;
    private String status;
    private String itemName;
    private Integer quantity;

    public Order() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "Order{orderId='" + orderId + "', customer='" + customerName
                + "', amount=" + amount + ", status='" + status
                + "', item='" + itemName + "', qty=" + quantity + "}";
    }
}