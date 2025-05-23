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

        // Sadece bir kez setContentView çağrısı yap
        setContentView(R.layout.activity_car);

        // Bildirim kanalı oluştur
        createNotificationChannel();

        // İlk giriş kontrolü yap
        checkFirstTimeLogin();

        // Tüm bakım hatırlatmalarını planla
        MaintenanceScheduler scheduler = new MaintenanceScheduler(this);
        scheduler.scheduleAllMaintenance();

        // Alt navigasyon barını ayarla
        setupBottomNavigation();
    }

    /**
     * Alt navigasyon barını ayarlar
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                return true; // Zaten bu sayfadayız
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(MainActivity.this, StoreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(MainActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    /**
     * İlk giriş kontrolü yapar ve hoş geldiniz bildirimi ekler
     */
    private void checkFirstTimeLogin() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Kullanıcıya özel bir SharedPreferences anahtarı kullan
        SharedPreferences prefs = getSharedPreferences("AppPrefs_" + userId, MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        if (isFirstTime) {
            // Hoş geldiniz bildirimi göster
            NotificationHelper.showNotification(
                    this,
                    "CarCare+",
                    "CarCare+ uygulamamıza hoş geldiniz. Keyifli kullanımlar dileriz.",
                    99
            );

            // Firestore'a kaydet
            NotificationHelper.saveNotificationToFirestore(
                    "CarCare+",
                    "CarCare+ uygulamamıza hoş geldiniz. Keyifli kullanımlar dileriz."
            );

            // Artık ilk kez değil olarak işaretle
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isFirstTime", false);
            editor.apply();

            Log.d(TAG, "Kullanıcı " + userId + " için ilk giriş bildirimi gösterildi");
        }
    }

    /**
     * Android Oreo ve sonrası için bildirim kanalı oluşturur
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Maintenance Channel";
            String description = "Channel for maintenance reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Bildirim kanalı oluşturuldu");
            }
        }
    }
}