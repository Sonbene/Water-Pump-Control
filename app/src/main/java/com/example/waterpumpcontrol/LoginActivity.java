package com.example.waterpumpcontrol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvErrorMessage;
    private Button btnLogin;

    private static final String DB_URL = "jdbc:mysql://pvl.vn:3306/admin_db";
    private static final String DB_USER = "raspberry";
    private static final String DB_PASSWORD = "admin6789@";

    private WaterPumpManager waterPumpManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnLogin = findViewById(R.id.btnLogin);


        // Đăng ký sự kiện khi người dùng nhấn nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                tvErrorMessage.setText("Vui lòng nhập đầy đủ thông tin!");
                tvErrorMessage.setVisibility(View.VISIBLE);
            } else {
                loginUser(username, password);
            }
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
                waterPumpManager.checkMqttConnection(LoginActivity.this);

                // Lặp lại sau 10 giây (10,000ms)
                handler1.postDelayed(this, 10000); // Delay 10s
            }
        };

        // Bắt đầu kiểm tra ngay lập tức khi ứng dụng chạy
        handler1.post(checkMqttConnectionTask);

        CardView cardView = findViewById(R.id.cardView);

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    // Ẩn bàn phím
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);

                    currentFocus.clearFocus();
                }
            }
        });

    }



    private void loginUser(String username, String password) {
        new Thread(() -> {
            try {
                // Kết nối đến cơ sở dữ liệu MariaDB
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    String query = "SELECT * FROM waterpumpcontrolusers WHERE username = ? AND password = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, username);
                    ps.setString(2, password);  // Lưu ý rằng mật khẩu chưa được mã hóa trong ví dụ này

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        // Đăng nhập thành công
                        runOnUiThread(() -> {
                            startActivity(new Intent(LoginActivity.this, DashBoardActivity.class));
                            finish();
                        });
                    } else {
                        // Đăng nhập thất bại
                        runOnUiThread(() -> {
                            tvErrorMessage.setText("Sai tên đăng nhập hoặc mật khẩu!");
                            tvErrorMessage.setVisibility(View.VISIBLE);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("DB_ERROR", "Error logging in", e);
            }
        }).start();
    }
}
