package com.example.waterpumpcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class HelpActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private Toolbar toolbar;

    // Mảng lưu các TextView (câu trả lời)
    private final TextView[] tvAnswers = new TextView[12];
    // Mảng ID của các TextView
    private final int[] answerIds = {
            R.id.tvAnswer1,  R.id.tvAnswer2,  R.id.tvAnswer3,  R.id.tvAnswer4,
            R.id.tvAnswer5,  R.id.tvAnswer6,  R.id.tvAnswer7,  R.id.tvAnswer8,
            R.id.tvAnswer9,  R.id.tvAnswer10, R.id.tvAnswer11, R.id.tvAnswer12
    };
    // Mảng ID của các CardView (câu hỏi)
    private final int[] questionIds = {
            R.id.cardQuestion1,  R.id.cardQuestion2,  R.id.cardQuestion3,  R.id.cardQuestion4,
            R.id.cardQuestion5,  R.id.cardQuestion6,  R.id.cardQuestion7,  R.id.cardQuestion8,
            R.id.cardQuestion9,  R.id.cardQuestion10, R.id.cardQuestion11, R.id.cardQuestion12
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_layout);

        // 1. Thiết lập Toolbar + Drawer
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        // 2. Khởi tạo và ẩn hết các câu trả lời
        for (int i = 0; i < tvAnswers.length; i++) {
            tvAnswers[i] = findViewById(answerIds[i]);
            tvAnswers[i].setVisibility(View.GONE);
        }

        // 3. Gán sự kiện toggle cho từng câu hỏi
        for (int i = 0; i < questionIds.length; i++) {
            final TextView correspondingAnswer = tvAnswers[i];
            CardView cardQ = findViewById(questionIds[i]);
            cardQ.setOnClickListener(v -> {
                if (correspondingAnswer.getVisibility() == View.GONE) {
                    correspondingAnswer.setVisibility(View.VISIBLE);
                } else {
                    correspondingAnswer.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Đóng drawer
        drawer.closeDrawer(GravityCompat.START);

        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_dashboard) {
            intent = new Intent(this, DashBoardActivity.class);
        } else if (id == R.id.nav_schedule) {
            intent = new Intent(this, ScheduleActivity.class);
        } else if (id == R.id.nav_control) {
            intent = new Intent(this, controlActivity.class);
        } else if (id == R.id.nav_reports) {
            intent = new Intent(this, ReportsActivity.class);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (id == R.id.nav_logout) {
            intent = new Intent(this, LoginActivity.class);
        }else if (id == R.id.nav_alerts) {
            intent = new Intent(this, AlertsActivity.class);
        }

        if (intent != null) {
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
}
