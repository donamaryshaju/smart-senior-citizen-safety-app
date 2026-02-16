package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SeniorDashActivity extends BaseActivity {

    // Header Components
    private TextView txtWelcome, txtTime, badgeNotification;
    private View btnSettings, btnLogout;
    private View btnProfile, btnNotifications;

    // Active Banner

    // Direct Actions
    private MaterialCardView cardMedical, cardTransport, cardHomeCare, cardGrocery;
    private TextView btnSOS; // FIX: XML uses TextView for circular shape

    // Recent Requests
    private RecyclerView recyclerRecent;
    private View btnViewAllRequests;
    private RecentRequestAdapter recentAdapter;
    private List<RequestModel> recentRequestsList;

    private BottomNavigationView bottomNav;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // DEBUG: Catch crashes and show toast
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Toast
                    .makeText(getApplicationContext(), "DASHBOARD CRASH: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main); // Mapped to new layout

            boolean isGuest = getIntent().getBooleanExtra("GUEST_MODE", false);
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null && !isGuest) {
                startActivity(new Intent(SeniorDashActivity.this, LoginActivity.class));
                finish();
                return;
            }

            userId = isGuest ? "GUEST" : currentUser.getUid();

            initializeViews();
            setupListeners();

            // Load Data
            updateTime();
            loadUserProfile();
            // loadActiveRequest(); // Disabled as per user request
            loadRecentRequests(); // Query Logic

        } catch (Exception e) {
            Toast.makeText(this, "Init Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        // Header
        txtWelcome = findViewById(R.id.text_welcome_senior);
        txtTime = findViewById(R.id.text_time);
        badgeNotification = findViewById(R.id.badge_notification);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout_senior);
        btnProfile = findViewById(R.id.layout_profile);
        btnNotifications = findViewById(R.id.layout_notifications);

        // Active Banner Removed per user request

        // Actions
        btnSOS = findViewById(R.id.btn_sos_floating);
        cardMedical = findViewById(R.id.card_medical);
        cardGrocery = findViewById(R.id.card_grocery);
        cardTransport = findViewById(R.id.card_transport);
        cardHomeCare = findViewById(R.id.card_homecare);

        // Recent
        recyclerRecent = findViewById(R.id.recycler_recent_requests);
        btnViewAllRequests = findViewById(R.id.btn_view_all_requests);
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        recentRequestsList = new ArrayList<>();
        recentAdapter = new RecentRequestAdapter(recentRequestsList);
        recyclerRecent.setAdapter(recentAdapter);

        // Bottom Nav
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void updateTime() {
        // Simple time setting, realistically updated via Handler or BroadcastReceiver
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        txtTime.setText(sdf.format(new java.util.Date()));
    }

    private void loadUserProfile() {
        if ("GUEST".equals(userId)) {
            txtWelcome.setText("Hello, Guest");
            return;
        }
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name == null || name.isEmpty())
                            name = documentSnapshot.getString("Name");
                        if (name == null || name.isEmpty())
                            name = documentSnapshot.getString("fullName");

                        if (name == null || name.isEmpty()) {
                            // Try email as fallback
                            String email = documentSnapshot.getString("email");
                            if (email != null && !email.isEmpty()) {
                                name = email.split("@")[0]; // Use part before @
                                // Capitalize first letter
                                if (name.length() > 0)
                                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                            }
                        }

                        if (name == null || name.isEmpty()) {
                            name = "Senior";
                        }

                        // Capitalize formatting
                        txtWelcome.setText("Hello, " + name);
                    } else {
                        txtWelcome.setText("Hello, Senior");
                    }
                })
                .addOnFailureListener(e -> {
                    txtWelcome.setText("Hello, Error");
                    Toast.makeText(this, "Profile Load Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadRecentRequests() {
        if ("GUEST".equals(userId))
            return;

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recentRequestsList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot doc : docs) {
                            RequestModel model = doc.toObject(RequestModel.class); // Ensure RequestModel has empty
                                                                                   // constructor & getters/setters
                                                                                   // match
                            // Fallback manual mapping if needed if model mismatch
                            if (model != null)
                                recentRequestsList.add(model);
                        }
                        recentAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void setupListeners() {
        btnSOS.setOnClickListener(v -> triggerSOS());

        cardMedical.setOnClickListener(v -> startActivity(new Intent(this, MedicalSelectionActivity.class)));
        cardGrocery.setOnClickListener(v -> startActivity(new Intent(this, GrocerySelectionActivity.class)));
        cardTransport.setOnClickListener(v -> startActivity(new Intent(this, TransportSelectionActivity.class)));
        cardHomeCare.setOnClickListener(v -> startActivity(new Intent(this, HomeCareSelectionActivity.class)));

        btnViewAllRequests.setOnClickListener(v -> startActivity(new Intent(this, MyRequestsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, WelcomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        btnSettings.setOnClickListener(v -> showToast("Settings - Coming Soon"));

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)
                return true;
            if (id == R.id.nav_requests) {
                startActivity(new Intent(this, MyRequestsActivity.class));
                return false; // Don't select tab, keep Home selected as activity stack pushes
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false;
            }
            if (id == R.id.nav_alerts) {
                showToast("No new alerts");
                return false;
            }
            return false;
        });

        // Check for pending family requests
        checkPendingFamilyRequests();
    }

    private void checkPendingFamilyRequests() {
        if ("GUEST".equals(userId))
            return;

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("linkedSeniorId", userId)
                .whereEqualTo("linkStatus", "pending_approval")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            showFamilyApprovalDialog(doc);
                            // Show one at a time to avoid dialog stacking (simplification)
                            return;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Silent failure or log
                });
    }

    private void showFamilyApprovalDialog(DocumentSnapshot familyDoc) {
        String name = familyDoc.getString("name");
        String relation = familyDoc.getString("relationship");
        String familyId = familyDoc.getId();

        new AlertDialog.Builder(this)
                .setTitle("Family Connection Request")
                .setMessage(name + " (" + relation
                        + ") wants to link with your account to assist you.\n\nDo you know this person?")
                .setPositiveButton("Yes, Approve", (dialog, which) -> {
                    approveFamilyRequest(familyId, name);
                })
                .setNegativeButton("No, Decline", (dialog, which) -> {
                    declineFamilyRequest(familyId);
                })
                .setCancelable(false)
                .show();
    }

    private void approveFamilyRequest(String familyId, String name) {
        showProgressDialog("Approving...");
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(familyId)
                .update("linkStatus", "approved")
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("You are now linked with " + name);
                    // Re-check for more requests
                    checkPendingFamilyRequests();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Approval Failed: " + e.getMessage());
                });
    }

    private void declineFamilyRequest(String familyId) {
        showProgressDialog("Declining...");
        // Option 1: Delete the link status
        // Option 2: Set to rejected
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(familyId)
                .update("linkStatus", "rejected", "linkedSeniorId", null) // Unlink
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Request Declined");
                    checkPendingFamilyRequests();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Decline Failed: " + e.getMessage());
                });
    }

    private void triggerSOS() {
        // Launch Medical Emergency Activity directly
        Intent intent = new Intent(this, MedicalEmergencyActivity.class);
        startActivity(intent);
    }

    // --- ADAPTER INNER CLASS ---
    class RecentRequestAdapter extends RecyclerView.Adapter<RecentRequestAdapter.ViewHolder> {

        List<RequestModel> list;

        public RecentRequestAdapter(List<RequestModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_request, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RequestModel item = list.get(position);
            holder.type.setText(item.getType());
            holder.status.setText(item.getStatus());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
            if (item.getTimestamp() != null)
                holder.date.setText(sdf.format(item.getTimestamp().toDate()));

            // Icon logic
            String icon = "❓";
            if (item.getType() != null) {
                if (item.getType().contains("Medical") || item.getType().contains("SOS"))
                    icon = "🏥";
                else if (item.getType().contains("Grocery"))
                    icon = "🛒";
                else if (item.getType().contains("Transport"))
                    icon = "🚗";
                else if (item.getType().contains("Home"))
                    icon = "🏠";
            }
            holder.icon.setText(icon);

            // Status Color
            if (item.getStatus().equalsIgnoreCase("Pending"))
                holder.status.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            else if (item.getStatus().equalsIgnoreCase("Completed"))
                holder.status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView icon, type, date, status;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.text_request_icon);
                type = itemView.findViewById(R.id.text_request_type);
                date = itemView.findViewById(R.id.text_request_date);
                status = itemView.findViewById(R.id.text_request_status);
            }
        }
    }
}
