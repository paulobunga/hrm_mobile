package ug.go.health.ihrisbiometric.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.util.ArrayList;
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

    private final MutableLiveData<Integer> staffSyncProgressLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> clockSyncProgressLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> syncProgressLiveData = new MutableLiveData<>(0);

    private final MutableLiveData<List<String>> syncMessagesLiveData = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Integer> syncedStaffCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unsyncedStaffCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> syncedClockCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> unsyncedClockCountLiveData = new MutableLiveData<>(0);

    private final MutableLiveData<List<StaffRecord>> staffRecordsReadyForSyncLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<StaffRecord>> staffRecordsMissingInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ClockHistory>> clockHistoryReadyForSyncLiveData = new MutableLiveData<>();

    private final AtomicInteger totalItemsToSync = new AtomicInteger(0);
    private final AtomicInteger syncedItemsCount = new AtomicInteger(0);

    private final AtomicInteger syncedStaffCount = new AtomicInteger(0);
    private final AtomicInteger syncedClockCount = new AtomicInteger(0);

    private SessionService sessionService;

    public DataSyncViewModel(@NonNull Application application) {
        super(application);
        dbService = new DbService(application.getApplicationContext());
        sessionService = new SessionService(application.getApplicationContext());

        // Initialize ApiInterface
        apiService = ApiService.getApiInterface(application.getApplicationContext());

        executorService = Executors.newSingleThreadExecutor();

        updateSyncCounts();
        updateSyncCategories();
    }

    public LiveData<SyncStatus> getSyncStatus() {
        return syncStatusLiveData;
    }

    public LiveData<Integer> getStaffSyncProgress() {
        return staffSyncProgressLiveData;
    }

    public LiveData<Integer> getClockSyncProgress() {
        return clockSyncProgressLiveData;
    }

    public LiveData<List<String>> getSyncMessages() {
        return syncMessagesLiveData;
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
        List<String> messages = syncMessagesLiveData.getValue();
        messages.add("Starting sync...");
        syncMessagesLiveData.postValue(messages);

        executorService.execute(this::performSync);
    }

    private void performSync() {
        try {
            List<String> messages = syncMessagesLiveData.getValue();
            messages.add("Fetching unsynced records...");
            syncMessagesLiveData.postValue(messages);

            dbService.getUnsyncedStaffRecordsAsync(this::handleUnsyncedStaffRecords);
        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            syncStatusLiveData.postValue(SyncStatus.FAILED);
            List<String> messages = syncMessagesLiveData.getValue();
            messages.add("Sync failed: " + e.getMessage());
            syncMessagesLiveData.postValue(messages);
        }
    }

    private void handleUnsyncedStaffRecords(List<StaffRecord> unsyncedStaffRecords) {
        dbService.getUnsyncedClockHistoryAsync(unsyncedClockRecords -> {
            totalItemsToSync.set(unsyncedStaffRecords.size() + unsyncedClockRecords.size());

            if (totalItemsToSync.get() == 0) {
                syncStatusLiveData.postValue(SyncStatus.COMPLETED);
                List<String> messages = syncMessagesLiveData.getValue();
                messages.add("No records to sync");
                syncMessagesLiveData.setValue(messages);
                return;
            }

            syncStaffRecords(unsyncedStaffRecords);
            syncClockRecords(unsyncedClockRecords);
        });
    }

    private void syncStaffRecords(List<StaffRecord> unsyncedStaffRecords) {
        List<String> messages = syncMessagesLiveData.getValue();
        messages.add("Syncing staff records...");
        syncMessagesLiveData.setValue(messages);
        for (StaffRecord staffRecord : unsyncedStaffRecords) {
            apiService.syncStaffRecord(staffRecord).enqueue(new Callback<StaffRecord>() {
                @Override
                public void onResponse(Call<StaffRecord> call, Response<StaffRecord> response) {
                    if (response.isSuccessful()) {
                        staffRecord.setSynced(false);
                        dbService.updateStaffRecordAsync(staffRecord, success -> {
                            if (success) {
                                updateStaffSyncProgress();
                            } else {
                                Log.e(TAG, "Failed to update staff record in local database");
                            }
                        });
                    } else {
                        String errorMessage = "Sync failed for staff record";
                        if (response.errorBody() != null) {
                            try {
                                JSONObject errorObject = new JSONObject(response.errorBody().string());
                                errorMessage = errorObject.getString("message");
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        }
                        Log.e(TAG, errorMessage);
                        syncStatusLiveData.postValue(SyncStatus.FAILED);
                        List<String> messages = syncMessagesLiveData.getValue();
                        messages.add(errorMessage);
                        syncMessagesLiveData.postValue(messages);
                    }
                }

                @Override
                public void onFailure(Call<StaffRecord> call, Throwable t) {
                    Log.e(TAG, "Failed to sync staff record", t);
                    syncStatusLiveData.postValue(SyncStatus.FAILED);
                    List<String> messages = syncMessagesLiveData.getValue();
                    messages.add("Failed to sync staff record: " + t.getMessage());
                    syncMessagesLiveData.postValue(messages);
                }
            });
        }
    }

    private void syncClockRecords(List<ClockHistory> unsyncedClockRecords) {
        List<String> messages = syncMessagesLiveData.getValue();
        messages.add("Syncing clock records...");
        syncMessagesLiveData.setValue(messages);
        for (ClockHistory clockHistory : unsyncedClockRecords) {
            apiService.syncClockHistory(clockHistory).enqueue(new Callback<ClockHistory>() {
                @Override
                public void onResponse(Call<ClockHistory> call, Response<ClockHistory> response) {
                    if (response.isSuccessful()) {
                        clockHistory.setSynced(true);
                        dbService.updateClockHistoryAsync(clockHistory, (result) -> {
                            updateClockSyncProgress();
                        });
                    } else {
                        String errorMessage = "Sync failed for clock history";
                        if (response.errorBody() != null) {
                            try {
                                JSONObject errorObject = new JSONObject(response.errorBody().string());
                                errorMessage = errorObject.getString("message");
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        }
                        Log.e(TAG, errorMessage);
                        syncStatusLiveData.postValue(SyncStatus.FAILED);
                        List<String> messages = syncMessagesLiveData.getValue();
                        messages.add(errorMessage);
                        syncMessagesLiveData.postValue(messages);
                    }
                }

                @Override
                public void onFailure(Call<ClockHistory> call, Throwable t) {
                    Log.e(TAG, "Failed to sync clock history", t);
                    syncStatusLiveData.postValue(SyncStatus.FAILED);
                    List<String> messages = syncMessagesLiveData.getValue();
                    messages.add("Failed to sync clock record: " + t.getMessage());
                    syncMessagesLiveData.postValue(messages);
                }
            });
        }
    }

    private void updateStaffSyncProgress() {
        int progress = (int) ((syncedStaffCount.incrementAndGet() / (float) totalItemsToSync.get()) * 100);
        staffSyncProgressLiveData.postValue(progress);
        syncProgressLiveData.postValue(progress);
        checkSyncCompletion();
    }

    private void updateClockSyncProgress() {
        int progress = (int) ((syncedClockCount.incrementAndGet() / (float) totalItemsToSync.get()) * 100);
        clockSyncProgressLiveData.postValue(progress);
        syncProgressLiveData.postValue(progress);
        checkSyncCompletion();
    }

    private void checkSyncCompletion() {
        if (syncedItemsCount.get() == totalItemsToSync.get()) {
            syncStatusLiveData.postValue(SyncStatus.COMPLETED);
            List<String> messages = syncMessagesLiveData.getValue();
            messages.add("Sync completed successfully");
            syncMessagesLiveData.postValue(messages);
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
