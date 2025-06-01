package com.example.carcare;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView; // TextView importu zaten var

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.carcare.ProfilePage.ProfileActivity;
import com.example.carcare.models.NearbyPlace;
import com.example.carcare.services.CarLogosService; // Servis importu
// UserVehicleService importu zaten vardı, doğru.
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;

interface CriticalDataAlertListener {
    void onHighEngineTemperature(double temperature, double threshold);
    void onLowFuelLevel(double fuelLevel, double threshold);
    void onNewDtcDetected(List<SimpleOBD2Manager.VehicleData.DTC> newDtcs, List<SimpleOBD2Manager.VehicleData.DTC> allDtcs);
}

public class CarActivity extends AppCompatActivity implements CriticalDataAlertListener {
    private static final String TAG = "CarActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;

    // UI Elements
    private TextView tvWelcomeUser, tvCarName, tvCarYear;
    private TextView tvSpeedValue, tvRpmValue, tvEngineTempValue, tvFuelValue;
    private TextView tvEngineLoadValue;
    private TextView tvIntakeAirTempValue, tvMafValue;
    private MaterialButton btnOpenSite, btnTrafficFineInquiry, btnMotorVehicleFineInquiry;
    private MaterialButton btnCarDetails, btnRefuel;
    private FloatingActionButton fabConnectOBD;
    private ImageView imgCarLogo; // Logo ImageView

    // DTC UI Elements
    private CardView cardDtcStatus;
    private ImageView imgDtcIcon;
    private TextView tvDtcStatusMessage;
    private MaterialButton btnShowDtcDetails;

    // Bluetooth ve OBD2 nesneleri
    private BluetoothManager bluetoothManager;
    private SimpleOBD2Manager obd2Manager;
    private boolean isObdConnected = false;

    // Firebase Servisleri
    private UserVehicleService userVehicleService;
    private CarLogosService carLogosService; // Logo servisi
    private String lastProcessedVin = null;

    // Kritik Durum Bildirimleri için
    private NotificationActivity.FirebaseNotificationManager firebaseNotificationManager;
    private Map<String, Long> lastCriticalAlertTimestamps = new HashMap<>();
    private static final long CRITICAL_ALERT_COOLDOWN_MS = 20 * 60 * 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeViews(); // imgCarLogo burada bağlanır
        setupBottomNavigation();

        // Servisleri initialize et
        firebaseNotificationManager = new NotificationActivity.FirebaseNotificationManager();
        userVehicleService = new UserVehicleService(); // UserVehicleService'i initialize et
        carLogosService = new CarLogosService(); // Logo servisini initialize et

        loadUserAndCarData(); // Kullanıcı ve araç verilerini yükle (logo yüklemesi burada tetiklenir)

        setupBluetoothAndOBD();
        if (obd2Manager != null) {
            obd2Manager.setCriticalDataAlertListener(this);
        }
        setupDataUpdateListener();

