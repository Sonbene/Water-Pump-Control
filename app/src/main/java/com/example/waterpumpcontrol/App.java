package com.example.waterpumpcontrol;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();  // Lưu lại context toàn cục
    }

    public static Context getAppContext() {
        return context;  // Trả về context toàn cục
    }
}
