package com.example.waterpumpcontrol;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final String DB_URL      = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER     = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private WaterPumpManager waterPumpManager;
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

        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(ReportsActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);
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
        dbExecutor.execute(() -> {
            try {
                // Kết nối đến cơ sở dữ liệu MariaDB
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                    // Format the start and end date into 'YYYY-MM-DD'
                    String startDateStr = String.format("%04d-%02d-%02d", startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH) + 1, startDate.get(Calendar.DAY_OF_MONTH));
                    String endDateStr = String.format("%04d-%02d-%02d", endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH) + 1, endDate.get(Calendar.DAY_OF_MONTH));

                    // Log the formatted dates
                    Log.d("DB_QUERY", "Formatted Start Date: " + startDateStr + ", End Date: " + endDateStr);

                    // Truy vấn tổng lưu lượng bơm và tổng giờ bơm từ bảng waterpump_daily_reports
                    String query = "SELECT * FROM waterpump_daily_reports WHERE report_date >= ? AND report_date <= ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setDate(1, java.sql.Date.valueOf(startDateStr));
                    ps.setDate(2, java.sql.Date.valueOf(endDateStr));

                    Log.d("DB_QUERY", "Executing Query: " + query);
                    Log.d("DB_QUERY", "Start Date: " + startDateStr + ", End Date: " + endDateStr); // Log ngày bắt đầu và kết thúc

                    ResultSet rs = ps.executeQuery();

                    // Các mảng lưu trữ dữ liệu cho đồ thị
                    List<Entry> volumeEntries = new ArrayList<>();
                    List<Entry> pumpTimeEntries = new ArrayList<>();
                    List<Entry> alertEntries = new ArrayList<>();

                    int dayCount = 1;

                    // Duyệt qua các dòng dữ liệu từ bảng waterpump_daily_reports
                    while (rs.next()) {
                        float totalVolume = rs.getFloat("total_volume");
                        float totalPumpTime = rs.getFloat("total_pump_time");

                        Log.d("DB_DATA", "Total Volume: " + totalVolume + ", Total Pump Time: " + totalPumpTime); // Log dữ liệu lấy từ bảng waterpump_daily_reports

                        // Truy vấn số báo cáo cho ngày này từ bảng waterpumpcontrolalerts
                        String alertQuery = "SELECT COUNT(*) AS alert_count FROM waterpumpcontrolalerts WHERE DATE(timestamp) = ?";
                        PreparedStatement alertPs = conn.prepareStatement(alertQuery);
                        alertPs.setDate(1, rs.getDate("report_date"));
                        ResultSet alertRs = alertPs.executeQuery();
                        alertRs.next();
                        int totalAlerts = alertRs.getInt("alert_count");

                        Log.d("DB_ALERTS", "Total Alerts for Date: " + rs.getDate("report_date") + " = " + totalAlerts); // Log dữ liệu báo cáo cảnh báo

                        // Thêm dữ liệu vào đồ thị (lưu lượng, giờ bơm, cảnh báo)
                        volumeEntries.add(new Entry(dayCount, totalVolume));
                        pumpTimeEntries.add(new Entry(dayCount, totalPumpTime));
                        alertEntries.add(new Entry(dayCount, totalAlerts));

                        dayCount++;
                    }

                    // Cập nhật dữ liệu vào đồ thị
                    runOnUiThread(() -> {
                        // Gọi hàm updateChartData() để cập nhật đồ thị
                        updateChartData(volumeEntries, pumpTimeEntries, alertEntries);

                        // Cập nhật thông tin báo cáo
                        Log.d("REPORT_DATA", "Total Volume: " + getTotalFromEntries(volumeEntries)); // Log tổng lưu lượng
                        Log.d("REPORT_DATA", "Total Alerts: " + getTotalFromEntries(alertEntries)); // Log tổng cảnh báo
                        Log.d("REPORT_DATA", "Total Pump Time: " + getTotalFromEntries(pumpTimeEntries)); // Log tổng giờ bơm

                        tvTotalVolume.setText(String.format("%.2f", getTotalFromEntries(volumeEntries)));
                        tvTotalAlerts.setText(String.format("%.0f", getTotalFromEntries(alertEntries)));  // Chỉnh sửa ở đây
                        tvTotalPumpTime.setText(String.format("%.2f", getTotalFromEntries(pumpTimeEntries)));

                    });

                } catch (Exception e) {
                    Log.e("DB_ERROR", "Error loading report data", e);
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "Error loading report data", e);
            }
        });
    }



    private void updateChartData(List<Entry> volumeEntries, List<Entry> pumpTimeEntries, List<Entry> alertEntries) {
        // Thiết lập dữ liệu cho đồ thị LineChart
        LineDataSet volumeDataSet = new LineDataSet(volumeEntries, "Lưu lượng bơm");
        LineDataSet pumpTimeDataSet = new LineDataSet(pumpTimeEntries, "Thời gian bơm");
        LineDataSet alertDataSet = new LineDataSet(alertEntries, "Cảnh báo");

        volumeDataSet.setColor(Color.RED);
        volumeDataSet.setValueTextColor(Color.RED);
        pumpTimeDataSet.setColor(Color.GREEN);
        pumpTimeDataSet.setValueTextColor(Color.GREEN);
        alertDataSet.setColor(Color.BLUE);
        alertDataSet.setValueTextColor(Color.BLUE);

        LineData lineData = new LineData(volumeDataSet, pumpTimeDataSet, alertDataSet);
        chartReportData.setData(lineData);
        chartReportData.invalidate();  // Cập nhật đồ thị
    }


    private float getTotalFromEntries(List<Entry> entries) {
        float total = 0;
        for (Entry entry : entries) {
            total += entry.getY();
        }
        return total;
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
        } else if (item.getItemId() == R.id.nav_logout) {
            intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_help) {
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
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
