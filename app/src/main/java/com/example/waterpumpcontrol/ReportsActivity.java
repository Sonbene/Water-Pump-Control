package com.example.waterpumpcontrol;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private TextView tvTotalVolume, tvTotalAlerts, tvTotalPumpTime;
    private Button btnStartDate, btnEndDate, btnGenerateReport;
    private LineChart chartReportData;
    private RecyclerView rvReports;
    private ReportsAdapter adapter;
    private final List<String> reportList = new ArrayList<>();
    private final Calendar startDate = Calendar.getInstance();
    private final Calendar endDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_layout);

        // Toolbar & Drawer toggle
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // NavigationView listener
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        // Find views
        tvTotalVolume     = findViewById(R.id.tvTotalVolume);
        tvTotalAlerts     = findViewById(R.id.tvTotalAlerts);
        tvTotalPumpTime   = findViewById(R.id.tvTotalPumpTime);
        btnStartDate      = findViewById(R.id.btnStartDate);
        btnEndDate        = findViewById(R.id.btnEndDate);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        chartReportData   = findViewById(R.id.chartReportData);
        rvReports         = findViewById(R.id.rvReports);

        // Setup RecyclerView
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter(reportList);
        rvReports.setAdapter(adapter);

        // Date pickers
        btnStartDate.setOnClickListener(v ->
                showDatePicker(startDate, date -> btnStartDate.setText(date))
        );
        btnEndDate.setOnClickListener(v ->
                showDatePicker(endDate, date -> btnEndDate.setText(date))
        );

        // Generate report button
        btnGenerateReport.setOnClickListener(v -> generateReport());
    }

    private void showDatePicker(Calendar cal, OnDateSelectedListener listener) {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    String dateStr = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    listener.onDateSelected(dateStr);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void generateReport() {
        // TODO: Thay bằng truy vấn dữ liệu thực tế
        tvTotalVolume.setText("123.4");
        tvTotalAlerts.setText("5");
        tvTotalPumpTime.setText("12.5");

        // Tạo dữ liệu line chart: các Entry với x là thứ tự điểm, y là giá trị lưu lượng
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(1f, 10f));
        entries.add(new Entry(2f, 8f));
        entries.add(new Entry(3f, 12f));
        // Thiết lập dataset và lineData
        LineDataSet dataSet = new LineDataSet(entries, "Lưu lượng");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // nối các điểm
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        chartReportData.setData(lineData);
        chartReportData.invalidate();

        // Update report history list
        reportList.clear();
        reportList.add("Báo cáo: " + btnStartDate.getText() + " - " + btnEndDate.getText());
        reportList.add("Xuất PDF: 04/05/2025");
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawer.closeDrawers();
        Intent intent = null;
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) {
            intent = new Intent(this, DashBoardActivity.class);
        } else if (id == R.id.nav_control) {
            intent = new Intent(this, controlActivity.class);
        } else if (id == R.id.nav_schedule) {
            intent = new Intent(this, ScheduleActivity.class);
        } else if (id == R.id.nav_alerts) {
            intent = new Intent(this, AlertsActivity.class);
        } else if (id == R.id.nav_reports) {
            return true;
        }  else if (item.getItemId() == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            // handle logout
        }
        if (intent != null) {
            startActivity(intent);
            finish();
        }
        return true;
    }

    private interface OnDateSelectedListener {
        void onDateSelected(String date);
    }

    private static class ReportsAdapter
            extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {
        private final List<String> items;
        ReportsAdapter(List<String> items) { this.items = items; }
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.text1.setText(items.get(position));
        }
        @Override public int getItemCount() { return items.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
