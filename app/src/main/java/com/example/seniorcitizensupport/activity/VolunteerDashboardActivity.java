package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VolunteerDashboardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private RequestAdapter adapter;
    private List<RequestModel> requestList;

    private CardView btnMedical, btnGrocery, btnTransport, btnHomecare;
    private TextView txtVolunteerName;
    private TextView txtBottomSheetTitle;
    private Button btnLogout;

    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        // -------- UI References --------
        btnMedical = findViewById(R.id.card_medical);
        btnGrocery = findViewById(R.id.card_grocery);
        btnTransport = findViewById(R.id.card_transport);
        btnHomecare = findViewById(R.id.card_homecare);
        txtVolunteerName = findViewById(R.id.text_volunteer_name);
        txtBottomSheetTitle = findViewById(R.id.bottom_sheet_title);
        recyclerView = findViewById(R.id.recycler_requests);
        btnLogout = findViewById(R.id.btn_logout);

        // -------- Recycler Setup --------
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList, this, firestore, auth);
        recyclerView.setAdapter(adapter);

        // -------- Button Actions --------
        btnMedical.setOnClickListener(v -> loadRequests(Constants.TYPE_MEDICAL));
        btnGrocery.setOnClickListener(v -> loadRequests(Constants.TYPE_GROCERY));
        btnTransport.setOnClickListener(v -> loadRequests(Constants.TYPE_TRANSPORT));
        btnHomecare.setOnClickListener(v -> loadRequests(Constants.TYPE_HOMECARE));

        btnLogout.setOnClickListener(v -> {
            // Stop Firestore listener first
            if (firestoreListener != null) {
                firestoreListener.remove();
                firestoreListener = null;
            }

            auth.signOut();
            Intent intent = new Intent(VolunteerDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // -------- Load Initial Data --------
        loadVolunteerInfo();
        loadRequests(Constants.TYPE_MEDICAL); // Default view
    }

    // ---------------- LOAD VOLUNTEER NAME ----------------
    private void loadVolunteerInfo() {
        if (auth.getCurrentUser() == null)
            return;

        firestore.collection(Constants.KEY_COLLECTION_USERS).document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString(Constants.KEY_NAME);
                    if (name == null)
                        name = documentSnapshot.getString("fName");
                    if (name == null)
                        name = documentSnapshot.getString("fullName");

                    if (name == null || name.trim().isEmpty()) {
                        name = "Volunteer";
                    }

                    txtVolunteerName.setText("Hello, " + name);
                })
                .addOnFailureListener(e -> txtVolunteerName.setText("Hello, Volunteer"));
    }

    // ---------------- LOAD REQUESTS ----------------
    private void loadRequests(String category) {

        txtBottomSheetTitle.setText("Showing: " + category + " Requests");

        if (firestoreListener != null)
            firestoreListener.remove();

        // Standardized to always use Constants.KEY_COLLECTION_REQUESTS
        Query query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("type", category)
                .whereEqualTo("status", Constants.STATUS_PENDING);

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                showToast("Failed to load requests");
                requestList.clear();
                adapter.notifyDataSetChanged();
                return;
            }

            if (value != null) {
                requestList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        RequestModel req = doc.toObject(RequestModel.class);
                        if (req != null) {
                            req.setDocumentId(doc.getId());
                            requestList.add(req);
                        }
                    } catch (Exception ignored) {
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // ---------------- ADAPTER ----------------
    static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

        private final List<RequestModel> list;
        private final Context context;
        private final FirebaseFirestore fStore;
        private final FirebaseAuth mAuth;

        public RequestAdapter(List<RequestModel> list, Context context,
                FirebaseFirestore firestore, FirebaseAuth auth) {
            this.list = list;
            this.context = context;
            this.fStore = firestore;
            this.mAuth = auth;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_request_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            final RequestModel req = list.get(position);
            if (req == null)
                return;

            String type = req.getType() != null ? req.getType() : "N/A";
            holder.txtType.setText(type);

            String location = req.getLocation() != null
                    ? req.getLocation()
                    : "No location provided";
            holder.txtLocation.setText(location);

            // Show description or items count
            String description = req.getDescription();
            if (description != null && !description.trim().isEmpty()) {
                holder.txtDesc.setText(description);
            } else if (Constants.TYPE_GROCERY.equalsIgnoreCase(type)) {
                List<Map<String, Object>> items = req.getItems();
                holder.txtDesc.setText(
                        (items != null ? items.size() : 0) + " items in order");
            } else {
                holder.txtDesc.setText("No details provided");
            }

            String priority = req.getPriority() != null ? req.getPriority() : "Normal";
            holder.txtPriority.setText(priority.toUpperCase(Locale.US) + " PRIORITY");
            holder.txtPriority.setTextColor(
                    ContextCompat.getColor(context,
                            "High".equalsIgnoreCase(priority)
                                    ? android.R.color.holo_red_dark
                                    : android.R.color.holo_green_dark));

            String userId = req.getUserId();
            if (userId != null && !userId.isEmpty()) {
                holder.txtName.setText("Loading...");
                fStore.collection(Constants.KEY_COLLECTION_USERS).document(userId).get()
                        .addOnSuccessListener(ds -> {
                            String name = ds.getString(Constants.KEY_NAME);
                            if (name == null)
                                name = ds.getString("fName");
                            if (name == null)
                                name = ds.getString("fullName");
                            holder.txtName.setText(
                                    name != null ? "Senior: " + name : "Senior: Name not found");
                        });
            } else {
                holder.txtName.setText("Senior: Unknown");
            }

            holder.btnAccept.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null && req.getDocumentId() != null) {

                    fStore.collection(Constants.KEY_COLLECTION_REQUESTS)
                            .document(req.getDocumentId())
                            .update("status", Constants.STATUS_ACCEPTED,
                                    "volunteerId", mAuth.getCurrentUser().getUid())
                            .addOnSuccessListener(a -> {
                                // Provide visual feedback using Toast (context is BaseActivity or similar)
                                if (context instanceof BaseActivity) {
                                    ((BaseActivity) context).showToast("Request Accepted!");
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (context instanceof BaseActivity) {
                                    ((BaseActivity) context).showToast("Failed: " + e.getMessage());
                                }
                            });
                }
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView txtType, txtDesc, txtPriority, txtName, txtLocation;
            Button btnAccept;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                txtLocation = itemView.findViewById(R.id.req_location);
                btnAccept = itemView.findViewById(R.id.btn_accept);
            }
        }
    }
}
