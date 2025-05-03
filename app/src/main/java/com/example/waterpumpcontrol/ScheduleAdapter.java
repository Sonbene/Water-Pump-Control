// ScheduleAdapter.java
package com.example.waterpumpcontrol;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter
        extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnEdit { void onEdit(int pos); }
    public interface OnDelete { void onDelete(int pos); }
    public interface OnToggle { void onToggle(int pos, boolean isActive); }

    private final List<Schedule> data;
    private final OnEdit editCb;
    private final OnDelete deleteCb;
    private final OnToggle toggleCb;

    public ScheduleAdapter(List<Schedule> data,
                           OnEdit editCb,
                           OnDelete deleteCb,
                           OnToggle toggleCb) {
        this.data = data;
        this.editCb = editCb;
        this.deleteCb = deleteCb;
        this.toggleCb = toggleCb;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Schedule s = data.get(pos);
        // 1) Lấy time
        String time = s.getStartTime();

        // 2) Build phần detail tùy recurrence
        String detail;
        switch (s.getRecurrence()) {
            case "Một lần":
                detail = time + "\n" +s.getDate();
                break;
            case "Hàng ngày":
                detail = time;
                break;
            case "Hàng tuần":
                // map List<Integer> daysOfWeek -> ["T2","T4",...]
                List<String> names = new ArrayList<>();
                for (int d : s.getDaysOfWeek()) {
                    switch(d) {
                        case 1: names.add("T2"); break;
                        case 2: names.add("T3"); break;
                        case 3: names.add("T4"); break;
                        case 4: names.add("T5"); break;
                        case 5: names.add("T6"); break;
                        case 6: names.add("T7"); break;
                        case 7: names.add("CN"); break;
                    }
                }
                detail = time + "\n" + TextUtils.join(", ", names);
                break;
            case "Hàng tháng":
                // List<Integer> daysOfMonth -> "1, 5, 15"
                List<Integer> dom = s.getDaysOfMonth();
                String days = dom.isEmpty()
                        ? "-"
                        : TextUtils.join(", ", dom);
                detail = time + " \n" + days;
                break;
            default:
                detail = time;
        }

        // 3) Set text
        h.tvSummary.setText(
                String.format("%s • %s %s",
                        s.getPumpMode(),
                        s.getRecurrence(),
                        detail
                )
        );

        h.switchActive.setChecked(s.isActive());
        h.switchActive.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) ->
                        toggleCb.onToggle(pos, isChecked)
        );
        h.btnEdit.setOnClickListener(v -> editCb.onEdit(pos));
        h.btnDelete.setOnClickListener(v -> deleteCb.onDelete(pos));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSummary;
        Switch  switchActive;
        ImageButton btnEdit, btnDelete;
        ViewHolder(View itemView) {
            super(itemView);
            tvSummary    = itemView.findViewById(R.id.tvScheduleSummary);
            switchActive = itemView.findViewById(R.id.switchActive);
            btnEdit      = itemView.findViewById(R.id.btnEdit);
            btnDelete    = itemView.findViewById(R.id.btnDelete);
        }
    }
}
