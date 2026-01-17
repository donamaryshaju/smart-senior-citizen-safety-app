package com.example.seniorcitizensupport.activity;

import android.util.Log;
import androidx.annotation.Keep; // --- IMPORT @Keep ---
import java.io.Serializable;

@Keep // --- ADDED: Prevents ProGuard from removing this class in release builds ---
public class GroceryItem implements Serializable {
    private String name;
    private double price;
    private String description;
    private int stock;
    private boolean available;
    private String unit;

    // Empty constructor required for Firestore's automatic data mapping
    public GroceryItem() {
        // This is used by Firestore. You can add a log to see when it's called.
        Log.d("GroceryItem", "Empty constructor called by Firestore");
    }

    public GroceryItem(String name, double price, String description, int stock, boolean available, String unit) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
        this.available = available;
        this.unit = unit;
    }

    // --- GETTERS ---
    // Added null checks to prevent crashes
    public String getName() {
        return name != null ? name : "Unknown Item";
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public int getStock() {
        return stock;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getUnit() {
        return unit != null ? unit : "";
    }

    // --- SETTERS ---
    // These are required for Firebase to map data from the database back into the object.
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setStock(int stock) { this.stock = stock; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setUnit(String unit) { this.unit = unit; }
}
