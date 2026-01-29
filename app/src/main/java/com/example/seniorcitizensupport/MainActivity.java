package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.example.seniorcitizensupport.model.RequestModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {

    // UI Components
    private TextView txtWelcome, txtLastStatus;
    private TextView btnSOS;
    private Button btnViewRequests;
    private MaterialCardView cardMedical, cardTransport, cardHomeCare, cardGrocery;
    private View btnLogout;

    // Bottom Navigation View
    private BottomNavigationView bottomNav;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();

        // Initialize Views
        initializeViews();

        // Load User Data
        loadUserProfile();

        // Set Listeners
        setupListeners();
    }

    private void initializeViews() {
        txtWelcome = findViewById(R.id.text_welcome_senior);
        txtLastStatus = findViewById(R.id.text_last_status);
        btnSOS = findViewById(R.id.btn_sos);
        btnViewRequests = findViewById(R.id.btn_view_requests);

        cardMedical = findViewById(R.id.card_medical);
        cardGrocery = findViewById(R.id.card_grocery);
        cardTransport = findViewById(R.id.card_transport);
        cardHomeCare = findViewById(R.id.card_homecare);

        btnLogout = findViewById(R.id.btn_logout_senior);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void loadUserProfile() {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                        if (name == null)
                            name = documentSnapshot.getString("fullName"); // Fallback
                        if (name == null)
                            name = documentSnapshot.getString("fName"); // Fallback

                        if (name != null && !name.isEmpty()) {
                            txtWelcome.setText("Hello, " + name);
                        }
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to load profile"));
    }

    private void setupListeners() {
        // --- SOS BUTTON ---
        if (btnSOS != null) {
            btnSOS.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("EMERGENCY SOS")
                        .setMessage("Send Emergency Alert to everyone?")
                        .setPositiveButton("YES, SEND HELP", (dialog, which) -> {
                            fetchAddressAndCreateRequest(Constants.TYPE_SOS, "Emergency", "High",
                                    "Emergency Alert triggered!");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // --- DIRECT ACTION BUTTONS ---
        if (cardMedical != null)
            cardMedical.setOnClickListener(v -> launchRequestActivity(Constants.TYPE_MEDICAL));
        if (cardGrocery != null)
            cardGrocery.setOnClickListener(v -> launchRequestActivity(Constants.TYPE_GROCERY));
        if (cardTransport != null)
            cardTransport.setOnClickListener(v -> launchRequestActivity(Constants.TYPE_TRANSPORT));
        if (cardHomeCare != null)
            cardHomeCare.setOnClickListener(v -> launchRequestActivity(Constants.TYPE_HOMECARE));

        // --- VIEW REQUEST HISTORY BUTTON ---
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MyRequestsActivity.class);
                startActivity(intent);
            });
        }

        // --- Logout ---
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                auth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- BOTTOM NAVIGATION ---
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home)
                    return true;
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private void showDetailedDialog(String categoryType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm_request, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView title = view.findViewById(R.id.dialog_title);
        EditText inputNote = view.findViewById(R.id.input_note);
        Button btnSubmit = view.findViewById(R.id.btn_confirm_send);
        View btnClose = view.findViewById(R.id.btn_close_dialog);

        TextView specificHelpTitle = view.findViewById(R.id.text_specific_help_title);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_options);
        RadioButton rb1 = view.findViewById(R.id.rb_doctor);
        RadioButton rb2 = view.findViewById(R.id.rb_pharmacy);
        RadioButton rb3 = view.findViewById(R.id.rb_hospital);

        title.setText(categoryType + " Request");

        specificHelpTitle.setVisibility(View.VISIBLE);
        radioGroup.setVisibility(View.VISIBLE);

        if (categoryType.equals(Constants.TYPE_TRANSPORT)) {
            rb1.setText("Regular Car (I can walk)");
            rb2.setText("Wheelchair Accessible Van");
            rb3.setText("Non-Emergency Ambulance");
            inputNote.setHint("Where do you need to go?");
        } else if (categoryType.equals(Constants.TYPE_HOMECARE)) {
            rb1.setText("House Cleaning");
            rb2.setText("Cooking Help");
            rb3.setText("Companionship / Chat");
            inputNote.setHint("Describe what help you need...");
        } else if (categoryType.equals(Constants.TYPE_GROCERY)) {
            rb1.setText("Buy Specific List");
            rb2.setText("Essentials (Milk, Bread, etc.)");
            rb3.setText("Fruits & Vegetables only");
            inputNote.setHint("Please type your list or add details");
        } else { // Default case for "Medical"
            rb1.setText("Doctor Consultation");
            rb2.setText("Buy Medicine from Pharmacy");
            rb3.setText("Hospital Visit (Transport)");
            inputNote.setHint("Describe symptoms or list medicines...");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String note = inputNote.getText().toString().trim();
            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1 && note.isEmpty()) {
                showToast("Please select an option or add details");
                return;
            }

            String specificNeed = "General Request";
            if (selectedId != -1) {
                RadioButton selectedRb = view.findViewById(selectedId);
                specificNeed = selectedRb.getText().toString();
            }

            String finalDescription;
            if (note.isEmpty()) {
                finalDescription = "Request: " + specificNeed;
            } else {
                finalDescription = "Request: " + specificNeed + "\nDetails: " + note;
            }

            String priority = specificNeed.contains("Ambulance") ? "High" : "Normal";

            fetchAddressAndCreateRequest(categoryType, "Pending", priority, finalDescription);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchAddressAndCreateRequest(String type, String status, String priority, String description) {
        firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String address = "Address not provided";
                    if (documentSnapshot.exists() && documentSnapshot.getString("address") != null) {
                        address = documentSnapshot.getString("address");
                    }
                    createRequestWithLocation(type, status, priority, description, address);
                })
                .addOnFailureListener(e -> {
                    createRequestWithLocation(type, status, priority, description, "Address fetch error");
                });
    }

    private void createRequestWithLocation(String type, String status, String priority, String description,
            String location) {
        RequestModel request = new RequestModel();
        request.setUserId(userId);
        request.setType(type);
        request.setStatus(status);
        request.setPriority(priority);
        request.setDescription(description);
        request.setTimestamp(Timestamp.now());
        request.setLocation(location);

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS).add(request)
                .addOnSuccessListener(documentReference -> {
                    showToast("Request Sent Successfully.");
                    txtLastStatus.setText("Last Request: " + type + " (Pending)");
                })
                .addOnFailureListener(e -> showToast("Error: " + e.getMessage()));
    }

    private void launchRequestActivity(String type) {
        Intent intent = new Intent(MainActivity.this, CreateRequestActivity.class);
        intent.putExtra("EXTRA_REQUEST_TYPE", type);
        startActivity(intent);
    }
}
