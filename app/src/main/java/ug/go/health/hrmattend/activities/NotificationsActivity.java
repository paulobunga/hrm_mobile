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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ug.go.health.hrmattend.R;
import ug.go.health.hrmattend.adapters.NotificationListAdapter;
import ug.go.health.hrmattend.models.NotificationListResponse;
import ug.go.health.hrmattend.services.ApiInterface;
import ug.go.health.hrmattend.services.ApiService;
import ug.go.health.hrmattend.services.SessionService;

public class NotificationsActivity extends AppCompatActivity {

    ApiInterface apiService;
    SessionService session;
    private RecyclerView mRecyclerView;
    private NotificationListAdapter mAdapter;
    private List<NotificationListResponse.Notification> mNotificationsList;

    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_notifications);
        Toolbar toolbar = findViewById(R.id.notifications_toolbar);
        toolbar.setTitle("Notifications");

        // Add back button to the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Make Back button go back to the previous activity
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );

        mRecyclerView = findViewById(R.id.rv_notification_list);
        mNotificationsList = new ArrayList<>();
        mAdapter = new NotificationListAdapter(mNotificationsList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyView = getLayoutInflater().inflate(R.layout.layout_empty_view, null);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.clock_history_toolbar);

        ((RelativeLayout) findViewById(R.id.fragment_notifications_container)).addView(emptyView, layoutParams);

        session = new SessionService(this);

        String token = session.getToken();
        apiService = ApiService.getApiInterface(this, token);

        mRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);

        apiService.getNotificationList().enqueue(new Callback<NotificationListResponse>() {
            @Override
            public void onResponse(Call<NotificationListResponse> call, Response<NotificationListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mNotificationsList.addAll(response.body().getNotifications());
                    mAdapter.notifyDataSetChanged();

                    if (mNotificationsList.isEmpty()) {
                        mRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        mRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<NotificationListResponse> call, Throwable t) {

            }
        });
    }
}