package ug.go.health.ihrisbiometric.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ug.go.health.ihrisbiometric.R;
import ug.go.health.ihrisbiometric.fragments.DatePickerFragment;
import ug.go.health.ihrisbiometric.models.OutOfStationRequest;
import ug.go.health.ihrisbiometric.models.OutOfStationResponse;
import ug.go.health.ihrisbiometric.services.ApiService;
import ug.go.health.ihrisbiometric.services.SessionService;

public class OutOfStationActivity extends AppCompatActivity {

    private SessionService sessionService;
    EditText requestStartDate;
    EditText requestEndDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_of_station);

        sessionService = new SessionService(this);


        requestStartDate = findViewById(R.id.request_start_date);
        requestEndDate = findViewById(R.id.request_end_date);

        requestStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(requestStartDate);
            }
        });

        requestEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(requestEndDate);
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
        String startDate = requestStartDate.getText().toString();
        String endDate = requestEndDate.getText().toString();

        String reason = ((Spinner) findViewById(R.id.reason)).getSelectedItem().toString();
        String comments = ((EditText) findViewById(R.id.comments)).getText().toString();

        // Create a request object (assuming you have a model class for this)
        OutOfStationRequest request = new OutOfStationRequest(startDate, endDate, reason, comments);

        // Get the token from SessionService
        String token = sessionService.getToken();

        // Send the request to the server using ApiService
        ApiService.getApiInterface(this, token).submitOutOfStationRequest(request).enqueue(new Callback<OutOfStationResponse>() {
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
    private void showDatePickerDialog(final EditText editText) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                String selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
                editText.setText(selectedDate);
            }
        });
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
