package ug.go.health.ihrisbiometric.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import ug.go.health.ihrisbiometric.services.SessionService;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ug.go.health.ihrisbiometric.R;
import ug.go.health.ihrisbiometric.adapters.SyncCategoryAdapter;
import ug.go.health.ihrisbiometric.models.ClockHistory;
import ug.go.health.ihrisbiometric.models.StaffRecord;
import ug.go.health.ihrisbiometric.utils.NonScrollableExpandableListView;
import ug.go.health.ihrisbiometric.viewmodels.DataSyncViewModel;

public class DataSyncFragment extends Fragment {

    private DataSyncViewModel viewModel;
    private TextView tvStaffSyncedCount, tvStaffUnsyncedCount, tvClockSyncedCount, tvClockUnsyncedCount, tvSyncMessage;
    private MaterialButton btnSync;
    private ProgressBar progressBar;
    private NonScrollableExpandableListView expandableListView;
    private SyncCategoryAdapter syncCategoryAdapter;
    private List<String> categories;
    private Map<String, List<?>> categoryItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupToolbar();
        setupViewModel();
        initializeExpandableListView();
        observeViewModel();
        setupSyncButton();
    }

    private void initializeViews(View view) {
        tvStaffSyncedCount = view.findViewById(R.id.tvStaffSyncedCount);
        tvStaffUnsyncedCount = view.findViewById(R.id.tvStaffUnsyncedCount);
        tvClockSyncedCount = view.findViewById(R.id.tvClockSyncedCount);
        tvClockUnsyncedCount = view.findViewById(R.id.tvClockUnsyncedCount);
        tvSyncMessage = view.findViewById(R.id.tvSyncMessage);
        btnSync = view.findViewById(R.id.btnSync);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        MaterialToolbar topAppBar = requireView().findViewById(R.id.topAppBar);
        // Set the navigation icon (back button) for the toolbar
        topAppBar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
        topAppBar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupViewModel() {
        SessionService sessionService = new SessionService(requireContext());
        String token = sessionService.getToken();
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(DataSyncViewModel.class)) {
                    return (T) new DataSyncViewModel(requireActivity().getApplication());
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        }).get(DataSyncViewModel.class);
    }

    private void initializeExpandableListView() {
        categories = new ArrayList<>();
        categories.add("Staff Records Ready for Sync");
        categories.add("Staff Records Missing Info");
        categories.add("Clock History Ready for Sync");

        categoryItems = new HashMap<>();
        categoryItems.put(categories.get(0), new ArrayList<StaffRecord>());
        categoryItems.put(categories.get(1), new ArrayList<StaffRecord>());
        categoryItems.put(categories.get(2), new ArrayList<ClockHistory>());

        syncCategoryAdapter = new SyncCategoryAdapter(requireContext(), categories, categoryItems);
//        expandableListView.setAdapter(syncCategoryAdapter);
    }

    private void observeViewModel() {
        viewModel.getSyncStatus().observe(getViewLifecycleOwner(), this::updateSyncStatus);
        viewModel.getSyncProgress().observe(getViewLifecycleOwner(), this::updateProgressBar);
        viewModel.getSyncMessage().observe(getViewLifecycleOwner(), this::updateSyncMessage);
        viewModel.getSyncedStaffCount().observe(getViewLifecycleOwner(), count -> updateCountView(tvStaffSyncedCount, "Staff Synced", count));
        viewModel.getUnsyncedStaffCount().observe(getViewLifecycleOwner(), count -> updateCountView(tvStaffUnsyncedCount, "Staff Unsynced", count));
        viewModel.getSyncedClockCount().observe(getViewLifecycleOwner(), count -> updateCountView(tvClockSyncedCount, "Clock Records Synced", count));
        viewModel.getUnsyncedClockCount().observe(getViewLifecycleOwner(), count -> updateCountView(tvClockUnsyncedCount, "Clock Records Unsynced", count));

        // Observe lists for expandable list view
        viewModel.getStaffRecordsReadyForSync().observe(getViewLifecycleOwner(), this::updateStaffRecordsReadyForSync);
        viewModel.getStaffRecordsMissingInfo().observe(getViewLifecycleOwner(), this::updateStaffRecordsMissingInfo);
        viewModel.getClockHistoryReadyForSync().observe(getViewLifecycleOwner(), this::updateClockHistoryReadyForSync);
    }

    private void setupSyncButton() {
        btnSync.setOnClickListener(v -> viewModel.startSync());
    }

    private void updateSyncStatus(DataSyncViewModel.SyncStatus status) {
        switch (status) {
            case IDLE:
                btnSync.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                break;
            case IN_PROGRESS:
                btnSync.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                btnSync.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Sync completed", Toast.LENGTH_SHORT).show();
                break;
            case FAILED:
                btnSync.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Sync failed", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void updateProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    private void updateSyncMessage(String message) {
        tvSyncMessage.setText(message);
    }

    private void updateCountView(TextView textView, String label, int count) {
        textView.setText(String.format("%s: %d", label, count));
    }

    private void updateStaffRecordsReadyForSync(List<StaffRecord> staffRecords) {
        categoryItems.put(categories.get(0), staffRecords);
        syncCategoryAdapter.notifyDataSetChanged();
    }

    private void updateStaffRecordsMissingInfo(List<StaffRecord> staffRecords) {
        categoryItems.put(categories.get(1), staffRecords);
        syncCategoryAdapter.notifyDataSetChanged();
    }

    private void updateClockHistoryReadyForSync(List<ClockHistory> clockHistories) {
        categoryItems.put(categories.get(2), clockHistories);
        syncCategoryAdapter.notifyDataSetChanged();
    }
}
