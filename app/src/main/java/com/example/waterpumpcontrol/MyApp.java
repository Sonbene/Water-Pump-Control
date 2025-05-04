package com.example.waterpumpcontrol;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences("waterpump_prefs", MODE_PRIVATE);

        // Áp dụng theme đã lưu
        switch (prefs.getInt("theme_index", 0)) {
            case 0: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES); break;
            case 2: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            case 3: AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY); break;
        }

        // Áp dụng locale đã lưu
        int idx = prefs.getInt("language_index", 0);
        String lang;
        switch(idx) {
            case 1: lang="en"; break;
            case 2: lang="zh"; break;
            case 3: lang="es"; break;
            case 4: lang="th"; break;
            default: lang="vi";
        }
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(
                config, getResources().getDisplayMetrics()
        );
    }
}
