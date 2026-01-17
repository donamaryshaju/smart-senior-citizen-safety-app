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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.R;
// *** FIX 1: Import the correct RequestModel class from your 'model' package ***
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyRequestAdapter adapter;
    // *** FIX 2: Use the imported RequestModel directly ***
    private List<RequestModel> requestList;
    private FirebaseFirestore fStore;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        // 1. Initialize Views
        recyclerView = findViewById(R.id.recycler_my_requests);

        if (recyclerView == null) {
            Toast.makeText(this, "Error: RecyclerView ID not found!", Toast.LENGTH_LONG).show();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        // The adapter will now use the corrected list
        adapter = new MyRequestAdapter(requestList);
        recyclerView.setAdapter(adapter);

        // 2. Initialize Firebase
        fStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadMyRequests();
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadMyRequests() {
        // This query correctly fetches documents from the 'requests' collection for the current user.
        fStore.collection("requests")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(MyRequestsActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                            Log.e("FirestoreError", error.getMessage());
                            return;
                        }

                        if (value != null) {
                            requestList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                // *** FIX 3: Convert the Firestore document to the correct RequestModel class ***
                                RequestModel req = doc.toObject(RequestModel.class);
                                if (req != null) {
                                    requestList.add(req);
                                }
                            }
                            adapter.notifyDataSetChanged();

                            if (requestList.isEmpty()) {
                                Toast.makeText(MyRequestsActivity.this, "No requests found.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    // --- INTERNAL ADAPTER CLASS ---
    private class MyRequestAdapter extends RecyclerView.Adapter<MyRequestAdapter.ViewHolder> {
        // *** FIX 4: The adapter's internal list must also use the correct RequestModel ***
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
            // The 'req' object is now of the correct RequestModel type
            RequestModel req = list.get(position);

            // Make sure your RequestModel class has these getter methods
            if (holder.txtType != null) holder.txtType.setText(req.getType());
            if (holder.txtDesc != null) holder.txtDesc.setText(req.getDescription());

            if (holder.txtPriority != null) {
                String status = req.getStatus();
                holder.txtPriority.setText(status);

                if ("Accepted".equals(status)) {
                    holder.txtPriority.setTextColor(0xFF2E7D32); // Green
                } else {
                    holder.txtPriority.setTextColor(0xFFEF6C00); // Orange for "Pending"
                }
            }

            // Hide UI elements not relevant for the user viewing their own requests
            if (holder.btnAccept != null) holder.btnAccept.setVisibility(View.GONE);
            if (holder.txtName != null) holder.txtName.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtType, txtDesc, txtPriority, txtName;
            Button btnAccept;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtType = itemView.findViewById(R.id.req_type);
                txtDesc = itemView.findViewById(R.id.req_desc);
                txtPriority = itemView.findViewById(R.id.req_priority);
                txtName = itemView.findViewById(R.id.req_senior_name);
                btnAccept = itemView.findViewById(R.id.btn_accept);
            }
        }
    }
}
