package com.example.seniorcitizensupport.model;

import java.util.List;

public class Medicine {
    // These fields match your Firebase document
    private String name;
    private String description;
    private double price;
    private int stock;
    private boolean available;
    // The 'variants' field is complex, so we will omit it for now for simplicity.
    // We can add it later if needed.

    // IMPORTANT: A public no-argument constructor is required for Firestore
    public Medicine() {}

    public Medicine(String name, String description, double price, int stock, boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.available = available;
    }

    // --- Getters and Setters ---
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
