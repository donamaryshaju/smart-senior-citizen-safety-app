package com.example.seniorcitizensupport.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrescriptionActivity extends BaseActivity {

    private LinearLayout layoutExpandableUpload;
    private LinearLayout layoutExpandableVoice;
    private LinearLayout layoutExpandableManual;
    private LinearLayout layoutMedicineListContainer;
    
    private TextView iconExpandManual;
    private ImageView imgCheckManual;
    
    private RadioGroup radioGroupTime;
    private TextView txtAddress;
    
    private boolean isVoiceRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription);

        // Header
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Bind Views
        CardView cardUpload = findViewById(R.id.card_upload);
        layoutExpandableUpload = findViewById(R.id.layout_expandable_upload);
        
        CardView cardVoice = findViewById(R.id.card_voice);
        layoutExpandableVoice = findViewById(R.id.layout_expandable_voice);
        
        CardView cardManual = findViewById(R.id.card_manual);
        layoutExpandableManual = findViewById(R.id.layout_expandable_manual);
        iconExpandManual = findViewById(R.id.icon_expand_manual);
        imgCheckManual = findViewById(R.id.img_check_manual);
        
        CardView cardRepeat = findViewById(R.id.card_repeat);
        
        layoutMedicineListContainer = findViewById(R.id.layout_medicine_list_container);
        Button btnAddMedicine = findViewById(R.id.btn_add_medicine);
        
        radioGroupTime = findViewById(R.id.radio_group_time);
        txtAddress = findViewById(R.id.txt_address);
        Button btnSubmit = findViewById(R.id.btn_submit);

        // --- ACCORDION LOGIC ---
        cardUpload.setOnClickListener(v -> toggleAccordion(layoutExpandableUpload));
        cardVoice.setOnClickListener(v -> toggleAccordion(layoutExpandableVoice));
        cardManual.setOnClickListener(v -> {
             toggleAccordion(layoutExpandableManual);
             // Auto-add first row if empty
             if(layoutExpandableManual.getVisibility() == View.VISIBLE && layoutMedicineListContainer.getChildCount() == 0) {
                 addMedicineRow();
             }
             updateManualIcon();
        });
        
        btnAddMedicine.setOnClickListener(v -> addMedicineRow());

        // Stub Buttons logic
        findViewById(R.id.btn_camera).setOnClickListener(v -> Toast.makeText(this, "Opening Camera...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_gallery).setOnClickListener(v -> Toast.makeText(this, "Opening Gallery...", Toast.LENGTH_SHORT).show());
        
        Button btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(v -> {
            if(!isVoiceRecording) {
                isVoiceRecording = true;
                btnRecord.setText("🔴 Recording...");
                btnRecord.setBackgroundColor(0xFFFF0000); // Red
                Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
            } else {
                isVoiceRecording = false;
                btnRecord.setText("🎤 Start Recording");
                btnRecord.setBackgroundColor(0xFF4CAF50); // Green
            }
        });

        // Repeat Logic - Fills Manual List
        cardRepeat.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Repeat Order")
                .setMessage("Load medicines from your last order on Dec 25?")
                .setPositiveButton("Yes", (d, w) -> {
                    // Open Manual Section
                    if(layoutExpandableManual.getVisibility() != View.VISIBLE) {
                        toggleAccordion(layoutExpandableManual);
                    }
                    layoutMedicineListContainer.removeAllViews();
                    addMedicineRow("Metformin 500mg");
                    addMedicineRow("Atorvastatin 10mg");
                    Toast.makeText(this, "Medicines Loaded", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Load Address
        loadUserAddress();
        
        btnSubmit.setOnClickListener(v -> submitOrder());
    }

    private void toggleAccordion(View viewToOpen) {
        // Close others
        if(viewToOpen != layoutExpandableUpload) layoutExpandableUpload.setVisibility(View.GONE);
        if(viewToOpen != layoutExpandableVoice) layoutExpandableVoice.setVisibility(View.GONE);
        if(viewToOpen != layoutExpandableManual) {
            layoutExpandableManual.setVisibility(View.GONE);
            updateManualIcon(); // Reset arrow
        }

        // Toggle target
        if (viewToOpen.getVisibility() == View.VISIBLE) {
            viewToOpen.setVisibility(View.GONE);
        } else {
            viewToOpen.setVisibility(View.VISIBLE);
        }
        
        updateManualIcon();
    }
    
    private void updateManualIcon() {
        if(layoutExpandableManual.getVisibility() == View.VISIBLE) {
            iconExpandManual.setText("▲");
        } else {
            iconExpandManual.setText("▼");
            // If functionality is used, show check?
            if(layoutMedicineListContainer.getChildCount() > 0) {
                 imgCheckManual.setVisibility(View.VISIBLE);
            } else {
                 imgCheckManual.setVisibility(View.GONE);
            }
        }
    }

    private void addMedicineRow() {
        addMedicineRow("");
    }

    private void addMedicineRow(String prefillText) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_medicine_row, layoutMedicineListContainer, false);
        EditText editName = row.findViewById(R.id.edt_medicine_name);
        ImageButton btnRemove = row.findViewById(R.id.btn_remove);
        TextView txtNumber = row.findViewById(R.id.txt_number);

        editName.setText(prefillText);
        txtNumber.setText((layoutMedicineListContainer.getChildCount() + 1) + ".");

        btnRemove.setOnClickListener(v -> {
            layoutMedicineListContainer.removeView(row);
            renumberRows();
        });

        layoutMedicineListContainer.addView(row);
    }

    private void renumberRows() {
        for (int i = 0; i < layoutMedicineListContainer.getChildCount(); i++) {
            View row = layoutMedicineListContainer.getChildAt(i);
            TextView txtNumber = row.findViewById(R.id.txt_number);
            txtNumber.setText((i + 1) + ".");
        }
    }

    private void loadUserAddress() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null)
            return;

        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String addr = snap.getString("address");
                        if (addr != null)
                            txtAddress.setText(addr);
                    }
                });
    }

    private void submitOrder() {
        List<String> medicines = new ArrayList<>();
        for (int i = 0; i < layoutMedicineListContainer.getChildCount(); i++) {
            View row = layoutMedicineListContainer.getChildAt(i);
            EditText edt = row.findViewById(R.id.edt_medicine_name);
            String name = edt.getText().toString().trim();
            if (!name.isEmpty())
                medicines.add(name);
        }

        if (medicines.isEmpty()) {
            Toast.makeText(this, "Please add at least one medicine", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Request
        Map<String, Object> request = new HashMap<>();
        request.put("userId", FirebaseAuth.getInstance().getUid());
        request.put("type", "Prescription Purchase");
        request.put("details", "Medicines: " + medicines.toString());
        request.put("status", "Pending");
        request.put("timestamp", System.currentTimeMillis());

        // Time Pref
        int selectedId = radioGroupTime.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_urgent)
            request.put("priority", "High (Urgent)");
        else
            request.put("priority", "Normal");

        // Save
        FirebaseFirestore.getInstance().collection("requests")
                .add(request)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Order Submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
