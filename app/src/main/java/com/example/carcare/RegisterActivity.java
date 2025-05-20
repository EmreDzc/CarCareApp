package com.example.carcare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Tema tercihini kontrol et ve uygula - aktivite oluşturulmadan önce yapılmalı
        SharedPreferences themePref = newBase.getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePref.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.attachBaseContext(newBase);
    }

    private EditText editEmail, editPassword, editConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Yeni tasarım XML'in adı

        // View'leri tanımla
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        auth = FirebaseAuth.getInstance();

        // "REGISTER" butonuna tıklayınca kayıt işlemi
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();
                String confirmPassword = editConfirmPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Şifreler uyuşmuyor.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // FirebaseAuth ile kullanıcı kaydı
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            Toast.makeText(RegisterActivity.this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show();
                            // Kayıt başarılı ise MainActivity'ye geç
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Kayıt başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // "Already have an account? Sign In" metnine tıklayınca LoginActivity'ye dön
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