        checkBluetoothPermissions();
        showDefaultValues();
        setupButtonListeners();
        updateConnectionStatus();
        setupMaintenanceScheduler();
        setupWelcomeNotification();
    }

    private void initializeViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvCarName = findViewById(R.id.tvCarName);
        tvCarYear = findViewById(R.id.tvCarYear);
        imgCarLogo = findViewById(R.id.imgCarLogo); // ImageView'i bağla

        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvRpmValue = findViewById(R.id.tvRpmValue);
        tvEngineTempValue = findViewById(R.id.tvEngineTempValue);
        tvFuelValue = findViewById(R.id.tvFuelValue);
        tvEngineLoadValue = findViewById(R.id.tvEngineLoadValue);
        tvIntakeAirTempValue = findViewById(R.id.tvIntakeAirTempValue);
        tvMafValue = findViewById(R.id.tvMafValue);

        btnOpenSite = findViewById(R.id.btnOpenSite);
        btnTrafficFineInquiry = findViewById(R.id.btnTrafficFineInquiry);
        btnMotorVehicleFineInquiry = findViewById(R.id.btnMotorVehicleFineInquiry);
        btnCarDetails = findViewById(R.id.btnCarDetails);
        btnRefuel = findViewById(R.id.btnRefuel);

        fabConnectOBD = findViewById(R.id.fabConnectOBD);
        fabConnectOBD.setOnClickListener(v -> connectToOBD());

        cardDtcStatus = findViewById(R.id.cardDtcStatus);
        imgDtcIcon = findViewById(R.id.imgDtcIcon);
        tvDtcStatusMessage = findViewById(R.id.tvDtcStatusMessage);
        btnShowDtcDetails = findViewById(R.id.btnShowDtcDetails);

        Log.d(TAG, "UI elemanları başarıyla bağlandı");
    }

    private void loadUserAndCarData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // ... (Kullanıcı adı vb. ayarları)
                            String fullName = documentSnapshot.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                tvWelcomeUser.setText(fullName);
                            } else {
                                String displayName = currentUser.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    tvWelcomeUser.setText(displayName);
                                } else {
                                    String email = currentUser.getEmail();
                                    tvWelcomeUser.setText(email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Car Owner");
                                }
                            }
                            // ...

                            String carBrandForLogo = null;

                            if (documentSnapshot.contains(UserVehicleService.MAP_VIN_DETAILS)) {
                                Map<String, Object> vinDetails = (Map<String, Object>) documentSnapshot.get(UserVehicleService.MAP_VIN_DETAILS);
                                if (vinDetails != null && !vinDetails.isEmpty()) {
                                    updateCarInfoUIFromVinDetails(vinDetails);
                                    carBrandForLogo = CarLogosService.extractBrandFromVinDetails(vinDetails);
                                    Log.d(TAG, "loadUserAndCarData: VINDetails'den marka çıkarıldı: " + carBrandForLogo);
                                }
                            }

                            if ((carBrandForLogo == null || carBrandForLogo.isEmpty())) {
                                String firestoreCarName = documentSnapshot.getString("carName");
                                if (firestoreCarName != null && !firestoreCarName.isEmpty()) {
                                    if (tvCarName.getText().toString().equalsIgnoreCase("My Car") || tvCarName.getText().toString().isEmpty()){
                                        setCarNameFromFirestore(firestoreCarName); // tvCarName'i Firestore'dan gelenle güncelle
                                    }
                                    String[] parts = firestoreCarName.split("\\s+");
                                    if (parts.length > 0) {
                                        carBrandForLogo = parts[0];
                                        Log.d(TAG, "loadUserAndCarData: Firestore 'carName' alanından marka çıkarıldı: " + carBrandForLogo);
                                    }
                                }
                            }
                            // tvCarYear için de fallback
                            if (tvCarYear.getText().toString().equalsIgnoreCase("Not Specified") || tvCarYear.getText().toString().isEmpty()){
                                setCarYearFromFirestore(documentSnapshot.getString("carYear"));
                            }


                            if (carBrandForLogo != null && !carBrandForLogo.isEmpty()) {
                                loadAndDisplayCarLogo(carBrandForLogo);
                            } else {
                                Log.w(TAG, "loadUserAndCarData: Logo yüklemek için geçerli marka adı bulunamadı. Varsayılan logo.");
                                if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                            }

                        } else {
                            Log.w(TAG, "loadUserAndCarData: Kullanıcı dokümanı Firestore'da bulunamadı: " + userId);
                            // ... (varsayılan kullanıcı adı vb.)
                            setDefaultCarInfo();
                            if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadUserAndCarData: Firestore'dan kullanıcı/araç verisi yüklenirken hata", e);
                        // ... (hata durumunda kullanıcı adı vb.)
                        setDefaultCarInfo();
                        if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default_error).into(imgCarLogo);
                    });
        } else {
            Log.w(TAG, "loadUserAndCarData: Kullanıcı giriş yapmamış.");
            // ... (misafir kullanıcı için ayarlar)
            setDefaultCarInfo();
            if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
        }
    }

    private void loadAndDisplayCarLogo(String carBrandName) {
        if (carBrandName == null || carBrandName.trim().isEmpty() || imgCarLogo == null) {
            Log.w(TAG, "loadAndDisplayCarLogo: Marka adı boş/null veya ImageView null. Marka: '" + carBrandName + "'. Varsayılan logo gösteriliyor.");
            if (imgCarLogo != null) {
                Glide.with(this)
                        .load(R.drawable.ic_car_default) // Varsayılan drawable
                        .into(imgCarLogo);
            }
            return;
        }

        Log.i(TAG, "loadAndDisplayCarLogo: Logo aranıyor: '" + carBrandName + "'");
        carLogosService.loadLogoForBrand(carBrandName, new CarLogosService.LogoLoadListener() {
            @Override
            public void onLogoBitmapLoaded(Bitmap bitmap) {
                runOnUiThread(() -> {
                    if (imgCarLogo != null && bitmap != null) {
                        Glide.with(CarActivity.this)
                                .load(bitmap)
                                .placeholder(R.drawable.ic_car_default)
                                .error(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                        Log.d(TAG, "loadAndDisplayCarLogo: '" + carBrandName + "' için Base64 logo ImageView'a yüklendi.");
                    } else {
                        Log.e(TAG, "loadAndDisplayCarLogo: Base64 logo yüklenirken ImageView veya Bitmap null. Marka: " + carBrandName);
                        if(imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default_error).into(imgCarLogo);
                    }
                });
            }

            @Override
            public void onLogoUrlLoaded(String logoUrl) {
                runOnUiThread(() -> {
                    if (imgCarLogo != null && logoUrl != null && !logoUrl.isEmpty()) {
                        Glide.with(CarActivity.this)
                                .load(logoUrl)
                                .placeholder(R.drawable.ic_car_default)
                                .error(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                        Log.d(TAG, "loadAndDisplayCarLogo: '" + carBrandName + "' için URL logo Glide ile yükleniyor: " + logoUrl);
                    } else {
                        Log.e(TAG, "loadAndDisplayCarLogo: URL logo yüklenirken ImageView veya logoUrl null/boş. Marka: " + carBrandName);
                        if(imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default_error).into(imgCarLogo);
                    }
                });
            }

            @Override
            public void onLogoNotFound() {
                runOnUiThread(() -> {
                    Log.w(TAG, "loadAndDisplayCarLogo: '" + carBrandName + "' için logo bulunamadı. Varsayılan gösteriliyor.");
                    if (imgCarLogo != null) {
                        Glide.with(CarActivity.this)
                                .load(R.drawable.ic_car_default)
                                .into(imgCarLogo);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "loadAndDisplayCarLogo: '" + carBrandName + "' için logo yüklenirken hata: ", e);
                    if (imgCarLogo != null) {
                        Glide.with(CarActivity.this)
                                .load(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                    }
                });
            }
        });
    }

    private void updateUI(SimpleOBD2Manager.VehicleData data) {
        runOnUiThread(() -> {
            if (data == null) {
                Log.w(TAG, "updateUI: Gelen VehicleData null. Varsayılanlar gösteriliyor.");
                showDefaultValues(); // Bu metod imgCarLogo'yu da varsayılana çevirmeli
                return;
            }

            tvSpeedValue.setText(String.format("%.0f", data.getSpeed() != null ? data.getSpeed() : 0.0));
            updateDTCDisplay(data.getDiagnosticTroubleCodes());
            tvSpeedValue.setText(String.format("%.0f", data.getSpeed() != null ? data.getSpeed() : 0.0));
            tvRpmValue.setText(String.format("%.0f", data.getRpm() != null ? data.getRpm() : 0.0));
            tvEngineTempValue.setText(data.getEngineTemp() != null ? String.format("%.0f°C", data.getEngineTemp()) : "N/A");
            tvFuelValue.setText(data.getFuelLevel() != null && data.getFuelLevel() >= 0 ? String.format("%.0f%%", data.getFuelLevel()) : "N/A");
            tvEngineLoadValue.setText(data.getEngineLoad() != null ? String.format("%.0f%%", data.getEngineLoad()) : "N/A");
            tvIntakeAirTempValue.setText(data.getIntakeTemp() != null ? String.format("%.0f°C", data.getIntakeTemp()) : "N/A");
            tvMafValue.setText(data.getMafAirFlow() != null && data.getMafAirFlow() >= 0 ? String.format("%.1f g/s", data.getMafAirFlow()) : "N/A");
            updateDTCDisplay(data.getDiagnosticTroubleCodes());
            // ... (diğer logMessage.append satırları eklenebilir)


            String currentVinFromOBD = data.getVin();
            Log.d(TAG, "updateUI - Alınan VIN: " + (currentVinFromOBD != null ? currentVinFromOBD : "N/A"));

            if (userVehicleService != null && currentVinFromOBD != null && !currentVinFromOBD.isEmpty() && !currentVinFromOBD.equals(lastProcessedVin)) {
                Log.d(TAG, "updateUI: OBD'den yeni/farklı VIN alındı (" + currentVinFromOBD + "), işleniyor...");
                userVehicleService.updateProfileWithVin(currentVinFromOBD, new UserVehicleService.VinUpdateCallback() {
                    @Override
                    public void onSuccess(String vin, boolean newVinRegistered, Map<String, Object> vehicleDetails) {
                        lastProcessedVin = vin;
                        Log.i(TAG, "updateUI (onSuccess): VIN (" + vin + ") işlendi. Detaylar: " + (vehicleDetails != null && !vehicleDetails.isEmpty()));
                        if (vehicleDetails != null && !vehicleDetails.isEmpty()) {
                            updateCarInfoUIFromVinDetails(vehicleDetails);
                            String brandFromVin = CarLogosService.extractBrandFromVinDetails(vehicleDetails);
                            if (brandFromVin != null && !brandFromVin.isEmpty()) {
                                loadAndDisplayCarLogo(brandFromVin);
                            } else {
                                Log.w(TAG, "updateUI (onSuccess): VIN detaylarından marka çıkarılamadı.");
                                if(imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                            }
                        } else {
                            Log.w(TAG, "updateUI (onSuccess): VIN işlendi ancak vehicleDetails boş/null.");
                            if(imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                        }
                       // Toast.makeText(CarActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CarActivity.this, "VIN & detaylar işlenirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onVinAlreadyCurrent(String vin, Map<String, Object> existingDetails) {
                        lastProcessedVin = vin;
                        Log.d(TAG, "VIN zaten Firestore'da güncel: " + vin);
                        if (existingDetails != null && !existingDetails.isEmpty()) {
                            updateCarInfoUIFromVinDetails(existingDetails);
                            String brandFromVin = CarLogosService.extractBrandFromVinDetails(existingDetails);
                            if (brandFromVin != null && !brandFromVin.isEmpty()) {
                                loadAndDisplayCarLogo(brandFromVin); // Logoyu yükle
                            } else {
                                Log.w(TAG, "VIN detaylarından marka çıkarılamadı (onVinAlreadyCurrent). Varsayılan logo gösterilecek.");
                                if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                            }
                        } else {
                            Log.w(TAG, "VIN güncel ama Firestore'da detaylar eksik/yok (onVinAlreadyCurrent).");
                            // Burada da belki sadece VIN'den marka çıkarmaya çalışılabilir,
                            // ancak UserVehicleService'in zaten detayları getirmesi beklenir.
                        }
                    }
                });
            } else if (currentVinFromOBD != null && currentVinFromOBD.equals(lastProcessedVin)) {
                // Log.v(TAG, "VIN (" + currentVinFromOBD + ") zaten bu oturumda işlenmişti.");
            }
            // Log.d(TAG, logMessage.toString()); // Bu satır, yukarıdaki logMessage oluşturulursa açılabilir.
        });
    }

    private void setCarNameFromFirestore(String carName){
        if (tvCarName != null) { // UI elemanının null olmadığını kontrol et
            if (carName != null && !carName.isEmpty()) {
                tvCarName.setText(carName);
            } else {
                tvCarName.setText("My Car");
            }
        }
    }

    private void setCarYearFromFirestore(String carYear){
        if (tvCarYear != null) { // UI elemanının null olmadığını kontrol et
            if (carYear != null && !carYear.isEmpty()) {
                tvCarYear.setText(carYear + " Model");
            } else {
                tvCarYear.setText("Not Specified");
            }
        }
    }

    private void setDefaultCarInfo() {
        setCarNameFromFirestore(null); // Bu "My Car" ayarlar
        setCarYearFromFirestore(null); // Bu "Not Specified" ayarlar
    }

    private void showDefaultValues() {
        Log.d(TAG, "showDefaultValues: Varsayılan değerler gösteriliyor...");
        // OBD verilerini sıfırla
        tvSpeedValue.setText("0");
        tvRpmValue.setText("0");
        tvEngineTempValue.setText("N/A");
        tvFuelValue.setText("N/A");
        tvEngineLoadValue.setText("N/A");
        tvIntakeAirTempValue.setText("N/A");
        tvMafValue.setText("N/A");
        updateDTCDisplay(null); // DTC ekranını temizle

        // Varsayılan logoyu yükle
        if (imgCarLogo != null) {
            Glide.with(this)
                    .load(R.drawable.ic_car_default)
                    .into(imgCarLogo);
        }
        Log.d(TAG, "showDefaultValues: Varsayılan değerler başarıyla gösterildi.");
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_dashboard);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return true;
            else if (id == R.id.nav_store) startActivity(new Intent(this, StoreActivity.class));
            else if (id == R.id.nav_map) startActivity(new Intent(this, MapsActivity.class));
            else if (id == R.id.nav_notifications) startActivity(new Intent(this, NotificationActivity.class));
            else if (id == R.id.nav_settings) startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            return true;
        });
    }

    private void setupBluetoothAndOBD() {
        if (CarCareApplication.getBluetoothManager() == null) {
            bluetoothManager = new BluetoothManager(this);
            CarCareApplication.setBluetoothManager(bluetoothManager);
        } else {
            bluetoothManager = CarCareApplication.getBluetoothManager();
        }

        if (CarCareApplication.getObd2Manager() == null) {
            obd2Manager = new SimpleOBD2Manager(this, bluetoothManager);
            CarCareApplication.setObd2Manager(obd2Manager);
        } else {
            obd2Manager = CarCareApplication.getObd2Manager();
        }
    }

    private void setupDataUpdateListener() {
        if (obd2Manager != null) {
            obd2Manager.setDataUpdateListener(new SimpleOBD2Manager.DataUpdateListener() {
                @Override
                public void onDataUpdate(SimpleOBD2Manager.VehicleData data) {
                    CarActivity.this.updateUI(data); // Bu, logo güncellemesini de tetikler
                }

                @Override
                public void onConnectionLost() {
                    runOnUiThread(() -> {
                        CarCareApplication.setObd2Connected(false);
                        updateConnectionStatus();
                        showDefaultValues(); // Bağlantı kesildiğinde varsayılanları göster
                        Toast.makeText(CarActivity.this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "OBD2 bağlantısı kesildi (onConnectionLost callback)");
                        lastProcessedVin = null;
                        if (lastCriticalAlertTimestamps != null) lastCriticalAlertTimestamps.clear();
                    });
                }
            });
        } else {
            Log.e(TAG, "setupDataUpdateListener: obd2Manager null!");
        }
    }

    private void updateCarInfoUIFromVinDetails(Map<String, Object> vinDetails) {
        if (vinDetails == null || vinDetails.isEmpty()) {
            Log.w(TAG, "updateCarInfoUIFromVinDetails: Detaylar boş veya null.");
            // Eğer detaylar boşsa, mevcut tvCarName ve tvCarYear'ı değiştirmeyebiliriz
            // veya Firestore'dan gelen carName/carYear'a fallback yapabiliriz.
            // Bu mantık loadUserAndCarData içinde daha iyi ele alınır.
            return;
        }
        runOnUiThread(() -> {
            // UserVehicleService.FIELD_DETAIL_MAKE gibi sabitleri kullanın
            String make = (String) vinDetails.get(com.example.carcare.UserVehicleService.FIELD_DETAIL_MAKE);
            String model = (String) vinDetails.get(com.example.carcare.UserVehicleService.FIELD_DETAIL_MODEL);
            String year = (String) vinDetails.get(com.example.carcare.UserVehicleService.FIELD_DETAIL_YEAR);

            StringBuilder carNameBuilder = new StringBuilder();
            if (make != null && !make.isEmpty()) carNameBuilder.append(make.toUpperCase());
            if (model != null && !model.isEmpty()) {
                if (carNameBuilder.length() > 0) carNameBuilder.append(" ");
                carNameBuilder.append(model);
            }

            if (tvCarName != null) {
                if (carNameBuilder.length() > 0) {
                    tvCarName.setText(carNameBuilder.toString());
                    Log.i(TAG, "updateCarInfoUIFromVinDetails: Araç adı güncellendi: " + carNameBuilder.toString());
                } else {
                    Log.w(TAG, "updateCarInfoUIFromVinDetails: VIN'den make/model çıkarılamadı, tvCarName güncellenmedi.");
                    // tvCarName.setText("My Car"); // Veya varsayılan bir değer
                }
            }

            if (tvCarYear != null) {
                if (year != null && !year.isEmpty()) {
                    tvCarYear.setText(year + " Model");
                    Log.i(TAG, "updateCarInfoUIFromVinDetails: Araç yılı güncellendi: " + year);
                } else {
                    Log.w(TAG, "updateCarInfoUIFromVinDetails: VIN'den yıl çıkarılamadı, tvCarYear güncellenmedi.");
                    // tvCarYear.setText("Not Specified"); // Veya varsayılan bir değer
                }
            }
        });
    }
    // --- KODUNUZUN GERİ KALANINI (showVehicleDetailsDialog, DTC metodları, izinler, connectToOBD, lifecycle metodları, CriticalDataAlertListener vb.) BURAYA EKLEYİN ---
    // ...
    // ... Bu metodların içeriğini orijinal kodunuzdan olduğu gibi alabilirsiniz,
    // ... logo ile doğrudan bir etkileşimleri yoksa.
    // ... (Aşağıya kalan metodlarınızı yapıştırın)

    private void setupButtonListeners() {
        btnOpenSite.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tuvturk.com.tr"))));
        btnTrafficFineInquiry.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://dijital.gib.gov.tr/hizliOdemeler/MTVTPCOdeme"))));
        btnMotorVehicleFineInquiry.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://dijital.gib.gov.tr/hizliOdemeler/MTVTPCOdeme"))));
        btnCarDetails.setOnClickListener(v -> showVehicleDetailsDialog());
        btnRefuel.setOnClickListener(v -> {
            Intent intentToMaps = new Intent(CarActivity.this, MapsActivity.class);
            intentToMaps.putExtra("TARGET_PLACE_TYPE", NearbyPlace.Type.GAS);
            startActivity(intentToMaps);
        });
    }

    private void setupMaintenanceScheduler() {
        new MaintenanceScheduler(this).scheduleAllMaintenance();
    }

    private void setupWelcomeNotification() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            if (!currentUser.getUid().equals(prefs.getString("last_user_id", ""))) {
                if (firebaseNotificationManager != null) {
                    firebaseNotificationManager.addWelcomeNotification(new NotificationActivity.FirebaseNotificationManager.SimpleCallback() {
                        @Override public void onSuccess() { Log.d(TAG, "Hoş geldiniz mesajı gönderildi"); }
                        @Override public void onFailure(Exception e) { Log.e(TAG, "Hoş geldiniz mesajı gönderilemedi", e); }
                    });
                }
                prefs.edit().putString("last_user_id", currentUser.getUid()).apply();
            }
        }
    }

    private void showVehicleDetailsDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to see vehicle details.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String vehicleVin = documentSnapshot.getString(UserVehicleService.FIELD_VEHICLE_VIN);
                        Map<String, Object> vinDetails = null;
                        if (documentSnapshot.contains(UserVehicleService.MAP_VIN_DETAILS)) {
                            vinDetails = (Map<String, Object>) documentSnapshot.get(UserVehicleService.MAP_VIN_DETAILS);
                        }

                        if (vehicleVin == null && (vinDetails == null || vinDetails.isEmpty())) {
                            Toast.makeText(CarActivity.this, "No vehicle details found. Please connect to OBD2 to retrieve VIN.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(CarActivity.this);
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_vehicle_details, null);
                        builder.setView(dialogView);

                        TextView tvDialogVin = dialogView.findViewById(R.id.tvDialogVin);
                        TextView tvDialogMake = dialogView.findViewById(R.id.tvDialogMake);
                        TextView tvDialogModelYear = dialogView.findViewById(R.id.tvDialogModelYear);
                        TextView tvDialogManufacturer = dialogView.findViewById(R.id.tvDialogManufacturer);
                        TextView tvDialogVehicleType = dialogView.findViewById(R.id.tvDialogVehicleType);
                        MaterialButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

                        tvDialogVin.setText(vehicleVin != null ? vehicleVin : "N/A");

                        if (vinDetails != null) {
                            tvDialogMake.setText(vinDetails.get(UserVehicleService.FIELD_DETAIL_MAKE) != null ? (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MAKE) : "N/A");
                            tvDialogModelYear.setText(vinDetails.get(UserVehicleService.FIELD_DETAIL_YEAR) != null ? (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_YEAR) : "N/A");
                            tvDialogManufacturer.setText(vinDetails.get(UserVehicleService.FIELD_DETAIL_MANUFACTURER) != null ? (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MANUFACTURER) : "N/A");
                            tvDialogVehicleType.setText(vinDetails.get(UserVehicleService.FIELD_DETAIL_VEHICLE_TYPE) != null ? (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_VEHICLE_TYPE) : "N/A");
                        } else {
                            tvDialogMake.setText("N/A");
                            tvDialogModelYear.setText("N/A");
                            tvDialogManufacturer.setText("N/A");
                            tvDialogVehicleType.setText("N/A");
                        }

                        final AlertDialog dialog = builder.create();
                        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
                        dialog.show();

                    } else {
                        Toast.makeText(CarActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching vehicle details from Firestore", e);
                    Toast.makeText(CarActivity.this, "Error fetching details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateDTCDisplay(List<SimpleOBD2Manager.VehicleData.DTC> dtcs) {
        int positiveColor = ContextCompat.getColor(this, R.color.primary);
        int negativeColor = ContextCompat.getColor(this, R.color.negative_status_color); // R.color.negative_status_color tanımlı olmalı

        if (dtcs == null || dtcs.isEmpty()) {
            tvDtcStatusMessage.setText("No Active Diagnostic Trouble Codes Found.");
            tvDtcStatusMessage.setTextColor(positiveColor);
            if (imgDtcIcon != null) imgDtcIcon.setColorFilter(positiveColor);
            if (btnShowDtcDetails != null) {
                btnShowDtcDetails.setVisibility(View.GONE);
                btnShowDtcDetails.setOnClickListener(null);
            }
        } else {
            String message = dtcs.size() + (dtcs.size() == 1 ? " Diagnostic Trouble Code detected." : " Diagnostic Trouble Codes detected.");
            tvDtcStatusMessage.setText(message);
            tvDtcStatusMessage.setTextColor(negativeColor);
            if (imgDtcIcon != null) imgDtcIcon.setColorFilter(negativeColor);
            if (btnShowDtcDetails != null) {
                btnShowDtcDetails.setVisibility(View.VISIBLE);
                btnShowDtcDetails.setText("View Details");
                btnShowDtcDetails.setOnClickListener(v -> showDtcDetailsDialog(dtcs));
            }
        }
    }

    private void showDtcDetailsDialog(List<SimpleOBD2Manager.VehicleData.DTC> dtcs) {
        if (dtcs == null || dtcs.isEmpty()) {
            Toast.makeText(this, "No trouble codes to display.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detected Diagnostic Trouble Codes");
        StringBuilder message = new StringBuilder();
        for (SimpleOBD2Manager.VehicleData.DTC dtc : dtcs) {
            message.append("<b>").append(dtc.code).append(":</b><br>").append(dtc.description);
            if (!dtc.isUserUnderstandable) { // Bu alan DTC sınıfınızda olmalı
                message.append("<br><small><i>(This is a technical description. Check service manual or consult a professional.)</i></small>");
            }
            message.append("<br><br>");
        }
        builder.setMessage(Html.fromHtml(message.toString().trim(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Html.FROM_HTML_MODE_LEGACY : 0));
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton("Clear Codes (Experimental)", (dialog, which) -> {
            Toast.makeText(CarActivity.this, "Clear DTCs feature will be added soon.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void updateConnectionStatus() {
        isObdConnected = CarCareApplication.isObd2Connected();
        if (fabConnectOBD != null) {
            fabConnectOBD.setImageResource(isObdConnected ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_menu_add);
        }
        Log.d(TAG, "Bağlantı durumu UI güncellendi: " + (isObdConnected ? "Bağlı" : "Bağlı değil"));
        if (!isObdConnected) {
            lastProcessedVin = null;
            if (lastCriticalAlertTimestamps != null) lastCriticalAlertTimestamps.clear();
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 ve üzeri
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        REQUEST_BLUETOOTH_PERMISSION);
            }
        } else { // Android 11 ve altı
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { // ACCESS_FINE_LOCATION Bluetooth taraması için gerekebilir
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION},
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
                Toast.makeText(this, "OBD2 bağlantısı için Bluetooth izinleri gerekli.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void connectToOBD() {
        Log.d(TAG, "connectToOBD çağrıldı.");
        if (CarCareApplication.isObd2Connected()) {
            if (obd2Manager != null) obd2Manager.stopReading();
            if (bluetoothManager != null) bluetoothManager.disconnect();
            CarCareApplication.setObd2Connected(false); // Global durumu güncelle
            updateConnectionStatus();
            showDefaultValues(); // Bağlantı kesildiğinde varsayılanları göster
            Toast.makeText(this, "OBD2 bağlantısı kesildi.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothManager == null || obd2Manager == null) {
            Log.e(TAG, "BluetoothManager veya OBD2Manager başlatılmamış!");
            Toast.makeText(this, "Bluetooth servisi hatası.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, "Lütfen Bluetooth'u açın.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothManager.hasBluetoothPermissions()) { // Bu metod BluetoothManager'da olmalı
            checkBluetoothPermissions();
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Toast.makeText(this, "Eşleştirilmiş OBD2 cihazı bulunamadı.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> deviceNamesList = new ArrayList<>();
        List<String> deviceAddressesList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Toast.makeText(this, "Bluetooth bağlantı izni eksik.", Toast.LENGTH_SHORT).show();
                // İzin isteyebilir veya kullanıcıyı ayarlara yönlendirebilirsiniz.
                return;
            }
            deviceNamesList.add((device.getName() != null ? device.getName() : "Unknown Device") + " (" + device.getAddress() + ")");
            deviceAddressesList.add(device.getAddress());
        }
        final String[] deviceNames = deviceNamesList.toArray(new String[0]);
        final String[] deviceAddresses = deviceAddressesList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("OBD2 Cihazını Seçin")
                .setItems(deviceNames, (dialog, which) -> {
                    String deviceAddress = deviceAddresses[which];
                    Toast.makeText(this, "Bağlanılıyor: " + deviceNames[which], Toast.LENGTH_SHORT).show();
                    bluetoothManager.connectToDevice(deviceAddress, new BluetoothManager.ConnectionCallback() {
                        @Override
                        public void onConnectionSuccessful() {
                            CarCareApplication.setObd2Connected(true); // Global durumu güncelle
                            runOnUiThread(() -> {
                                updateConnectionStatus();
                                Toast.makeText(CarActivity.this, "OBD2 cihazına bağlandı!", Toast.LENGTH_SHORT).show();
                                if (obd2Manager != null) obd2Manager.startReading();
                            });
                        }
                        @Override
                        public void onConnectionFailed(String reason) {
                            CarCareApplication.setObd2Connected(false); // Global durumu güncelle
                            runOnUiThread(() -> {
                                updateConnectionStatus();
                                Toast.makeText(CarActivity.this, "Bağlantı hatası: " + reason, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume çağrıldı.");
        // BluetoothManager ve OBD2Manager'ın global instance'larını al veya oluştur
        if (CarCareApplication.getBluetoothManager() != null) {
            bluetoothManager = CarCareApplication.getBluetoothManager();
        } else {
            bluetoothManager = new BluetoothManager(this);
            CarCareApplication.setBluetoothManager(bluetoothManager);
        }

        if (CarCareApplication.getObd2Manager() != null) {
            obd2Manager = CarCareApplication.getObd2Manager();
        } else {
            obd2Manager = new SimpleOBD2Manager(this, bluetoothManager);
            CarCareApplication.setObd2Manager(obd2Manager);
        }

        // Listener'ları yeniden ata (özellikle OBD2Manager yeniden oluşturulduysa)
        if (obd2Manager != null) {
            obd2Manager.setCriticalDataAlertListener(this);
            // DataUpdateListener'ı da burada yeniden ayarlamak iyi bir pratik olabilir
            // Eğer uygulama arka plana alınıp tekrar açıldığında bağlantı devam ediyorsa
            // ve listener kaybolduysa.
            setupDataUpdateListener();
        }
        updateConnectionStatus(); // Bağlantı durumunu UI'da güncelle
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy çağrıldı");
        // OBD2 okumayı durdur ve bağlantıyı kes (eğer hala açıksa ve uygulama tamamen kapanıyorsa)
        // Ancak, bu genellikle CarCareApplication gibi bir yerde yönetilir,
        // activity destroy olduğunda bağlantının kesilmesi her zaman istenmeyebilir.
        // if (obd2Manager != null) obd2Manager.stopReading();
        // if (bluetoothManager != null) bluetoothManager.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause çağrıldı");
        // OBD2 okumayı durdurmak isteyebilirsiniz (pil tasarrufu için),
        // ama bağlantıyı kesmek gerekmeyebilir.
        // if (CarCareApplication.isObd2Connected() && obd2Manager != null) {
        //     obd2Manager.stopReadingTemporarily(); // Örneğin
        // }
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

    // --- CriticalDataAlertListener Implementasyonu ---
    @Override
    public void onHighEngineTemperature(double temperature, double threshold) {
        String alertType = "HIGH_ENGINE_TEMP";
        if (canSendCriticalAlert(alertType)) {
            String title = "🚨 High Engine Temperature!";
            String message = String.format("Engine temperature: %.0f°C (Threshold: %.0f°C). Please check your vehicle!", temperature, threshold);
            sendAndSaveCriticalAlert(title, message, 201); // Notification ID 201
            updateLastCriticalAlertTimestamp(alertType);
            Log.i(TAG, "High engine temperature notification sent: " + temperature);
        }
    }

    @Override
    public void onLowFuelLevel(double fuelLevel, double threshold) {
        String alertType = "LOW_FUEL_LEVEL";
        if (canSendCriticalAlert(alertType)) {
            String title = "⛽ Low Fuel Level!";
            String message = String.format("Fuel level: %%%.0f (Threshold: %%%.0f). Please refuel!", fuelLevel, threshold);
            sendAndSaveCriticalAlert(title, message, 202); // Notification ID 202
            updateLastCriticalAlertTimestamp(alertType);
            Log.i(TAG, "Low fuel level notification sent: " + fuelLevel);
        }
    }

    @Override
    public void onNewDtcDetected(List<SimpleOBD2Manager.VehicleData.DTC> newDtcs, List<SimpleOBD2Manager.VehicleData.DTC> allDtcs) {
        String alertType = "NEW_DTC_DETECTED";
        if (canSendCriticalAlert(alertType)) {
            String title = "🛠️ New Trouble Code Detected!";
            StringBuilder messageBuilder = new StringBuilder("New trouble code(s) found in your vehicle:\n");
            for (SimpleOBD2Manager.VehicleData.DTC dtc : newDtcs) {
                messageBuilder.append(dtc.code).append(": ").append(dtc.description).append("\n");
            }
            sendAndSaveCriticalAlert(title, messageBuilder.toString().trim(), 203); // Notification ID 203
            updateLastCriticalAlertTimestamp(alertType);
            Log.i(TAG, "New DTC notification sent. New codes: " + newDtcs.size());
        }
    }

    private boolean canSendCriticalAlert(String alertType) {
        long currentTime = System.currentTimeMillis();
        if (lastCriticalAlertTimestamps == null) {
            lastCriticalAlertTimestamps = new HashMap<>();
        }
        long lastTime = lastCriticalAlertTimestamps.getOrDefault(alertType, 0L);
        if (currentTime - lastTime > CRITICAL_ALERT_COOLDOWN_MS) {
            return true;
        }
        Log.d(TAG, alertType + " için kritik uyarı cooldown süresinde.");
        return false;
    }

    private void updateLastCriticalAlertTimestamp(String alertType) {
        if (lastCriticalAlertTimestamps == null) {
            lastCriticalAlertTimestamps = new HashMap<>();
        }
        lastCriticalAlertTimestamps.put(alertType, System.currentTimeMillis());
    }

    private void sendAndSaveCriticalAlert(String title, String message, int notificationId) {
        // Lokal bildirim göster
        NotificationHelper.showNotification(getApplicationContext(), title, message, notificationId);

        // Firebase'e bildirim kaydet
        if (firebaseNotificationManager != null) {
            NotificationActivity.NotificationData notificationData = new NotificationActivity.NotificationData();
            notificationData.setTitle(title);
            notificationData.setMessage(message);
            notificationData.setTimestamp(new Date()); // Şu anki zamanı ata

            firebaseNotificationManager.addCustomNotification(notificationData, new NotificationActivity.FirebaseNotificationManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Kritik uyarı Firestore'a başarıyla kaydedildi: " + title);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Kritik uyarı Firestore'a kaydedilemedi: " + title, e);
                }
            });
        } else {
            Log.e(TAG, "firebaseNotificationManager null, kritik uyarı Firestore'a kaydedilemedi.");
        }
    }
}