package com.example.waterpumpcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;



public class SettingsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WaterPumpManager waterPumpManager;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private CoordinatorLayout coordinator_layout;

    private EditText etTestDuration, etDefaultThreshold, etDefaultSpeed;
    private Button btnStartTest, btnStopTest, btnRestoreDefaults, btnChangePin, btnLogout;
    private Switch switchnotification, switchbell, switchAutoReport;
    private Spinner spinnerNotifyChannel, spinnerRetention, spinnerTheme, spinnerLanguage;

    private SharedPreferences prefs;
    private CountDownTimer testTimer;

    // Flags to skip initial spinner callbacks
    private boolean themeInitialized = false;
    private boolean languageInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);

        prefs = getSharedPreferences("waterpump_prefs", Context.MODE_PRIVATE);

        // Apply saved theme and locale before UI
        applySavedTheme();
        applySavedLocale();

        // Drawer + NavigationView
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(this);

        // Toolbar + Toggle
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.open_drawer, R.string.close_drawer
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Bind views
        etTestDuration     = findViewById(R.id.etTestDuration);
        btnStartTest       = findViewById(R.id.btnStartTest);
        btnStopTest        = findViewById(R.id.btnStopTest);
        etDefaultThreshold = findViewById(R.id.etDefaultThreshold);
        etDefaultSpeed     = findViewById(R.id.etDefaultSpeed);
        btnRestoreDefaults = findViewById(R.id.btnRestoreDefaults);
        switchnotification        = findViewById(R.id.switchnotification);
        switchbell      = findViewById(R.id.switchbell);
        switchAutoReport   = findViewById(R.id.switchAutoReport);
        spinnerNotifyChannel = findViewById(R.id.spinnerNotifyChannel);
        spinnerRetention     = findViewById(R.id.spinnerRetention);
        spinnerTheme         = findViewById(R.id.spinnerTheme);
        spinnerLanguage      = findViewById(R.id.spinnerLanguage);
        btnChangePin         = findViewById(R.id.btnChangePin);
        btnLogout            = findViewById(R.id.btnLogout);
        coordinator_layout   = findViewById(R.id.coordinator_layout);

        // Setup adapters
        spinnerNotifyChannel.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.notify_channels, android.R.layout.simple_spinner_item));
        spinnerRetention.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.retention_options, android.R.layout.simple_spinner_item));
        spinnerTheme.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.theme_options, android.R.layout.simple_spinner_item));
        spinnerLanguage.setAdapter(ArrayAdapter.createFromResource(
                this, R.array.language_options, android.R.layout.simple_spinner_item));

        // Disable stop initially
        btnStopTest.setEnabled(false);

        // Load saved preferences into UI
        loadPreferences();

        // Register listeners
        initListeners();

        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(SettingsActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);

        unFocus();
    }

    private void unFocus() {
        // Lấy DrawerLayout
        coordinator_layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Kiểm tra xem có EditText nào đang được focus không
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    // Ẩn bàn phím
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                    // Bỏ focus khỏi trường hiện tại
                    currentFocus.clearFocus();
                }
            }
            return false; // Trả về false để tiếp tục xử lý sự kiện
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveAllSettings();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void loadPreferences() {
        etTestDuration.setText(String.valueOf(prefs.getInt("test_duration", 5)));
        switchnotification.setChecked(prefs.getBoolean("alert_sound", true));
        switchbell.setChecked(prefs.getBoolean("alert_vibrate", true));
        spinnerNotifyChannel.setSelection(prefs.getInt("notify_channel_index", 0));
        spinnerRetention.setSelection(prefs.getInt("retention_index", 2));
        switchAutoReport.setChecked(prefs.getBoolean("auto_report", false));
        spinnerTheme.setSelection(prefs.getInt("theme_index", 0));
        spinnerLanguage.setSelection(prefs.getInt("language_index", 0));
    }

    private void initListeners() {
        btnStartTest.setOnClickListener(v -> startTestPump());
        btnStopTest.setOnClickListener(v -> stopTestPump());
        btnRestoreDefaults.setOnClickListener(v -> restoreDefaults());

        switchnotification.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean("alert_sound", c).apply());
        switchbell.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean("alert_vibrate", c).apply());
        switchAutoReport.setOnCheckedChangeListener((b, c) -> prefs.edit().putBoolean("auto_report", c).apply());

        spinnerNotifyChannel.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                prefs.edit().putInt("notify_channel_index", pos).apply()));
        spinnerRetention.setOnItemSelectedListener(new SimpleItemSelectedListener(pos ->
                prefs.edit().putInt("retention_index", pos).apply()));

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!themeInitialized) themeInitialized = true;
                else {
                    prefs.edit().putInt("theme_index", position).apply();
                    applyTheme(position);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!languageInitialized) languageInitialized = true;
                else {
                    prefs.edit().putInt("language_index", position).apply();
                    applyLanguage(position);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnChangePin.setOnClickListener(v -> showChangePinDialog());
        btnLogout.setOnClickListener(v -> performLogout());

        // 1) Event khi bật/tắt Thông báo chung
        switchnotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Bật/tắt tính năng notification
                WaterPumpManager.getInstance().setAlarmEnabled(isChecked);
                Toast.makeText(
                        SettingsActivity.this,
                        isChecked ? "Thông báo đã bật" : "Thông báo đã tắt",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // 2) Event khi bật/tắt Chuông
        switchbell.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // isChecked = true → user muốn có chuông → vibration-only = false
                WaterPumpManager.getInstance().setVibrationEnabled(isChecked);
                Toast.makeText(
                        SettingsActivity.this,
                        isChecked ? "Chuông đã bật" : "Chuông đã tắt, chỉ rung",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void startTestPump() {
        int sec;
        try { sec = Integer.parseInt(etTestDuration.getText().toString()); }
        catch (NumberFormatException e) { sec = 5; }
        prefs.edit().putInt("test_duration", sec).apply();

        btnStartTest.setEnabled(false);
        btnStopTest.setEnabled(true);

        testTimer = new CountDownTimer(sec * 1000L, 1000) {
            @Override public void onTick(long left) {
                btnStartTest.setText("Chạy thử (" + (left/1000) + "s)");
                waterPumpManager.MQTT_Publish(WaterPumpManager.getTopicPumpSpeed(), etDefaultThreshold.getText().toString());
            }
            @Override public void onFinish() { stopTestPump(); }
        }.start();
    }

    private void stopTestPump() {
        if (testTimer != null) testTimer.cancel();
        btnStartTest.setEnabled(true);
        btnStopTest.setEnabled(false);
        btnStartTest.setText("Chạy thử");
        waterPumpManager.MQTT_Publish(WaterPumpManager.getTopicPumpSpeed(), "0");
    }

    private void restoreDefaults() {
        prefs.edit()
                .putInt("default_threshold", 80)
                .putInt("default_speed", 50)
                .apply();
        etDefaultThreshold.setText("80");
        etDefaultSpeed.setText("50");
        Toast.makeText(this, "Khôi phục mặc định thành công", Toast.LENGTH_SHORT).show();
    }

    private void saveAllSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        String dur = etTestDuration.getText().toString();
        editor.putInt("test_duration", dur.isEmpty()?5:Integer.parseInt(dur));
        String thr = etDefaultThreshold.getText().toString();
        editor.putInt("default_threshold", thr.isEmpty()?80:Integer.parseInt(thr));
        String spd = etDefaultSpeed.getText().toString();
        editor.putInt("default_speed", spd.isEmpty()?50:Integer.parseInt(spd));
        editor.putBoolean("alert_sound", switchnotification.isChecked());
        editor.putBoolean("alert_vibrate", switchbell.isChecked());
        editor.putInt("notify_channel_index", spinnerNotifyChannel.getSelectedItemPosition());
        editor.putInt("retention_index", spinnerRetention.getSelectedItemPosition());
        editor.putBoolean("auto_report", switchAutoReport.isChecked());
        editor.putInt("theme_index", spinnerTheme.getSelectedItemPosition());
        editor.putInt("language_index", spinnerLanguage.getSelectedItemPosition());
        editor.apply();

    }

    private void applyTheme(int index) {
        switch (index) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            case 3: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY); break;
        }
    }

    private void applyLanguage(int index) {
        // Restart activity instead of recreate() to avoid freeze
        Locale locale;
        switch (index) {
            case 1: locale = new Locale("en"); break;
            case 2: locale = new Locale("zh"); break;
            case 3: locale = new Locale("es"); break;
            case 4: locale = new Locale("th"); break;
            default: locale = new Locale("vi");
        }
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // restart activity
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void applySavedTheme() {
        int idx = prefs.getInt("theme_index", 0);
        switch (idx) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            case 3: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY); break;
        }
    }

    private void applySavedLocale() {
        int idx = prefs.getInt("language_index", 0);
        Locale locale;
        switch (idx) {
            case 1: locale = new Locale("en"); break;
            case 2: locale = new Locale("zh"); break;
            case 3: locale = new Locale("es"); break;
            case 4: locale = new Locale("th"); break;
            default: locale = new Locale("vi");
        }
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void showChangePinDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Thay đổi PIN");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        b.setView(input);
        b.setPositiveButton("Lưu", (d,w) -> {
            prefs.edit().putString("app_pin", input.getText().toString()).apply();
            Toast.makeText(this, "PIN đã được thay đổi", Toast.LENGTH_SHORT).show();
        });
        b.setNegativeButton("Huỷ", (d,w) -> d.cancel());
        b.show();
    }

    private void performLogout() {
        prefs.edit().clear().apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);
        Intent intent = null;
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) intent = new Intent(this, DashBoardActivity.class);
        else if (id == R.id.nav_control) intent = new Intent(this, controlActivity.class);
        else if (id == R.id.nav_schedule) intent = new Intent(this, ScheduleActivity.class);
        else if (id == R.id.nav_alerts) intent = new Intent(this, AlertsActivity.class);
        else if (id == R.id.nav_reports) intent = new Intent(this, ReportsActivity.class);
        else if (id == R.id.nav_settings) return true;
        else if (item.getItemId() == R.id.nav_logout) {
            intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_help) {
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        if (intent != null) { startActivity(intent); finish(); }
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

    private static class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
        interface OnItem { void onSelected(int pos); }
        private final OnItem callback;
        SimpleItemSelectedListener(OnItem cb) { this.callback = cb; }
        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            callback.onSelected(position);
        }
        @Override public void onNothingSelected(AdapterView<?> parent) {}
    }
}
