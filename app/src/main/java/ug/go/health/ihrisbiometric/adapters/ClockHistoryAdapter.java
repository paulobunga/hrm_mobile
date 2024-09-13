package ug.go.health.ihrisbiometric.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

import ug.go.health.ihrisbiometric.R;
import ug.go.health.ihrisbiometric.models.ClockHistory;

public class ClockHistoryAdapter extends RecyclerView.Adapter<ClockHistoryAdapter.ClockHistoryListViewHolder> {
    private List<ClockHistory> mClockHistoryList;
    public ClockHistoryAdapter(List<ClockHistory> clockHistoryList) {
        mClockHistoryList = clockHistoryList;
    }

    @Override
    public ClockHistoryAdapter.ClockHistoryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.clock_history_list_item, parent, false);
        return new ClockHistoryAdapter.ClockHistoryListViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ClockHistoryAdapter.ClockHistoryListViewHolder holder, int position) {
        ClockHistory clockHistory = mClockHistoryList.get(position);
        Log.d("YISSH", clockHistory.toString());
        String name = clockHistory.getName();
        String clockStatus = clockHistory.getClockStatus();
        Date clockTime = clockHistory.getClockTime();
        holder.tvName.setText(name);
        holder.tvClockStatus.setText(clockStatus);
        holder.tvClockTime.setText(clockTime.toString());

    }

    @Override
    public int getItemCount() {
        return mClockHistoryList.size();
    }

    public class ClockHistoryListViewHolder extends RecyclerView.ViewHolder {

        public TextView tvName;
        public TextView tvClockTime;
        public TextView tvClockStatus;

        public ClockHistoryListViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_clock_history_name);
            tvClockTime = itemView.findViewById(R.id.tv_clock_history_time);
            tvClockStatus = itemView.findViewById(R.id.tv_clock_history_status);
        }
    }
}
