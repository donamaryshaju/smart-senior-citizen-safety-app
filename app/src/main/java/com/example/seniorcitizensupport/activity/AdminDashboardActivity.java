package com.example.seniorcitizensupport.activity;

import android.content.Context;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends BaseActivity {

    private ImageView btnLogout, iconProfile, iconNotifications;
    private TextView txtWelcome, txtVolunteerCount, txtSeniorCount, txtRequestCount;
    private RecyclerView recyclerView;
    private AdminRequestAdapter adapter;
    private List<RequestModel> requestList;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize UI Components
        txtWelcome = findViewById(R.id.text_admin_welcome);
        btnLogout = findViewById(R.id.button_logout_admin);
        txtVolunteerCount = findViewById(R.id.text_volunteer_count);
        txtSeniorCount = findViewById(R.id.text_senior_count);
        txtRequestCount = findViewById(R.id.text_request_count);
        iconProfile = findViewById(R.id.icon_profile);
        iconNotifications = findViewById(R.id.icon_notifications);
        recyclerView = findViewById(R.id.recycler_admin_requests);

        // Header Icons Action
        iconProfile.setOnClickListener(v -> showToast("Admin Profile"));
        iconNotifications.setOnClickListener(v -> showToast("Notifications"));

        // Logout
        btnLogout.setOnClickListener(v -> {
            if (firestoreListener != null)
                firestoreListener.remove();
            auth.signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new AdminRequestAdapter(requestList, this);
        recyclerView.setAdapter(adapter);

        // Load Data
        loadStats();
        loadAllRequests();
    }

    private void loadStats() {
        // Count Seniors
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_ROLE, Constants.ROLE_SENIOR)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> txtSeniorCount.setText(String.valueOf(snapshot.getCount())));

        // Count Volunteers
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_ROLE, Constants.ROLE_VOLUNTEER)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> txtVolunteerCount.setText(String.valueOf(snapshot.getCount())));

        // Count Requests
        firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> txtRequestCount.setText(String.valueOf(snapshot.getCount())));

        // Make Cards Clickable
        findViewById(R.id.card_stats_seniors).setOnClickListener(v -> openUserList(Constants.ROLE_SENIOR));
        findViewById(R.id.card_stats_volunteers).setOnClickListener(v -> openUserList(Constants.ROLE_VOLUNTEER));
        findViewById(R.id.card_stats_requests).setOnClickListener(v -> {
            showToast("Scroll down to see requests");
            if (recyclerView != null) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void openUserList(String role) {
        Intent intent = new Intent(AdminDashboardActivity.this, UserListActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
    }

    private void loadAllRequests() {
        Query query = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50); // Limit to 50 for performance

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null)
                return;
            if (value != null) {
                requestList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    RequestModel req = doc.toObject(RequestModel.class);
                    if (req != null) {
                        req.setDocumentId(doc.getId());
                        requestList.add(req);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // --- ADAPTER ---
    static class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.ViewHolder> {
        private final List<RequestModel> list;
        private final Context context;
        private final FirebaseFirestore fStore = FirebaseFirestore.getInstance();

        public AdminRequestAdapter(List<RequestModel> list, Context context) {
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
            holder.txtPriority.setText("Status: " + req.getStatus() + " | " + req.getPriority());

            // SOS / High Priority Highlight
            if (Constants.TYPE_SOS.equalsIgnoreCase(req.getType()) || "High".equalsIgnoreCase(req.getPriority())) {
                holder.cardView.setStrokeColor(0xFFFF0000); // Red
                holder.cardView.setStrokeWidth(4);
                holder.txtPriority.setTextColor(0xFFFF0000);
            } else {
                holder.cardView.setStrokeWidth(0);
                holder.txtPriority.setTextColor(0xFF000000);
            }

            // DELETE ACTION
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("DELETE REQUEST");
            holder.btnAccept
                    .setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_dark));

            holder.btnAccept.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete Request")
                        .setMessage("Are you sure you want to delete this request?")
                        .setPositiveButton("DELETE", (dialog, which) -> deleteRequest(req.getDocumentId(), position))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Fetch Name
            fStore.collection(Constants.KEY_COLLECTION_USERS).document(req.getUserId()).get()
                    .addOnSuccessListener(ds -> {
                        String name = ds.getString(Constants.KEY_NAME);
                        if (name == null)
                            name = ds.getString("fName");
                        holder.txtName.setText("User: " + (name != null ? name : "Unknown"));
                    });
        }

        private void deleteRequest(String docId, int position) {
            fStore.collection(Constants.KEY_COLLECTION_REQUESTS).document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Request Deleted", Toast.LENGTH_SHORT).show();
                        // Adapter update will happen via SnapshotListener automatically
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show());
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

                if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                    cardView = (com.google.android.material.card.MaterialCardView) itemView;
                } else {
                    cardView = itemView.findViewById(R.id.item_card_root);
                }
            }
        }
    }
}
