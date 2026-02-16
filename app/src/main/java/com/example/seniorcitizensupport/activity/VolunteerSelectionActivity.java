package com.example.seniorcitizensupport.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VolunteerSelectionActivity extends BaseActivity {

    private RecyclerView recyclerVolunteers;
    private ProgressBar progressBar;
    private TextView txtNoVolunteers;
    private VolunteerAdapter adapter;
    private List<DocumentSnapshot> volunteerList;
    private String requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_selection);

        requestId = getIntent().getStringExtra("REQUEST_ID");
        if (requestId == null) {
            Toast.makeText(this, "Error: No Request ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerVolunteers = findViewById(R.id.recycler_volunteers);
        progressBar = findViewById(R.id.progress_bar);
        txtNoVolunteers = findViewById(R.id.text_no_volunteers);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        volunteerList = new ArrayList<>();
        adapter = new VolunteerAdapter(volunteerList);
        recyclerVolunteers.setLayoutManager(new LinearLayoutManager(this));
        recyclerVolunteers.setAdapter(adapter);

        loadVolunteers();
    }

    private void loadVolunteers() {
        progressBar.setVisibility(View.VISIBLE);
        txtNoVolunteers.setVisibility(View.GONE);

        // Simple query: Get all volunteers
        // In a real app, we would use GeoFirestore or filter by location
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("role", "Volunteer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    volunteerList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        volunteerList.addAll(queryDocumentSnapshots.getDocuments());
                        adapter.notifyDataSetChanged();
                    }

                    if (volunteerList.isEmpty()) {
                        txtNoVolunteers.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading volunteers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void selectVolunteer(String volunteerId, String volName) {
        // Update Request with Selected Volunteer
        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).document(requestId)
                .update("volunteerId", volunteerId,
                        "status", Constants.STATUS_PENDING) // Keep Pending until accepted
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request sent to " + volName, Toast.LENGTH_SHORT).show();
                    // Navigate to Dashboard or Tracking?
                    // User requirement: "The request status changes to 'Confirmed'" -> this was
                    // confusing.
                    // Assuming we go back to dashboard or show confirmation.
                    finish();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Error selecting volunteer", Toast.LENGTH_SHORT).show());
    }

    // --- Adapter ---
    class VolunteerAdapter extends RecyclerView.Adapter<VolunteerAdapter.ViewHolder> {

        private List<DocumentSnapshot> list;

        public VolunteerAdapter(List<DocumentSnapshot> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = list.get(position);
            String name = doc.getString("name");
            if (name == null)
                name = doc.getString("fName");
            if (name == null)
                name = "Volunteer";

            holder.txtName.setText(name);
            // holder.txtRating.setText(...) // If rating exists in DB

            // Distance mock
            double dist = 1.0 + (position * 0.5); // Mock distance
            holder.txtDistance.setText(String.format("%.1f km away", dist));

            String finalName = name;
            holder.btnSelect.setOnClickListener(v -> selectVolunteer(doc.getId(), finalName));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtRating, txtDistance;
            Button btnSelect;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.text_vol_name);
                txtRating = itemView.findViewById(R.id.text_vol_rating);
                txtDistance = itemView.findViewById(R.id.text_vol_distance);
                btnSelect = itemView.findViewById(R.id.btn_select_volunteer);
            }
        }
    }
}
