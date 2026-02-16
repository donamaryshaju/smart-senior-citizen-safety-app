package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.R;

public class HomeCareSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homecare_selection);

        ImageButton btnBack = findViewById(R.id.btn_back);

        CardView cardCleaning = findViewById(R.id.card_cleaning);
        CardView cardCooking = findViewById(R.id.card_cooking);
        CardView cardNursing = findViewById(R.id.card_nursing);
        CardView cardMaintenance = findViewById(R.id.card_maintenance);

        btnBack.setOnClickListener(v -> finish());

        cardCleaning.setOnClickListener(v -> launchHomeForm("Cleaning / Housekeeping"));
        cardCooking.setOnClickListener(v -> launchHomeForm("Cooking Help"));
        cardNursing.setOnClickListener(v -> launchHomeForm("Nursing Assistance"));
        cardMaintenance.setOnClickListener(v -> launchHomeForm("Home Maintenance"));
    }

    private void launchHomeForm(String subType) {
        Intent intent = new Intent(this, HomeCareFormActivity.class);
        intent.putExtra("HOMECARE_SUBTYPE", subType);
        startActivity(intent);
    }
}
