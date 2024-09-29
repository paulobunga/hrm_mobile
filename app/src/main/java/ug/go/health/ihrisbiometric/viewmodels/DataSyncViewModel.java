package ug.go.health.ihrisbiometric.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ug.go.health.ihrisbiometric.models.ClockHistory;
import ug.go.health.ihrisbiometric.models.StaffRecord;
import ug.go.health.ihrisbiometric.services.ApiInterface;
import ug.go.health.ihrisbiometric.services.ApiService;
import ug.go.health.ihrisbiometric.services.DbService;
import ug.go.health.ihrisbiometric.services.SessionService;

public class DataSyncViewModel extends AndroidViewModel {
    private static final String TAG = "DataSyncViewModel";

    private final DbService dbService;
    private final ApiInterface apiService;
    private final ExecutorService executorService;

    private final MutableLiveData<SyncStatus> syncStatusLiveData = new MutableLiveData<>(SyncStatus.IDLE);
    private final MutableLiveData<Integer> syncProgressLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> syncMessageLiveData = new MutableLiveData<>();

    private final MutableLiveData<Integer> syncedStaffCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unsyncedStaffCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> syncedClockCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unsyncedClockCountLiveData = new MutableLiveData<>(0);

    private final MutableLiveData<List<StaffRecord>> staffRecordsReadyForSyncLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StaffRecord>> staffRecordsMissingInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ClockHistory>> clockHistoryReadyForSyncLiveData = new MutableLiveData<>();

    private final AtomicInteger totalItemsToSync = new AtomicInteger(0);
    private final AtomicInteger syncedItemsCount = new AtomicInteger(0);

    private SessionService sessionService;

    private final String token;

