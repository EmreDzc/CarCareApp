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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.widget.LinearLayout; // Eğer tıklanabilir alanlarınız LinearLayout ise
import android.os.Build; // Build.VERSION kontrolü için

import com.example.carcare.ProfilePage.ProfileActivity;
import com.example.carcare.models.NearbyPlace;
// UserVehicleService importu zaten vardı, doğru.
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.TextView;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.example.carcare.services.CarLogosService;

import java.util.ArrayList;
import java.util.Date; // Kritik uyarılar için eklendi
import java.util.HashMap; // Kritik uyarılar için eklendi
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Set;

interface CriticalDataAlertListener {
    void onHighEngineTemperature(double temperature, double threshold);
    void onLowBatteryVoltage(double voltage, double threshold, boolean engineRunning);
    void onHighBatteryVoltage(double voltage, double threshold, boolean engineRunning);
    void onLowFuelLevel(double fuelLevel, double threshold);
    void onNewDtcDetected(List<SimpleOBD2Manager.VehicleData.DTC> newDtcs, List<SimpleOBD2Manager.VehicleData.DTC> allDtcs);
}

public class CarActivity extends AppCompatActivity implements CriticalDataAlertListener { // Listener implement edildi
    private static final String TAG = "CarActivity";

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1001;

    // UI Elements
    private TextView tvWelcomeUser, tvCarName, tvCarYear;
    private TextView tvSpeedValue, tvRpmValue, tvEngineTempValue, tvFuelValue;
    private TextView tvEngineLoadValue; // tvThrottleValue kaldırıldı
    private TextView tvIntakeAirTempValue, tvMafValue;
    private MaterialButton btnOpenSite, btnTrafficFineInquiry, btnMotorVehicleFineInquiry;
    private MaterialButton btnCarDetails, btnRefuel;
    private FloatingActionButton fabConnectOBD;

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
    private String lastProcessedVin = null;

    // Kritik Durum Bildirimleri için
    private NotificationActivity.FirebaseNotificationManager firebaseNotificationManager;
    private Map<String, Long> lastCriticalAlertTimestamps = new HashMap<>();
    private static final long CRITICAL_ALERT_COOLDOWN_MS = 20 * 60 * 1000;

    private CarLogosService carLogosService;
    private ImageView imgCarLogo;

    private MaterialCardView cardMetricRpm, cardMetricEngineTemp;
    private LinearLayout layoutMetricEngineLoad, layoutMetricIntakeAirTemp, layoutMetricMaf;
    private MaterialCardView cardMetricBatteryVoltage;
    private TextView tvBatteryVoltageValue;
    private Spinner spinnerProtocol;
    private TextView tvProtocolLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeViews();
        setupBottomNavigation();
        loadUserAndCarData();

        firebaseNotificationManager = new NotificationActivity.FirebaseNotificationManager(); // Initialize FNM

        setupBluetoothAndOBD(); // Bu metod obd2Manager'ı initialize eder
        if (obd2Manager != null) {
            obd2Manager.setCriticalDataAlertListener(this); // Listener'ı burada ata
        }
        setupDataUpdateListener(); // Data listener'ı da burada ayarla

        checkBluetoothPermissions();
        showDefaultValues();
        setupButtonListeners();
        setupMetricClickListeners();
        updateConnectionStatus();
        setupMaintenanceScheduler();
        setupWelcomeNotification();
        carLogosService = new CarLogosService();
        userVehicleService = new UserVehicleService();

