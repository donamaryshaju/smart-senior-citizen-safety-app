package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.seniorcitizensupport.BaseActivity;

public class TrackingActivity extends BaseActivity {

    private TextView txtEta, txtVolunteerName;
    private Button btnCall;
    private ImageView btnBack;
    private String volunteerPhone;

    private com.google.firebase.firestore.ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        txtEta = findViewById(R.id.text_eta);
        txtVolunteerName = findViewById(R.id.text_volunteer_name);
        btnCall = findViewById(R.id.btn_call_volunteer);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Get details from Intent (Passed from SeniorDashActivity)
        String volName = getIntent().getStringExtra("VOLUNTEER_NAME");
        String status = getIntent().getStringExtra("STATUS");
        volunteerPhone = getIntent().getStringExtra("VOLUNTEER_PHONE");

        if (volName != null) {
            txtVolunteerName.setText("Volunteer: " + volName);
        } else {
            txtVolunteerName.setText("Assigning Volunteer...");
            btnCall.setEnabled(false);
            btnCall.setText("Please Wait...");
            btnCall.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        }

        // Simulating Status logic
        if (status != null && status.equalsIgnoreCase("Accepted")) {
            txtEta.setText("Arriving in 15 mins");
        } else if (status != null && status.equalsIgnoreCase("On The Way")) {
            txtEta.setText("Arriving in 5 mins");
        } else {
            txtEta.setText("Status: " + status);
        }

        btnCall.setOnClickListener(v -> {
            if (volunteerPhone != null && !volunteerPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + volunteerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        updateTimeline(status);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String requestId = getIntent().getStringExtra("REQUEST_ID");
        if (requestId != null) {
            startListening(requestId);
        }
    }

    private void startListening(String requestId) {
        registration = firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .document(requestId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null)
                        return;
                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        String volId = snapshot.getString("volunteerId");

                        updateTimeline(status);

                        if (volId != null && !volId.isEmpty()) {
                            fetchVolunteerDetails(volId);
                        }
                    }
                });
    }

    private void fetchVolunteerDetails(String volId) {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(volId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name == null)
                            name = doc.getString("fName");
                        volunteerPhone = doc.getString("phone");

                        txtVolunteerName.setText("Volunteer: " + (name != null ? name : "Assigned"));
                        btnCall.setEnabled(true);
                        btnCall.setText("CALL VOLUNTEER");
                        btnCall.setBackgroundTintList(
                                getResources().getColorStateList(android.R.color.holo_green_dark));
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null)
            registration.remove();
    }

    private void updateTimeline(String status) {
        TextView stepAccepted = findViewById(R.id.step_accepted);
        View line1 = findViewById(R.id.line_1);
        TextView stepOnWay = findViewById(R.id.step_on_way);
        View line2 = findViewById(R.id.line_2);
        TextView stepArrived = findViewById(R.id.step_arrived);
        View line3 = findViewById(R.id.line_3);
        TextView stepCompleted = findViewById(R.id.step_completed);

        int activeColor = 0xFF4CAF50; // Green
        int inactiveColor = 0xFFBDBDBD; // Gray

        // Reset all
        stepAccepted.setTextColor(inactiveColor);
        line1.setBackgroundColor(inactiveColor);
        stepOnWay.setTextColor(inactiveColor);
        line2.setBackgroundColor(inactiveColor);
        stepArrived.setTextColor(inactiveColor);
        line3.setBackgroundColor(inactiveColor);
        stepCompleted.setTextColor(inactiveColor);

        if (status == null)
            return;

        // Progress Logic
        // Accepted -> (Accepted)
        // On The Way -> (Accepted, On Way)
        // Arrived / In Progress -> (Accepted, On Way, Arrived)
        // Completed -> (All)

        stepAccepted.setTextColor(activeColor); // Always active if here

        if (status.equalsIgnoreCase("On The Way") || status.equalsIgnoreCase("On the way")) {
            line1.setBackgroundColor(activeColor);
            stepOnWay.setTextColor(activeColor);
        } else if (status.equalsIgnoreCase("Arrived") || status.equalsIgnoreCase("In Progress")
                || status.equalsIgnoreCase("Service in progress")) {
            line1.setBackgroundColor(activeColor);
            stepOnWay.setTextColor(activeColor);
            line2.setBackgroundColor(activeColor);
            stepArrived.setTextColor(activeColor);
        } else if (status.equalsIgnoreCase("Completed")) {
            line1.setBackgroundColor(activeColor);
            stepOnWay.setTextColor(activeColor);
            line2.setBackgroundColor(activeColor);
            stepArrived.setTextColor(activeColor);
            line3.setBackgroundColor(activeColor);
            stepCompleted.setTextColor(activeColor);

            txtEta.setText("Service Completed");
        }
    }
}
