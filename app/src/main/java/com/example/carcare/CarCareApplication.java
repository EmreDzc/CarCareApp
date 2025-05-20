package com.example.carcare;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class CarCareApplication extends Application {

    private static boolean obd2Connected = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Tema tercihini uygula
        applyTheme();
    }

    private void applyTheme() {
        SharedPreferences themePref = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePref.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Tema değiştirme metodu
    public static void changeTheme(boolean isDarkMode, SharedPreferences themePref) {
        SharedPreferences.Editor editor = themePref.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // OBD2 bağlantı durumunu saklama
    public static void setObd2Connected(boolean connected) {
        obd2Connected = connected;
    }

    public static boolean isObd2Connected() {
        return obd2Connected;
    }
}