package com.example.seniorcitizensupport.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FamilyDashboardActivity extends BaseActivity {

    private TextView welcomeText;
    private Button btnLogout;
    private RecyclerView recyclerView;
    private FamilyAlertAdapter adapter;
    private List<RequestModel> alertList;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_dashboard);

        // Initialize Views
        welcomeText = findViewById(R.id.text_family_welcome);
        btnLogout = findViewById(R.id.button_logout_family);
        recyclerView = findViewById(R.id.recycler_family_alerts);

        // Fetch user name
        if (auth.getCurrentUser() != null) {
            fetchFamilyName(auth.getCurrentUser().getUid());
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        alertList = new ArrayList<>();
        adapter = new FamilyAlertAdapter(alertList, this);
        recyclerView.setAdapter(adapter);

        // Load Alerts
        loadFamilyAlerts();

        // Logout Listener
        btnLogout.setOnClickListener(v -> {
            if (firestoreListener != null)
                firestoreListener.remove();
            auth.signOut();
            Intent intent = new Intent(FamilyDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchFamilyName(String uid) {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(uid)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                        if (name == null)
                            name = documentSnapshot.getString("fullName");
                        if (name == null)
                            name = documentSnapshot.getString("fName");

                        if (name != null) {
                            welcomeText.setText("Welcome, " + name + "!");
                        }
                    }
                });
    }

    private void loadFamilyAlerts() {
        // Query for SOS or High Priority requests
        // Note: Firestore OR queries are limited, so we'll query all requests and
        // filter or query by priority "High"
        // For this MVP, let's just show ALL "SOS" and "High" priority requests to be
        // safe.
        // Actually, let's just show recent requests and highlight the dangerous ones.

        // Simpler: Show all SOS requests
        Query query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20);

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                showToast("Error loading alerts");
                return;
            }

            if (value != null) {
                alertList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    RequestModel req = doc.toObject(RequestModel.class);
                    if (req != null) {
                        // Filter for relevant stuff (High priority or SOS)
                        // Or just show everything for visibility in MVP
                        alertList.add(req);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // --- ADAPTER ---
    static class FamilyAlertAdapter extends RecyclerView.Adapter<FamilyAlertAdapter.ViewHolder> {
        private final List<RequestModel> list;
        private final Context context;
        private final FirebaseFirestore fStore = FirebaseFirestore.getInstance();

        public FamilyAlertAdapter(List<RequestModel> list, Context context) {
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
            RequestModel req = list.get(position);

            holder.txtType.setText(req.getType());
            holder.txtDesc.setText(req.getDescription());
            holder.txtLocation.setText(req.getLocation());

            String priority = req.getPriority();
            holder.txtPriority.setText("Status: " + req.getStatus() + " | " + priority);

            // Red for SOS/High
            if ("High".equalsIgnoreCase(priority) || Constants.TYPE_SOS.equalsIgnoreCase(req.getType())) {
                holder.cardView.setStrokeColor(0xFFFF0000); // Red Border
                holder.cardView.setStrokeWidth(4);
                holder.txtPriority.setTextColor(0xFFFF0000);
            } else {
                holder.cardView.setStrokeWidth(0);
                holder.txtPriority.setTextColor(0xFF000000);
            }

            // Hide Buttons for Family (Read Only)
            holder.btnAccept.setVisibility(View.GONE);

            // Load Name
            fStore.collection(Constants.KEY_COLLECTION_USERS).document(req.getUserId()).get()
                    .addOnSuccessListener(ds -> {
                        String name = ds.getString(Constants.KEY_NAME);
                        if (name == null)
                            name = ds.getString("fName");
                        holder.txtName.setText("Senior: " + (name != null ? name : "Unknown"));
                    });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtType, txtDesc, txtPriority, txtName, txtLocation;
            Button btnAccept;
            com.google.android.material.card.MaterialCardView cardView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                txtLocation = itemView.findViewById(R.id.req_location);
                btnAccept = itemView.findViewById(R.id.btn_accept);

                // Note: item_request_card root should be a MaterialCardView for stroke to work
                if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                    cardView = (com.google.android.material.card.MaterialCardView) itemView;
                } else {
                    cardView = itemView.findViewById(R.id.item_card_root);
                }
            }
        }
    }
}
