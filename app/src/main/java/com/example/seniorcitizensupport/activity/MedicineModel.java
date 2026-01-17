package com.example.seniorcitizensupport.activity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MedicineModel implements Serializable {
    private String name;
    private double price;
    private String description;
    private int stock;
    private boolean available;
    private List<Map<String, Object>> variants;

    // NEW: Variable to store the Firestore Document ID
    private String docId;

    public MedicineModel() { }

    // UPDATED: Constructor now accepts docId
    public MedicineModel(String name, double price, String description, int stock, boolean available, List<Map<String, Object>> variants, String docId) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.stock = stock;
        this.available = available;
        this.variants = variants;
        this.docId = docId;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public int getStock() { return stock; }
    public boolean isAvailable() { return available; }
    public List<Map<String, Object>> getVariants() { return variants; }

    // NEW: Getter for the Document ID
    public String getDocId() { return docId; }

    // Gets price from the variant if it exists, otherwise base price
    public double getDisplayPrice() {
        if (variants != null && !variants.isEmpty()) {
            Object p = variants.get(0).get("price");
            if (p instanceof Number) {
                return ((Number) p).doubleValue();
            } else if (p instanceof String) {
                try {
                    return Double.parseDouble((String) p);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }
        return price;
    }

    // Gets e.g., "500 mg"
    public String getStrength() {
        if (variants != null && !variants.isEmpty()) {
            Object s = variants.get(0).get("strength");
            if (s != null) return s.toString();
        }
        return "";
    }

    // Gets number of tablets to calculate unit cost
    public int getTabletsPerStrip() {
        if (variants != null && !variants.isEmpty()) {
            Object t = variants.get(0).get("tabletsPerStrip");
            if (t instanceof Number) {
                return ((Number) t).intValue();
            } else if (t instanceof String) {
                try {
                    return Integer.parseInt((String) t);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return 1;
    }
}
