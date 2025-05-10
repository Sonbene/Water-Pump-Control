package com.example.waterpumpcontrol;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Locale;

public class DashBoardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;

    private TextView tvCurrentWaterLevel;
    private TextView tvPumpStatus;
    private RecyclerView rvAlerts;
    private AlertsAdapter alertsAdapter;

    // Chart
    private LineChart chartHistory;
    private LineDataSet dataSetLevel;   // mực nước
    private LineDataSet dataSetControl; // xung điều khiển
    private LineData lineData;
    private int timeIndex = 0;

    // MQTT
    private MqttHandler mqtt;
    private static final String BROKER_URI    = "ssl://d3fd0fd59ed14b6d9fe037c0ef1bf662.s1.eu.hivemq.cloud:8883";
    private static final String MQTT_USER     = "sonbe";
    private static final String MQTT_PASS     = "Son@1234";
    private static final String TOPIC_LEVEL   = "level";
    private static final String TOPIC_CONTROL = "control";

    private boolean alertedFull = false;
    private boolean alertedEmpty = false;

    private float lastLevel = 0f;

    private Handler Update_Handler = new Handler();

    private WaterPumpManager waterPumpManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_layout);

        // toolbar + drawer
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout   = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // TextViews & RecyclerView
        tvCurrentWaterLevel = findViewById(R.id.tvCurrentWaterLevel);
        tvPumpStatus        = findViewById(R.id.tvPumpStatus);
        rvAlerts            = findViewById(R.id.rvAlerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        alertsAdapter = new AlertsAdapter(new ArrayList<>());
        rvAlerts.setAdapter(alertsAdapter);

        // Chart setup
        chartHistory = findViewById(R.id.chartControl);
        dataSetLevel   = new LineDataSet(new ArrayList<>(), "Mực nước (cm)");
        dataSetControl = new LineDataSet(new ArrayList<>(), "DutyCycle(%)");


// sau khi tạo dataSetLevel, dataSetControl nhưng trước khi setData(...)
        ValueFormatter lastPointFormatterLevel = new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                int lastIndex = dataSetLevel.getEntryCount() - 1;
                if (entry == dataSetLevel.getEntryForIndex(lastIndex)) {
                    // thêm 3 khoảng trắng để đẩy label sang trái
                    return "" + String.format(Locale.US, "%.2f", entry.getY());
                }
                return "";
            }
        };
        dataSetLevel.setValueFormatter(lastPointFormatterLevel);

        ValueFormatter lastPointFormatterControl = new ValueFormatter() {
            @Override
            public String getPointLabel(Entry entry) {
                int lastIndex = dataSetControl.getEntryCount() - 1;
                if (entry == dataSetControl.getEntryForIndex(lastIndex)) {
                    return ""+ String.format(Locale.US, "%.2f", entry.getY());
                }
                return "";
            }
        };
        dataSetControl.setValueFormatter(lastPointFormatterControl);

// giữ các thiết lập màu & size như trước
        dataSetLevel.setValueTextColor(Color.MAGENTA);
        dataSetLevel.setValueTextSize(10f);
        dataSetControl.setValueTextColor(Color.WHITE);
        dataSetControl.setValueTextSize(10f);


        // màu và kích thước text
        dataSetLevel.setColor(Color.BLUE);
        dataSetLevel.setValueTextColor(Color.BLUE);
        dataSetLevel.setValueTextSize(10f);

        dataSetControl.setColor(Color.RED);
        dataSetControl.setValueTextColor(Color.RED);
        dataSetControl.setValueTextSize(10f);

        // Description
        chartHistory.getDescription().setEnabled(true);
        chartHistory.getDescription().setText("");
        chartHistory.getDescription().setTextColor(Color.BLUE);
        chartHistory.getDescription().setTextSize(14f);

        // Trục X, Y
        chartHistory.getXAxis().setTextColor(Color.BLUE);
        chartHistory.getXAxis().setTextSize(12f);
        chartHistory.getAxisLeft().setTextColor(Color.GREEN);
        chartHistory.getAxisLeft().setTextSize(12f);
        chartHistory.getAxisRight().setTextColor(Color.RED);
        chartHistory.getAxisRight().setTextSize(12f);

        // Ẩn nhãn số bên phải
        chartHistory.getAxisRight().setDrawLabels(false);
