package ug.go.health.hrmattend.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ug.go.health.hrmattend.R;
import ug.go.health.hrmattend.models.ClockHistory;
import ug.go.health.hrmattend.models.Location;
import ug.go.health.hrmattend.models.StaffRecord;
import ug.go.health.hrmattend.services.ApiInterface;
import ug.go.health.hrmattend.services.ApiService;
import ug.go.health.hrmattend.services.DbService;
import ug.go.health.hrmattend.services.SessionService;

public class DataSyncActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "DataSyncActivity";

    private FusedLocationProviderClient fusedLocationClient;

    private DbService dbService;
    private ApiInterface apiService;
    private SessionService sessionService;

    private TextView tvSyncedStaffRecords;
    private TextView tvUnsyncedStaffRecords;
    private TextView tvSyncedClockHistory;
    private TextView tvUnsyncedClockHistory;
    private TextView tvDataSyncStatus;
    private ProgressBar progressBar;
    private Button btnSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_sync);

        Toolbar toolbar = findViewById(R.id.data_sync_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Data Sync");

        dbService = new DbService(this);
        sessionService = new SessionService(this);
        apiService = ApiService.getApiInterface(this, sessionService.getToken());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvSyncedStaffRecords = findViewById(R.id.tv_synced_staff_records);
        tvUnsyncedStaffRecords = findViewById(R.id.tv_unsynced_staff_records);
        tvSyncedClockHistory = findViewById(R.id.tv_synced_clock_history);
        tvUnsyncedClockHistory = findViewById(R.id.tv_unsynced_clock_history);
        tvDataSyncStatus = findViewById(R.id.data_sync_status);
        progressBar = findViewById(R.id.data_sync_progress_bar);
        btnSync = findViewById(R.id.data_sync_btn);

        updateSyncStatus();

        btnSync.setOnClickListener(v -> startSync());
    }

    private void updateSyncStatus() {
        dbService.countSyncedStaffRecordsAsync(syncedStaff -> {
            dbService.countUnsyncedStaffRecordsAsync(unsyncedStaff -> {
                dbService.countSyncedClockRecordsAsync(syncedClock -> {
                    dbService.countUnsyncedClockRecordsAsync(unsyncedClock -> {
                        runOnUiThread(() -> {
                            tvSyncedStaffRecords.setText(String.format("Synced: %d", syncedStaff));
                            tvUnsyncedStaffRecords.setText("Unsynced: " + unsyncedStaff);
                            tvSyncedClockHistory.setText("Synced: " + syncedClock);
                            tvUnsyncedClockHistory.setText("Unsynced: " + unsyncedClock);
                        });
                    });
                });
            });
        });
    }

    private void startSync() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndSync();
        }
    }

    private void getLocationAndSync() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        syncData(location);
                    } else {
                        syncData(null);
                    }
                });
    }

    private void syncData(android.location.Location deviceLocation) {
        progressBar.setVisibility(View.VISIBLE);
        btnSync.setEnabled(false);
        tvDataSyncStatus.setText("Syncing data...");

        dbService.getUnsyncedStaffRecordsAsync(unsyncedStaffRecords -> {
            dbService.getUnsyncedClockHistoryAsync(unsyncedClockHistory -> {
                syncStaffRecords(unsyncedStaffRecords, deviceLocation);
                syncClockHistory(unsyncedClockHistory, deviceLocation);
            });
        });
    }

    private void syncStaffRecords(List<StaffRecord> unsyncedStaffRecords, android.location.Location deviceLocation) {
        for (StaffRecord staffRecord : unsyncedStaffRecords) {
            if (deviceLocation != null) {
                Location location = new Location();
                location.setLatitude(deviceLocation.getLatitude());
                location.setLongitude(deviceLocation.getLongitude());
                staffRecord.setLocation(location);
            }

            apiService.syncStaffRecord(staffRecord).enqueue(new Callback<StaffRecord>() {
                @Override
                public void onResponse(Call<StaffRecord> call, Response<StaffRecord> response) {
                    if (response.isSuccessful()) {
                        staffRecord.setSynced(true);
                        dbService.updateStaffRecordAsync(staffRecord, success -> {
                            if (success) {
                                updateSyncStatus();
                            } else {
                                Log.e(TAG, "Failed to update staff record sync status");
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<StaffRecord> call, Throwable t) {
                    Log.e(TAG, "Failed to sync staff record", t);
                }
            });
        }
    }

    private void syncClockHistory(List<ClockHistory> unsyncedClockHistory, android.location.Location deviceLocation) {
        for (ClockHistory clockHistory : unsyncedClockHistory) {
            if (deviceLocation != null) {
                Location location = new Location();
                location.setLatitude(deviceLocation.getLatitude());
                location.setLongitude(deviceLocation.getLongitude());
                clockHistory.setLocation(location);
            }

            apiService.syncClockHistory(clockHistory).enqueue(new Callback<ClockHistory>() {
                @Override
                public void onResponse(Call<ClockHistory> call, Response<ClockHistory> response) {
                    if (response.isSuccessful()) {
                        clockHistory.setSynced(true);
                        dbService.updateClockHistoryAsync(clockHistory, new DbService.Callback<Void>() {
                            @Override
                            public void onResult(Void result) {
                                updateSyncStatus();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<ClockHistory> call, Throwable t) {
                    Log.e(TAG, "Failed to sync clock history", t);
                }
            });
        }

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnSync.setEnabled(true);
            tvDataSyncStatus.setText("Sync completed");
            Toast.makeText(this, "Data sync completed", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSync();
            } else {
                Toast.makeText(this, "Location permission denied. Syncing without location.", Toast.LENGTH_SHORT).show();
                syncData(null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbService.shutdown();
    }
}