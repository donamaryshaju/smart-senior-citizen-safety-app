package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegistrationSelectionActivity extends AppCompatActivity {

    private MaterialCardView cardSenior, cardVolunteer, cardFamily, cardAdmin;
    private TextView textLoginLink;
    private FirebaseFirestore db;
    private boolean adminExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_selection);

        db = FirebaseFirestore.getInstance();

        cardSenior = findViewById(R.id.card_senior);
        cardVolunteer = findViewById(R.id.card_volunteer);
        cardFamily = findViewById(R.id.card_family);
        cardAdmin = findViewById(R.id.card_admin);
        textLoginLink = findViewById(R.id.text_login_link);

        checkIfAdminExists();
        setupListeners();
    }

    private void checkIfAdminExists() {
        // Check if ANY user has role 'admin'
        db.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo("role", Constants.ROLE_ADMIN)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Admin exists
                                adminExists = true;
                                disableAdminCard();
                            }
                        }
                    }
                });
    }

    private void disableAdminCard() {
        cardAdmin.setAlpha(0.5f);
        cardAdmin.setCardBackgroundColor(Color.parseColor("#EEEEEE"));
        // Remove click listener logic for blocking effectively happens in listener
    }

    private void setupListeners() {
        cardSenior.setOnClickListener(v -> navigateToRegistration(Constants.ROLE_SENIOR));
        cardVolunteer.setOnClickListener(v -> navigateToRegistration(Constants.ROLE_VOLUNTEER));
        cardFamily.setOnClickListener(v -> navigateToRegistration(Constants.ROLE_FAMILY));

        cardAdmin.setOnClickListener(v -> {
            if (adminExists) {
                Toast.makeText(this, "Admin already registered. Please login.", Toast.LENGTH_LONG).show();
            } else {
                navigateToRegistration(Constants.ROLE_ADMIN);
            }
        });

        textLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationSelectionActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToRegistration(String userType) {
        Intent intent = null;

        switch (userType) {
            case Constants.ROLE_SENIOR:
                intent = new Intent(this, SeniorRegistrationActivity.class);
                break;
            case Constants.ROLE_VOLUNTEER:
                intent = new Intent(this, VolunteerRegistrationActivity.class);
                break;
            case Constants.ROLE_FAMILY:
                intent = new Intent(this, FamilyRegistrationActivity.class);
                break;
            case Constants.ROLE_ADMIN:
                intent = new Intent(this, AdminRegistrationActivity.class);
                break;
            default:
                intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("USER_TYPE", userType);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }
}
