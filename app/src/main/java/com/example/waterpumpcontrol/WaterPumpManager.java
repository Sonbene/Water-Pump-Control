package com.example.waterpumpcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class WaterPumpManager {

    private static WaterPumpManager instance;

    private int timeCheckWaterLevel = 5000;
    private int timeDelayCheckWaterLevel = 0;

    private int timeEachWarning = 30000;

    private long lastAlertTime = 0;  // Time of the last alert sent
    private float waterLevel = 0f;

    private float minLevel = 5f;

    private float maxLevel = 20f;

    private int waterPWM = 0;
    private boolean isAlarmEnabled = true;
    private boolean isVibrationEnabled = true;
    private float threshold = 20.0f; // Example threshold for water level

    private static final String DB_URL      = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER     = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";
    private static final String DB_TABLE    = "waterpumpcontrolalerts";

    private MqttHandler mqtt;
    private static final String BROKER = "ssl://d3fd0fd59ed14b6d9fe037c0ef1bf662.s1.eu.hivemq.cloud:8883";
    private static final String USER = "sonbe";
    private static final String PASS = "Son@1234";
    private static final String TOPIC_LEVEL = "level";
    private static final String TOPIC_CONTROL = "control";
    private static final String TOPIC_THRESHOLD = "threshold";
    private static final String TOPIC_AUTO = "auto";
    private static final String TOPIC_PUMPSPEED = "pumpspeed";

    // ở đầu class, cùng chỗ bạn khai báo TOPIC_…
    private static final String CHANNEL_ID_SOUND     = "WaterPumpChannelWithSound";
    private static final String CHANNEL_ID_VIBRATE   = "WaterPumpChannelVibrateOnly";



    private long lastMqttMessageTime = 0; // Thời gian nhận thông điệp MQTT cuối cùng
    private boolean isMqttDisconnected = true; // Kiểm tra trạng thái MQTT (có bị mất kết nối không)

    private boolean firstTimeReceiveFromMQTT = true;


    private WaterPumpManager() {
        // Initialize MQTT Handler here or other services
    }

    // Singleton Pattern
    public static WaterPumpManager getInstance() {
        if (instance == null) {
            instance = new WaterPumpManager();
        }
        return instance;
    }

    // Getter methods
    public static String getTopicControl() {
        return TOPIC_CONTROL;
    }

    public static String getTopicThreshold() {
        return TOPIC_THRESHOLD;
    }

    public static String getTopicAuto() {
        return TOPIC_AUTO;
    }

    public static String getTopicPumpSpeed() {
        return TOPIC_PUMPSPEED;
    }

    // Getters and Setters for Variables
    public int getTimeCheckWaterLevel() {
        return timeCheckWaterLevel;
    }

    public void setTimeCheckWaterLevel(int timeCheckWaterLevel) {
        this.timeCheckWaterLevel = timeCheckWaterLevel;
    }

    public int getTimeDelayCheckWaterLevel() {
        return timeDelayCheckWaterLevel;
    }

    public void setTimeDelayCheckWaterLevel(int timeDelayCheckWaterLevel) {
        this.timeDelayCheckWaterLevel = timeDelayCheckWaterLevel;
    }

    public float getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(float waterLevel) {
        this.waterLevel = waterLevel;
    }

    public int getWaterPWM() {
        return waterPWM;
    }

    public void setWaterPWM(int waterPWM) {
        this.waterPWM = waterPWM;
    }

    public boolean isAlarmEnabled() {
        return isAlarmEnabled;
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        this.isAlarmEnabled = alarmEnabled;
    }

    public boolean isVibrationEnabled() {
        return isVibrationEnabled;
    }

    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.isVibrationEnabled = vibrationEnabled;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public void setMinLevel(float min)
    {
        this.minLevel = min;
    }

    public float getMinLevel()
    {
        return this.minLevel;
    }

    public void setMaxLevel(float max)
    {
        this.maxLevel = max;
    }

    public float getMaxLevel()
    {
        return this.maxLevel;
    }

    // Getter cho timeEachWarning
    public int getTimeEachWarning() {
        return timeEachWarning;
    }

    // Setter cho timeEachWarning
    public void setTimeEachWarning(int timeEachWarning) {
        this.timeEachWarning = timeEachWarning;
    }

    public void checkMqttConnection(Context context) {
        long currentTime = System.currentTimeMillis();

        // Kiểm tra nếu đã 10 giây (10000ms) không nhận được dữ liệu từ MQTT
        if (currentTime - lastMqttMessageTime >= 10000 && !isMqttDisconnected) {
            isMqttDisconnected = true;  // Đánh dấu mất kết nối
            sendNotification(context,"Warning: No data received from MQTT!", "No data received in the last 10 seconds.");
            sendAlertToDb("critical", "No data received from MQTT for 10 seconds");  // Gửi cảnh báo lên DB
        }

        // Kiểm tra nếu MQTT đã kết nối lại sau khi mất kết nối
        if (isMqttDisconnected && currentTime - lastMqttMessageTime < 10000) {
            isMqttDisconnected = false;  // Đánh dấu đã kết nối lại
            sendNotification(context, "Info: MQTT reconnected", "Data is now being received.");
            sendAlertToDb("info", "MQTT reconnected after a disconnection");  // Gửi thông báo lên DB
        }
    }


    // Method to check water level and send notifications if needed
    public void checkWaterLevelAndSendNotification(Context context) {
        // Get current time in milliseconds
        long currentTime = System.currentTimeMillis();

        // Ensure there is at least 5 minutes (300,000ms) between notifications
        if (currentTime - lastAlertTime >= getTimeEachWarning()) {  // 5 minutes
            if (waterLevel < minLevel) {
                sendNotification(context,"Warning: Water level too low!", "Current water level: " + waterLevel + " cm");
                sendAlertToDb("warning", "Water level is too low: " + waterLevel + " cm");  // Send to DB
                lastAlertTime = currentTime;  // Update last alert time
                setTimeDelayCheckWaterLevel(getTimeEachWarning());  // Delay for 5 minutes
            } else if (waterLevel > maxLevel) {
                sendNotification(context, "Warning: Water level too high!", "Current water level: " + waterLevel + " cm");
                sendAlertToDb("critical", "Water level is too high: "+ waterLevel + " cm");  // Send to DB
                lastAlertTime = currentTime;  // Update last alert time
                setTimeDelayCheckWaterLevel(getTimeEachWarning());  // Delay for 5 minutes
            } else {
                setTimeDelayCheckWaterLevel(5000);  // Normal check interval
            }
        } else {
            // No alert, check again in 5 seconds
            setTimeDelayCheckWaterLevel(5000);  // Normal check interval
        }
    }

    // Method to send notification
//    @SuppressLint("MissingPermission")
//    public void sendNotification(Context context, String title, String message) {
//        try {
//            //Context context = App.getAppContext(); // Use global context
//            if (context == null) {
//                Log.e("NotificationError", "Context is null, cannot send notification.");
//                return;
//            }
//
//            // Create an Intent to open the activity when the notification is clicked
//            Intent intent = new Intent(context, LoginActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//            // Create PendingIntent from the Intent
//            PendingIntent pendingIntent = PendingIntent.getActivity(
//                    context,
//                    0,
//                    intent,
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//            // Create notification
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "WaterPumpChannel")
//                    .setSmallIcon(R.drawable.ic_alert)
//                    .setContentTitle(title)
//                    .setContentText(message)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH)
//                    .setAutoCancel(true)
//                    .setContentIntent(pendingIntent);
//
//            // Send notification
//            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//            notificationManager.notify(1, builder.build());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("NotificationError", "Error sending notification: " + e.getMessage());
//        }
//    }

    @SuppressLint("MissingPermission")
    public void sendNotification(Context context, String title, String message) {
        if(isAlarmEnabled()) {
            if (context == null) return;
            createNotificationChannel(context);

            Intent intent = new Intent(context, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Nếu isVibrationEnabled = true → chọn channel chỉ rung
            String channelId = isVibrationEnabled()
                    ? CHANNEL_ID_SOUND
                    : CHANNEL_ID_VIBRATE;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_alert)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

            NotificationManagerCompat.from(context)
                    .notify(1, builder.build());
        }
    }





    public void checkPostNotificationPermission(Context context)
    {
        // Kiểm tra quyền POST_NOTIFICATIONS nếu là Android 13 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Nếu chưa có quyền POST_NOTIFICATIONS, yêu cầu quyền
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void sendAlertToDb(String severity, String message) {
        new Thread(() -> {
            try {
                // Create the SQL connection and insert query
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                String query = "INSERT INTO " + DB_TABLE + " (severity, message, timestamp, status) " +
                        "VALUES (?, ?, NOW(), 'active')";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, severity);
                    stmt.setString(2, message);
                    stmt.executeUpdate();
                }

                conn.close();
            } catch (Exception e) {
                Log.e("DB_ERROR", "Error inserting alert into DB", e);
            }
        }).start();
    }

    // Method to create notification channel (for Android Oreo and above)
