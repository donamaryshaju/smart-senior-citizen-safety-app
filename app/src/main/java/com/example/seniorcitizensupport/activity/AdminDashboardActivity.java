package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private ImageView btnLogout;

    private TextView txtWelcome, txtVolunteerCount, txtSeniorCount;

    // --- FIX: Updated CardView variables ---
    private CardView cardManageMedicines, cardManageGroceries, cardManageTransport, cardManageHomeCare;

    private ImageView iconProfile, iconNotifications;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Components
        txtWelcome = findViewById(R.id.text_admin_welcome);
        btnLogout = findViewById(R.id.button_logout_admin);
        txtVolunteerCount = findViewById(R.id.text_volunteer_count);
        txtSeniorCount = findViewById(R.id.text_senior_count);
        iconProfile = findViewById(R.id.icon_profile);
        iconNotifications = findViewById(R.id.icon_notifications);

        // --- FIX: Updated findViewById calls for the CardViews ---
        cardManageMedicines = findViewById(R.id.card_manage_medicines);
        cardManageGroceries = findViewById(R.id.card_manage_groceries);
        cardManageTransport = findViewById(R.id.card_manage_transport); // Corrected from card_manage_users
        cardManageHomeCare = findViewById(R.id.card_manage_homecare);   // Corrected from card_view_orders

        // --- Set Click Listeners ---

        // Header Icons
        iconProfile.setOnClickListener(v -> Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show());
        iconNotifications.setOnClickListener(v -> Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show());

        // Logout Button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(AdminDashboardActivity.this, "Admin logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- FIX: Updated CardView Click Listeners ---
        cardManageMedicines.setOnClickListener(v -> Toast.makeText(this, "Manage Medicines Clicked", Toast.LENGTH_SHORT).show());
        cardManageGroceries.setOnClickListener(v -> Toast.makeText(this, "Manage Groceries Clicked", Toast.LENGTH_SHORT).show());
        cardManageTransport.setOnClickListener(v -> Toast.makeText(this, "Manage Transport Clicked", Toast.LENGTH_SHORT).show());
        cardManageHomeCare.setOnClickListener(v -> Toast.makeText(this, "Home Care Clicked", Toast.LENGTH_SHORT).show());
    }
}
