package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransportFormActivity extends BaseActivity {

    private String subType;
    private EditText inputPickup, inputDestination, inputDate, inputTime, inputAssistanceNote;
    private RadioGroup radioGroupTripType;
    private RadioButton radioRoundTrip;
    private LinearLayout layoutDateTime, layoutWheelchairDetails;
    private CheckBox checkWheelchairNeeded, checkLift, checkStairs;
    private TextView txtCostEstimate;
    private Button btnFindVolunteer;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0, currentLng = 0.0;

    private Calendar calendar = Calendar.getInstance();
    private boolean isScheduled = false;

    // Default Rates
    private double baseRate = 50.0;
    private double perKmRate = 15.0;
    private double roundTripMultiplier = 1.8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_form);

        subType = getIntent().getStringExtra("TRANSPORT_SUBTYPE");
        if (subType == null)
            subType = "Book a Ride";

        isScheduled = subType.contains("Scheduled");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupDynamicUI();
        setupListeners();
        fetchPricingRates(); // Fetch dynamic rates
        fetchCurrentLocation();
    }

    private void fetchPricingRates() {
        firestore.collection("configuration").document("transport_rates").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double base = documentSnapshot.getDouble("base_rate");
                        Double perKm = documentSnapshot.getDouble("per_km_rate");
                        Double multi = documentSnapshot.getDouble("round_trip_multiplier");

                        if (base != null)
                            baseRate = base;
                        if (perKm != null)
                            perKmRate = perKm;
                        if (multi != null)
                            roundTripMultiplier = multi;

                        calculateEstimatedCost(); // Recalculate with new rates
                    } else {
                        // Create default config if not exists (Optional, mainly for dev)
                        createDefaultConfig();
                    }
                });
    }

    private void createDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("base_rate", 50.0);
        config.put("per_km_rate", 15.0);
        config.put("round_trip_multiplier", 1.8);
        firestore.collection("configuration").document("transport_rates").set(config);
    }

    private void initializeViews() {
        TextView title = findViewById(R.id.text_form_title);
        title.setText(subType);

        inputPickup = findViewById(R.id.input_pickup);
        inputDestination = findViewById(R.id.input_destination);

        radioGroupTripType = findViewById(R.id.radio_group_trip_type);
        radioRoundTrip = findViewById(R.id.radio_round_trip);

        layoutDateTime = findViewById(R.id.layout_date_time);
        inputDate = findViewById(R.id.input_date);
        inputTime = findViewById(R.id.input_time);

        checkWheelchairNeeded = findViewById(R.id.check_wheelchair_needed);
        layoutWheelchairDetails = findViewById(R.id.layout_wheelchair_details);
        checkLift = findViewById(R.id.check_lift);
        checkStairs = findViewById(R.id.check_stairs);
        inputAssistanceNote = findViewById(R.id.input_assistance_note);

        txtCostEstimate = findViewById(R.id.text_cost_estimate);
        btnFindVolunteer = findViewById(R.id.btn_find_volunteer);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupDynamicUI() {
        if (isScheduled) {
            layoutDateTime.setVisibility(View.VISIBLE);
        } else {
            layoutDateTime.setVisibility(View.GONE);
        }

        // Wheelchair toggle logic handled in listeners
    }

    private void setupListeners() {
        // Wheelchair Toggle
        checkWheelchairNeeded.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutWheelchairDetails.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Date Picker
        inputDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time Picker
        inputTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateTimeLabel();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        });

        inputDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateEstimatedCost();
            }
        });

        radioGroupTripType.setOnCheckedChangeListener((group, checkedId) -> calculateEstimatedCost());

        btnFindVolunteer.setOnClickListener(v -> submitRequest());
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        inputDate.setText(sdf.format(calendar.getTime()));
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        inputTime.setText(sdf.format(calendar.getTime()));
    }

    private void calculateEstimatedCost() {
        double distance = 5.0; // Assume 5km default
        if (inputDestination.getText().length() > 5) {
            distance = 12.5; // Simulate longer distance
        }

        double total = baseRate + (distance * perKmRate);

        if (radioRoundTrip.isChecked()) {
            total = total * roundTripMultiplier;
        }

        txtCostEstimate.setText(String.format(Locale.getDefault(), "₹%.2f", total));
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

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                    if (!addresses.isEmpty()) {
                        inputPickup.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (IOException e) {
                    // Ignore
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

        if (isScheduled && inputDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Select Date", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFindVolunteer.setEnabled(false);
        btnFindVolunteer.setText("Finding Volunteers...");

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", Constants.TYPE_TRANSPORT);
        request.put("subType", subType);
        request.put("pickup", pickup);
        request.put("destination", destination);
        request.put("tripType", radioRoundTrip.isChecked() ? "Round-trip" : "One-way");
        request.put("scheduledTime", isScheduled ? inputDate.getText() + " " + inputTime.getText() : "Immediate");
        request.put("fuelCost", txtCostEstimate.getText().toString());
        request.put("status", Constants.STATUS_PENDING);

        boolean isWheelchair = checkWheelchairNeeded.isChecked();
        boolean isAmbulance = subType.contains("Ambulance");
        request.put("priority", (isWheelchair || isAmbulance) ? "High" : "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", pickup); // For compatibility
        request.put("latitude", currentLat);
        request.put("longitude", currentLng);

        String desc = subType + " to " + destination + " (" + txtCostEstimate.getText().toString() + ")";
        if (isWheelchair)
            desc += " [WHEELCHAIR NEEDED]";
        request.put("description", desc);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Request Sent Successfully! You can track it on your Dashboard.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnFindVolunteer.setEnabled(true);
                    btnFindVolunteer.setText("FIND VOLUNTEER");
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
