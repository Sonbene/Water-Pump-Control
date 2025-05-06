package com.example.waterpumpcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertsActivity extends AppCompatActivity {

    private WaterPumpManager waterPumpManager;
    private DrawerLayout drawer;
    private RecyclerView rvAlerts;
    private SwipeRefreshLayout swipeRefresh;
    private Button btnAckAll;
    private TextView tvActiveCount;
    private AlertsAdapter adapter;

    private static final String DB_URL      = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER     = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.warning_layout);

        // Toolbar + Drawer toggle
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

        // NavigationView
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this::onNavItemSelected);

        // Views
        rvAlerts = findViewById(R.id.rvAlerts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        btnAckAll = findViewById(R.id.btnAcknowledgeAll);
        tvActiveCount = findViewById(R.id.tvActiveCount);

        // RecyclerView setup
        adapter = new AlertsAdapter(new ArrayList<>());
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> {
            loadAlerts();
            swipeRefresh.setRefreshing(false);
        });

        // Acknowledge all
        btnAckAll.setOnClickListener(v -> {
            acknowledgeAllAlerts();
            adapter.clearAlerts();
            updateActiveCount();
        });

        // Load initial
        loadAlerts();

        // Set up chip filter listener
        ChipGroup chipSeverityFilter = findViewById(R.id.chipSeverityFilter);
        chipSeverityFilter.setOnCheckedChangeListener((group, checkedId) -> loadAlerts());

        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(AlertsActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);
    }

    private void loadAlerts() {
        String selectedSeverity = getSelectedSeverity(); // Lấy mức độ cảnh báo đã chọn
        dbExecutor.execute(() -> {
            try {
                // Kết nối tới cơ sở dữ liệu MariaDB
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     Statement stmt = conn.createStatement()) {

                    // Câu lệnh SQL để lấy các cảnh báo từ cơ sở dữ liệu theo cấp độ
                    String query = "SELECT * FROM waterpumpcontrolalerts WHERE status = 'active'";

                    // Nếu không phải chọn tất cả, thì thêm điều kiện theo severity
                    if (!selectedSeverity.equals("all")) {
                        query += " AND severity = '" + selectedSeverity + "'";
                    }

                    ResultSet rs = stmt.executeQuery(query);

                    List<String> alerts = new ArrayList<>();
                    while (rs.next()) {
                        String alertMessage = rs.getString("timestamp") + " — " + rs.getString("message");
                        alerts.add(alertMessage);
                    }

                    runOnUiThread(() -> {
                        adapter.clearAlerts();
                        for (String alert : alerts) {
                            adapter.addAlert(alert);
                        }
                        updateActiveCount();  // Cập nhật số lượng cảnh báo sau khi load
                    });

                    // Đếm tổng số cảnh báo đang 'active' theo severity
                    String countQuery = "SELECT severity, COUNT(*) AS count FROM waterpumpcontrolalerts WHERE status = 'active' GROUP BY severity";
                    ResultSet countRs = stmt.executeQuery(countQuery);

                    // Tạo mảng để chứa các giá trị đếm
                    final int[] totals = new int[4]; // [0] = totalAll, [1] = totalCritical, [2] = totalWarning, [3] = totalInfo

                    // Duyệt qua kết quả để tính tổng theo từng mức độ
                    while (countRs.next()) {
                        String severity = countRs.getString("severity");
                        int count = countRs.getInt("count");

                        // Đếm theo từng mức độ severity
                        if (severity.equals("critical")) {
                            totals[1] = count;
                        } else if (severity.equals("warning")) {
                            totals[2] = count;
                        } else if (severity.equals("info")) {
                            totals[3] = count;
                        }
                    }

                    // Đếm tổng số tất cả các cảnh báo
                    String totalQuery = "SELECT COUNT(*) AS totalCount FROM waterpumpcontrolalerts WHERE status = 'active'";
                    ResultSet totalRs = stmt.executeQuery(totalQuery);
                    if (totalRs.next()) {
                        totals[0] = totalRs.getInt("totalCount"); // Tất cả cảnh báo
                    }

                    // Cập nhật tổng số cảnh báo vào TextView
                    runOnUiThread(() -> {
                        // Hiển thị số lượng cảnh báo theo mức độ đã chọn
                        String displayText = "";
                        if (selectedSeverity.equals("all")) {
                            displayText = "Tất cả: " + totals[0];
                        } else if (selectedSeverity.equals("critical")) {
                            displayText = "Nguy cấp: " + totals[1];
                        } else if (selectedSeverity.equals("warning")) {
                            displayText = "Cảnh báo: " + totals[2];
                        } else if (selectedSeverity.equals("info")) {
                            displayText = "Thông tin: " + totals[3];
                        }

                        tvActiveCount.setText(displayText);
                    });

                } catch (Exception e) {
                    Log.e("DB_ERROR", "Error loading alerts from DB", e);
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "Error loading alerts", e);
            }
        });
    }



    private String getSelectedSeverity() {
        ChipGroup chipGroup = findViewById(R.id.chipSeverityFilter);
        int selectedChipId = chipGroup.getCheckedChipId();

        if (selectedChipId == R.id.chipAll) {
            return "all";
        } else if (selectedChipId == R.id.chipCritical) {
            return "critical";
        } else if (selectedChipId == R.id.chipWarning) {
            return "warning";
        } else if (selectedChipId == R.id.chipInfo) {
            return "info";
        }

        return "all"; // Default
    }

    private void acknowledgeAllAlerts() {
        dbExecutor.execute(() -> {
            try {
                // Kết nối tới cơ sở dữ liệu MariaDB
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String updateQuery = "UPDATE waterpumpcontrolalerts SET status = 'acknowledged' WHERE status = 'active'";
                    try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "Error acknowledging all alerts", e);
            }
        });
    }

    private void updateActiveCount() {
        int count = adapter.getItemCount();
        tvActiveCount.setText(count + " cảnh báo đang chờ");
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        drawer.closeDrawers();
        if (item.getItemId() == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashBoardActivity.class));
        } else if (item.getItemId() == R.id.nav_schedule) {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_control) {
            Intent intent = new Intent(this, controlActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_reports) {
            Intent intent = new Intent(this, ReportsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_logout) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.nav_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra kết nối MQTT khi ứng dụng quay lại
        waterPumpManager.checkMqttConnection(this);
    }

}
