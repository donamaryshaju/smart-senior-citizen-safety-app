package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button btnLogout;
    private TextView txtWelcome;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure you have a layout file named activity_admin_dashboard.xml
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();

        txtWelcome = findViewById(R.id.text_admin_welcome);
        btnLogout = findViewById(R.id.button_logout_admin);

        // Logout Logic
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(AdminDashboardActivity.this, "Admin logged out", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
