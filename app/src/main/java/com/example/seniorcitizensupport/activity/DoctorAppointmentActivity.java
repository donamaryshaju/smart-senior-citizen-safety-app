package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.R;

public class DoctorAppointmentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointment);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        CardView cardAccompaniment = findViewById(R.id.card_accompaniment);
        CardView cardHomeVisit = findViewById(R.id.card_home_visit);
        CardView cardTalkDoctor = findViewById(R.id.card_talk_doctor);
        CardView cardSchedule = findViewById(R.id.card_schedule);

        cardAccompaniment.setOnClickListener(v -> {
            Intent intent = new Intent(this, HospitalAccompanimentActivity.class);
            startActivity(intent);
        });

        cardHomeVisit.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorHomeVisitActivity.class);
            startActivity(intent);
        });

        cardTalkDoctor.setOnClickListener(v -> {
            // Initiate a dialer intent
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:102")); // Emergency or support number
            startActivity(intent);
        });

        cardSchedule.setOnClickListener(v -> {
            // For now, redirect to generic medical form or show toast
            // Based on user request, this seems less priority than the first two
            // Reusing MedicalFormActivity for now
            Intent intent = new Intent(this, MedicalFormActivity.class);
            intent.putExtra("MEDICAL_SUBTYPE", "Clinical Appointment");
            startActivity(intent);
        });
    }
}
