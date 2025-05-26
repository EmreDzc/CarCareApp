package com.example.carcare;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.TextView;

import java.util.Set;

public class CarActivity extends AppCompatActivity {
    private static final String TAG = "CarActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;

    // UI Elements - GÜNCELLENMIŞ İSİMLER
    private TextView tvWelcomeUser, tvCarName, tvCarYear;

    // Ana veriler
    private TextView tvSpeedValue, tvRpmValue, tvEngineTempValue, tvFuelValue;

    // Motor performans verileri
    private TextView tvEngineLoadValue, tvThrottleValue;
    private TextView tvIntakeAirTempValue, tvMafValue;

    // Butonlar
    private MaterialButton btnOpenSite, btnTrafficFineInquiry, btnMotorVehicleFineInquiry;
    private MaterialButton btnCarDetails, btnRefuel;
    private FloatingActionButton fabConnectOBD;

    // Bluetooth ve OBD2 nesneleri
    private BluetoothManager bluetoothManager;
    private SimpleOBD2Manager obd2Manager;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        // Toolbar'ı action bar olarak ayarla
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI elemanlarını initialize et
        initializeViews();

        // Alt navigasyonu ayarla
        setupBottomNavigation();

        // Kullanıcı bilgilerini Firebase'den yükle
        loadUserAndCarData();

        // Bluetooth ve OBD2 nesnelerini oluştur veya al
        setupBluetoothAndOBD();

        // Veri güncelleme dinleyicisini ayarla
        setupDataUpdateListener();

        // Bluetooth izinlerini kontrol et
        checkBluetoothPermissions();

        // Varsayılan değerleri göster
        showDefaultValues();

        // Buton tıklama olaylarını ayarla
        setupButtonListeners();

