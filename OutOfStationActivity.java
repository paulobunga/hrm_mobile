package com.example.yourpackage.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class OutOfStationActivity extends AppCompatActivity {

    private EditText requestStartDate;
    private EditText requestEndDate;
    private Spinner reason;
    private EditText comments;
    private Button attachDocuments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_of_station);

        DatePicker requestStartDate = findViewById(R.id.request_start_date);
        DatePicker requestEndDate = findViewById(R.id.request_end_date);

        // Example of setting a listener for date changes
        requestStartDate.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Handle start date change
            }
        });

        requestEndDate.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Handle end date change
            }
        });
        reason = findViewById(R.id.reason);
        comments = findViewById(R.id.comments);
        attachDocuments = findViewById(R.id.attach_documents);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.reasons_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reason.setAdapter(adapter);

        attachDocuments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        Button submitRequest = findViewById(R.id.submit_request);
        submitRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitOutOfStationRequest();
            }
        });
    }

    private void submitOutOfStationRequest() {
        // Gather form data
        String startDate = ((DatePicker) findViewById(R.id.request_start_date)).getYear() + "-" +
                ((DatePicker) findViewById(R.id.request_start_date)).getMonth() + "-" +
                ((DatePicker) findViewById(R.id.request_start_date)).getDayOfMonth();

        String endDate = ((DatePicker) findViewById(R.id.request_end_date)).getYear() + "-" +
                ((DatePicker) findViewById(R.id.request_end_date)).getMonth() + "-" +
                ((DatePicker) findViewById(R.id.request_end_date)).getDayOfMonth();

        String reason = ((Spinner) findViewById(R.id.reason)).getSelectedItem().toString();
        String comments = ((EditText) findViewById(R.id.comments)).getText().toString();

        // Create a request object (assuming you have a model class for this)
        OutOfStationRequest request = new OutOfStationRequest(startDate, endDate, reason, comments);

        // Send the request to the server using ApiService
        ApiService.getApiInterface(this, "your_token_here").submitOutOfStationRequest(request).enqueue(new Callback<OutOfStationResponse>() {
            @Override
            public void onResponse(Call<OutOfStationResponse> call, Response<OutOfStationResponse> response) {
                if (response.isSuccessful()) {
                    // Handle successful response
                    Toast.makeText(OutOfStationActivity.this, "Request submitted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // Handle error response
                    Toast.makeText(OutOfStationActivity.this, "Failed to submit request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OutOfStationResponse> call, Throwable t) {
                // Handle failure
                Toast.makeText(OutOfStationActivity.this, "Request submission failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
