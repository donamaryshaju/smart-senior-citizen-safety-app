package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import androidx.cardview.widget.CardView;

public class TransportSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_selection);

        ImageButton btnBack = findViewById(R.id.btn_back);
        CardView cardAmbulance = findViewById(R.id.card_ambulance);
        CardView cardWheelchair = findViewById(R.id.card_wheelchair);
        CardView cardNormal = findViewById(R.id.card_normal_transport);

        btnBack.setOnClickListener(v -> finish());

        cardAmbulance.setOnClickListener(v -> launchTransportForm("Ambulance"));
        cardWheelchair.setOnClickListener(v -> launchTransportForm("Wheelchair Assistance"));
        cardNormal.setOnClickListener(v -> launchTransportForm("Normal Transport"));
    }

    private void launchTransportForm(String subType) {
        Intent intent = new Intent(this, TransportFormActivity.class);
        intent.putExtra("TRANSPORT_SUBTYPE", subType);
        startActivity(intent);
    }
}
