package com.example.seniorcitizensupport.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.R;

public class GrocerySelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grocery_selection);

        ImageButton btnBack = findViewById(R.id.btn_back);

        CardView cardMonthly = findViewById(R.id.card_monthly);
        CardView cardEssentials = findViewById(R.id.card_essentials);
        CardView cardFruits = findViewById(R.id.card_fruits);

        btnBack.setOnClickListener(v -> finish());

        cardMonthly.setOnClickListener(v -> launchGroceryForm("Monthly Provisions"));
        cardEssentials.setOnClickListener(v -> launchGroceryForm("Daily Essentials"));
        cardFruits.setOnClickListener(v -> launchGroceryForm("Fruits & Vegetables"));
    }

    private void launchGroceryForm(String subType) {
        Intent intent = new Intent(this, GroceryOrderActivity.class);
        intent.putExtra("GROCERY_SUBTYPE", subType);
        startActivity(intent);
    }
}
