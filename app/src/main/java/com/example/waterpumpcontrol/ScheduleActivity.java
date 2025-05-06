package com.example.waterpumpcontrol;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.widget.Toast;


public class ScheduleActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // UI
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Spinner spinnerPumpMode, spinnerRecurrence;
    private View layoutDate;
    private Button btnDate, btnStartTime, btnSaveSchedule;
    private ChipGroup chipDaysOfWeek, chipDaysOfMonth;
    private RecyclerView rvSchedules;

    // Data & adapter
    private final List<Schedule> schedules = new ArrayList<>();
    private ScheduleAdapter adapter;
    private int editingPosition = -1;

    // Labels tuần
    private final String[] weekLabels = {"T2","T3","T4","T5","T6","T7","CN"};

    // DB config
    private static final String DB_URL      = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER     = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    // Executor for DB calls
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private WaterPumpManager waterPumpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_layout);

        // 1. Bind views
        drawerLayout    = findViewById(R.id.drawer_layout);
        navigationView  = findViewById(R.id.nav_view);
        spinnerPumpMode   = findViewById(R.id.spinnerPumpMode);
        spinnerRecurrence = findViewById(R.id.spinnerRecurrence);
        layoutDate        = findViewById(R.id.layoutDate);
        btnDate           = findViewById(R.id.btnDate);
        btnStartTime      = findViewById(R.id.btnStartTime);
        btnSaveSchedule   = findViewById(R.id.btnSaveSchedule);
        chipDaysOfWeek    = findViewById(R.id.chipDaysOfWeek);
        chipDaysOfMonth   = findViewById(R.id.chipDaysOfMonth);
        rvSchedules       = findViewById(R.id.rvSchedules);

        // 2. Toolbar + drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle t = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        drawerLayout.addDrawerListener(t);
        t.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // 3. Pump mode spinner
        spinnerPumpMode.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.pump_mode_options)
        ));

        // 4. Recurrence spinner
        spinnerRecurrence.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.recurrence_options)
        ));
        spinnerRecurrence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String r = p.getItemAtPosition(pos).toString();
                layoutDate.setVisibility(r.equals("One time") ? View.VISIBLE : View.GONE);
                chipDaysOfWeek.setVisibility(r.equals("Weekly") ? View.VISIBLE : View.GONE);
                chipDaysOfMonth.setVisibility(r.equals("Monthly") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // 5. Chips ngày trong tháng
        for (int d = 1; d <= 31; d++) {
            Chip c = new Chip(this);
            c.setText(String.valueOf(d));
            c.setCheckable(true);
            chipDaysOfMonth.addView(c);
        }

        // 6. Date picker
        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this,
                    (DatePicker view, int y, int m, int d) ->
                            btnDate.setText(String.format("%02d/%02d/%04d", d, m+1, y)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // 7. Time picker
        btnStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this,
                    (tp, h, m) -> btnStartTime.setText(String.format("%02d:%02d", h, m)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
            ).show();
        });

        // 8. Chips ngày trong tuần
        for (int i = 0; i < weekLabels.length; i++) {
            Chip c = new Chip(this);
            c.setText(weekLabels[i]);
            c.setCheckable(true);
            c.setTag(i+1);
            chipDaysOfWeek.addView(c);
        }

        // 9. RecyclerView + adapter với callback
        adapter = new ScheduleAdapter(
                schedules,
                this::onEditSchedule,
                this::onDeleteSchedule,
                this::onToggleActive
        );
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvSchedules.setAdapter(adapter);

        // 10. Load từ DB khi mở
        fetchSchedulesFromDb();

        // 11. Lưu / cập nhật khi bấm
        // 11. Lưu / cập nhật khi bấm
        btnSaveSchedule.setOnClickListener(v -> {
            Schedule s = collectFormData();

            // Kiểm tra xem có thiếu thông tin không
            if (s == null || !validateForm(s)) {
                // Nếu form không hợp lệ, dừng lại và thông báo lỗi
                Toast.makeText(ScheduleActivity.this, "Vui lòng điền đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editingPosition >= 0) {
                // giữ id, cập nhật list
                s.setId(schedules.get(editingPosition).getId());
                schedules.set(editingPosition, s);
                adapter.notifyItemChanged(editingPosition);
                editingPosition = -1;
            } else {
                // thêm mới
                schedules.add(s);
                adapter.notifyItemInserted(schedules.size()-1);
            }
            clearForm();
            saveOrUpdateScheduleToDb(s);
        });


        waterPumpManager = WaterPumpManager.getInstance();

        waterPumpManager.checkPostNotificationPermission(this);
        waterPumpManager.initMqtt(this);
        waterPumpManager.createNotificationChannel(this);


        Handler handler1 = new Handler();
        Runnable checkMqttConnectionTask = new Runnable() {
            @Override
            public void run() {
                // Gọi hàm checkMqttConnection mỗi 10 giây
                waterPumpManager.checkMqttConnection(ScheduleActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);
    }

    private boolean validateForm(Schedule s) {
        String recurrence = s.getRecurrence();

        // Kiểm tra cho mỗi trường hợp Recurrence
        if ("One time".equals(recurrence)) {
            // Phải nhập đủ ngày và giờ
            if (s.getDate() == null || s.getDate().isEmpty() || "--:--".equals(s.getStartTime())) {
                return false; // Không hợp lệ nếu thiếu ngày hoặc giờ
            }
        } else if ("Daily".equals(recurrence)) {
            // Phải nhập đủ giờ
            if ("--:--".equals(s.getStartTime())) {
                return false; // Không hợp lệ nếu thiếu giờ
            }
        } else if ("Weekly".equals(recurrence)) {
            // Phải chọn ít nhất 1 ngày trong tuần và giờ
            if (s.getDaysOfWeek().isEmpty() || "--:--".equals(s.getStartTime())) {
                return false; // Không hợp lệ nếu thiếu ngày trong tuần hoặc giờ
            }
        } else if ("Monthly".equals(recurrence)) {
            // Phải chọn ít nhất 1 ngày trong tháng và giờ
            if (s.getDaysOfMonth().isEmpty() || "--:--".equals(s.getStartTime())) {
                return false; // Không hợp lệ nếu thiếu ngày trong tháng hoặc giờ
            }
        }

        return true; // Nếu tất cả các kiểm tra hợp lệ
    }


    private Schedule collectFormData() {
        Schedule s = new Schedule();
        s.setPumpMode(spinnerPumpMode.getSelectedItem().toString());
        s.setRecurrence(spinnerRecurrence.getSelectedItem().toString());
        if ("One time".equals(s.getRecurrence())) {
            s.setDate(btnDate.getText().toString());
        }
        List<Integer> dow = new ArrayList<>();
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfWeek.getChildAt(i);
            if (c.isChecked()) dow.add((Integer)c.getTag());
        }
        s.setDaysOfWeek(dow);
        List<Integer> dom = new ArrayList<>();
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfMonth.getChildAt(i);
            if (c.isChecked()) dom.add(Integer.parseInt(c.getText().toString()));
        }
        s.setDaysOfMonth(dom);
        s.setStartTime(btnStartTime.getText().toString());
        s.setActive(true);

        // Kiểm tra tính hợp lệ của dữ liệu trước khi trả về
        if (!validateForm(s)) {
            Log.e("Form Validation", "Form không hợp lệ, vui lòng kiểm tra lại.");
            return null; // Trả về null nếu form không hợp lệ
        }

        return s;
    }


    // Chỉnh sửa: fill form với item đã chọn
    private void onEditSchedule(int pos) {
        Schedule s = schedules.get(pos);
        editingPosition = pos;

        // Thay đổi cách gọi setSelection
        spinnerPumpMode.setSelection(((ArrayAdapter<String>)spinnerPumpMode.getAdapter()).getPosition(s.getPumpMode()));
        spinnerRecurrence.setSelection(((ArrayAdapter<String>)spinnerRecurrence.getAdapter()).getPosition(s.getRecurrence()));

        if ("One time".equals(s.getRecurrence())) {
            btnDate.setText(s.getDate());
        }

        // Set trạng thái chip ngày trong tuần
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++) {
            Chip c = (Chip) chipDaysOfWeek.getChildAt(i);
            c.setChecked(s.getDaysOfWeek().contains((Integer) c.getTag()));
        }

        // Set trạng thái chip ngày trong tháng
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++) {
            Chip c = (Chip) chipDaysOfMonth.getChildAt(i);
            c.setChecked(s.getDaysOfMonth().contains(Integer.parseInt(c.getText().toString())));
        }

        // Set thời gian bắt đầu
        btnStartTime.setText(s.getStartTime());
    }


    // Xóa: gọi DB rồi cập nhật UI
    private void onDeleteSchedule(int pos) {
        // Kiểm tra xem chỉ mục có hợp lệ không trước khi xóa
        if (pos >= 0 && pos < schedules.size()) {
            Schedule s = schedules.get(pos);
            deleteScheduleFromDb(s.getId());
            schedules.remove(pos);  // Xóa mục khỏi danh sách
            adapter.notifyItemRemoved(pos);  // Cập nhật giao diện
            if (editingPosition == pos) clearForm();
        } else {
            Log.e("DB_SAVE", "Invalid position: " + pos);  // Log lỗi nếu chỉ mục không hợp lệ
        }
    }


    private void onToggleActive(int pos, boolean active) {
        Schedule s = schedules.get(pos);
        s.setActive(active);
        saveOrUpdateScheduleToDb(s);
    }

    private void clearForm() {
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++)
            ((Chip)chipDaysOfWeek.getChildAt(i)).setChecked(false);
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++)
            ((Chip)chipDaysOfMonth.getChildAt(i)).setChecked(false);
        btnDate.setText("--/--/----");
        btnStartTime.setText("--:--");
        editingPosition = -1;
    }

    /** FETCH tất cả lịch từ DB và đổ vào `schedules` */
    private void fetchSchedulesFromDb() {
        dbExecutor.execute(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM WaterPumpControlSchedule")) {

                    List<Schedule> list = new ArrayList<>();
                    while (rs.next()) {
                        Schedule s = new Schedule();
                        s.setId(rs.getInt("id"));
                        s.setPumpMode(rs.getString("pump_mode"));
                        s.setRecurrence(rs.getString("recurrence"));
                        if (rs.getDate("scheduled_date") != null) {
                            s.setDate(rs.getDate("scheduled_date").toString());
                        }
                        s.setStartTime(rs.getTime("start_time").toString().substring(0,5));
                        // parse JSON arrays
                        JSONArray jw = new JSONArray(rs.getString("days_of_week"));
                        for (int i=0; i<jw.length(); i++) s.getDaysOfWeek().add(jw.getInt(i));
                        JSONArray jm = new JSONArray(rs.getString("days_of_month"));
                        for (int i=0; i<jm.length(); i++) s.getDaysOfMonth().add(jm.getInt(i));
                        s.setActive(rs.getBoolean("is_active"));
                        list.add(s);
                    }
                    runOnUiThread(() -> {
                        schedules.clear();
                        schedules.addAll(list);
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e("DB_FETCH", "Lỗi fetch schedules", e);
            }
        });
    }

    private void saveOrUpdateScheduleToDb(Schedule s) {
        dbExecutor.execute(() -> {
            try {
                Log.d("Recurrence Value", "Recurrence selected: " + s.getRecurrence()); // Kiểm tra giá trị

                String recurrence = s.getRecurrence().trim();  // Loại bỏ khoảng trắng

                // Kiểm tra giá trị hợp lệ của recurrence
                if (recurrence.equals("Weekly") || recurrence.equals("One time") ||
                        recurrence.equals("Monthly") || recurrence.equals("Daily")) {

                    Class.forName("com.mysql.jdbc.Driver");
                    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        String sql;
                        PreparedStatement ps = null;

                        if (s.getId() == null) {
                            // INSERT
                            sql = "INSERT INTO WaterPumpControlSchedule " +
                                    "(pump_mode, recurrence, scheduled_date, start_time, " +
                                    "days_of_week, days_of_month, is_active) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

                            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                            ps.setString(1, s.getPumpMode());
                            ps.setString(2, recurrence);  // Gán giá trị hợp lệ cho recurrence

                            // Nếu recurrence là "One time", thiết lập ngày, nếu không thì gán NULL
                            if ("One time".equals(recurrence) && s.getDate() != null) {
                                String[] p = s.getDate().split("/");
                                ps.setDate(3, java.sql.Date.valueOf(p[2] + "-" + p[1] + "-" + p[0]));
                            } else {
                                ps.setNull(3, Types.DATE); // Nếu không phải "One time", không cần ngày
                            }

                            String startTime = s.getStartTime();
                            if ("--:--".equals(startTime)) {
                                ps.setNull(4, Types.TIME);  // Nếu thời gian là "--:--", thì bỏ qua
                            } else {
                                ps.setTime(4, java.sql.Time.valueOf(startTime + ":00"));  // Nếu có thời gian hợp lệ, chuyển đổi thành Time
                            }

                            ps.setString(5, new JSONArray(s.getDaysOfWeek()).toString());
                            ps.setString(6, new JSONArray(s.getDaysOfMonth()).toString());
                            ps.setBoolean(7, s.isActive());
                            ps.executeUpdate();

                            // Lấy ID tự động sinh ra sau khi insert
                            try (ResultSet keys = ps.getGeneratedKeys()) {
                                if (keys.next()) s.setId(keys.getInt(1));
                            }

                        } else {
                            // UPDATE
                            sql = "UPDATE WaterPumpControlSchedule SET " +
                                    "pump_mode=?, recurrence=?, scheduled_date=?, start_time=?, " +
                                    "days_of_week=?, days_of_month=?, is_active=? " +
                                    "WHERE id=?";

                            ps = conn.prepareStatement(sql);

                            ps.setString(1, s.getPumpMode());
                            ps.setString(2, recurrence);  // Gán giá trị hợp lệ cho recurrence

                            // Nếu recurrence là "One time", thiết lập ngày, nếu không thì gán NULL
                            if ("One time".equals(recurrence) && s.getDate() != null) {
                                String[] p = s.getDate().split("/");
                                ps.setDate(3, java.sql.Date.valueOf(p[2] + "-" + p[1] + "-" + p[0]));
                            } else {
                                ps.setNull(3, Types.DATE); // Nếu không phải "One time", không cần ngày
                            }

                            // Thời gian bắt đầu
                            String startTime = s.getStartTime();
                            if ("--:--".equals(startTime)) {
                                ps.setNull(4, Types.TIME);  // Nếu thời gian là "--:--", thì bỏ qua
                            } else {
                                ps.setTime(4, java.sql.Time.valueOf(startTime + ":00"));
                            }

                            ps.setString(5, new JSONArray(s.getDaysOfWeek()).toString());
                            ps.setString(6, new JSONArray(s.getDaysOfMonth()).toString());
                            ps.setBoolean(7, s.isActive());
                            ps.setInt(8, s.getId());
                            ps.executeUpdate();
                        }
                    }
                } else {
                    Log.e("DB_SAVE", "Invalid recurrence value: " + recurrence);
                }
            } catch (Exception e) {
                Log.e("DB_SAVE", "Error saving or updating schedule", e);
            }
        });
    }

    /** Xóa 1 schedule theo id */
    private void deleteScheduleFromDb(Integer id) {
        if (id == null) return;
        dbExecutor.execute(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement ps = conn.prepareStatement(
                             "DELETE FROM WaterPumpControlSchedule WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                Log.e("DB_DELETE", "Lỗi delete schedule", e);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_control) {
            Intent intent = new Intent(this, controlActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.nav_dashboard) {
            Intent intent = new Intent(this, DashBoardActivity.class);
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
        dbExecutor.shutdown();
    }
}
