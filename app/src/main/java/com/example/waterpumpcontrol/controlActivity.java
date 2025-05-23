package com.example.waterpumpcontrol;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.Manifest;


public class controlActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private TextView tvConnectionStatus, tvLastUpdate, tvWaterLevel, tvPumpStatus;
    private TextInputEditText etThreshold, etPumpSpeed;
    private Button btnApplyThreshold, btnApplyPumpSpeed;
    private Switch switchManual, switchAuto;
    private LineChart chartControl;

    // Chart
    private LineDataSet dataSetLevel, dataSetControl;
    private LineData lineData;
    private int timeIndex = 0;

    DrawerLayout drawer_layout;

    // MQTT
    private MqttHandler mqtt;
    private static final String BROKER    = "ssl://d3fd0fd59ed14b6d9fe037c0ef1bf662.s1.eu.hivemq.cloud:8883";
    private static final String USER      = "sonbe";
    private static final String PASS      = "Son@1234";
    private static final String TOPIC_LEVEL     = "level";
    private static final String TOPIC_CONTROL   = "control";
    private static final String TOPIC_THRESHOLD = "threshold";
    private static final String TOPIC_AUTO      = "auto";
    private static final String TOPIC_PUMPSPEED = "pumpspeed";

    private long lastMsgTime;
    private Handler handler = new Handler();

    private Handler UpdateUI_Handler = new Handler();

    private Runnable connectionChecker;

    private float lastLevel = 0f;
    private int   lastPwm   = 0;

    private int timeCheckWaterLevel = 5000;
    private int timeDelayCheckWaterLevel = 0;

    private WaterPumpManager waterPumpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_layout);

        bindViews();
        setupDrawer();
        initChart();
        //initMqtt();
        setupControls();
        startConnectionChecker();
        showLastValues();

        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(controlActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);
        startUpdatingUi();
        unFocus();

    }
    private void startUpdatingUi() {
        // Định nghĩa Runnable sẽ gọi updateUi mỗi 1 giây
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Cập nhật UI
                updateUi(waterPumpManager.getWaterLevel(), waterPumpManager.getWaterPWM());
                // Lập lại Runnable để chạy sau 1 giây nữa
                UpdateUI_Handler.postDelayed(this, 500);
            }
        };

        // Bắt đầu thực thi runnable ngay lập tức
        UpdateUI_Handler.post(updateRunnable);
    }

    private void unFocus()
    {
        drawer_layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Kiểm tra xem có EditText nào đang được focus không
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    // Ẩn bàn phím
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    // Bỏ focus khỏi trường hiện tại
                    currentFocus.clearFocus();
                }
            }
            return false;
        });
    }


    private void bindViews() {
        toolbar            = findViewById(R.id.toolbar);
        drawerLayout       = findViewById(R.id.drawer_layout);
        navigationView     = findViewById(R.id.nav_view);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        etThreshold        = findViewById(R.id.etThreshold);
        etPumpSpeed        = findViewById(R.id.etPumpSpeed);
        btnApplyThreshold  = findViewById(R.id.btnApplyThreshold);
        btnApplyPumpSpeed  = findViewById(R.id.btnApplyPumpSpeed);
        chartControl       = findViewById(R.id.chartControl);  // ← phải khớp id này
        tvLastUpdate       = findViewById(R.id.tvLastUpdate);
        tvWaterLevel       = findViewById(R.id.tvWaterLevel);
        tvPumpStatus       = findViewById(R.id.tvPumpStatus);
        switchManual       = findViewById(R.id.switchManual);
        switchAuto         = findViewById(R.id.switchAuto);
        drawer_layout = findViewById(R.id.drawer_layout);
    }

    private void setupDrawer() {
        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        toggle.setToolbarNavigationClickListener(v -> {
            String s = etThreshold.getText().toString().trim();
            if (!s.isEmpty()) waterPumpManager.MQTT_Publish(TOPIC_THRESHOLD, s);
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void initChart() {
        dataSetLevel   = new LineDataSet(new ArrayList<>(), "Mực nước (cm)");
        dataSetControl = new LineDataSet(new ArrayList<>(), "DutyCycle (%)");

        dataSetLevel.setColor(Color.BLUE);
        dataSetControl.setColor(Color.RED);

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
                    return "" + String.format(Locale.US, "%.2f", entry.getY());
                }
                return "";
            }
        };
        dataSetControl.setValueFormatter(lastPointFormatterControl);

        dataSetLevel.setColor(Color.BLUE);
        dataSetLevel.setValueTextColor(Color.BLUE);
        dataSetLevel.setValueTextSize(10f);
        dataSetLevel.setDrawCircles(true);

        dataSetControl.setColor(Color.RED);
        dataSetControl.setValueTextColor(Color.RED);
        dataSetControl.setValueTextSize(10f);
        dataSetControl.setDrawCircles(true);

        dataSetLevel.setDrawCircles(true);
        dataSetControl.setDrawCircles(true);

        lineData = new LineData(dataSetLevel, dataSetControl);
        chartControl.setData(lineData);

        chartControl.getDescription().setEnabled(true);
        chartControl.getDescription().setText("");
        chartControl.getDescription().setTextColor(Color.BLUE);
        chartControl.getDescription().setTextSize(14f);

        chartControl.getXAxis().setTextColor(Color.BLUE);
        chartControl.getXAxis().setTextSize(12f);
        chartControl.getAxisLeft().setTextColor(Color.GREEN);
        chartControl.getAxisLeft().setTextSize(12f);
        chartControl.getAxisRight().setTextColor(Color.RED);
        chartControl.getAxisRight().setTextSize(12f);
        chartControl.getAxisRight().setDrawLabels(false);
        chartControl.getAxisRight().setDrawAxisLine(false);

        Legend legend = chartControl.getLegend();
        legend.setTextColor(Color.BLACK);
        legend.setTextSize(16f);
        legend.setForm(Legend.LegendForm.LINE);

        chartControl.invalidate();
    }

    private void initMqtt() {
        mqtt = new MqttHandler();
        mqtt.connect(this, BROKER, USER, PASS);
        mqtt.subscribe(TOPIC_LEVEL);
        lastMsgTime = System.currentTimeMillis();
        mqtt.setCallback(new MqttCallback() {
            @Override public void connectionLost(Throwable c) {}
            @Override public void deliveryComplete(IMqttDeliveryToken t) {}
            @Override public void messageArrived(String topic, MqttMessage m) {
                if (TOPIC_LEVEL.equals(topic))
                {
                    String[] p = new String(m.getPayload()).trim().split("\\s+");
                    float x = Float.parseFloat(p[0].replace(',','.'));
                    int   y = Integer.parseInt(p[1]);
                    lastLevel = x; lastPwm = y;
                    lastMsgTime = System.currentTimeMillis();
                    runOnUiThread(() -> updateUi(x, y));
                }
                else{

                }

            }
        });
    }

    private void setupControls() {
        switchManual.setEnabled(!switchAuto.isChecked());
        btnApplyThreshold.setOnClickListener(v -> {
            String s = etThreshold.getText().toString().trim();
            if (!s.isEmpty()) waterPumpManager.MQTT_Publish(TOPIC_THRESHOLD, s);
            if(s.isEmpty()) {
                Toast.makeText(this, "Hãy nhập mức nước trước", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, "Đặt mức nước: " + s + "cm", Toast.LENGTH_SHORT).show();
            }

        });
        btnApplyPumpSpeed.setOnClickListener(v -> {
            String s = etPumpSpeed.getText().toString().trim();
            if (!s.isEmpty()) waterPumpManager.MQTT_Publish(TOPIC_PUMPSPEED, s);

            if(s == "") {
                Toast.makeText(this, "Hãy nhập tốc độ bơm trước", Toast.LENGTH_SHORT).show();
            } else{
                Toast.makeText(this, "Đặt tốc độ bơm: " + s + "%", Toast.LENGTH_SHORT).show();
            }

        });
        switchAuto.setOnCheckedChangeListener((b,on) -> {
            waterPumpManager.MQTT_Publish(TOPIC_AUTO, on?"1":"0");
            switchManual.setEnabled(!on);
            btnApplyPumpSpeed.setEnabled(!on);
            etPumpSpeed.setEnabled(!on);

            Toast.makeText(this, "Chế độ tự động: " + (on?"Bật":"Tắt"), Toast.LENGTH_SHORT).show();
        });
        switchManual.setOnCheckedChangeListener((b,on) -> {
            waterPumpManager.MQTT_Publish(TOPIC_CONTROL, on?"1":"0");
            Toast.makeText(this, "Bơm " + (on?"Bật":"Tắt"), Toast.LENGTH_SHORT).show();
        });
    }

    private void startConnectionChecker() {
        connectionChecker = () -> {
            long d = System.currentTimeMillis() - lastMsgTime;
            tvConnectionStatus.setText(d<5000?"Connected":"Disconnected");
            handler.postDelayed(connectionChecker,1000);
        };
        handler.post(connectionChecker);
    }

    private void showLastValues() {
        tvWaterLevel.setText(String.format(Locale.US,"%.2f cm",lastLevel));
        tvPumpStatus.setText(String.format(Locale.US,"%.0f%%", lastPwm/255f*100f));
    }

    private void updateUi(float lvl, int pwm) {
        tvWaterLevel.setText(String.format(Locale.US,"%.2f cm",lvl));
        tvPumpStatus.setText(String.format(Locale.US,"%.2f%%", pwm/255f*100f));
        tvLastUpdate.setText(new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date()));

        float pct = pwm / 255f * 100f;

        dataSetLevel.addEntry(new Entry(timeIndex,lvl));
        dataSetControl.addEntry(new Entry(timeIndex, pct));
        if (dataSetLevel.getEntryCount()>30) {
            dataSetLevel.removeFirst();
            dataSetControl.removeFirst();
        }
        lineData.notifyDataChanged();
        chartControl.notifyDataSetChanged();
        chartControl.invalidate();
        timeIndex++;
    }

    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==R.id.nav_dashboard) {
            startActivity(new Intent(this, DashBoardActivity.class));
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

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(connectionChecker);
        handler.removeCallbacksAndMessages(null); // Dừng tất cả các callback
        mqtt.disconnect();
        UpdateUI_Handler.removeCallbacksAndMessages(null);
    }

    @Override public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}
