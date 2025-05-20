package com.example.carcare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StoreActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_store);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_store); // Başlangıçta Store seçili olsun

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(StoreActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(StoreActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(StoreActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(StoreActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}