// (tùy chọn) ẩn cả đường trục nếu muốn
        chartHistory.getAxisRight().setDrawAxisLine(false);


        // Legend
        Legend legend = chartHistory.getLegend();
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(16f);
        legend.setForm(Legend.LegendForm.LINE);

        // finalize chart
        dataSetLevel.setDrawCircles(true);
        dataSetControl.setDrawCircles(true);
        lineData = new LineData(dataSetLevel, dataSetControl);
        chartHistory.setData(lineData);
        chartHistory.invalidate();


        // MQTT setup
//        mqtt = new MqttHandler();
//        mqtt.connect(this, BROKER_URI, MQTT_USER, MQTT_PASS);
//
//        mqtt.setCallback(new MqttCallback() {
//            @Override
//            public void connectionLost(Throwable cause) {}
//
//            @Override
//            public void messageArrived(String topic, MqttMessage message) {
//                handleMessageArrived(topic, message);
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken token) {
//                // Optional, if you need to handle delivery completion
//            }
//        });
//
//        mqtt.subscribe(TOPIC_LEVEL);

        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);

        startUpdatingUi();


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(DashBoardActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);
    }

    private void handleMessageArrived(String topic, MqttMessage message) {
        if (TOPIC_LEVEL.equals(topic)) {
            String payload = new String(message.getPayload()).trim();
            String[] parts = payload.split("\\s+");

            if (parts.length != 2) return;

            try {
                float x = Float.parseFloat(parts[0].replace(',', '.'));
                int y = Integer.parseInt(parts[1]);

                lastLevel = x;

                updateUiWithData(x, y);
                updateAlerts(x);
                updateChartData(x, y);

            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void startUpdatingUi() {
        // Định nghĩa Runnable sẽ gọi updateUi mỗi 1 giây
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Cập nhật UI
                updateUiWithData(waterPumpManager.getWaterLevel(), waterPumpManager.getWaterPWM());
                updateAlerts(waterPumpManager.getWaterLevel());
                updateChartData(waterPumpManager.getWaterLevel(), waterPumpManager.getWaterPWM());
                // Lập lại Runnable để chạy sau 1 giây nữa
                Update_Handler.postDelayed(this, 500);
            }
        };

        // Bắt đầu thực thi runnable ngay lập tức
        Update_Handler.post(updateRunnable);
    }

    private void updateUiWithData(float x, int y) {
        runOnUiThread(() -> {
            tvCurrentWaterLevel.setText(x + " cm");
            float pct = y / 255f * 100f;
            tvPumpStatus.setText(String.format(Locale.US, "%.2f%%", pct));
        });
    }

    private void updateAlerts(float x) {
        runOnUiThread(() -> {
            if (x > 18f) {
                if (!alertedFull) {
                    alertsAdapter.clearAlerts();
                    alertsAdapter.addAlert("⚠️ Bể đầy: " + x + " cm");
                    alertedFull = true;
                    alertedEmpty = false;
                }
            } else if (x < 2f) {
                if (!alertedEmpty) {
                    alertsAdapter.clearAlerts();
                    alertsAdapter.addAlert("⚠️ Bể cạn: " + x + " cm");
                    alertedEmpty = true;
                    alertedFull = false;
                }
            } else {
                alertsAdapter.clearAlerts();
                alertedFull = false;
                alertedEmpty = false;
            }
        });
    }

    private void updateChartData(float x, int y) {
        runOnUiThread(() -> {
            dataSetLevel.addEntry(new Entry(timeIndex, x));
            dataSetControl.addEntry(new Entry(timeIndex, y / 255f * 100f));

            if (dataSetLevel.getEntryCount() > 30) {
                dataSetLevel.removeFirst();
                dataSetControl.removeFirst();
            }

            lineData.notifyDataChanged();
            chartHistory.notifyDataSetChanged();
            chartHistory.invalidate();
            timeIndex++;
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_control) {
            Intent intent = new Intent(this, controlActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_schedule) {
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_alerts) {
            Intent intent = new Intent(this, AlertsActivity.class);
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
        else if (item.getItemId() == R.id.nav_logout) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mqtt.disconnect();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}
