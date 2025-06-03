package com.example.carcare;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.HashMap;
import java.util.HashSet; // EKLENDİ
import java.util.Map;
import java.util.Set;     // EKLENDİ

public class CarCareApplication extends Application {

    private static boolean obd2Connected = false;
    private static BluetoothManager bluetoothManager;
    private static SimpleOBD2Manager obd2Manager;
    // Cooldown için global map (Sıcaklık ve Yakıt için)
    private static Map<String, Long> globalLastCriticalAlertTimestamps = new HashMap<>();

    // Bildirilmiş DTC'leri takip etmek için global Set
    private static Set<String> notifiedDtcCodes = new HashSet<>(); // YENİ EKLENDİ
    private static boolean lowVoltageNotifiedThisSession = false;
    private static boolean highVoltageNotifiedThisSession = false;


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
        if (!connected) { // Bağlantı kesildiğinde bildirim bayraklarını sıfırla
            resetVoltageNotificationFlags();
            clearGlobalLastCriticalAlertTimestamps(); // Bunu zaten yapıyordun
            clearNotifiedDtcs(); // Bunu da
        }
    }

    public static boolean isObd2Connected() {
        return obd2Connected;
    }

    // Global OBD2 manager'ları saklama
    public static void setBluetoothManager(BluetoothManager manager) {
        bluetoothManager = manager;
    }

    public static BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public static boolean hasLowVoltageBeenNotifiedThisSession() {
        return lowVoltageNotifiedThisSession;
    }

    public static void setLowVoltageNotifiedThisSession(boolean notified) {
        lowVoltageNotifiedThisSession = notified;
    }

    public static boolean hasHighVoltageBeenNotifiedThisSession() {
        return highVoltageNotifiedThisSession;
    }

    public static void setHighVoltageNotifiedThisSession(boolean notified) {
        highVoltageNotifiedThisSession = notified;
    }

    public static void resetVoltageNotificationFlags() {
        lowVoltageNotifiedThisSession = false;
        highVoltageNotifiedThisSession = false;
        Log.d("CarCareApp", "Voltaj bildirim bayrakları sıfırlandı.");
    }
    public static void setObd2Manager(SimpleOBD2Manager manager) {
        obd2Manager = manager;
    }

    public static SimpleOBD2Manager getObd2Manager() {
        return obd2Manager;
    }

    // --- Kritik Uyarı Cooldown Metodları (Sıcaklık ve Yakıt için) ---
    public static void updateGlobalLastCriticalAlertTimestamp(String alertType, long timestamp) {
        globalLastCriticalAlertTimestamps.put(alertType, timestamp);
    }

    public static long getGlobalLastCriticalAlertTimestamp(String alertType) {
        return globalLastCriticalAlertTimestamps.getOrDefault(alertType, 0L);
    }

    public static void clearGlobalLastCriticalAlertTimestamps() {
        globalLastCriticalAlertTimestamps.clear();
    }
    // --- Kritik Uyarı Cooldown Metodları SONU ---


    // --- Bildirilmiş DTC Takip Metodları --- YENİ EKLENDİ
    public static boolean hasDtcBeenNotified(String dtcCode) {
        return notifiedDtcCodes.contains(dtcCode);
    }

    public static void addNotifiedDtc(String dtcCode) {
        notifiedDtcCodes.add(dtcCode);
    }

    public static void clearNotifiedDtcs() {
        notifiedDtcCodes.clear();
        // Opsiyonel: Eğer bir DTC bildirildiğinde 20dk cooldown da tetikleniyorsa
        // ve bu istenmiyorsa, DTC için cooldown map'inden de ilgili kaydı silebiliriz.
        // Ancak mevcut isteğinizde DTC için cooldown yok, sadece bir kerelik bildirim var.
        // globalLastCriticalAlertTimestamps.remove("NEW_DTC_DETECTED"); // Bu satır şimdilik gereksiz
    }
    // --- Bildirilmiş DTC Takip Metodları SONU ---
}