package com.example.seniorcitizensupport;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    protected FirebaseFirestore firestore;
    protected FirebaseAuth auth;
    private Dialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new Dialog(this);
            progressDialog.setContentView(R.layout.dialog_progress); // You might need to create this layout
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    // Simple version of showProgressDialog if layout is not ready
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
