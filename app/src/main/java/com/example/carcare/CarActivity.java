package com.example.carcare;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
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

import java.util.Set;

public class CarActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;

    private TextView tvKilometerValue, tvFuelValue, tvEngineTempValue;
    private TextView tvTPFL, tvTPFR, tvTPRL, tvTPRR, tvOilLevelValue;
    private TextView tvWelcomeUser, tvCarName, tvCarYear;
    private MaterialButton btnOpenSite, btnTrafficFineInquiry, btnMotorVehicleFineInquiry;
    private MaterialButton btnCarDetails, btnCheckOil;
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

        // Alt navigasyonu bağla
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

        // Karşılama mesajını ve kullanıcı bilgilerini ayarla
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvCarName = findViewById(R.id.tvCarName);
        tvCarYear = findViewById(R.id.tvCarYear);

        // Kullanıcı bilgilerini Firebase'den yükle
        loadUserAndCarData();

        // Değer TextView'lerini bağla
        tvKilometerValue = findViewById(R.id.tvKilometerValue);
        tvFuelValue = findViewById(R.id.tvFuelValue);
        tvEngineTempValue = findViewById(R.id.tvEngineTempValue);
        tvTPFL = findViewById(R.id.tvTPFL);
        tvTPFR = findViewById(R.id.tvTPFR);
        tvTPRL = findViewById(R.id.tvTPRL);
        tvTPRR = findViewById(R.id.tvTPRR);
        tvOilLevelValue = findViewById(R.id.tvOilLevelValue);

        // Butonları bağla
        btnOpenSite = findViewById(R.id.btnOpenSite);
        btnTrafficFineInquiry = findViewById(R.id.btnTrafficFineInquiry);
        btnMotorVehicleFineInquiry = findViewById(R.id.btnMotorVehicleFineInquiry);
        btnCarDetails = findViewById(R.id.btnCarDetails);
        btnCheckOil = findViewById(R.id.btnCheckOil);

        // FloatingActionButton'u bağla
        fabConnectOBD = findViewById(R.id.fabConnectOBD);
        fabConnectOBD.setOnClickListener(v -> connectToOBD());

        // Bluetooth ve OBD2 nesnelerini oluştur
        bluetoothManager = new BluetoothManager(this);
        obd2Manager = new SimpleOBD2Manager(this, bluetoothManager);

        // Veri güncelleme dinleyicisini ayarla
        obd2Manager.setDataUpdateListener(data -> updateUI(data));

        // Bluetooth izinlerini kontrol et
        checkBluetoothPermissions();

        // Varsayılan değerleri göster
        showDefaultValues();

        // Buton tıklama olaylarını ayarla
        Intent govIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://turkiye.gov.tr"));
        btnOpenSite.setOnClickListener(v -> startActivity(govIntent));
        btnTrafficFineInquiry.setOnClickListener(v -> startActivity(govIntent));
        btnMotorVehicleFineInquiry.setOnClickListener(v -> startActivity(govIntent));

        btnCarDetails.setOnClickListener(v -> {
            // Burada araba detaylarına gidecek kodu yazabilirsiniz
            Toast.makeText(this, "Car details feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnCheckOil.setOnClickListener(v -> {
            Toast.makeText(this, "Oil check feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Bağlantı durumunu kontrol et ve arayüzü güncelle
        isConnected = CarCareApplication.isObd2Connected();
        if (isConnected) {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Kapat ikonu
        } else {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add); // Bağlan ikonu
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
                        tvCarName.setText("My Car");
                        tvCarYear.setText("2023 Model");
                    });
        } else {
            // Kullanıcı giriş yapmamışsa, varsayılan değerler
            tvWelcomeUser.setText("Guest");
            tvCarName.setText("My Car");
            tvCarYear.setText("2023 Model");
        }
    }

    private void showDefaultValues() {
        // Varsayılan değerleri göster
        SimpleOBD2Manager.VehicleData defaultData = new SimpleOBD2Manager.VehicleData();
        defaultData.setDefaultValues();
        updateUI(defaultData);
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
            }
        }
    }

    private void connectToOBD() {
        if (isConnected) {
            // Bağlantıyı kes
            obd2Manager.stopReading();
            bluetoothManager.disconnect();
            isConnected = false;
            CarCareApplication.setObd2Connected(false); // Bağlantı durumunu güncelle
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add); // Bağlan ikonu
            showDefaultValues();
            Toast.makeText(this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bluetooth açık mı kontrol et
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, "Lütfen Bluetooth'u açın", Toast.LENGTH_SHORT).show();
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
            return;
        }

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
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OBD2 Cihazını Seçin");

        builder.setItems(deviceNames, (dialog, which) -> {
            String deviceAddress = deviceAddresses[which];

            // Seçilen cihaza bağlan
            bluetoothManager.connectToDevice(deviceAddress, new BluetoothManager.ConnectionCallback() {
                @Override
                public void onConnectionSuccessful() {
                    // Bağlantı başarılı, OBD2 okumaya başla
                    obd2Manager.startReading();
                    isConnected = true;
                    CarCareApplication.setObd2Connected(true); // Bağlantı durumunu güncelle
                    runOnUiThread(() -> {
                        fabConnectOBD.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Kapat ikonu
                        Toast.makeText(CarActivity.this, "OBD2 cihazına bağlandı", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onConnectionFailed(String reason) {
                    runOnUiThread(() -> {
                        Toast.makeText(CarActivity.this, "Bağlantı hatası: " + reason, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        builder.show();
    }

    private void updateUI(SimpleOBD2Manager.VehicleData data) {
        // UI'ı güncelle (ana thread üzerinde)
        runOnUiThread(() -> {
            // Metinleri güncelle (daha modern formatla)
            tvKilometerValue.setText(String.format("%.0f", data.getSpeed()));
            tvFuelValue.setText(String.format("%.0f%%", data.getFuelLevel()));
            tvEngineTempValue.setText(String.format("%.0f°C", data.getEngineTemp()));
            tvTPFL.setText(String.format("%.0f PSI", data.getTirePressureFL()));
            tvTPFR.setText(String.format("%.0f PSI", data.getTirePressureFR()));
            tvTPRL.setText(String.format("%.0f PSI", data.getTirePressureRL()));
            tvTPRR.setText(String.format("%.0f PSI", data.getTirePressureRR()));
            tvOilLevelValue.setText(String.format("%.0f°C", data.getOilTemp()));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Sadece aktivite kapatılırken bağlantıyı kapat, tema değiştiğinde değil
        if (!CarCareApplication.isObd2Connected()) {
            if (obd2Manager != null) {
                obd2Manager.stopReading();
            }
            if (bluetoothManager != null) {
                bluetoothManager.disconnect();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Aktivite tekrar görünür olduğunda bağlantı durumunu kontrol et
        isConnected = CarCareApplication.isObd2Connected();
        if (isConnected) {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Kapat ikonu
        } else {
            fabConnectOBD.setImageResource(android.R.drawable.ic_menu_add); // Bağlan ikonu
        }
    }
}