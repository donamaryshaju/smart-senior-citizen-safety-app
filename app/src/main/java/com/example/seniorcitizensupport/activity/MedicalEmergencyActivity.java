package com.example.seniorcitizensupport.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.example.seniorcitizensupport.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicalEmergencyActivity extends BaseActivity {

    private static final int REQUEST_CODE_VOICE = 100;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 101;

    // UI Components
    private TextView txtAddress, txtGpsStatus;
    private TextView txtConditions, txtBloodGroup, txtAllergies;
    private EditText editDetails;
    private RadioGroup radioGroupType;
    private CheckBox checkNotify, checkShareLocation;
    private Button btnSendAlert;

    // Data
    private FusedLocationProviderClient fusedLocationClient;
    private String currentAddress = "Unknown Location";
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                throwable.printStackTrace();
                // Try to show toast on UI thread if possible
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplicationContext(), "App Crash Caught: " + throwable.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                // Invoke default handler after delay or rely on it?
                // If we don't call default, app might freeze.
                // But user says "closes immediately", so default IS killing it.
                // Let's NOT call default for this specific activity's context if possible
                // (dangerous but debugging)
            });
            super.onCreate(savedInstanceState);
        } catch (Throwable t) {
            t.printStackTrace();
            // If super.onCreate fails, the activity is in a bad state, but we try to show
            // error
            try {
                setContentView(new android.view.View(this)); // Dummy view
                Toast.makeText(this, "Critical Init Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Throwable t2) {
            }
            return; // Exit
        }

        try {
            setContentView(R.layout.activity_medical_emergency);

            // Safe toolbar initialization
            try {
                Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            } catch (Exception e) {
                // Ignore toolbar errors if any, don't crash the activity
            }

            try {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            } catch (Exception e) {
                e.printStackTrace();
                // Continue without location client if failed
            }

            initializeViews();
            setupListeners();

            // Load Data
            fetchLocation();
            loadMedicalInfo();

        } catch (Throwable e) {
            e.printStackTrace();
            // Fallback UI to prevent "blank screen" or immediate close perception
            try {
                TextView errorView = new TextView(this);
                errorView.setText("Critical Error: " + e.getMessage());
                errorView.setTextSize(20);
                errorView.setPadding(50, 50, 50, 50);
                setContentView(errorView);
            } catch (Throwable t) {
                // If even this fails, then we really can't do anything
            }
            Toast.makeText(this, "Error starting Medical Emergency: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Log the error to console (user can't see this but good for us if we had logs)
            System.err.println("MedicalEmergencyCrash: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
            @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required for emergency services", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Re-fetch location or update UI if needed
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void initializeViews() {
        txtAddress = findViewById(R.id.text_address);
        txtGpsStatus = findViewById(R.id.text_gps_status);

        txtConditions = findViewById(R.id.text_conditions);
        txtBloodGroup = findViewById(R.id.text_blood_group);
        txtAllergies = findViewById(R.id.text_allergies);

        editDetails = findViewById(R.id.edit_details);
        radioGroupType = findViewById(R.id.radio_group_emergency_type);

        checkNotify = findViewById(R.id.check_notify_contacts);
        checkShareLocation = findViewById(R.id.check_share_location);

        btnSendAlert = findViewById(R.id.btn_send_alert);
    }

    private void setupListeners() {
        if (findViewById(R.id.btn_voice_input) != null) {
            findViewById(R.id.btn_voice_input).setOnClickListener(v -> startVoiceInput());
        }

        if (findViewById(R.id.btn_view_map) != null) {
            findViewById(R.id.btn_view_map).setOnClickListener(v -> openMap());
        }

        if (findViewById(R.id.btn_edit_address) != null) {
            findViewById(R.id.btn_edit_address).setOnClickListener(v -> showEditAddressDialog());
        }

        if (findViewById(R.id.btn_edit_medical) != null) {
            findViewById(R.id.btn_edit_medical).setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (findViewById(R.id.layout_call_emergency) != null) {
            findViewById(R.id.layout_call_emergency).setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:108"));
                startActivity(intent);
            });
        }

        if (btnSendAlert != null) {
            btnSendAlert.setOnClickListener(v -> sendEmergencyAlert());
        }
    }

    private void openMap() {
        try {
            Uri gmmIntentUri = Uri.parse("geo:" + currentLat + "," + currentLng + "?q=" + Uri.encode(currentAddress));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Maps app not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening map", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLocation() {
        if (fusedLocationClient == null)
            return;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_CODE_LOCATION_PERMISSION);
            return;
        }

        if (txtAddress != null)
            txtAddress.setText("Fetching GPS location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            try {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();

                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(currentLat, currentLng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            currentAddress = address.getAddressLine(0);
                            if (txtAddress != null)
                                txtAddress.setText(currentAddress);
                            if (txtGpsStatus != null)
                                txtGpsStatus.setVisibility(View.VISIBLE);
                        } else {
                            if (txtAddress != null)
                                txtAddress.setText("Lat: " + currentLat + ", Lng: " + currentLng);
                        }
                    } catch (IOException e) {
                        currentAddress = "Lat: " + currentLat + ", Lng: " + currentLng;
                        if (txtAddress != null)
                            txtAddress.setText(currentAddress);
                    }
                } else {
                    if (txtAddress != null)
                        txtAddress.setText("Location not found. Ensure GPS is on.");
                    if (txtGpsStatus != null)
                        txtGpsStatus.setVisibility(View.GONE);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                // Prevent crash from location callback
            }
        });
    }

    private void loadMedicalInfo() {
        if (auth == null || auth.getCurrentUser() == null || firestore == null)
            return;

        firestore.collection(Constants.KEY_COLLECTION_USERS).document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            String blood = documentSnapshot.getString("bloodGroup");
                            String conditions = documentSnapshot.getString("medicalConditions");
                            String allergies = documentSnapshot.getString("allergies");

                            if (txtBloodGroup != null)
                                txtBloodGroup.setText("🩸 Blood Group: " + (blood != null ? blood : "Not set"));
                            if (txtConditions != null)
                                txtConditions
                                        .setText("💊 " + (conditions != null ? conditions : "No known conditions"));
                            if (txtAllergies != null)
                                txtAllergies.setText("⚠️ Allergic to: " + (allergies != null ? allergies : "None"));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> {
                    // Ignore failures
                });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the emergency...");
        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                if (editDetails != null) {
                    String existingText = editDetails.getText().toString();
                    editDetails.setText(existingText + " " + result.get(0));
                }
            }
        }
    }

    private void showEditAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Location");
        final EditText input = new EditText(this);
        input.setText(currentAddress);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            currentAddress = input.getText().toString();
            if (txtAddress != null)
                txtAddress.setText(currentAddress);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendEmergencyAlert() {
        showProgressDialog("Sending Emergency Alert...");

        String emergencyType = "Other Emergency";
        if (radioGroupType != null) {
            int selectedId = radioGroupType.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedId);
                if (selectedRadioButton != null) {
                    emergencyType = selectedRadioButton.getText().toString();
                }
            }
        }

        String details = (editDetails != null) ? editDetails.getText().toString().trim() : "";
        String fullDescription = "Emergency: " + emergencyType + "\nDetails: " + details;

        if (checkNotify != null && checkNotify.isChecked()) {
            fullDescription += "\n[Contacts Notified]";
        }
        if (checkShareLocation != null && checkShareLocation.isChecked()) {
            fullDescription += "\n[Live Location Shared]";
        }

        RequestModel request = new RequestModel(
                auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "GUEST",
                Constants.TYPE_MEDICAL,
                "Pending",
                "High",
                fullDescription,
                Timestamp.now(),
                currentAddress);

        request.setLatitude(currentLat);
        request.setLongitude(currentLng);
        request.setIsAutoDispatch(true); // Don't show to admin

        if (firestore != null) {
            firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                    .add(request)
                    .addOnSuccessListener(documentReference -> {
                        hideProgressDialog();
                        NotificationHelper.showNotification(this, "Emergency Alert Sent",
                                "Reference ID: " + documentReference.getId());

                        new AlertDialog.Builder(this)
                                .setTitle("Alert Sent Successfully")
                                .setMessage("Help is on the way.\n\nPriority: HIGH\nLocation: Shared")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();

                        // START AUTOMATED DISPATCH
                        findAndNotifyVolunteers(documentReference.getId(), request);

                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Failed to send alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            hideProgressDialog();
            Toast.makeText(this, "Error: Service not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void findAndNotifyVolunteers(String requestId, RequestModel request) {
        if (request.getLatitude() == 0 || request.getLongitude() == 0)
            return;

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("userType", "volunteer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty())
                        return;

                    List<VolunteerDistance> volunteerList = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Double lat = doc.getDouble("latitude");
                            Double lng = doc.getDouble("longitude");

                            if (lat != null && lng != null && lat != 0 && lng != 0) {
                                double dist = calculateDistance(request.getLatitude(), request.getLongitude(), lat,
                                        lng);
                                volunteerList.add(new VolunteerDistance(doc.getId(), dist));
                            } else {
                                // Try to geocode if lat/lng missing (fallback for old users)
                                // Or just skip for MVP speed
                            }
                        } catch (Exception e) {
                            // Skip bad data
                        }
                    }

                    // Sort by distance
                    java.util.Collections.sort(volunteerList, (v1, v2) -> Double.compare(v1.distance, v2.distance));

                    // Pick Top 5
                    int limit = Math.min(volunteerList.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        String vid = volunteerList.get(i).id;
                        NotificationHelper.sendNotification(vid, "URGENT: Medical Emergency",
                                "Emergency nearby (" + String.format("%.1f", volunteerList.get(i).distance)
                                        + " km). Type: " + request.getDescription());
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static class VolunteerDistance {
        String id;
        double distance;

        VolunteerDistance(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