        userVehicleService = new UserVehicleService();
    }

    private void initializeViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvCarName = findViewById(R.id.tvCarName);
        tvCarYear = findViewById(R.id.tvCarYear);

        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvRpmValue = findViewById(R.id.tvRpmValue);
        tvEngineTempValue = findViewById(R.id.tvEngineTempValue);
        tvFuelValue = findViewById(R.id.tvFuelValue);

        tvEngineLoadValue = findViewById(R.id.tvEngineLoadValue);
        // tvThrottleValue = findViewById(R.id.tvThrottleValue); // Bu satır silindi
        tvIntakeAirTempValue = findViewById(R.id.tvIntakeAirTempValue);
        tvMafValue = findViewById(R.id.tvMafValue);

        cardMetricRpm = findViewById(R.id.cardMetricRpm);
        cardMetricEngineTemp = findViewById(R.id.cardMetricEngineTemp);
        layoutMetricEngineLoad = findViewById(R.id.layoutMetricEngineLoad);
        layoutMetricIntakeAirTemp = findViewById(R.id.layoutMetricIntakeAirTemp);
        layoutMetricMaf = findViewById(R.id.layoutMetricMaf);

        btnOpenSite = findViewById(R.id.btnOpenSite);
        btnTrafficFineInquiry = findViewById(R.id.btnTrafficFineInquiry);
        btnMotorVehicleFineInquiry = findViewById(R.id.btnMotorVehicleFineInquiry);
        btnCarDetails = findViewById(R.id.btnCarDetails);
        btnRefuel = findViewById(R.id.btnRefuel);

        fabConnectOBD = findViewById(R.id.fabConnectOBD);
        fabConnectOBD.setOnClickListener(v -> connectToOBD());

        cardMetricBatteryVoltage = findViewById(R.id.cardMetricBatteryVoltage);
        tvBatteryVoltageValue = findViewById(R.id.tvBatteryVoltageValue);

        tvProtocolLabel = findViewById(R.id.tvProtocolLabel);
        spinnerProtocol = findViewById(R.id.spinnerProtocol);
        ArrayAdapter<CharSequence> protocolAdapter = ArrayAdapter.createFromResource(this,
                R.array.obd2_protocol_display, android.R.layout.simple_spinner_item);
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(protocolAdapter);

        cardDtcStatus = findViewById(R.id.cardDtcStatus);
        imgDtcIcon = findViewById(R.id.imgDtcIcon);
        tvDtcStatusMessage = findViewById(R.id.tvDtcStatusMessage);
        btnShowDtcDetails = findViewById(R.id.btnShowDtcDetails);
        imgCarLogo = findViewById(R.id.imgCarLogo);



        Log.d(TAG, "UI elemanları başarıyla bağlandı");
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
        // Listener atamasını onCreate içinde, bu metodun çağrısından sonra yapıyoruz.
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
                            Log.d(TAG, "showVehicleDetailsDialog - vinDetails from Firestore: " + vinDetails);
                        } else {
                            Log.w(TAG, "showVehicleDetailsDialog - User document does not contain vinDetails map.");
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

    private void setupDataUpdateListener() {
        if (obd2Manager != null) {
            obd2Manager.setDataUpdateListener(new SimpleOBD2Manager.DataUpdateListener() {
                @Override
                public void onDataUpdate(SimpleOBD2Manager.VehicleData data) {
                    CarActivity.this.updateUI(data);
                }

                @Override
                public void onConnectionLost() {
                    runOnUiThread(() -> {
                        Log.d(TAG, "OBD2 bağlantısı kesildi (onConnectionLost callback)");
                        CarCareApplication.setObd2Connected(false);
                        updateConnectionStatus(); // Bu metod içinde cooldown ve notifiedDTCs temizliği yapılacak
                        showDefaultValues();
                        Toast.makeText(CarActivity.this, "OBD2 bağlantısı kesildi", Toast.LENGTH_SHORT).show();
                        lastProcessedVin = null;
                    });
                }
            });
        } else {
            Log.e(TAG, "setupDataUpdateListener: obd2Manager null!");
        }
    }

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

    private void setupMetricClickListeners() {
        if (cardMetricRpm != null) {
            cardMetricRpm.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_rpm_title),
                    getString(R.string.metric_rpm_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: cardMetricRpm is null!");
        }

        if (cardMetricEngineTemp != null) {
            cardMetricEngineTemp.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_engine_temp_title),
                    getString(R.string.metric_engine_temp_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: cardMetricEngineTemp is null!");
        }

        if (cardMetricBatteryVoltage != null) {
            cardMetricBatteryVoltage.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_battery_voltage_title),
                    getString(R.string.metric_battery_voltage_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: cardMetricBatteryVoltage is null!");
        }

        if (layoutMetricEngineLoad != null) {
            layoutMetricEngineLoad.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_engine_load_title),
                    getString(R.string.metric_engine_load_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: layoutMetricEngineLoad is null!");
        }

        if (layoutMetricIntakeAirTemp != null) {
            layoutMetricIntakeAirTemp.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_intake_air_temp_title),
                    getString(R.string.metric_intake_air_temp_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: layoutMetricIntakeAirTemp is null!");
        }

        if (layoutMetricMaf != null) {
            layoutMetricMaf.setOnClickListener(v -> showMetricInfoDialog(
                    getString(R.string.metric_maf_title),
                    getString(R.string.metric_maf_description)
            ));
        } else {
            Log.w(TAG, "setupMetricClickListeners: layoutMetricMaf is null!");
        }
    }

    private void showMetricInfoDialog(String title, String descriptionHtml) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_metric_info, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogMetricTitle);
        TextView tvDialogDescription = dialogView.findViewById(R.id.tvDialogMetricDescription);

        tvDialogTitle.setText(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvDialogDescription.setText(Html.fromHtml(descriptionHtml, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvDialogDescription.setText(Html.fromHtml(descriptionHtml));
        }

        builder.setPositiveButton(getString(R.string.dialog_close_button), (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

    private void loadUserAndCarData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Kullanıcı adı ayarlama
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

                            // Araç bilgileri ve logo yükleme
                            String carBrandForLogo = null;

                            // VIN detaylarından marka çıkarma
                            if (documentSnapshot.contains(UserVehicleService.MAP_VIN_DETAILS)) {
                                Map<String, Object> vinDetails = (Map<String, Object>) documentSnapshot.get(UserVehicleService.MAP_VIN_DETAILS);
                                if (vinDetails != null && !vinDetails.isEmpty()) {
                                    updateCarInfoUIFromVinDetails(vinDetails);
                                    carBrandForLogo = CarLogosService.extractBrandFromVinDetails(vinDetails);
                                    Log.d(TAG, "loadUserAndCarData: VINDetails'den marka çıkarıldı: " + carBrandForLogo);

                                    // VIN'den araç bilgileri eksikse Firestore'dan tamamla
                                    String make = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MAKE);
                                    String model = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MODEL);
                                    if((make == null || make.isEmpty()) && (model == null || model.isEmpty())){
                                        setCarNameFromFirestore(documentSnapshot.getString("carName"));
                                    }
                                    String yearFromVin = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_YEAR);
                                    if(yearFromVin == null || yearFromVin.isEmpty()){
                                        setCarYearFromFirestore(documentSnapshot.getString("carYear"));
                                    }
                                }
                            }

                            // VIN'den marka bulunamazsa Firestore carName'den dene
                            if ((carBrandForLogo == null || carBrandForLogo.isEmpty())) {
                                String firestoreCarName = documentSnapshot.getString("carName");
                                if (firestoreCarName != null && !firestoreCarName.isEmpty()) {
                                    if (tvCarName.getText().toString().equalsIgnoreCase("My Car") || tvCarName.getText().toString().isEmpty()){
                                        setCarNameFromFirestore(firestoreCarName);
                                    }
                                    String[] parts = firestoreCarName.split("\\s+");
                                    if (parts.length > 0) {
                                        carBrandForLogo = parts[0];
                                        Log.d(TAG, "loadUserAndCarData: Firestore 'carName' alanından marka çıkarıldı: " + carBrandForLogo);
                                    }
                                }

                                // Eğer hala carName yoksa, fallback olarak ayarla
                                if (tvCarName.getText().toString().equalsIgnoreCase("My Car") || tvCarName.getText().toString().isEmpty()) {
                                    setCarNameFromFirestore(documentSnapshot.getString("carName"));
                                }
                                if (tvCarYear.getText().toString().equalsIgnoreCase("Not Specified") || tvCarYear.getText().toString().isEmpty()) {
                                    setCarYearFromFirestore(documentSnapshot.getString("carYear"));
                                }
                            }

                            // Logo yükleme
                            if (carBrandForLogo != null && !carBrandForLogo.isEmpty()) {
                                loadAndDisplayCarLogo(carBrandForLogo);
                            } else {
                                Log.w(TAG, "loadUserAndCarData: Logo yüklemek için geçerli marka adı bulunamadı. Varsayılan logo.");
                                if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                            }

                        } else {
                            Log.w(TAG, "loadUserAndCarData: Kullanıcı dokümanı Firestore'da bulunamadı: " + userId);
                            // Varsayılan kullanıcı adı
                            String displayName = currentUser.getDisplayName();
                            if (displayName != null && !displayName.isEmpty()) {
                                tvWelcomeUser.setText(displayName);
                            } else {
                                String email = currentUser.getEmail();
                                tvWelcomeUser.setText(email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Car Owner");
                            }
                            setDefaultCarInfo();
                            if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadUserAndCarData: Firestore'dan kullanıcı/araç verisi yüklenirken hata", e);
                        // Hata durumunda kullanıcı adı
                        String displayName = currentUser.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            tvWelcomeUser.setText(displayName);
                        } else {
                            String email = currentUser.getEmail();
                            tvWelcomeUser.setText(email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Car Owner");
                        }
                        setDefaultCarInfo();
                        if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default_error).into(imgCarLogo);
                    });
        } else {
            Log.w(TAG, "loadUserAndCarData: Kullanıcı giriş yapmamış.");
            tvWelcomeUser.setText("Guest");
            setDefaultCarInfo();
            if (imgCarLogo != null) Glide.with(CarActivity.this).load(R.drawable.ic_car_default).into(imgCarLogo);
        }
    }



    private void loadAndDisplayCarLogo(String carBrandName) {
        if (carBrandName == null || carBrandName.trim().isEmpty() || imgCarLogo == null) {
            Log.w(TAG, "loadAndDisplayCarLogo: Marka adı boş/null veya ImageView null. Marka: '" + carBrandName + "'. Varsayılan logo gösteriliyor.");
            if (imgCarLogo != null) {
                Glide.with(this)
                        .load(R.drawable.ic_car_default)
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
                        Log.d(TAG, "✅ Base64 logo başarıyla yüklendi: " + carBrandName);
                        Glide.with(CarActivity.this)
                                .load(bitmap)
                                .placeholder(R.drawable.ic_car_default)
                                .error(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                    } else {
                        Log.e(TAG, "❌ Base64 logo yüklenirken ImageView veya Bitmap null. Marka: " + carBrandName);
                        if(imgCarLogo != null) {
                            Glide.with(CarActivity.this)
                                    .load(R.drawable.ic_car_default_error)
                                    .into(imgCarLogo);
                        }
                    }
                });
            }

            @Override
            public void onLogoUrlLoaded(String logoUrl) {
                runOnUiThread(() -> {
                    if (imgCarLogo != null && logoUrl != null && !logoUrl.isEmpty()) {
                        Log.d(TAG, "✅ URL logo yükleniyor: " + logoUrl + " (Marka: " + carBrandName + ")");
                        Glide.with(CarActivity.this)
                                .load(logoUrl)
                                .placeholder(R.drawable.ic_car_default)
                                .error(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                    } else {
                        Log.e(TAG, "❌ URL logo yüklenirken ImageView veya logoUrl null/boş. Marka: " + carBrandName);
                        if(imgCarLogo != null) {
                            Glide.with(CarActivity.this)
                                    .load(R.drawable.ic_car_default_error)
                                    .into(imgCarLogo);
                        }
                    }
                });
            }

            @Override
            public void onLogoNotFound() {
                runOnUiThread(() -> {
                    Log.w(TAG, "⚠️ '" + carBrandName + "' için logo bulunamadı. Varsayılan gösteriliyor.");
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
                    Log.e(TAG, "❌ '" + carBrandName + "' için logo yüklenirken hata: ", e);
                    if (imgCarLogo != null) {
                        Glide.with(CarActivity.this)
                                .load(R.drawable.ic_car_default_error)
                                .into(imgCarLogo);
                    }
                });
            }
        });
    }

    private void setCarNameFromFirestore(String carName){
        if (carName != null && !carName.isEmpty()) {
            tvCarName.setText(carName);
        } else {
            tvCarName.setText("My Car");
        }
    }
    private void setCarYearFromFirestore(String carYear){
        if (carYear != null && !carYear.isEmpty()) {
            tvCarYear.setText(carYear + " Model");
        } else {
            tvCarYear.setText("Not Specified");
        }
    }

    private void setDefaultCarInfo() {
        tvCarName.setText("My Car");
        tvCarYear.setText("Not Specified");
    }


    private void showDefaultValues() {
        Log.d(TAG, "Varsayılan değerler gösteriliyor...");
        SimpleOBD2Manager.VehicleData defaultData = new SimpleOBD2Manager.VehicleData();
        defaultData.clearDiagnosticTroubleCodes();
        defaultData.clearVin();
        updateUI(defaultData);
        Log.d(TAG, "Varsayılan değerler başarıyla gösterildi.");
    }

    private void updateUI(SimpleOBD2Manager.VehicleData data) {
        runOnUiThread(() -> {
            if (data == null) {
                Log.w(TAG, "updateUI: Gelen VehicleData null. Varsayılanlar gösteriliyor.");
                showDefaultValues();
                return;
            }

            StringBuilder logMessage = new StringBuilder("UI güncellendi - ");
            try {
                tvSpeedValue.setText(String.format("%.0f", data.getSpeed() != null ? data.getSpeed() : 0.0));
                logMessage.append("Hız: ").append(data.getSpeed() != null ? String.format("%.0f", data.getSpeed()) : "N/A").append(", ");
                tvRpmValue.setText(String.format("%.0f", data.getRpm() != null ? data.getRpm() : 0.0));
                logMessage.append("RPM: ").append(data.getRpm() != null ? String.format("%.0f", data.getRpm()) : "N/A").append(", ");
                tvEngineTempValue.setText(data.getEngineTemp() != null ? String.format("%.0f°C", data.getEngineTemp()) : "N/A");
                logMessage.append("Sıcaklık: ").append(data.getEngineTemp() != null ? String.format("%.0f°C", data.getEngineTemp()) : "N/A").append(", ");
                tvFuelValue.setText(data.getFuelLevel() != null && data.getFuelLevel() >= 0 ? String.format("%.0f%%", data.getFuelLevel()) : "N/A");
                logMessage.append("Yakıt: ").append(data.getFuelLevel() != null && data.getFuelLevel() >= 0 ? String.format("%.0f%%", data.getFuelLevel()) : "N/A").append(", ");
                tvEngineLoadValue.setText(data.getEngineLoad() != null ? String.format("%.0f%%", data.getEngineLoad()) : "N/A");
                logMessage.append("Yük: ").append(data.getEngineLoad() != null ? String.format("%.0f%%", data.getEngineLoad()) : "N/A").append(", ");
                if (tvBatteryVoltageValue != null) {
                    tvBatteryVoltageValue.setText(data.getBatteryVoltage() != null ? String.format("%.1fV", data.getBatteryVoltage()) : "N/A");
                    logMessage.append("Voltaj: ").append(data.getBatteryVoltage() != null ? String.format("%.1fV", data.getBatteryVoltage()) : "N/A").append(", ");
                }
                tvIntakeAirTempValue.setText(data.getIntakeTemp() != null ? String.format("%.0f°C", data.getIntakeTemp()) : "N/A");
                logMessage.append("EmmeSıc: ").append(data.getIntakeTemp() != null ? String.format("%.0f°C", data.getIntakeTemp()) : "N/A").append(", ");
                tvMafValue.setText(data.getMafAirFlow() != null && data.getMafAirFlow() >= 0 ? String.format("%.1f g/s", data.getMafAirFlow()) : "N/A");
                logMessage.append("MAF: ").append(data.getMafAirFlow() != null && data.getMafAirFlow() >= 0 ? String.format("%.1f g/s", data.getMafAirFlow()) : "N/A").append(", ");

                updateDTCDisplay(data.getDiagnosticTroubleCodes());
                logMessage.append("DTC: ").append(data.getDiagnosticTroubleCodes() != null ? data.getDiagnosticTroubleCodes().size() : "null").append(", ");

                String currentVinFromOBD = data.getVin();
                logMessage.append("VIN: ").append(currentVinFromOBD != null ? currentVinFromOBD : "N/A");

                if (userVehicleService != null && currentVinFromOBD != null && !currentVinFromOBD.isEmpty() && !currentVinFromOBD.equals(lastProcessedVin)) {
                    Log.d(TAG, "OBD'den yeni/farklı VIN alındı, işleniyor: " + currentVinFromOBD);
                    userVehicleService.updateProfileWithVin(currentVinFromOBD, new UserVehicleService.VinUpdateCallback() {
                        @Override
                        public void onSuccess(String vin, boolean newVinRegistered, Map<String, Object> vehicleDetails) {
                            lastProcessedVin = vin;
                            String toastMsg = (newVinRegistered ? "Yeni araç VIN & detaylar kaydedildi: " : "Araç VIN doğrulandı, detaylar güncel: ") + vin;
                            if (vehicleDetails != null && !vehicleDetails.isEmpty()) {
                                toastMsg += "\n" + vehicleDetails.get(UserVehicleService.FIELD_DETAIL_MAKE) + " " + vehicleDetails.get(UserVehicleService.FIELD_DETAIL_MODEL);
                                updateCarInfoUIFromVinDetails(vehicleDetails);
                            } else if (newVinRegistered) {
                                toastMsg += " (Detaylar alınamadı)";
                            }
                            Toast.makeText(CarActivity.this, toastMsg, Toast.LENGTH_LONG).show();
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
                            } else {
                                Log.w(TAG, "VIN güncel ama Firestore'da detaylar eksik/yok.");
                            }
                        }
                    });
                } else if (currentVinFromOBD != null && currentVinFromOBD.equals(lastProcessedVin)) {
                    // Log.v(TAG, "VIN (" + currentVinFromOBD + ") zaten bu oturumda işlenmişti.");
                }

            } catch (IllegalFormatException ife) {
                Log.e(TAG, "UI güncellenirken formatlama hatası.", ife);
            } catch (Exception e) {
                Log.e(TAG, "UI güncellenirken genel hata.", e);
            }
            Log.d(TAG, logMessage.toString());
        });
    }


    private void updateCarInfoUIFromVinDetails(Map<String, Object> vinDetails) {
        if (vinDetails == null || vinDetails.isEmpty() || tvCarName == null || tvCarYear == null) {
            Log.w(TAG, "updateCarInfoUIFromVinDetails: Detaylar boş veya UI elemanları null.");
            return;
        }
        runOnUiThread(() -> {
            String make = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MAKE);
            String model = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_MODEL);
            String year = (String) vinDetails.get(UserVehicleService.FIELD_DETAIL_YEAR);

            StringBuilder carNameBuilder = new StringBuilder();
            if (make != null && !make.isEmpty()) carNameBuilder.append(make.toUpperCase());
            if (model != null && !model.isEmpty()) {
                if (carNameBuilder.length() > 0) carNameBuilder.append(" ");
                carNameBuilder.append(model);
            }

            if (carNameBuilder.length() > 0) {
                tvCarName.setText(carNameBuilder.toString());
                Log.i(TAG, "Araç adı VIN detaylarından güncellendi: " + carNameBuilder.toString());
            }

            if (year != null && !year.isEmpty()) {
                tvCarYear.setText(year + " Model");
                Log.i(TAG, "Araç yılı VIN detaylarından güncellendi: " + year);
            }
        });
    }

    private void updateDTCDisplay(List<SimpleOBD2Manager.VehicleData.DTC> dtcs) {
        int positiveColor = ContextCompat.getColor(this, R.color.grey_dark);
        int negativeColor = ContextCompat.getColor(this, R.color.negative_status_color);

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
            if (!dtc.isUserUnderstandable) {
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
            CarCareApplication.clearGlobalLastCriticalAlertTimestamps(); // Sıcaklık ve Yakıt cooldown'larını sıfırla
            CarCareApplication.clearNotifiedDtcs(); // Bildirilmiş DTC listesini sıfırla
            Log.d(TAG, "OBD Bağlantısı kapalı, global kritik uyarı zaman damgaları ve bildirilmiş DTC listesi temizlendi.");
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        REQUEST_BLUETOOTH_PERMISSION);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        boolean wasProtocolSpinnerVisible = spinnerProtocol != null && spinnerProtocol.getVisibility() == View.VISIBLE;
        if (CarCareApplication.isObd2Connected()) {
            if (obd2Manager != null) obd2Manager.stopReading();
            if (bluetoothManager != null) bluetoothManager.disconnect();
            CarCareApplication.setObd2Connected(false);
            updateConnectionStatus();
            showDefaultValues();
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
        if (!bluetoothManager.hasBluetoothPermissions()) {
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
                    final boolean finalWasProtocolSpinnerVisible = wasProtocolSpinnerVisible;
                    bluetoothManager.connectToDevice(deviceAddress, new BluetoothManager.ConnectionCallback() {
                        @Override
                        public void onConnectionSuccessful() {
                            CarCareApplication.setObd2Connected(true);
                            runOnUiThread(() -> {
                                updateConnectionStatus();
                                Toast.makeText(CarActivity.this, "OBD2 cihazına bağlandı!", Toast.LENGTH_SHORT).show();
                                if (obd2Manager != null) obd2Manager.startReading();
                                if (finalWasProtocolSpinnerVisible && spinnerProtocol != null && tvProtocolLabel != null) {
                                    spinnerProtocol.setVisibility(View.GONE);
                                    tvProtocolLabel.setVisibility(View.GONE);
                                }
                            });
                        }
                        @Override
                        public void onConnectionFailed(String reason) {
                            CarCareApplication.setObd2Connected(false);
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
        if (obd2Manager != null) {
            obd2Manager.setCriticalDataAlertListener(this);
        }
        setupDataUpdateListener();
        updateConnectionStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy çağrıldı");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause çağrıldı");
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
        String alertType = "HIGH_ENGINE_TEMP"; // Bu alertType, cooldown map'i için anahtar olacak
        if (canSendCriticalAlert(alertType)) { // Sadece 20dk cooldown kontrolü
            String title = "🚨 High Engine Temperature!";
            String message = String.format("Engine temperature: %.0f°C (Threshold: %.0f°C). Please check your vehicle!", temperature, threshold);
            sendAndSaveCriticalAlert(title, message, 201);
            // Sadece bu tip uyarı için cooldown zamanını güncelle
            CarCareApplication.updateGlobalLastCriticalAlertTimestamp(alertType, System.currentTimeMillis());
            Log.i(TAG, "Yüksek motor sıcaklığı bildirimi gönderildi: " + temperature);
        } else {
            Log.d(TAG, "Yüksek motor sıcaklığı (" + temperature + "°C) tespit edildi ancak '" + alertType + "' için bildirim cooldown periyodunda.");
        }
    }

    @Override
    public void onLowFuelLevel(double fuelLevel, double threshold) {
        String alertType = "LOW_FUEL_LEVEL"; // Bu alertType, cooldown map'i için anahtar olacak
        if (canSendCriticalAlert(alertType)) { // Sadece 20dk cooldown kontrolü
            String title = "⛽ Low Fuel Level!";
            String message = String.format("Fuel level: %%%.0f (Threshold: %%%.0f). Please refuel!", fuelLevel, threshold);
            sendAndSaveCriticalAlert(title, message, 202);
            // Sadece bu tip uyarı için cooldown zamanını güncelle
            CarCareApplication.updateGlobalLastCriticalAlertTimestamp(alertType, System.currentTimeMillis());
            Log.i(TAG, "Düşük yakıt seviyesi bildirimi gönderildi: " + fuelLevel);
        } else {
            Log.d(TAG, "Düşük yakıt seviyesi (%" + fuelLevel + ") tespit edildi ancak '" + alertType + "' için bildirim cooldown periyodunda.");
        }
    }

    @Override
    public void onLowBatteryVoltage(double voltage, double threshold, boolean engineRunning) {
        if (!CarCareApplication.hasLowVoltageBeenNotifiedThisSession()) {
            String title = "⚠️ Low Battery Voltage!";
            String engineStatus = engineRunning ? " (Engine Running)" : " (Engine Off)";
            String message = String.format("Battery voltage: %.1fV (Threshold: %.1fV)%s. Check battery or charging system!",
                    voltage, threshold, engineStatus);
            sendAndSaveCriticalAlert(title, message, 205); // Farklı notificationId
            CarCareApplication.setLowVoltageNotifiedThisSession(true); // Bildirildi olarak işaretle
            Log.i(TAG, "Düşük akü voltajı bildirimi gönderildi: " + voltage + engineStatus);
        } else {
            Log.d(TAG, "Düşük akü voltajı (" + voltage + "V) tespit edildi ancak bu oturumda zaten bildirilmişti.");
        }
    }

    @Override
    public void onHighBatteryVoltage(double voltage, double threshold, boolean engineRunning) {
        if (!CarCareApplication.hasHighVoltageBeenNotifiedThisSession()) {
            String title = "⚡ High Battery Voltage!";
            String engineStatus = engineRunning ? " (Engine Running)" : ""; // Yüksek voltaj genellikle motor çalışırken önemlidir
            String message = String.format("Battery voltage: %.1fV (Threshold: %.1fV)%s. Possible overcharging, check charging system!",
                    voltage, threshold, engineStatus);
            sendAndSaveCriticalAlert(title, message, 206); // Farklı notificationId
            CarCareApplication.setHighVoltageNotifiedThisSession(true); // Bildirildi olarak işaretle
            Log.i(TAG, "Yüksek akü voltajı bildirimi gönderildi: " + voltage + engineStatus);
        } else {
            Log.d(TAG, "Yüksek akü voltajı (" + voltage + "V) tespit edildi ancak bu oturumda zaten bildirilmişti.");
        }
    }

    @Override
    public void onNewDtcDetected(List<SimpleOBD2Manager.VehicleData.DTC> newDtcs, List<SimpleOBD2Manager.VehicleData.DTC> allDtcs) {
        // Birden fazla yeni DTC olabileceği için her birini ayrı kontrol et
        boolean didSendAnyDtcNotification = false;
        StringBuilder notifiedCodesMessage = new StringBuilder();

        for (SimpleOBD2Manager.VehicleData.DTC dtc : newDtcs) {
            if (!CarCareApplication.hasDtcBeenNotified(dtc.code)) {
                // Bu DTC daha önce bildirilmemiş, şimdi bildir.
                String title = "🛠️ New Trouble Code Detected!";
                // Tek bir DTC için mesaj oluşturma (isterseniz tüm yeni DTC'leri tek mesajda birleştirebilirsiniz)
                String message = "New trouble code found: " + dtc.code + ": " + dtc.description;

                sendAndSaveCriticalAlert(title, message, 203 + dtc.code.hashCode()); // Her DTC için farklı bir ID oluşturmaya çalışalım
                CarCareApplication.addNotifiedDtc(dtc.code); // Bu DTC'yi bildirilmiş olarak işaretle
                didSendAnyDtcNotification = true;
                notifiedCodesMessage.append(dtc.code).append(" ");
                Log.i(TAG, "Yeni DTC (" + dtc.code + ") için bildirim gönderildi.");
            } else {
                Log.d(TAG, "DTC (" + dtc.code + ") zaten daha önce bildirilmiş, tekrar bildirim atılmıyor.");
            }
        }

        if (didSendAnyDtcNotification) {
            Log.i(TAG, "Yeni DTC bildirimleri gönderildi: " + notifiedCodesMessage.toString().trim());
        } else if (!newDtcs.isEmpty()){
            Log.d(TAG, "Yeni DTC'ler tespit edildi ancak hepsi daha önce bildirilmişti.");
        }
        // DTC'ler için `updateLastCriticalAlertTimestamp` ÇAĞRILMAYACAK, çünkü 20dk cooldown istemiyoruz.
    }

    // Bu metod artık SADECE sıcaklık ve yakıt gibi 20dk cooldown gerektirenler için kullanılacak.
    private boolean canSendCriticalAlert(String alertType) {
        long currentTime = System.currentTimeMillis();
        long lastTime = CarCareApplication.getGlobalLastCriticalAlertTimestamp(alertType);
        if (currentTime - lastTime > CRITICAL_ALERT_COOLDOWN_MS) {
            return true;
        }
        Log.d(TAG, alertType + " için kritik uyarı cooldown süresinde. Son gönderim zamanı: " +
                new Date(lastTime) + ", Mevcut zaman: " + new Date(currentTime));
        return false;
    }

    private void sendAndSaveCriticalAlert(String title, String message, int notificationId) {
        NotificationHelper.showNotification(getApplicationContext(), title, message, notificationId);

        if (firebaseNotificationManager != null) {
            NotificationActivity.NotificationData notificationData = new NotificationActivity.NotificationData();
            notificationData.setTitle(title);
            notificationData.setMessage(message);
            notificationData.setTimestamp(new Date());

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