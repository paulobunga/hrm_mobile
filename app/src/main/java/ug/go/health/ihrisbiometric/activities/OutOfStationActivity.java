package ug.go.health.ihrisbiometric.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ug.go.health.ihrisbiometric.R;
import ug.go.health.ihrisbiometric.fragments.DatePickerFragment;
import ug.go.health.ihrisbiometric.models.OutOfStationResponse;
import ug.go.health.ihrisbiometric.services.ApiService;
import ug.go.health.ihrisbiometric.services.SessionService;

public class OutOfStationActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private SessionService sessionService;
    private EditText requestStartDate;
    private EditText requestEndDate;
    private TextView selectedFileNameTextView;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_of_station);

        sessionService = new SessionService(this);

        requestStartDate = findViewById(R.id.request_start_date);
        requestEndDate = findViewById(R.id.request_end_date);
        selectedFileNameTextView = findViewById(R.id.selected_file_name);

        requestStartDate.setOnClickListener(v -> showDatePickerDialog(requestStartDate));
        requestEndDate.setOnClickListener(v -> showDatePickerDialog(requestEndDate));

        Button submitRequest = findViewById(R.id.submit_request);
        submitRequest.setOnClickListener(v -> submitOutOfStationRequest());

        Button selectFileButton = findViewById(R.id.select_file_button);
        selectFileButton.setOnClickListener(v -> openFilePicker());
    }

    private void submitOutOfStationRequest() {
        String startDate = requestStartDate.getText().toString();
        String endDate = requestEndDate.getText().toString();
        String reason = ((Spinner) findViewById(R.id.reason)).getSelectedItem().toString();
        String comments = ((EditText) findViewById(R.id.comments)).getText().toString();

        String token = sessionService.getToken();

        // Create request body
        RequestBody startDateBody = RequestBody.create(MediaType.parse("text/plain"), startDate);
        RequestBody endDateBody = RequestBody.create(MediaType.parse("text/plain"), endDate);
        RequestBody reasonBody = RequestBody.create(MediaType.parse("text/plain"), reason);
        RequestBody commentsBody = RequestBody.create(MediaType.parse("text/plain"), comments);

        // Handle file upload
        MultipartBody.Part filePart = null;
        if (selectedFileUri != null) {
            File file = new File(getCacheDir(), getFileName(selectedFileUri));
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                OutputStream outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedFileUri)), file);
            filePart = MultipartBody.Part.createFormData("attachment", file.getName(), requestFile);
        }

        ApiService.getApiInterface(this, token).submitOutOfStationRequest(
                startDateBody, endDateBody, reasonBody, commentsBody, filePart
        ).enqueue(new Callback<OutOfStationResponse>() {
            @Override
            public void onResponse(Call<OutOfStationResponse> call, Response<OutOfStationResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OutOfStationActivity.this, "Request submitted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OutOfStationActivity.this, "Failed to submit request", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OutOfStationResponse> call, Throwable t) {
                Toast.makeText(OutOfStationActivity.this, "Request submission failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerDialog(final EditText editText) {
        DatePickerFragment newFragment = new DatePickerFragment();
        newFragment.setDateSetListener((view, year, month, dayOfMonth) -> {
            String selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            editText.setText(selectedDate);
        });
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                String fileName = getFileName(selectedFileUri);
                selectedFileNameTextView.setText("Selected file: " + fileName);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}