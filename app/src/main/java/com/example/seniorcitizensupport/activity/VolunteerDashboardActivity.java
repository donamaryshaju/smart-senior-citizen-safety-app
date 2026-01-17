package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VolunteerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "VolunteerDashboard";

    private RecyclerView recyclerView;
    private RequestAdapter adapter;
    private List<RequestModel> requestList;
    private FirebaseFirestore fStore;
    private FirebaseAuth mAuth;

    private CardView btnMedical, btnGrocery, btnTransport, btnHomecare;
    private TextView txtVolunteerName;
    private TextView txtBottomSheetTitle;
    private BottomSheetBehavior<CardView> bottomSheetBehavior;

    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_dashboard);

        // 1. Initialize Firebase
        fStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI components
        btnMedical = findViewById(R.id.card_medical);
        btnGrocery = findViewById(R.id.card_grocery);
        btnTransport = findViewById(R.id.card_transport);
        btnHomecare = findViewById(R.id.card_homecare);
        txtVolunteerName = findViewById(R.id.text_volunteer_name);
        txtBottomSheetTitle = findViewById(R.id.bottom_sheet_title);
        recyclerView = findViewById(R.id.recycler_requests);
        CardView bottomSheetCard = findViewById(R.id.bottom_sheet_requests);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard);

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList, this);
        recyclerView.setAdapter(adapter);

        // 4. Set Click Listeners
        btnMedical.setOnClickListener(v -> loadRequests("Medical Assistance"));
        btnGrocery.setOnClickListener(v -> loadRequests("Grocery"));
        btnTransport.setOnClickListener(v -> loadRequests("Transport"));
        btnHomecare.setOnClickListener(v -> loadRequests("Homecare"));

        // 5. Load initial data
        loadVolunteerInfo();
        loadRequests("Medical Assistance"); // Default to medical
    }

    private void loadVolunteerInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fStore.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("fName");
                            if (name != null && !name.isEmpty()) {
                                txtVolunteerName.setText("Hello, " + name);
                            } else {
                                txtVolunteerName.setText("Hello, Volunteer");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load volunteer name", e);
                        txtVolunteerName.setText("Hello, Volunteer");
                    });
        }
    }

    private void loadRequests(String category) {
        txtBottomSheetTitle.setText("Showing: " + category + " Requests");

        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        Log.d(TAG, "Loading requests for category: " + category);

        Query query;
        String collectionName;

        if ("Grocery".equalsIgnoreCase(category)) {
            collectionName = "orders";
            // *** THE FIX: We remove the status filter to show ALL grocery orders ***
            query = fStore.collection(collectionName);
        } else {
            collectionName = "requests";
            // We keep the status filter for other requests because it's working correctly
            query = fStore.collection(collectionName)
                    .whereEqualTo("type", category)
                    .whereIn("status", Arrays.asList("Pending", "pending", "requested"));
        }

        Log.d(TAG, "Querying Collection: '" + collectionName + "' for type: '" + category + "'");

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Firestore listen failed.", error);
                Toast.makeText(VolunteerDashboardActivity.this, "Error loading requests. Check Logcat.", Toast.LENGTH_LONG).show();
                requestList.clear();
                adapter.notifyDataSetChanged();
                return;
            }

            if (value != null) {
                Log.d(TAG, "Query successful. Found " + value.size() + " docs in '" + collectionName + "'");
                requestList.clear();

                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        RequestModel req = doc.toObject(RequestModel.class);
                        if (req != null) {
                            req.setDocumentId(doc.getId());
                            req.setTempCollectionName(collectionName);

                            if ("orders".equals(collectionName)) {
                                req.setType("Grocery");
                            }
                            requestList.add(req);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse document: " + doc.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();

                if (requestList.isEmpty()) {
                    Toast.makeText(VolunteerDashboardActivity.this, "No pending " + category + " requests found.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- INNER CLASS: Smart RequestAdapter (NO CHANGES NEEDED HERE) ---
    class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
        private List<RequestModel> list;
        private Context context;
        private FirebaseFirestore fStore = FirebaseFirestore.getInstance();
        private FirebaseAuth mAuth = FirebaseAuth.getInstance();

        public RequestAdapter(List<RequestModel> list, Context context) {
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final RequestModel req = list.get(position);

            String type = req.getType() != null ? req.getType() : "Request";
            holder.txtType.setText(type);

            final boolean isGrocery = "Grocery".equalsIgnoreCase(type);

            // --- SMART DISPLAY LOGIC ---
            if (isGrocery) {
                // This block handles GROCERY orders from the 'orders' collection
                List<Map<String, Object>> items = req.getItems();
                if (items != null && !items.isEmpty()) {
                    String firstItemName = "Grocery Items";
                    try { firstItemName = String.valueOf(items.get(0).get("name")); } catch (Exception e) {/*ignore*/}

                    if (items.size() > 1) {
                        holder.txtDesc.setText(firstItemName + " + " + (items.size() - 1) + " others");
                    } else {
                        holder.txtDesc.setText(firstItemName);
                    }
                } else if (req.getDescription() != null && !req.getDescription().isEmpty()) {
                    // Fallback to description if items array is missing
                    holder.txtDesc.setText(req.getDescription());
                } else {
                    holder.txtDesc.setText("Grocery Order Details Unavailable");
                }
                String amount = req.getTotalAmount();
                holder.txtLocation.setText(amount != null && !amount.equals("0") ? "Total: " + amount : "Price Pending");

            } else {
                // This block handles ALL OTHER requests (Medical, Transport, etc.) from the 'requests' collection
                holder.txtDesc.setText(req.getDescription() != null ? req.getDescription() : "No details provided");
                holder.txtLocation.setText(req.getLocation() != null ? req.getLocation() : "No location provided");
            }

            // --- COMMON LOGIC FOR ALL REQUESTS ---
            String priority = req.getPriority() != null ? req.getPriority() : "Normal";
            holder.txtPriority.setText(priority.toUpperCase() + " PRIORITY");
            if ("High".equalsIgnoreCase(priority)) {
                holder.txtPriority.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            } else {
                holder.txtPriority.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            }

            // Fetch and display senior's name
            if (req.getUserId() != null && !req.getUserId().isEmpty()) {
                fStore.collection("users").document(req.getUserId()).get().addOnSuccessListener(ds -> {
                    if (ds.exists()) {
                        String name = ds.getString("fName");
                        holder.txtName.setText(name != null ? "Senior: " + name : "Senior: Unknown");
                    } else {
                        holder.txtName.setText("Senior: User not found");
                    }
                });
            } else {
                holder.txtName.setText("Senior: ID missing");
            }

            // Accept Button Logic
            holder.btnAccept.setOnClickListener(v -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String collection = req.getTempCollectionName();
                    if (collection == null || collection.isEmpty()) {
                        collection = isGrocery ? "orders" : "requests";
                    }

                    fStore.collection(collection).document(req.getDocumentId())
                            .update("status", "Accepted", "volunteerId", currentUser.getUid())
                            .addOnSuccessListener(a -> Toast.makeText(context, "Request Accepted!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Failed to accept: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
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
