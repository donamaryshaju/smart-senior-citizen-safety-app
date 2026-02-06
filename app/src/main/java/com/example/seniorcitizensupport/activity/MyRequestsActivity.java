package com.example.seniorcitizensupport.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private MyRequestAdapter adapter;
    private List<RequestModel> requestList;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        // Initialize Views
        recyclerView = findViewById(R.id.recycler_my_requests);

        if (recyclerView == null) {
            showToast("Error: RecyclerView ID not found!");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new MyRequestAdapter(requestList);
        recyclerView.setAdapter(adapter);

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            loadMyRequests();
        } else {
            showToast("Please login first");
            finish();
        }
    }

    private void loadMyRequests() {
        firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            showToast("Error loading data");
                            Log.e("FirestoreError", error.getMessage());
                            return;
                        }

                        if (value != null) {
                            requestList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                RequestModel req = doc.toObject(RequestModel.class);
                                if (req != null) {
                                    requestList.add(req);
                                }
                            }
                            adapter.notifyDataSetChanged();

                            if (requestList.isEmpty()) {
                                showToast("No requests found.");
                            }
                        }
                    }
                });
    }

    // --- INTERNAL ADAPTER CLASS ---
    private class MyRequestAdapter extends RecyclerView.Adapter<MyRequestAdapter.ViewHolder> {
        private List<RequestModel> list;

        public MyRequestAdapter(List<RequestModel> list) {
            this.list = list;
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
            String status = req.getStatus();

            if (holder.txtType != null)
                holder.txtType.setText(req.getType());
            if (holder.txtDesc != null)
                holder.txtDesc.setText(req.getDescription());

            if (holder.txtPriority != null) {
                holder.txtPriority.setText(status);

                if ("Accepted".equals(status)) {
                    holder.txtPriority.setTextColor(0xFF2E7D32); // Green
                } else {
                    holder.txtPriority.setTextColor(0xFFEF6C00); // Orange
                }
            }

            // Hide UI elements not relevant for the user viewing their own requests
            if (holder.btnAccept != null)
                holder.btnAccept.setVisibility(View.GONE);

            // Logic to show Volunteer Name if Request is active (Accepted, On The Way,
            // Arrived, In Progress, Completed)
            if (!Constants.STATUS_PENDING.equalsIgnoreCase(status)) {
                if (req.getVolunteerId() != null && !req.getVolunteerId().isEmpty()) {
                    holder.txtName.setVisibility(View.VISIBLE);
                    holder.txtName.setText("Volunteer: Loading...");

                    firestore.collection(Constants.KEY_COLLECTION_USERS)
                            .document(req.getVolunteerId())
                            .get()
                            .addOnSuccessListener(snippet -> {
                                String vName = snippet.getString(Constants.KEY_NAME);
                                if (vName == null)
                                    vName = snippet.getString("fullName");
                                if (vName != null) {
                                    holder.txtName.setText("Volunteer: " + vName);
                                } else {
                                    holder.txtName.setText("Volunteer: Assigned");
                                }
                            });
                } else {
                    holder.txtName.setVisibility(View.GONE);
                }
            } else {
                // Pending
                holder.txtName.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtType, txtDesc, txtPriority, txtName;
            Button btnAccept, btnCall;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnCall = itemView.findViewById(R.id.btn_call_volunteer);
            }
        }
    }
}
