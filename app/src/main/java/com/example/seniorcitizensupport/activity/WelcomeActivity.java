package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Check if a user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If user is already logged in, go directly to the MainActivity
            goToMainActivity();
            return; // Important: stops the rest of this method from running
        }

        // If no user is logged in, show the welcome screen
        setContentView(R.layout.activity_welcome);

        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);
        Button guestButton = findViewById(R.id.button_continue_guest);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        guestButton.setOnClickListener(v -> {
            goToMainActivity();
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