//    public void createNotificationChannel(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "WaterPump Alerts";
//            String description = "Channel for water pump alerts";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//            NotificationChannel channel = new NotificationChannel("WaterPumpChannel", name, importance);
//            channel.setDescription(description);
//
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }

    public void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager mgr = context.getSystemService(NotificationManager.class);

        // 1) Channel có âm thanh + rung
        NotificationChannel chSound = new NotificationChannel(
                CHANNEL_ID_SOUND,
                "Water Pump Alerts (Sound)",
                NotificationManager.IMPORTANCE_HIGH
        );
        chSound.setDescription("Alerts with sound and vibration");
        chSound.enableVibration(true);

        // 2) Channel chỉ rung, không có âm thanh
        NotificationChannel chVibrate = new NotificationChannel(
                CHANNEL_ID_VIBRATE,
                "Water Pump Alerts (Vibrate Only)",
                NotificationManager.IMPORTANCE_HIGH
        );
        chVibrate.setDescription("Alerts with vibration only");
        chVibrate.setSound(null, null);
        chVibrate.enableVibration(true);
        // (tùy chọn) bạn có thể set pattern:
        // chVibrate.setVibrationPattern(new long[]{0, 500, 200, 500});

        mgr.createNotificationChannel(chSound);
        mgr.createNotificationChannel(chVibrate);
    }



    // Method to handle MQTT message and update water level and PWM
    public void handleMqttMessage(Context context, String topic, MqttMessage message) {
        if (topic.equals(TOPIC_LEVEL)) {
            String[] data = new String(message.getPayload()).trim().split("\\s+");
            float level = Float.parseFloat(data[0].replace(',', '.'));
            int pwm = Integer.parseInt(data[1]);
            setWaterLevel(level);
            setWaterPWM(pwm);
            checkWaterLevelAndSendNotification(context);
        }
    }

    // Method to initialize MQTT connection
    public void initMqtt(Context context) {
        if(mqtt != null)
        {
            if(mqtt.isConnected())
            {
                return;
            }
        }
        mqtt = new MqttHandler();
        mqtt.connect(context, BROKER, USER, PASS);
        mqtt.subscribe(TOPIC_LEVEL);
        mqtt.setCallback(new MqttCallback() {
            @Override public void connectionLost(Throwable c) {
                Toast.makeText(context, "MQTT connection lost", Toast.LENGTH_SHORT).show();
            }
            @Override public void deliveryComplete(IMqttDeliveryToken t) {}
            @Override public void messageArrived(String topic, MqttMessage m) {
                if (TOPIC_LEVEL.equals(topic))
                {
                    if(firstTimeReceiveFromMQTT == true)
                    {
                        firstTimeReceiveFromMQTT = false;
                    }
                    else
                    {
                        String[] p = new String(m.getPayload()).trim().split("\\s+");
                        float x = Float.parseFloat(p[0].replace(',','.'));
                        int   y = Integer.parseInt(p[1]);
                        waterLevel = x;
                        waterPWM= y ;

                        setWaterLevel(waterLevel);
                        setWaterPWM(waterPWM);
                        lastMqttMessageTime = System.currentTimeMillis();  // Cập nhật thời gian nhận dữ liệu

                        checkWaterLevelAndSendNotification(context);
                    }


                }
                else{

                }

            }
        });
    }

    public void MQTT_Publish(String topic, String message)
    {
        mqtt.publish(topic, message);
    }

    // Method to initialize MQTT callback
    public void setMqttCallback(MqttCallback callback) {
        mqtt.setCallback(callback);
    }
}
