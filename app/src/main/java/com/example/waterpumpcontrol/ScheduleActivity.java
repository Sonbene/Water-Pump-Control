package com.example.waterpumpcontrol;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Spinner spinnerPumpMode, spinnerRecurrence;
    private View layoutDate;
    private Button btnDate, btnStartTime, btnSaveSchedule;
    private ChipGroup chipDaysOfWeek, chipDaysOfMonth;

    private RecyclerView rvSchedules;
    private ScheduleAdapter adapter;
    private final List<Schedule> schedules = new ArrayList<>();

    // để đánh dấu đang edit item nào
    private int editingPosition = -1;

    // Nhãn cho 7 ngày trong tuần
    private final String[] weekLabels = {"T2","T3","T4","T5","T6","T7","CN"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_layout);

        // 1. bind all views
        drawerLayout      = findViewById(R.id.drawer_layout);
        navigationView    = findViewById(R.id.nav_view);
        spinnerPumpMode   = findViewById(R.id.spinnerPumpMode);
        spinnerRecurrence = findViewById(R.id.spinnerRecurrence);
        layoutDate        = findViewById(R.id.layoutDate);
        btnDate           = findViewById(R.id.btnDate);
        btnStartTime      = findViewById(R.id.btnStartTime);
        btnSaveSchedule   = findViewById(R.id.btnSaveSchedule);
        chipDaysOfWeek    = findViewById(R.id.chipDaysOfWeek);
        chipDaysOfMonth   = findViewById(R.id.chipDaysOfMonth);
        rvSchedules       = findViewById(R.id.rvSchedules);

        // 2. toolbar + drawer toggle
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle t = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        drawerLayout.addDrawerListener(t);
        t.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // 3. spinner PumpMode
        spinnerPumpMode.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.pump_mode_options)
        ));

        // 4. spinner Recurrence
        spinnerRecurrence.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.recurrence_options)
        ));
        spinnerRecurrence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String r = p.getItemAtPosition(pos).toString();
                layoutDate.setVisibility(r.equals("Một lần") ? View.VISIBLE : View.GONE);
                chipDaysOfWeek.setVisibility(r.equals("Hàng tuần") ? View.VISIBLE : View.GONE);
                chipDaysOfMonth.setVisibility(r.equals("Hàng tháng") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // 5. sinh Chips ngày trong tháng 1..31
        for (int d = 1; d <= 31; d++) {
            Chip c = new Chip(this);
            c.setText(String.valueOf(d));
            c.setCheckable(true);
            chipDaysOfMonth.addView(c);
        }

        // 6. date picker
        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this,
                    (DatePicker view, int y, int m, int d) ->
                            btnDate.setText(String.format("%02d/%02d/%04d", d, m+1, y)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // 7. time picker
        btnStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this,
                    (tp, h, m) -> btnStartTime.setText(String.format("%02d:%02d", h, m)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
            ).show();
        });

        // 8. sinh Chips cho ngày trong tuần
        for (int i = 0; i < weekLabels.length; i++) {
            Chip c = new Chip(this);
            c.setText(weekLabels[i]);
            c.setCheckable(true);
            c.setTag(i+1);           // 1=Thứ2…7=CN
            chipDaysOfWeek.addView(c);
        }

        // 9. RecyclerView + adapter
        adapter = new ScheduleAdapter(
                schedules,
                this::onEditSchedule,
                this::onDeleteSchedule,
                this::onToggleActive
        );
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvSchedules.setAdapter(adapter);

        // 10. Save / Update
        btnSaveSchedule.setOnClickListener(v -> {
            Schedule s = collectFormData();
            if (editingPosition >= 0) {
                schedules.set(editingPosition, s);
                editingPosition = -1;
            } else {
                schedules.add(s);
            }
            adapter.notifyDataSetChanged();
            clearForm();
        });
    }

    private Schedule collectFormData() {
        Schedule s = new Schedule();
        s.setPumpMode(spinnerPumpMode.getSelectedItem().toString());
        s.setRecurrence(spinnerRecurrence.getSelectedItem().toString());
        if ("Một lần".equals(s.getRecurrence())) {
            s.setDate(btnDate.getText().toString());
        }
        // ngày trong tuần
        List<Integer> dow = new ArrayList<>();
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfWeek.getChildAt(i);
            if (c.isChecked()) dow.add((Integer)c.getTag());
        }
        s.setDaysOfWeek(dow);
        // ngày trong tháng
        List<Integer> dom = new ArrayList<>();
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfMonth.getChildAt(i);
            if (c.isChecked()) dom.add(Integer.parseInt(c.getText().toString()));
        }
        s.setDaysOfMonth(dom);
        s.setStartTime(btnStartTime.getText().toString());
        s.setActive(true);
        return s;
    }

    private void onEditSchedule(int pos) {
        Schedule s = schedules.get(pos);
        editingPosition = pos;
        // pump mode
        ArrayAdapter<String> pm = (ArrayAdapter<String>)spinnerPumpMode.getAdapter();
        spinnerPumpMode.setSelection(pm.getPosition(s.getPumpMode()));
        // recurrence
        ArrayAdapter<String> rc = (ArrayAdapter<String>)spinnerRecurrence.getAdapter();
        spinnerRecurrence.setSelection(rc.getPosition(s.getRecurrence()));
        // date
        if ("Một lần".equals(s.getRecurrence())) btnDate.setText(s.getDate());
        // restore tuần
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfWeek.getChildAt(i);
            c.setChecked(s.getDaysOfWeek().contains((Integer)c.getTag()));
        }
        // restore tháng
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++) {
            Chip c = (Chip)chipDaysOfMonth.getChildAt(i);
            c.setChecked(s.getDaysOfMonth().contains(Integer.parseInt(c.getText().toString())));
        }
        // time
        btnStartTime.setText(s.getStartTime());
    }

    private void onDeleteSchedule(int pos) {
        schedules.remove(pos);
        adapter.notifyItemRemoved(pos);
        if (editingPosition == pos) clearForm();
    }

    private void onToggleActive(int pos, boolean active) {
        schedules.get(pos).setActive(active);
    }

    private void clearForm() {
        //spinnerPumpMode.setSelection(0);
        //spinnerRecurrence.setSelection(0);
//        layoutDate.setVisibility(View.GONE);
//        chipDaysOfWeek.setVisibility(View.GONE);
        for (int i = 0; i < chipDaysOfWeek.getChildCount(); i++)
            ((Chip)chipDaysOfWeek.getChildAt(i)).setChecked(false);
        for (int i = 0; i < chipDaysOfMonth.getChildCount(); i++)
            ((Chip)chipDaysOfMonth.getChildAt(i)).setChecked(false);
        btnDate.setText("--/--/----");
        btnStartTime.setText("--:--");
        editingPosition = -1;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
