package com.example.waterpumpcontrol;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
