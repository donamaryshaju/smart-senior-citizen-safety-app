package com.example.seniorcitizensupport.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.example.seniorcitizensupport.BaseActivity;
import com.example.seniorcitizensupport.Constants;
import com.example.seniorcitizensupport.R;
import com.google.firebase.firestore.FieldValue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HospitalAccompanimentActivity extends BaseActivity {

    private EditText inputHospital;
    private CheckBox cbNearest;
    private RadioGroup rgTiming;
    private LinearLayout layoutSchedule;
    private Button btnDate, btnTime, btnRequest;
    private Spinner spinnerReason;
    private Switch switchWheelchair;

    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_accompaniment);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        inputHospital = findViewById(R.id.input_hospital_name);
        cbNearest = findViewById(R.id.cb_nearest_hospital);
        rgTiming = findViewById(R.id.rg_timing);
        layoutSchedule = findViewById(R.id.layout_schedule_time);
        btnDate = findViewById(R.id.btn_select_date);
        btnTime = findViewById(R.id.btn_select_time);
        spinnerReason = findViewById(R.id.spinner_reason);
        switchWheelchair = findViewById(R.id.switch_wheelchair);
        btnRequest = findViewById(R.id.btn_request_assistance);

        // Populate Spinner
        String[] reasons = { "Check-up", "Follow-up", "Emergency", "Therapy", "Surgery", "Other" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reasons);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReason.setAdapter(adapter);
    }

    private void setupListeners() {
        cbNearest.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                inputHospital.setText("Nearest Hospital");
                inputHospital.setEnabled(false);
            } else {
                inputHospital.setText("");
                inputHospital.setEnabled(true);
            }
        });

        rgTiming.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_schedule) {
                layoutSchedule.setVisibility(View.VISIBLE);
            } else {
                layoutSchedule.setVisibility(View.GONE);
                selectedDate = "";
                selectedTime = "";
            }
        });

        btnDate.setOnClickListener(v -> showDatePicker());
        btnTime.setOnClickListener(v -> showTimePicker());
        btnRequest.setOnClickListener(v -> submitRequest());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    btnDate.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    String amPm = hourOfDay >= 12 ? "PM" : "AM";
                    int showHour = hourOfDay > 12 ? hourOfDay - 12 : hourOfDay;
                    if (showHour == 0)
                        showHour = 12;
                    selectedTime = String.format("%02d:%02d %s", showHour, minute1, amPm);
                    btnTime.setText(selectedTime);
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void submitRequest() {
        String hospital = inputHospital.getText().toString().trim();
        if (TextUtils.isEmpty(hospital)) {
            inputHospital.setError("Required");
            return;
        }

        boolean isImmediate = rgTiming.getCheckedRadioButtonId() == R.id.rb_immediate;
        if (!isImmediate) {
            if (TextUtils.isEmpty(selectedDate) || TextUtils.isEmpty(selectedTime)) {
                showToast("Please select Date and Time for scheduling");
                return;
            }
        }

        showProgressDialog("Sending Request...");

        Map<String, Object> request = new HashMap<>();
        request.put("userId", auth.getCurrentUser().getUid());
        request.put("type", "Hospital Accompaniment");
        request.put("hospital", hospital);
        request.put("timingType", isImmediate ? "Immediate" : "Scheduled");
        request.put("scheduledDate", selectedDate);
        request.put("scheduledTime", selectedTime);
        request.put("reason", spinnerReason.getSelectedItem().toString());
        request.put("wheelchairNeeded", switchWheelchair.isChecked());

        String description = String.format("Hospital: %s\nTiming: %s %s\nReason: %s\nWheelchair: %s",
                hospital, isImmediate ? "IMMEDIATE" : "Scheduled",
                isImmediate ? "" : "(" + selectedDate + " " + selectedTime + ")",
                spinnerReason.getSelectedItem().toString(), switchWheelchair.isChecked() ? "Yes" : "No");

        request.put("description", description);
        request.put("status", Constants.STATUS_PENDING);
        request.put("priority", isImmediate ? "High" : "Normal");
        request.put("timestamp", FieldValue.serverTimestamp());
        request.put("location", "N/A"); // Fetch location if needed

        firestore.collection(Constants.KEY_COLLECTION_REQUESTS)
                .add(request)
                .addOnSuccessListener(ref -> {
                    hideProgressDialog();
                    showToast("Assistance Requested! Finding a volunteer...");
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Failed: " + e.getMessage());
                });
    }
}
