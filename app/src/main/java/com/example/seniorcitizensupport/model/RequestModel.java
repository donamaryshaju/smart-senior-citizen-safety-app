package com.example.seniorcitizensupport.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.List;
import java.util.Map;

public class RequestModel { // Fields that map to Firestore documents
    private String type;
    private String description;
    private String priority;
    private String location;
    private String userId;
    private String status;
    private String volunteerId;
    private Timestamp timestamp;
    private double totalAmount;
    private List<Map<String, Object>> items;

    private double latitude;
    private double longitude;

    // --- Field used locally in the app but NOT saved to Firestore ---
    @Exclude
    private String documentId;

    public RequestModel() {
        // Required public no-argument constructor
    }

    public RequestModel(String userId, String type, String status, String priority, String description,
            Timestamp timestamp, String location) {
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.description = description;
        this.timestamp = timestamp;
        this.location = location;
    }

    // --- GETTERS and SETTERS for all fields ---

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(String volunteerId) {
        this.volunteerId = volunteerId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
