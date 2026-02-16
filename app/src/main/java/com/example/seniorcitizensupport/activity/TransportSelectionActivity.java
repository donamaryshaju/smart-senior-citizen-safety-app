package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.R;

public class TransportSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_selection);

        ImageButton btnBack = findViewById(R.id.btn_back);
        androidx.cardview.widget.CardView cardAmbulance = findViewById(R.id.card_ambulance);
        androidx.cardview.widget.CardView cardBookRide = findViewById(R.id.card_book_ride);
        androidx.cardview.widget.CardView cardScheduledTrip = findViewById(R.id.card_scheduled_trip);
        btnBack.setOnClickListener(v -> finish());

        cardAmbulance.setOnClickListener(v -> launchTransportForm("Ambulance"));
        cardBookRide.setOnClickListener(v -> launchTransportForm("Book a Ride"));
        cardScheduledTrip.setOnClickListener(v -> launchTransportForm("Scheduled Trip"));
    }

    private void launchTransportForm(String subType) {
        Intent intent = new Intent(this, TransportFormActivity.class);
        intent.putExtra("TRANSPORT_SUBTYPE", subType);
        startActivity(intent);
    }
}
