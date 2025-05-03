package com.example.waterpumpcontrol;

// import cần thiết
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter hiển thị danh sách cảnh báo (Strings) trong RecyclerView.
 */
public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    private final List<String> alerts;

    public AlertsAdapter(List<String> alerts) {
        this.alerts = alerts;
    }

    @Override
    public AlertViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AlertViewHolder holder, int position) {
        String text = alerts.get(position);
        holder.tvAlert.setText(text);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    /**
     * Thêm 1 alert mới lên đầu danh sách, giữ tối đa 10 alert
     */
    public void addAlert(String alertText) {
        // thêm lên đầu
        alerts.add(0, alertText);
        notifyItemInserted(0);
        // nếu vượt 10, xoá phần tử cuối
        if (alerts.size() > 10) {
            int last = alerts.size() - 1;
            alerts.remove(last);
            notifyItemRemoved(last);
        }
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlert;
        public AlertViewHolder(View itemView) {
            super(itemView);
            tvAlert = itemView.findViewById(android.R.id.text1);
        }
    }

    public void clearAlerts() {
        int size = alerts.size();
        if (size > 0) {
            alerts.clear();
            notifyItemRangeRemoved(0, size);
        }
    }
}
