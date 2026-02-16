package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransportFormActivity extends BaseActivity {

    private String subType;
    private EditText inputPickup, inputDestination, inputTime, inputAssistanceNote;
    private Spinner spinnerEmergency;
    private CheckBox checkLift, checkStairs;
    private LinearLayout groupAmbulance, groupWheelchair, groupNormal;
    private Button btnSubmit;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0, currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_form);

        subType = getIntent().getStringExtra("TRANSPORT_SUBTYPE");
        if (subType == null)
            subType = "Normal Transport";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupDynamicUI();
        fetchCurrentLocation(); // Auto-fill pickup

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> submitRequest());
    }

    private void initializeViews() {
        TextView title = findViewById(R.id.text_form_title);
        title.setText(subType + " Request");

        inputPickup = findViewById(R.id.input_pickup);
        inputDestination = findViewById(R.id.input_destination);

        groupAmbulance = findViewById(R.id.group_ambulance_fields);
        groupWheelchair = findViewById(R.id.group_wheelchair_fields);
        groupNormal = findViewById(R.id.group_normal_fields);

        // Ambulance
        spinnerEmergency = findViewById(R.id.spinner_emergency_level);

        // Wheelchair
        checkLift = findViewById(R.id.check_lift);
        checkStairs = findViewById(R.id.check_stairs);
        inputAssistanceNote = findViewById(R.id.input_assistance_note);

        // Normal
        inputTime = findViewById(R.id.input_time);

        btnSubmit = findViewById(R.id.btn_submit_transport);
    }

    private void setupDynamicUI() {
        groupAmbulance.setVisibility(View.GONE);
        groupWheelchair.setVisibility(View.GONE);
        groupNormal.setVisibility(View.GONE);

        if (subType.contains("Ambulance")) {
            groupAmbulance.setVisibility(View.VISIBLE);
            String[] priorities = { "Critical Emergency", "Non-Critical Transport" };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                    priorities);
            spinnerEmergency.setAdapter(adapter);
            btnSubmit.setBackgroundColor(android.graphics.Color.RED);
            btnSubmit.setText("SEND EMERGENCY ALERT");
        } else if (subType.contains("Wheelchair")) {
            groupWheelchair.setVisibility(View.VISIBLE);
        } else {
            groupNormal.setVisibility(View.VISIBLE);
        }
    }

    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();

                // Reverse Geocode
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                    if (!addresses.isEmpty()) {
                        inputPickup.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (IOException e) {
                    inputPickup.setText("Lat: " + currentLat + ", Lng: " + currentLng);
                }
            }
        });
    }

    private void submitRequest() {
        String pickup = inputPickup.getText().toString().trim();
        String destination = inputDestination.getText().toString().trim();

        if (destination.isEmpty()) {
            inputDestination.setError("Destination required");
            return;
        }

        // Build description string
        StringBuilder details = new StringBuilder();
        details.append("Transport Type: ").append(subType).append("\n");
        details.append("Pickup: ").append(pickup).append("\n");
        details.append("Drop-off: ").append(destination).append("\n");

        String priority = "Normal";

        if (subType.contains("Ambulance")) {
            String urgency = spinnerEmergency.getSelectedItem().toString();
            details.append("Urgency: ").append(urgency);
            priority = "High"; // AUTO HIGH PRIORITY
        } else if (subType.contains("Wheelchair")) {
            if (checkLift.isChecked())
                details.append("- Needs Lift/Ramp\n");
            if (checkStairs.isChecked())
                details.append("- Needs Stair Assistance\n");
            details.append("Note: ").append(inputAssistanceNote.getText().toString());
        } else {
            details.append("Time: ").append(inputTime.getText().toString());
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_TRANSPORT);
        request.put("description", details.toString());
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", priority);
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", pickup); // Use pickup as the main location address
        request.put("latitude", currentLat);
        request.put("longitude", currentLng);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Transport Request Sent!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to Dashboard or Selection
                    // If selection activity is on stack, maybe finish affinity?
                    // For now, finishing this returns to Selection, user can back out.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("TRY AGAIN");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        }
    }
}
