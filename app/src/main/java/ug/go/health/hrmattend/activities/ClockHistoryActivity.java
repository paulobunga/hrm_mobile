package ug.go.health.hrmattend.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ug.go.health.hrmattend.R;
import ug.go.health.hrmattend.adapters.ClockHistoryAdapter;
import ug.go.health.hrmattend.models.ClockHistory;
import ug.go.health.hrmattend.services.DbService;

public class ClockHistoryActivity extends AppCompatActivity {

    DbService dbService;
    private RecyclerView recyclerView;
    private ClockHistoryAdapter adapter;
    private List<ClockHistory> clockHistoryList;

    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_history);

        dbService = new DbService(this);

        Toolbar toolbar = findViewById(R.id.clock_history_toolbar);
        toolbar.setTitle("Clock History");

        // Add back button to the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Make Back button go back to the previous activity
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );

        recyclerView = findViewById(R.id.rv_clock_history_list);
        clockHistoryList = new ArrayList<>();
        adapter = new ClockHistoryAdapter(clockHistoryList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyView = getLayoutInflater().inflate(R.layout.layout_empty_view, null);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.clock_history_toolbar);

        ((RelativeLayout) findViewById(R.id.activity_clock_history)).addView(emptyView, layoutParams);

        loadClockHistory();
    }

    private void loadClockHistory() {
        dbService.getClockHistoryAsync(new DbService.Callback<List<ClockHistory>>() {
            @Override
            public void onResult(List<ClockHistory> result) {
                clockHistoryList.clear();
                clockHistoryList.addAll(result);
                adapter.notifyDataSetChanged();

                updateViewVisibility();
            }
        });
    }

    private void updateViewVisibility() {
        if (clockHistoryList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbService.shutdown();
    }
}