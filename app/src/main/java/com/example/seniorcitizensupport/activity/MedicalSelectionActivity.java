package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.R;

public class MedicalSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_selection);

        ImageButton btnBack = findViewById(R.id.btn_back);
        CardView cardEmergency = findViewById(R.id.card_emergency);
        CardView cardPrescription = findViewById(R.id.card_prescription);
        CardView cardDoctor = findViewById(R.id.card_doctor);
        btnBack.setOnClickListener(v -> finish());

        cardEmergency.setOnClickListener(v -> launchMedicalForm("Medical Emergency"));
        cardPrescription.setOnClickListener(v -> launchMedicalForm("Prescription Purchase"));
        cardDoctor.setOnClickListener(v -> launchMedicalForm("Doctor Visit Assistance"));
    }

    private void launchMedicalForm(String subType) {
        if (subType.equals("Prescription Purchase")) {
            Intent intent = new Intent(this, BuyMedicineActivity.class);
            startActivity(intent);
        } else if (subType.equals("Medical Emergency")) {
            Intent intent = new Intent(this, MedicalEmergencyActivity.class);
            startActivity(intent);
        } else if (subType.equals("Doctor Visit Assistance")) {
            // New Doctor Appointment Activity
            Intent intent = new Intent(this, DoctorAppointmentActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MedicalFormActivity.class);
            intent.putExtra("MEDICAL_SUBTYPE", subType);
            startActivity(intent);
        }
    }
}