        // Bağlantı durumunu kontrol et ve arayüzü güncelle
        updateConnectionStatus();
        // Bakım hatırlatmalarını planla
        setupMaintenanceScheduler();

// Hoş geldiniz mesajını kontrol et
        setupWelcomeNotification();
    }

    private void initializeViews() {
        // Kullanıcı ve araç bilgileri
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvCarName = findViewById(R.id.tvCarName);
        tvCarYear = findViewById(R.id.tvCarYear);

        // Ana veriler - YENİ İSİMLER
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvRpmValue = findViewById(R.id.tvRpmValue);
        tvEngineTempValue = findViewById(R.id.tvEngineTempValue);
        tvFuelValue = findViewById(R.id.tvFuelValue);

        // Motor performans verileri - YENİ
        tvEngineLoadValue = findViewById(R.id.tvEngineLoadValue);
        tvThrottleValue = findViewById(R.id.tvThrottleValue);
        tvIntakeAirTempValue = findViewById(R.id.tvIntakeAirTempValue);
        tvMafValue = findViewById(R.id.tvMafValue);

        // Butonları bağla
        btnOpenSite = findViewById(R.id.btnOpenSite);
        btnTrafficFineInquiry = findViewById(R.id.btnTrafficFineInquiry);
        btnMotorVehicleFineInquiry = findViewById(R.id.btnMotorVehicleFineInquiry);
        btnCarDetails = findViewById(R.id.btnCarDetails);
        btnRefuel = findViewById(R.id.btnRefuel);

        // FloatingActionButton'u bağla
        fabConnectOBD = findViewById(R.id.fabConnectOBD);
        fabConnectOBD.setOnClickListener(v -> connectToOBD());

        Log.d(TAG, "UI elemanları başarıyla bağlandı");
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_dashboard);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(this, StoreActivity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapsActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void setupBluetoothAndOBD() {
        // Bluetooth ve OBD2 nesnelerini oluştur veya global'dan al
        if (CarCareApplication.getBluetoothManager() == null) {
            bluetoothManager = new BluetoothManager(this);
            CarCareApplication.setBluetoothManager(bluetoothManager);
            Log.d(TAG, "Yeni BluetoothManager oluşturuldu");
        } else {
            bluetoothManager = CarCareApplication.getBluetoothManager();
            Log.d(TAG, "Mevcut BluetoothManager kullanılıyor");
        }

        if (CarCareApplication.getObd2Manager() == null) {
            obd2Manager = new SimpleOBD2Manager(this, bluetoothManager);
            CarCareApplication.setObd2Manager(obd2Manager);
            Log.d(TAG, "Yeni OBD2Manager oluşturuldu");
        } else {
            obd2Manager = CarCareApplication.getObd2Manager();
            Log.d(TAG, "Mevcut OBD2Manager kullanılıyor");
        }
    }

    private void setupDataUpdateListener() {
        // Veri güncelleme dinleyicisini ayarla
        obd2Manager.setDataUpdateListener(new SimpleOBD2Manager.DataUpdateListener() {
            @Override
            public void onDataUpdate(SimpleOBD2Manager.VehicleData data) {
                Log.d(TAG, "Veri güncellendi - Hız: " + data.getSpeed() + ", RPM: " + data.getRpm());
                updateUI(data);
            }

            @Override
            public void onConnectionLost() {
                // Bağlantı kesildiğinde yapılacak işlemler
                runOnUiThread(() -> {
                    isConnected = false;
                    CarCareApplication.setObd2Connected(false);
                    fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add); // Bağlan ikonu
                    showDefaultValues(); // Varsayılan değerleri göster
                    Toast.makeText(CarActivity.this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "OBD2 bağlantısı kesildi");
                });
            }
        });
    }

    private void setupButtonListeners() {
        // SCHEDULE MAINTENANCE - TÜVTÜRK
        btnOpenSite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tuvturk.com.tr"));
            startActivity(intent);
        });

        // TRAFFIC FINE INQUIRY - GİB Dijital
        btnTrafficFineInquiry.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dijital.gib.gov.tr/hizliOdemeler/MTVTPCOdeme"));
            startActivity(intent);
        });

        // MOTOR VEHICLE FINE INQUIRY - GİB Dijital (aynı link)
        btnMotorVehicleFineInquiry.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://dijital.gib.gov.tr/hizliOdemeler/MTVTPCOdeme"));
            startActivity(intent);
        });

        btnCarDetails.setOnClickListener(v -> {
            Toast.makeText(this, "Car details feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // YENİ - Yakıt istasyonu bulucu
        btnRefuel.setOnClickListener(v -> {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=gas+station+near+me"));
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
                Log.d(TAG, "Google Maps ile yakıt istasyonu aranıyor");
            } catch (Exception e) {
                // Google Maps yoksa web'de ara
                Intent webIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/gas+station+near+me"));
                startActivity(webIntent);
                Log.d(TAG, "Web'de yakıt istasyonu aranıyor");
            }
        });
    }

    private void setupMaintenanceScheduler() {
        MaintenanceScheduler scheduler = new MaintenanceScheduler(this);
        scheduler.scheduleAllMaintenance();
        Log.d(TAG, "Tüm bakım hatırlatmaları planlandı");
    }

    private void setupWelcomeNotification() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String lastUserId = prefs.getString("last_user_id", "");

            if (!currentUser.getUid().equals(lastUserId)) {
                // Yeni kullanıcı - hoş geldiniz mesajı gönder
                NotificationActivity.FirebaseNotificationManager notifManager =
                        new NotificationActivity.FirebaseNotificationManager();

                notifManager.addWelcomeNotification(new NotificationActivity.FirebaseNotificationManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Hoş geldiniz mesajı gönderildi");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Hoş geldiniz mesajı gönderilemedi", e);
                    }
                });

                prefs.edit().putString("last_user_id", currentUser.getUid()).apply();
            }
        }
    }

    private void loadUserAndCarData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Kullanıcı adını göster
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvWelcomeUser.setText(displayName);
            } else {
                tvWelcomeUser.setText("Car Owner");
            }

            // Araç bilgilerini Firestore'dan yükle
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Kullanıcı belgesinden araç bilgilerini al
                            String carName = documentSnapshot.getString("carName");
                            String carYear = documentSnapshot.getString("carYear");

                            // Eğer bilgiler varsa, UI'ı güncelle
                            if (carName != null && !carName.isEmpty()) {
                                tvCarName.setText(carName);
                            }

                            if (carYear != null && !carYear.isEmpty()) {
                                tvCarYear.setText(carYear + " Model");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Hata durumunda varsayılan değerler kullan
                        tvCarName.setText("Opel Astra");
                        tvCarYear.setText("2017 Model");
                        Log.e(TAG, "Firestore'dan veri yüklenirken hata", e);
                    });
        } else {
            // Kullanıcı giriş yapmamışsa, varsayılan değerler
            tvWelcomeUser.setText("Guest");
            tvCarName.setText("Opel Astra");
            tvCarYear.setText("2017 Model");
        }
    }

    private void showDefaultValues() {
        // Varsayılan değerleri göster
        SimpleOBD2Manager.VehicleData defaultData = new SimpleOBD2Manager.VehicleData();
        updateUI(defaultData);
        Log.d(TAG, "Varsayılan değerler gösteriliyor");
    }

    private void updateUI(SimpleOBD2Manager.VehicleData data) {
        // UI'ı güncelle (ana thread üzerinde)
        runOnUiThread(() -> {
            try {
                // ANA VERILER (Her zaman gelecek)
                tvSpeedValue.setText(String.format("%.0f", data.getSpeed()));
                tvRpmValue.setText(String.format("%.0f", data.getRpm()));
                tvEngineTempValue.setText(String.format("%.0f", data.getEngineTemp()));

                // YAKIT SEVİYESİ (Gelmeyebilir)
                if (data.getFuelLevel() != null && data.getFuelLevel() > 0) {
                    tvFuelValue.setText(String.format("%.0f%%", data.getFuelLevel()));
                } else {
                    tvFuelValue.setText("N/A");
                }

                // MOTOR PERFORMANS VERİLERİ
                tvEngineLoadValue.setText(String.format("%.0f%%", data.getEngineLoad()));
                tvThrottleValue.setText(String.format("%.0f%%", data.getThrottlePosition()));

                // HAVA SICAKLIĞI VE AKIŞI (Gelmeyebilir)
                if (data.getIntakeTemp() != null && data.getIntakeTemp() > -30) {
                    tvIntakeAirTempValue.setText(String.format("%.0f°C", data.getIntakeTemp()));
                } else {
                    tvIntakeAirTempValue.setText("N/A");
                }

                if (data.getMafAirFlow() != null && data.getMafAirFlow() > 0) {
                    tvMafValue.setText(String.format("%.1f g/s", data.getMafAirFlow()));
                } else {
                    tvMafValue.setText("N/A");
                }

                Log.d(TAG, "UI güncellendi - Hız: " + data.getSpeed() +
                        " km/h, RPM: " + data.getRpm() +
                        ", Sıcaklık: " + data.getEngineTemp() + "°C" +
                        ", Motor Yükü: " + data.getEngineLoad() + "%" +
                        ", Gaz Pedalı: " + data.getThrottlePosition() + "%");

            } catch (Exception e) {
                Log.e(TAG, "UI güncellenirken hata", e);
            }
        });
    }

    private void updateConnectionStatus() {
        isConnected = CarCareApplication.isObd2Connected();
        if (isConnected) {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Kapat ikonu
            Log.d(TAG, "Bağlantı durumu: Bağlı");
        } else {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add); // Bağlan ikonu
            Log.d(TAG, "Bağlantı durumu: Bağlı değil");
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_BLUETOOTH_PERMISSION);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_BLUETOOTH_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "OBD2 bağlantısı için Bluetooth izinleri gerekli", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Bluetooth izinleri verilmedi");
            } else {
                Log.d(TAG, "Bluetooth izinleri verildi");
            }
        }
    }

    private void connectToOBD() {
        Log.d(TAG, "OBD bağlantısı başlatılıyor...");

        if (isConnected) {
            // Bağlantıyı kes
            Log.d(TAG, "Mevcut bağlantı kesiliyor...");
            obd2Manager.stopReading();
            bluetoothManager.disconnect();
            isConnected = false;
            CarCareApplication.setObd2Connected(false);
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add);
            showDefaultValues();
            Toast.makeText(this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bluetooth açık mı kontrol et
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, "Lütfen Bluetooth'u açın", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Bluetooth kapalı");
            return;
        }

        // Bluetooth izinlerini kontrol et
        if (!bluetoothManager.hasBluetoothPermissions()) {
            checkBluetoothPermissions();
            return;
        }

        // Eşleştirilmiş cihazları al
        Set<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Toast.makeText(this, "Eşleştirilmiş OBD2 cihazı bulunamadı", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Eşleştirilmiş cihaz bulunamadı");
            return;
        }

        Log.d(TAG, "Eşleştirilmiş cihaz sayısı: " + pairedDevices.size());

        // Eşleştirilmiş cihazları listele (Dialog ile)
        String[] deviceNames = new String[pairedDevices.size()];
        String[] deviceAddresses = new String[pairedDevices.size()];

        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth bağlantı izni gerekiyor", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            deviceNames[i] = device.getName() + " (" + device.getAddress() + ")";
            deviceAddresses[i] = device.getAddress();
            Log.d(TAG, "Cihaz " + i + ": " + deviceNames[i]);
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OBD2 Cihazını Seçin");

        builder.setItems(deviceNames, (dialog, which) -> {
            String deviceAddress = deviceAddresses[which];
            Log.d(TAG, "Seçilen cihaz: " + deviceAddress);

            // Seçilen cihaza bağlan
            bluetoothManager.connectToDevice(deviceAddress, new BluetoothManager.ConnectionCallback() {
                @Override
                public void onConnectionSuccessful() {
                    Log.d(TAG, "Bluetooth bağlantısı başarılı, OBD2 okumaya başlanıyor...");
                    // Bağlantı başarılı, OBD2 okumaya başla
                    obd2Manager.startReading();
                    isConnected = true;
                    CarCareApplication.setObd2Connected(true);
                    runOnUiThread(() -> {
                        fabConnectOBD.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                        Toast.makeText(CarActivity.this, "OBD2 cihazına bağlandı", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionFailed(String reason) {
                    Log.e(TAG, "Bağlantı hatası: " + reason);
                    runOnUiThread(() -> {
                        Toast.makeText(CarActivity.this, "Bağlantı hatası: " + reason, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy çağrıldı");
        // SADECE uygulamadan tamamen çıkılırken bağlantıyı kes
        // Normal aktivite geçişlerinde kesme!
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause çağrıldı");
        // onPause'da hiçbir şey yapma - bağlantıyı korumak için
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume çağrıldı");

        // Global manager'ları al
        if (CarCareApplication.getBluetoothManager() != null) {
            bluetoothManager = CarCareApplication.getBluetoothManager();
            Log.d(TAG, "BluetoothManager geri yüklendi");
        }

        if (CarCareApplication.getObd2Manager() != null) {
            obd2Manager = CarCareApplication.getObd2Manager();
            Log.d(TAG, "OBD2Manager geri yüklendi");

            // Listener'ı yeniden ayarla
            obd2Manager.setDataUpdateListener(new SimpleOBD2Manager.DataUpdateListener() {
                @Override
                public void onDataUpdate(SimpleOBD2Manager.VehicleData data) {
                    Log.d(TAG, "Veri güncellendi (onResume'dan) - Hız: " + data.getSpeed());
                    updateUI(data);
                }

                @Override
                public void onConnectionLost() {
                    runOnUiThread(() -> {
                        isConnected = false;
                        CarCareApplication.setObd2Connected(false);
                        fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add);
                        showDefaultValues();
                        Toast.makeText(CarActivity.this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Bağlantı kesildi (onResume'dan)");
                    });
                }
            });
        }

        // Bağlantı durumunu kontrol et ve arayüzü güncelle
        updateConnectionStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart çağrıldı");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop çağrıldı");
    }
}