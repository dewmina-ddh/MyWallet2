package com.example.mywallet;

public class TransactionModel {
    private int id;
    private String title;
    private double amount;
    private String type; // "INCOME" or "EXPENSE"
    private String category;
    private String date;
    private String paymentMethod;
    private long timestamp;

    public TransactionModel() {
    }

    public TransactionModel(int id, String title, double amount, String type, String category, String date, String paymentMethod, long timestamp) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.timestamp = timestamp;
    }

    public TransactionModel(String title, double amount, String type, String category, String date, String paymentMethod, long timestamp) {
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}