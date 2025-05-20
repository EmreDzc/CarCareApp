package com.example.carcare;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleOBD2Manager {
    private static final String TAG = "SimpleOBD2Manager";

    // OBD2 PID'leri (verilerin kimlikleri)
    private static final String COOLANT_TEMP_PID = "0105";
    private static final String ENGINE_RPM_PID = "010C";
    private static final String VEHICLE_SPEED_PID = "010D";
    private static final String FUEL_LEVEL_PID = "012F";
    private static final String OIL_TEMP_PID = "015C";

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final ScheduledExecutorService scheduler;
    private final Handler mainHandler;
    private final VehicleData vehicleData;
    private DataUpdateListener dataUpdateListener;
    private boolean isReading = false;

    public SimpleOBD2Manager(Context context, BluetoothManager bluetoothManager) {
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.vehicleData = new VehicleData();
    }

    public void startReading() {
        if (!bluetoothManager.isConnected()) {
            showToast("OBD2 cihazına bağlı değil!");
            return;
        }

        if (isReading) {
            return; // Zaten okuma yapılıyorsa tekrar başlatma
        }

        isReading = true;

        // Protokol başlatma ve veri okuma işlemini ayrı bir thread'de yap
        new Thread(() -> {
            try {
                // İlk önce OBD2 protokolünü başlat
                initializeOBD2Protocol();

                // Veri okuma işlemini 2 saniyede bir çalıştır
                scheduler.scheduleAtFixedRate(this::readVehicleData, 0, 2, TimeUnit.SECONDS);

            } catch (Exception e) {
                mainHandler.post(() -> {
                    isReading = false;
                    showToast("OBD2 başlatma hatası: " + e.getMessage());
                    Log.e(TAG, "OBD2 başlatma hatası", e);
                });
            }
        }).start();
    }

    private void initializeOBD2Protocol() throws IOException {
        InputStream in = bluetoothManager.getInputStream();
        OutputStream out = bluetoothManager.getOutputStream();

        if (in == null || out == null) {
            throw new IOException("Bluetooth giriş/çıkış akışları bulunamadı");
        }

        // Protokol başlatma komutları
        String[] initCommands = {
                "ATZ",       // Cihazı resetle
                "ATL0",      // Line feed kapalı
                "ATE0",      // Echo kapalı
                "ATH0",      // Headers kapalı
                "ATS0",      // Space kapalı
                "ATSP0"      // Otomatik protokol seçimi
        };

        // Her komut için
        for (String cmd : initCommands) {
            // Komutu gönder
            sendCommand(cmd, out);

            // Cevabı oku (ancak bu aşamada önemli değil)
            readResponse(in);

            // Komutlar arasında kısa bir süre bekle
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Log.d(TAG, "OBD2 protokolü başarıyla başlatıldı");
    }

    private void readVehicleData() {
        try {
            InputStream in = bluetoothManager.getInputStream();
            OutputStream out = bluetoothManager.getOutputStream();

            if (in == null || out == null || !bluetoothManager.isConnected()) {
                stopReading();
                return;
            }

            // Motor soğutma suyu sıcaklığı
            try {
                sendCommand(COOLANT_TEMP_PID, out);
                String response = readResponse(in);
                double coolantTemp = parseTemperature(response);
                vehicleData.setEngineTemp(coolantTemp);
            } catch (Exception e) {
                Log.e(TAG, "Motor sıcaklığı okunamadı", e);
            }

            // Motor RPM
            try {
                sendCommand(ENGINE_RPM_PID, out);
                String response = readResponse(in);
                double rpm = parseRPM(response);
                vehicleData.setRpm(rpm);
            } catch (Exception e) {
                Log.e(TAG, "Motor RPM okunamadı", e);
            }

            // Araç hızı
            try {
                sendCommand(VEHICLE_SPEED_PID, out);
                String response = readResponse(in);
                double speed = parseSpeed(response);
                vehicleData.setSpeed(speed);
            } catch (Exception e) {
                Log.e(TAG, "Araç hızı okunamadı", e);
            }

            // Yakıt seviyesi
            try {
                sendCommand(FUEL_LEVEL_PID, out);
                String response = readResponse(in);
                double fuelLevel = parseFuelLevel(response);
                vehicleData.setFuelLevel(fuelLevel);
            } catch (Exception e) {
                Log.e(TAG, "Yakıt seviyesi okunamadı", e);
            }

            // Yağ sıcaklığı
            try {
                sendCommand(OIL_TEMP_PID, out);
                String response = readResponse(in);
                double oilTemp = parseTemperature(response);
                vehicleData.setOilTemp(oilTemp);
            } catch (Exception e) {
                Log.e(TAG, "Yağ sıcaklığı okunamadı", e);
            }

            // Lastik basıncını simüle et (OBD2 ile doğrudan okunamaz)
            vehicleData.setTirePressureFL(32);
            vehicleData.setTirePressureFR(31);
            vehicleData.setTirePressureRL(30);
            vehicleData.setTirePressureRR(29);

            // Dinleyiciyi bilgilendir
            if (dataUpdateListener != null) {
                mainHandler.post(() -> dataUpdateListener.onDataUpdate(vehicleData));
            }

        } catch (Exception e) {
            Log.e(TAG, "Veri okuma hatası", e);
            stopReading();
        }
    }

    private void sendCommand(String command, OutputStream out) throws IOException {
        // Gönder ve sonuna CR ekle
        String cmdWithCR = command + "\r";
        out.write(cmdWithCR.getBytes());
        out.flush();
    }

    private String readResponse(InputStream in) throws IOException {
        StringBuilder res = new StringBuilder();
        byte b;
        char c;

        // Cevabı tamamen okumak için bekle
        while (true) {
            b = (byte) in.read();
            c = (char) b;

            if (c == '>') {
                break; // OBD prompt karakteri, cevap bitti
            }

            if (c != '\r' && c != '\n' && b != -1) {
                res.append(c);
            }

            // Okuma işlemi için kısa bir süre bekle
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return res.toString().trim();
    }

    // Sıcaklık değerini parse et (°C cinsinden)
    private double parseTemperature(String response) {
        // Cevabı ayrıştır, formata göre değişebilir
        // Tipik format: "41 05 7B" -> 7B = 123 -> 123-40 = 83°C
        try {
            // Boşlukları kaldır ve sadece veri kısmını al
            String[] parts = response.split(" ");
            if (parts.length >= 3) {
                int temp = Integer.parseInt(parts[2], 16);
                return temp - 40; // OBD2 protokolü: Sıcaklık = değer - 40
            }
        } catch (Exception e) {
            Log.e(TAG, "Sıcaklık ayrıştırma hatası: " + response, e);
        }
        return 0;
    }

    // RPM değerini parse et
    private double parseRPM(String response) {
        // Tipik format: "41 0C 1A F8" -> RPM = ((1A * 256) + F8) / 4
        try {
            String[] parts = response.split(" ");
            if (parts.length >= 4) {
                int a = Integer.parseInt(parts[2], 16);
                int b = Integer.parseInt(parts[3], 16);
                return ((a * 256) + b) / 4.0;
            }
        } catch (Exception e) {
            Log.e(TAG, "RPM ayrıştırma hatası: " + response, e);
        }
        return 0;
    }

    // Araç hızını parse et (km/h)
    private double parseSpeed(String response) {
        // Tipik format: "41 0D 32" -> 32 hex = 50 km/h
        try {
            String[] parts = response.split(" ");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2], 16);
            }
        } catch (Exception e) {
            Log.e(TAG, "Hız ayrıştırma hatası: " + response, e);
        }
        return 0;
    }

    // Yakıt seviyesini parse et (%)
    private double parseFuelLevel(String response) {
        // Tipik format: "41 2F 64" -> 64 hex = 100 -> 100 * 100/255 = 39.2%
        try {
            String[] parts = response.split(" ");
            if (parts.length >= 3) {
                int value = Integer.parseInt(parts[2], 16);
                return value * 100.0 / 255.0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Yakıt seviyesi ayrıştırma hatası: " + response, e);
        }
        return 0;
    }

    public void stopReading() {
        isReading = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Tema değişimi sırasında Toast gösterme
        if (!CarCareApplication.isObd2Connected()) {
            mainHandler.post(() -> showToast("OBD2 veri okuma durduruldu"));
        }
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public interface DataUpdateListener {
        void onDataUpdate(VehicleData data);
    }

    // Araç verilerini tutan sınıf
    public static class VehicleData {
        private double speed = 0; // km/h
        private double rpm = 0;
        private double engineTemp = 0; // Celsius
        private double fuelLevel = 0; // %
        private double oilTemp = 0; // Celsius

        // Lastik basıncı OBD2 üzerinden doğrudan okunamaz, TPMS gerekir
        private double tirePressureFL = 0; // PSI
        private double tirePressureFR = 0; // PSI
        private double tirePressureRL = 0; // PSI
        private double tirePressureRR = 0; // PSI

        // Örnek değerler ekle
        public void setDefaultValues() {
            speed = 0;
            rpm = 0;
            engineTemp = 90;
            fuelLevel = 75;
            oilTemp = 85;
            tirePressureFL = 32;
            tirePressureFR = 31;
            tirePressureRL = 30;
            tirePressureRR = 29;
        }

        // Getter ve Setter metodları
        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public double getRpm() {
            return rpm;
        }

        public void setRpm(double rpm) {
            this.rpm = rpm;
        }

        public double getEngineTemp() {
            return engineTemp;
        }

        public void setEngineTemp(double engineTemp) {
            this.engineTemp = engineTemp;
        }

        public double getFuelLevel() {
            return fuelLevel;
        }

        public void setFuelLevel(double fuelLevel) {
            this.fuelLevel = fuelLevel;
        }

        public double getOilTemp() {
            return oilTemp;
        }

        public void setOilTemp(double oilTemp) {
            this.oilTemp = oilTemp;
        }

        public double getTirePressureFL() {
            return tirePressureFL;
        }

        public void setTirePressureFL(double tirePressureFL) {
            this.tirePressureFL = tirePressureFL;
        }

        public double getTirePressureFR() {
            return tirePressureFR;
        }

        public void setTirePressureFR(double tirePressureFR) {
            this.tirePressureFR = tirePressureFR;
        }

        public double getTirePressureRL() {
            return tirePressureRL;
        }

        public void setTirePressureRL(double tirePressureRL) {
            this.tirePressureRL = tirePressureRL;
        }

        public double getTirePressureRR() {
            return tirePressureRR;
        }

        public void setTirePressureRR(double tirePressureRR) {
            this.tirePressureRR = tirePressureRR;
        }
    }
}