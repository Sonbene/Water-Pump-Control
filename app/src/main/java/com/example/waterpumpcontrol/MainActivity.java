package com.example.waterpumpcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2. Drawer + toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 3. Listener menu
        navigationView.setNavigationItemSelectedListener(this);


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashBoardActivity.class));

        } else if (id == R.id.nav_control) {
            startActivity(new Intent(this, controlActivity.class));

        } else if (id == R.id.nav_schedule) {
            startActivity(new Intent(this, ScheduleActivity.class));

        } else if (id == R.id.nav_alerts) {
            //startActivity(new Intent(this, AlertsActivity.class));

        } else if (id == R.id.nav_reports) {
            //startActivity(new Intent(this, ReportsActivity.class));

        } else if (id == R.id.nav_settings) {
            //startActivity(new Intent(this, SettingsActivity.class));

        } else if (id == R.id.nav_help) {
            //startActivity(new Intent(this, HelpActivity.class));

        } else if (id == R.id.nav_logout) {
//            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
