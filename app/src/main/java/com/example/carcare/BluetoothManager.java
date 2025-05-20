package com.example.carcare;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID OBD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private final Context context;
    private final Handler mainHandler;

    public BluetoothManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        if (bluetoothAdapter == null) return false;

        // İzin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasBluetoothPermissions() && bluetoothAdapter.isEnabled();
        } else {
            return bluetoothAdapter.isEnabled();
        }
    }

    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (bluetoothAdapter != null) {
            // İzin kontrolü
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Bluetooth bağlantı izni gerekiyor");
                    return null;
                }
            }
            return bluetoothAdapter.getBondedDevices();
        }
        return null;
    }

    public void connectToDevice(String deviceAddress, ConnectionCallback callback) {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth cihazınız desteklenmiyor");
            callback.onConnectionFailed("Bluetooth desteklenmiyor");
            return;
        }

        // İzin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                showToast("Bluetooth bağlantı izni gerekiyor");
                callback.onConnectionFailed("İzin verilmedi");
                return;
            }
        }

        // Mevcut bağlantıyı kapat
        if (isConnected) {
            disconnect();
        }

        try {
            device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            new Thread(() -> {
                try {
                    // Soket bağlantısı oluştur
                    socket = device.createRfcommSocketToServiceRecord(OBD_UUID);

                    // İzin kontrolü
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            mainHandler.post(() -> {
                                showToast("Bluetooth bağlantı izni gerekiyor");
                                callback.onConnectionFailed("İzin verilmedi");
                            });
                            return;
                        }
                    }

                    socket.connect();

                    // Giriş ve çıkış akışlarını al
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    isConnected = true;
                    mainHandler.post(() -> callback.onConnectionSuccessful());

                    Log.d(TAG, "Bağlantı başarılı: " + deviceAddress);
                } catch (IOException e) {
                    Log.e(TAG, "Bağlantı başarısız", e);
                    mainHandler.post(() -> {
                        showToast("Bağlantı başarısız: " + e.getMessage());
                        callback.onConnectionFailed(e.getMessage());
                    });
                }
            }).start();
        } catch (IllegalArgumentException e) {
            showToast("Geçersiz Bluetooth cihaz adresi");
            callback.onConnectionFailed("Geçersiz cihaz adresi");
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        showToast("Bluetooth bağlantı izni gerekiyor");
                        return;
                    }
                }
                socket.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Bağlantı kapatılırken hata", e);
        } finally {
            socket = null;
            inputStream = null;
            outputStream = null;
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public interface ConnectionCallback {
        void onConnectionSuccessful();
        void onConnectionFailed(String reason);
    }
}
