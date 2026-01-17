package com.example.seniorcitizensupport.activity;

import java.io.Serializable;

/**
 * Represents a single grocery item with all its properties.
 * This class is used for displaying items from the store and for the shopping cart.
 * Implements Serializable to be passed between activities.
 */
public class GroceryItem implements Serializable {

    // --- All Fields Needed ---
    private String name;
    private double price;
    private String unit;
    private String description;
    private int stock;
    private boolean available;
    private String documentId; // Optional: can store the item's original ID

    /**
     * No-argument constructor required for Firestore and deserialization.
     */
    public GroceryItem() {
        // Empty constructor
    }

    /**
     * A comprehensive constructor to create a new grocery item with all its details.
     * This is used when loading items from the Firestore 'groceries' collection.
     */
    public GroceryItem(String name, double price, String description, int stock, boolean available, String unit) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
        this.available = available;
        this.unit = unit;
    }

    // --- GETTERS and SETTERS ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}
