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

public class TrackingActivity extends AppCompatActivity {

    private TextView txtEta, txtVolunteerName;
    private Button btnCall;
    private ImageView btnBack;
    private String volunteerPhone;

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

        if (status.equalsIgnoreCase("On The Way")) {
            line1.setBackgroundColor(activeColor);
            stepOnWay.setTextColor(activeColor);
        } else if (status.equalsIgnoreCase("Arrived") || status.equalsIgnoreCase("In Progress")) {
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

    // If we had the Request ID, we could fetch real-time updates here using
    // addSnapshotListener
}
