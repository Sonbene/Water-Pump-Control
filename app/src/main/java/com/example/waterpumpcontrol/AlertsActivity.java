package com.example.waterpumpcontrol;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;

public class AlertsActivity extends AppCompatActivity {
    private DrawerLayout drawer;
    private RecyclerView rvAlerts;
    private SwipeRefreshLayout swipeRefresh;
    private Button btnAckAll;
    private TextView tvActiveCount;
    private AlertsAdapter adapter;

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
        rvAlerts      = findViewById(R.id.rvAlerts);
        swipeRefresh  = findViewById(R.id.swipeRefresh);
        btnAckAll     = findViewById(R.id.btnAcknowledgeAll);
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
            adapter.clearAlerts();
            updateActiveCount();
        });

        // Load initial
        loadAlerts();
    }

    private void loadAlerts() {
        adapter.clearAlerts();
        adapter.addAlert("08:15 04/05/2025 — Mực nước vượt ngưỡng 80cm");
        adapter.addAlert("08:20 04/05/2025 — MQTT mất kết nối");
        adapter.addAlert("08:25 04/05/2025 — Tự động tái kết nối thành công");
        updateActiveCount();
    }

    private void updateActiveCount() {
        int count = adapter.getItemCount();
        tvActiveCount.setText(count + " cảnh báo đang chờ");
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        drawer.closeDrawers();
        if (item.getItemId()==R.id.nav_dashboard) {
            startActivity(new Intent(this, DashBoardActivity.class));
        }
        else if (item.getItemId() == R.id.nav_schedule) {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
        }else if (item.getItemId() == R.id.nav_control) {
            Intent intent = new Intent(this, controlActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_reports) {
            Intent intent = new Intent(this, ReportsActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
