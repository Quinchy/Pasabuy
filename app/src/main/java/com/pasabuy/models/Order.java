package com.pasabuy.models;

public class Order {
    private String orderID; // Firestore document IDs are strings
    private String postID;
    private String buyerID;
    private String status; // "Pending", "Buying", "Delivering", "Delivered"
    private String paymentMethod; // "Cash on Delivery" only
    private String deliveryAddressID;
    private int quantity;
    private double totalPrice;

    public Order() {
        // Default constructor required for calls to DataSnapshot.getValue(Order.class)
    }

    public Order(String orderID, String postID, String buyerID, String status, String paymentMethod, String deliveryAddressID, int quantity, double totalPrice) {
        this.orderID = orderID;
        this.postID = postID;
        this.buyerID = buyerID;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.deliveryAddressID = deliveryAddressID;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // Getters and setters
    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getPostID() {
        return postID;
    }

    public void setPostID(String postID) {
        this.postID = postID;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(String buyerID) {
        this.buyerID = buyerID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDeliveryAddressID() {
        return deliveryAddressID;
    }

    public void setDeliveryAddressID(String deliveryAddressID) {
        this.deliveryAddressID = deliveryAddressID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
