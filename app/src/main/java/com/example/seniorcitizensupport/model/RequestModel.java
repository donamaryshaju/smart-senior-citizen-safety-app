// Correct File Path: app/src/main/java/com/example/seniorcitizensupport/model/RequestModel.java

package com.example.seniorcitizensupport.model;

import com.google.firebase.firestore.Exclude;
import java.util.List;
import java.util.Map;

/**
 * Represents a request made by a user. This can be a medical request,
 * transport request, or a grocery order. This class is designed to be
 * directly used with Firestore's toObject() method.
 */
public class RequestModel {

    // --- Fields that map directly to your Firestore documents ---
    private String type;        // e.g., "Medical", "Transport", "Grocery"
    private String description; // For non-grocery requests
    private String priority;    // e.g., "Normal", "High"
    private String location;    // For non-grocery requests
    private String userId;      // The UID of the user who made the request
    private String status;      // e.g., "Pending", "Accepted", "Completed"
    private String volunteerId; // UID of the volunteer who accepted the request

    // --- Fields specific to Grocery/Medicine orders ---
    private String totalAmount;
    private List<Map<String, Object>> items;

    // --- Fields that are used locally in the app but NOT saved to Firestore ---
    @Exclude
    private String documentId;
    @Exclude
    private String tempCollectionName;

    // IMPORTANT: A public no-argument constructor is REQUIRED for Firestore's toObject() method.
    public RequestModel() {
        // Default constructor
    }

    // --- GETTERS ---
    // These allow other classes to read the data from this object.

    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getLocation() { return location; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getVolunteerId() { return volunteerId; }
    public String getTotalAmount() { return totalAmount; }
    public List<Map<String, Object>> getItems() { return items; }

    @Exclude
    public String getDocumentId() { return documentId; }

    @Exclude
    public String getTempCollectionName() { return tempCollectionName; }


    // --- SETTERS ---
    // These allow other classes to set or change the data in this object.

    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setLocation(String location) { this.location = location; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setStatus(String status) { this.status = status; }
    public void setVolunteerId(String volunteerId) { this.volunteerId = volunteerId; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTempCollectionName(String tempCollectionName) { this.tempCollectionName = tempCollectionName; }
}