    public DataSyncViewModel(@NonNull Application application, String token) {
        super(application);
        this.token = token;
        dbService = new DbService(application.getApplicationContext());
        sessionService = new SessionService(application.getApplicationContext());

        Log.d(TAG, "DataSyncViewModel: Token Received " + token);

        // Initialize ApiInterface
        apiService = ApiService.getApiInterface(application.getApplicationContext(), token);

        // Set the token
        ApiService.setToken(token);

        executorService = Executors.newSingleThreadExecutor();

        updateSyncCounts();
        updateSyncCategories();
    }

    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatusLiveData;
    }

    public LiveData<Integer> getSyncProgress() {
        return syncProgressLiveData;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessageLiveData;
    }

    public LiveData<Integer> getSyncedStaffCount() {
        return syncedStaffCountLiveData;
    }

    public LiveData<Integer> getUnsyncedStaffCount() {
        return unsyncedStaffCountLiveData;
    }

    public LiveData<Integer> getSyncedClockCount() {
        return syncedClockCountLiveData;
    }

    public LiveData<Integer> getUnsyncedClockCount() {
        return unsyncedClockCountLiveData;
    }

    public LiveData<List<StaffRecord>> getStaffRecordsReadyForSync() {
        return staffRecordsReadyForSyncLiveData;
    }

    public LiveData<List<StaffRecord>> getStaffRecordsMissingInfo() {
        return staffRecordsMissingInfoLiveData;
    }

    public LiveData<List<ClockHistory>> getClockHistoryReadyForSync() {
        return clockHistoryReadyForSyncLiveData;
    }

    public void startSync() {
        if (syncStatusLiveData.getValue() == SyncStatus.IN_PROGRESS) {
            return; // Prevent multiple syncs
        }

        syncStatusLiveData.setValue(SyncStatus.IN_PROGRESS);
        syncedItemsCount.set(0);
        syncProgressLiveData.setValue(0);
        syncMessageLiveData.setValue("Starting sync...");

        executorService.execute(this::performSync);
    }

    private void performSync() {
        try {
            syncMessageLiveData.postValue("Fetching unsynced records...");

            dbService.getUnsyncedStaffRecordsAsync(this::handleUnsyncedStaffRecords);
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            syncStatusLiveData.postValue(SyncStatus.FAILED);
            syncMessageLiveData.postValue("Sync failed: " + e.getMessage());
        }
    }

    private void handleUnsyncedStaffRecords(List<StaffRecord> unsyncedStaffRecords) {
        dbService.getUnsyncedClockHistoryAsync(unsyncedClockRecords -> {
            totalItemsToSync.set(unsyncedStaffRecords.size() + unsyncedClockRecords.size());

            if (totalItemsToSync.get() == 0) {
                syncStatusLiveData.postValue(SyncStatus.COMPLETED);
                syncMessageLiveData.postValue("No records to sync");
                return;
            }

            syncStaffRecords(unsyncedStaffRecords);
            syncClockRecords(unsyncedClockRecords);
        });
    }

    private void syncStaffRecords(List<StaffRecord> unsyncedStaffRecords) {

        syncMessageLiveData.postValue("Syncing staff records...");
        for (StaffRecord staffRecord : unsyncedStaffRecords) {
            if (staffRecord.isFaceEnrolled() && staffRecord.isFingerprintEnrolled()) {
                apiService.syncStaffRecord(staffRecord).enqueue(new Callback<StaffRecord>() {
                    @Override
                    public void onResponse(Call<StaffRecord> call, Response<StaffRecord> response) {
                        if (response.isSuccessful()) {
                            staffRecord.setSynced(true);
                            dbService.updateStaffRecordAsync(staffRecord, success -> {
                                if (success) {
                                    updateSyncProgress();
                                } else {
                                    Log.e(TAG, "Failed to update staff record in local database");
                                }
                            });
                        } else {
                            Log.e(TAG, "Sync failed for staff record: " + response.message());
                            syncStatusLiveData.postValue(SyncStatus.FAILED);
                            syncMessageLiveData.postValue("Failed to sync staff record: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<StaffRecord> call, Throwable t) {
                        Log.e(TAG, "Failed to sync staff record", t);
                        syncStatusLiveData.postValue(SyncStatus.FAILED);
                        syncMessageLiveData.postValue("Failed to sync staff record: " + t.getMessage());
                    }
                });
            } else {
                Log.w(TAG, "Skipping staff record sync due to incomplete enrollment: " + staffRecord.getIhrisPid());
                updateSyncProgress(); // Still update progress even for skipped records
            }
        }
    }

    private void syncClockRecords(List<ClockHistory> unsyncedClockRecords) {
        syncMessageLiveData.postValue("Syncing clock records...");
        for (ClockHistory clockHistory : unsyncedClockRecords) {
            apiService.syncClockHistory(clockHistory).enqueue(new Callback<ClockHistory>() {
                @Override
                public void onResponse(Call<ClockHistory> call, Response<ClockHistory> response) {
                    if (response.isSuccessful()) {
                        clockHistory.setSynced(true);
                        dbService.updateClockHistoryAsync(clockHistory, (result) -> {
                            updateSyncProgress();
                        });
                    } else {
                        Log.e(TAG, "Sync failed for clock history: " + response.message());
                        syncStatusLiveData.postValue(SyncStatus.FAILED);
                        syncMessageLiveData.postValue("Failed to sync clock record: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ClockHistory> call, Throwable t) {
                    Log.e(TAG, "Failed to sync clock history", t);
                    syncStatusLiveData.postValue(SyncStatus.FAILED);
                    syncMessageLiveData.postValue("Failed to sync clock record: " + t.getMessage());
                }
            });
        }
    }

    private void updateSyncProgress() {
        int progress = (int) ((syncedItemsCount.incrementAndGet() / (float) totalItemsToSync.get()) * 100);
        syncProgressLiveData.postValue(progress);
        checkSyncCompletion();
    }

    private void checkSyncCompletion() {
        if (syncedItemsCount.get() == totalItemsToSync.get()) {
            syncStatusLiveData.postValue(SyncStatus.COMPLETED);
            syncMessageLiveData.postValue("Sync completed successfully");
            updateSyncCounts();
            updateSyncCategories();
        }
    }

    public void updateSyncCounts() {
        dbService.countSyncedStaffRecordsAsync(syncedStaffCountLiveData::postValue);
        dbService.countUnsyncedStaffRecordsAsync(unsyncedStaffCountLiveData::postValue);
        dbService.countSyncedClockRecordsAsync(syncedClockCountLiveData::postValue);
        dbService.countUnsyncedClockRecordsAsync(unsyncedClockCountLiveData::postValue);
    }

    public void updateSyncCategories() {
        dbService.getStaffRecordsReadyForSyncAsync(staffRecordsReadyForSyncLiveData::postValue);
        dbService.getStaffRecordsMissingInfoAsync(staffRecordsMissingInfoLiveData::postValue);
        dbService.getUnsyncedClockHistoryAsync(clockHistoryReadyForSyncLiveData::postValue);
    }

    public enum SyncStatus {
        IDLE, IN_PROGRESS, COMPLETED, FAILED
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
