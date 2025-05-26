package com.example.carcare;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "maintenance_channel";
    private static final String TAG = "MainActivity";


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Oturum kontrolü
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // ÇÖZÜM: CarActivity'ye yönlendir!
        Intent intent = new Intent(MainActivity.this, CarActivity.class);
        startActivity(intent);
        finish(); // MainActivity'yi kapat

        // Geri kalan kodlar CarActivity'ye taşındığı için gerek yok
    }
}