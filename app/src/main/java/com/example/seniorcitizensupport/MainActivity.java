
package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextView txtWelcome, txtLastStatus;
    private TextView btnSOS;
    private Button btnViewRequests;

    // RESTORED: cardGrocery added back here
    private MaterialCardView cardMedical, cardTransport, cardHomeCare, cardGrocery;
    private View btnLogout;

    // Bottom Navigation View
    private BottomNavigationView bottomNav;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();

        // 2. Initialize Views
        initializeViews();

        // 3. Load User Data
        loadUserProfile();

        // 4. Set Listeners

        // --- SOS BUTTON ---
        if (btnSOS != null) {
            btnSOS.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("EMERGENCY SOS")
                        .setMessage("Send Emergency Alert to everyone?")
                        .setPositiveButton("YES, SEND HELP", (dialog, which) -> {
                            fetchAddressAndCreateRequest("SOS", "Emergency", "High", "Emergency Alert triggered!");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // --- DIRECT ACTION BUTTONS ---
        if (cardMedical != null) {
            cardMedical.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MedicalActivity.class);
                startActivity(intent);
            });
        }

        // RESTORED: Grocery Click Listener
        if (cardGrocery != null) {
            cardGrocery.setOnClickListener(v -> {
                // This opens your new list of groceries
                Intent intent = new Intent(MainActivity.this, GroceryActivity.class);
                startActivity(intent);
            });
        }

        if (cardTransport != null) cardTransport.setOnClickListener(v -> showDetailedDialog("Transportation"));
        if (cardHomeCare != null) cardHomeCare.setOnClickListener(v -> showDetailedDialog("Home Care"));

        // --- VIEW REQUEST HISTORY BUTTON ---
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                try {
                    Toast.makeText(MainActivity.this, "Opening History...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, MyRequestsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error launching page: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        }

        // --- Logout ---
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
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
                if (id == R.id.nav_home) return true;
                if (id == R.id.nav_profile) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void initializeViews() {
        txtWelcome = findViewById(R.id.text_welcome_senior);
        txtLastStatus = findViewById(R.id.text_last_status);
        btnSOS = findViewById(R.id.btn_sos);
        btnViewRequests = findViewById(R.id.btn_view_requests);

        cardMedical = findViewById(R.id.card_medical);

        // RESTORED: Finding the view by ID
        cardGrocery = findViewById(R.id.card_grocery);

        cardTransport = findViewById(R.id.card_transport);
        cardHomeCare = findViewById(R.id.card_homecare);

        btnLogout = findViewById(R.id.btn_logout_senior);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void loadUserProfile() {
        DocumentReference docRef = fStore.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("fName");
                if (name == null) name = documentSnapshot.getString("fullName");

                if (name != null && !name.isEmpty()) {
                    txtWelcome.setText("Hello, " + name);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
        });
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

        RadioGroup radioGroup = view.findViewById(R.id.radio_group_options);
        RadioButton rb1 = view.findViewById(R.id.rb_doctor);
        RadioButton rb2 = view.findViewById(R.id.rb_pharmacy);
        RadioButton rb3 = view.findViewById(R.id.rb_hospital);

        title.setText(categoryType + " Request");

        if (categoryType.contains("Transport")) {
            rb1.setText("Regular Car (I can walk)");
            rb2.setText("Wheelchair Accessible Van");
            rb3.setText("Non-Emergency Ambulance");
            inputNote.setHint("Where do you need to go?");
        } else if (categoryType.contains("Home")) {
            rb1.setText("House Cleaning");
            rb2.setText("Cooking Help");
            rb3.setText("Companionship / Chat");
            inputNote.setHint("Describe what help you need...");
        } else {
            rb1.setText("Doctor Consultation");
            rb2.setText("Buy Medicine / Pharmacy");
            rb3.setText("Hospital Visit / Transport");
            inputNote.setHint("Describe symptoms or list medicines...");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String specificNeed = "";
            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId != -1) {
                RadioButton selectedRb = view.findViewById(selectedId);
                specificNeed = selectedRb.getText().toString();
            } else {
                specificNeed = "General Request";
            }

            String note = inputNote.getText().toString().trim();

            if (categoryType.contains("Transport") && note.isEmpty()) {
                inputNote.setError("Please enter where you need to go");
                return;
            }

            if (note.isEmpty()) {
                note = "No additional details.";
            }

            String finalDescription = "Specific: " + specificNeed + "\nNote: " + note;
            String priority = specificNeed.contains("Ambulance") ? "High" : "Normal";

            fetchAddressAndCreateRequest(categoryType, "Pending", priority, finalDescription);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchAddressAndCreateRequest(String type, String status, String priority, String description) {
        fStore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String address = "Address not provided";
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("address")) {
                            address = documentSnapshot.getString("address");
                        } else if (documentSnapshot.contains("location")) {
                            address = documentSnapshot.getString("location");
                        }
                    }
                    createRequestWithLocation(type, status, priority, description, address);
                })
                .addOnFailureListener(e -> {
                    createRequestWithLocation(type, status, priority, description, "Address fetch error");
                });
    }

    private void createRequestWithLocation(String type, String status, String priority, String description, String location) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("type", type);
        request.put("status", status);
        request.put("priority", priority);
        request.put("description", description);
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", location);

        fStore.collection("requests").add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Request Sent Successfully.", Toast.LENGTH_SHORT).show();
                    txtLastStatus.setText("Last Request: " + type + " (Pending)");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
