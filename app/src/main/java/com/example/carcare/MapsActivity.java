package com.example.carcare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carcare.ProfilePage.ProfileActivity;
import com.example.carcare.adapters.NearbyPlacesAdapter;
import com.example.carcare.models.NearbyPlace;
import com.example.carcare.services.NearbyPlacesService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, NearbyPlacesAdapter.PlaceClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MapsActivity";

    private String PLACES_API_KEY;

    private static final float DEFAULT_ZOOM = 15f;
    private static final double SEARCH_RADIUS_KM = 3.0;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean locationPermissionGranted = false;
    private EditText searchInput;
    private LatLng currentLocation;

    // Panel components
    private LinearLayout placesHeaderContainer;
    private FrameLayout placesPanelContainer;
    private TabLayout placeCategoryTabs;
    private RecyclerView nearbyPlacesRecyclerView;
    private ProgressBar placesLoadingProgress;
    private TextView emptyPlacesView;
    private boolean isPanelExpanded = false;

    // Places components
    private NearbyPlacesService placesService;
    private NearbyPlacesAdapter placesAdapter;
    private Map<String, Marker> placeMarkers;
    private String currentPlaceType = NearbyPlace.Type.GAS; // Default to gas stations
    private boolean isUsingDeviceLocation = true; // Varsayılan olarak cihaz konumu kullanılıyor
    private Marker searchLocationMarker; // Arama konumu işaretçisi



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        PLACES_API_KEY = getString(R.string.google_places_api_key);


        Log.d(TAG, "MapsActivity onCreate");

        // Places API ilklendirme kontrolü
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), PLACES_API_KEY);
            Log.d(TAG, "Places API initialized");
        } else {
            Log.d(TAG, "Places API already initialized");
        }

        // Konum sağlayıcı istemcisini başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Places servisini başlat
        placesService = new NearbyPlacesService(this, PLACES_API_KEY);
        placeMarkers = new HashMap<>();

        // UI bileşenlerini kur
        setupSearchFunctionality();
        setupBottomNavigation();
        setupPlacesPanel();

        // Konumuma dön butonu
        FloatingActionButton myLocationButton = findViewById(R.id.my_location_button);
        myLocationButton.setOnClickListener(v -> {
            // Konum izni kontrolü
            if (locationPermissionGranted) {
                // Cihaz konumunu al ve göster
                isUsingDeviceLocation = true;
                getDeviceLocation();
            } else {
                // İzin yoksa, izin iste
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show();
                getLocationPermission();
            }
        });

        // Konum izni al
        getLocationPermission();

        // Haritayı başlat
        initializeMap();
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_map);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(MapsActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(MapsActivity.this, StoreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(MapsActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MapsActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupPlacesPanel() {
        Log.d(TAG, "setupPlacesPanel çağrıldı.");
        // Find panel components
        placesHeaderContainer = findViewById(R.id.places_header_container);
        placesPanelContainer = findViewById(R.id.places_panel_container);
        placeCategoryTabs = findViewById(R.id.places_category_tabs);
        nearbyPlacesRecyclerView = findViewById(R.id.nearby_places_recyclerview);
        placesLoadingProgress = findViewById(R.id.places_loading_progress);
        emptyPlacesView = findViewById(R.id.empty_places_view);

        if (placeCategoryTabs == null) {
            Log.e(TAG, "setupPlacesPanel: placeCategoryTabs is null! UI düzgün yüklenmemiş olabilir.");
            // Hata durumuyla başa çıkmak için Toast gösterebilir veya aktiviteyi sonlandırabilirsiniz.
            Toast.makeText(this, "Harita bileşenleri yüklenemedi.", Toast.LENGTH_LONG).show();
            return; // placeCategoryTabs null ise devam etme
        }

        // Set up RecyclerView
        nearbyPlacesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new NearbyPlacesAdapter(this); // 'this' (Context) null olmamalı
        placesAdapter.setPlaceClickListener(this);
        nearbyPlacesRecyclerView.setAdapter(placesAdapter);

        // Make header clickable to toggle panel
        placesHeaderContainer.setOnClickListener(v -> togglePlacesPanel());

        // Set up tab selection listener
        placeCategoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Sekme seçildi: " + tab.getText() + " (Pozisyon: " + tab.getPosition() + ")");
                // Determine selected category
                switch (tab.getPosition()) {
                    case 0:
                        currentPlaceType = NearbyPlace.Type.GAS;
                        break;
                    case 1:
                        currentPlaceType = NearbyPlace.Type.SERVICE;
                        break;
                    case 2:
                        currentPlaceType = NearbyPlace.Type.WASH;
                        break;
                    case 3:
                        currentPlaceType = NearbyPlace.Type.PARTS;
                        break;
                    default:
                        Log.w(TAG, "Bilinmeyen sekme pozisyonu: " + tab.getPosition());
                        currentPlaceType = NearbyPlace.Type.GAS; // Varsayılana dön
                        break;
                }

                // Eğer harita ve konum hazırsa (currentLocation null değilse) yerleri yükle
                if (mMap != null && currentLocation != null) {
                    Log.d(TAG, "OnTabSelected: Konum mevcut, '" + currentPlaceType + "' için yerler yükleniyor.");
                    clearPlaceMarkers(); // Önceki işaretçileri temizle
                    loadNearbyPlaces();  // Yeni kategori için yerleri yükle

                    if (!isPanelExpanded) {
                        expandPanel();
                    }
                } else {
                    Log.w(TAG, "OnTabSelected: Konum mevcut değil, yerler yüklenemiyor. (mMap: " + (mMap != null) + ", currentLocation: " + (currentLocation != null) + ")");
                    if (mMap != null) { // Harita hazırsa ama konum yoksa mesaj göster
                        Toast.makeText(MapsActivity.this,
                                "Konum bilgisi bekleniyor veya arama yapmanız gerekiyor.",
                                Toast.LENGTH_SHORT).show();
                    }
                    // Panel açıksa ve konum yoksa, belki paneli kapatmak veya boş göstermek daha iyi olabilir.
                    // collapsePanel(); // Opsiyonel: Konum yoksa paneli kapat
                    placesAdapter.updatePlaces(new ArrayList<>()); // Liste boşaltılsın
                    emptyPlacesView.setText("Lütfen bir konum seçin veya konum izni verin.");
                    emptyPlacesView.setVisibility(View.VISIBLE);
                    placesLoadingProgress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Genellikle burada bir işlem yapmaya gerek yok
                Log.d(TAG, "Sekme seçimi kaldırıldı: " + tab.getText());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.d(TAG, "Sekme yeniden seçildi: " + tab.getText());
                // Sekme yeniden seçildiğinde davranışı tanımla
                if (mMap != null && currentLocation != null) {
                    // Panelin durumunu değiştir (açıksa kapat, kapalıysa aç ve yerleri yükle)
                    if (isPanelExpanded) {
                        collapsePanel();
                    } else {
                        Log.d(TAG, "OnTabReselected: Konum mevcut, '" + currentPlaceType + "' için yerler yeniden yükleniyor ve panel açılıyor.");
                        clearPlaceMarkers();
                        loadNearbyPlaces(); // Verileri tazelemek için
                        expandPanel();
                    }
                } else {
                    Toast.makeText(MapsActivity.this,
                            "Konum bilgisi bekleniyor.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (placeCategoryTabs.getTabCount() > 0) {
            TabLayout.Tab firstTab = placeCategoryTabs.getTabAt(0);
            if (firstTab != null) {
                Log.d(TAG, "setupPlacesPanel: Varsayılan olarak ilk sekme seçiliyor: " + firstTab.getText());
                firstTab.select();
            } else {
                Log.e(TAG, "setupPlacesPanel: İlk sekme null, seçilemiyor.");
            }
        } else {
            Log.e(TAG, "setupPlacesPanel: Hiç sekme yok!");
        }
    }

    private void togglePlacesPanel() {
        if (isPanelExpanded) {
            collapsePanel();
        } else {
            expandPanel();
        }
    }

    private void expandPanel() {
        if (!isPanelExpanded) {
            placesPanelContainer.setVisibility(View.VISIBLE);
            isPanelExpanded = true;

            // Load places if location is available
            if (currentLocation != null) {
                loadNearbyPlaces();
            }
        }
    }

    private void collapsePanel() {
        if (isPanelExpanded) {
            placesPanelContainer.setVisibility(View.GONE);
            isPanelExpanded = false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady çağrıldı.");

        // UI ayarları
        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
        } else {
            Log.e(TAG, "onMapReady: GoogleMap nesnesi null!");
            Toast.makeText(this, "Harita yüklenirken bir sorun oluştu.", Toast.LENGTH_SHORT).show();
            return; // mMap null ise devam etmenin anlamı yok
        }

        // Konum izni durumuna göre arayüzü güncelle
        updateLocationUI(); // Bu metod içinde mMap null kontrolü olmalı veya burada yapılmalı

        // İzin verildiyse cihaz konumunu al
        if (locationPermissionGranted) {
            Log.d(TAG, "onMapReady: Konum izni var, cihaz konumu alınıyor.");
            getDeviceLocation(); // Bu metod currentLocation'ı ayarlar ve haritayı hareket ettirir
        } else {
            // İzin yoksa ve mevcut bir konumumuz da yoksa (örneğin arama sonucu)
            // varsayılan bir konuma ayarla.
            if (currentLocation == null) {
                Log.d(TAG, "onMapReady: Konum izni yok ve currentLocation null, varsayılan konuma ayarlanıyor (Ankara).");
                currentLocation = new LatLng(39.9334, 32.8597); // Ankara
                isUsingDeviceLocation = false; // Cihaz konumu kullanılmıyor
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
            } else {
                // Eğer izin yok ama bir arama sonucuyla currentLocation ayarlandıysa
                // o konumu kullanmaya devam et.
                Log.d(TAG, "onMapReady: Konum izni yok ama currentLocation mevcut, harita hareket ettiriliyor.");
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
            }
        }

        // Harita tıklama dinleyicisi
        mMap.setOnMapClickListener(latLng -> {
            if (isPanelExpanded) {
                collapsePanel();
            }
        });

        // İşaretçi tıklama dinleyicisi
        mMap.setOnMarkerClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null && !placeId.equals("SEARCH_LOCATION_MARKER_TAG")) { // Arama işaretçisi değilse
                highlightPlaceInList(placeId);
                expandPanel();
                // marker.showInfoWindow(); // showInfoWindow zaten varsayılan davranışta olabilir.
                // Eğer highlightPlaceInList yavaşsa, önce info window gösterilebilir.
            } else if (placeId != null && placeId.equals("SEARCH_LOCATION_MARKER_TAG")) {
                // Arama işaretçisine tıklandı, bir şey yapmaya gerek yok veya bilgi penceresini göster
                marker.showInfoWindow();
            }
            return false;
        });

        Log.d(TAG, "onMapReady: handleIntentData çağrılıyor.");
        handleIntentData();
    }

    // MapsActivity.java sınıfının içine yeni bir metod
    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TARGET_PLACE_TYPE")) {
            String targetType = intent.getStringExtra("TARGET_PLACE_TYPE");
            Log.d(TAG, "Intent'ten gelen TARGET_PLACE_TYPE: " + targetType);

            if (targetType != null) {
                // Gelen tipe göre ilgili sekmeyi seç
                int tabIndexToSelect = -1;
                if (targetType.equals(NearbyPlace.Type.GAS)) {
                    tabIndexToSelect = 0;
                    currentPlaceType = NearbyPlace.Type.GAS;
                } else if (targetType.equals(NearbyPlace.Type.SERVICE)) {
                    tabIndexToSelect = 1;
                    currentPlaceType = NearbyPlace.Type.SERVICE;
                } else if (targetType.equals(NearbyPlace.Type.WASH)) {
                    tabIndexToSelect = 2;
                    currentPlaceType = NearbyPlace.Type.WASH;
                } else if (targetType.equals(NearbyPlace.Type.PARTS)) {
                    tabIndexToSelect = 3;
                    currentPlaceType = NearbyPlace.Type.PARTS;
                }

                if (tabIndexToSelect != -1 && placeCategoryTabs != null) {
                    TabLayout.Tab tab = placeCategoryTabs.getTabAt(tabIndexToSelect);
                    if (tab != null) {
                        Log.d(TAG, "Sekme seçiliyor: " + tab.getText());
                        tab.select(); // Bu, OnTabSelectedListener'ı tetikleyecektir.
                        // OnTabSelectedListener zaten loadNearbyPlaces() ve expandPanel() çağırıyor.
                    }
                } else if (placeCategoryTabs == null) {
                    Log.e(TAG, "handleIntentData: placeCategoryTabs is null!");
                } else {
                    Log.w(TAG, "handleIntentData: Geçersiz veya bulunamayan TARGET_PLACE_TYPE için sekme: " + targetType);
                }
            }
        } else {
            if (currentLocation != null && placeCategoryTabs != null && placeCategoryTabs.getSelectedTabPosition() == 0) {
                Log.d(TAG, "Intent'te hedef yok, varsayılan (GAS) için yerler yükleniyor.");
            }
        }
    }

    private void highlightPlaceInList(String placeId) {
        // Eğer bu bir yer işaretçisi ise (placeId değeri varsa)
        if (placeId != null) {
            // Listede yer ID'sine göre pozisyonu bul
            int position = -1;
            for (int i = 0; i < placesAdapter.getItemCount(); i++) {
                NearbyPlace place = placesAdapter.getPlaceAtPosition(i);
                if (place != null && place.getId().equals(placeId)) {
                    position = i;
                    break;
                }
            }

            // Eğer pozisyon bulunduysa, o pozisyona kaydır
            if (position != -1) {
                nearbyPlacesRecyclerView.smoothScrollToPosition(position);
            }
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            Log.d(TAG, "Location permission already granted");
        } else {
            // İzin verilmeden önce açıklama göstermek için
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Kullanıcıya izin talep gerekçesini açıklayalım
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Konum İzni Gerekli")
                        .setMessage("Bu uygulama, çevrenizdeki benzin istasyonları, servisler ve diğer araç bakım noktalarını göstermek için konum bilginize ihtiyaç duyar.")
                        .setPositiveButton("Tamam", (dialogInterface, i) -> {
                            // İzin isteyelim
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        })
                        .create()
                        .show();
            } else {
                // Doğrudan izin isteyelim
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
            Log.d(TAG, "Requesting location permission");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        locationPermissionGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                Log.d(TAG, "Location permission granted");

                // Arayüzü güncelle
                updateLocationUI();

                // Cihaz konumunu al
                getDeviceLocation();
            } else {
                Log.w(TAG, "Location permission denied");
                Toast.makeText(this,
                        "Konum izni verilmedi. Arama çubuğunu kullanarak konum araması yapabilirsiniz.",
                        Toast.LENGTH_LONG).show();

                // İzin olmasa da varsayılan konumla devam et
                if (currentLocation == null) {
                    currentLocation = new LatLng(39.9334, 32.8597); // Ankara
                    isUsingDeviceLocation = false;

                    // Harita hazırsa, konuma git
                    if (mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                    }
                }
            }
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                Log.d(TAG, "Getting device location...");
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                // Cihaz konumunu ayarla
                                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                isUsingDeviceLocation = true;

                                Log.d(TAG, "Device location obtained: " + currentLocation.latitude + "," + currentLocation.longitude);

                                // Arama konum işaretçisini kaldır
                                if (searchLocationMarker != null) {
                                    searchLocationMarker.remove();
                                    searchLocationMarker = null;
                                }

                                // Harita hazırsa, konuma git
                                if (mMap != null) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                                }

                                // Yakınlardaki yerleri yükle
                                loadNearbyPlaces();
                            } else {
                                Log.w(TAG, "Device location is null");
                                Toast.makeText(MapsActivity.this, "Konum bilgisi alınamadı. Arama çubuğunu kullanabilirsiniz.", Toast.LENGTH_LONG).show();

                                // Eğer daha önce hiçbir konum yoksa, varsayılan bir konum ayarla (Örneğin: Ankara)
                                if (currentLocation == null) {
                                    Log.d(TAG, "Setting default location (Ankara)");
                                    currentLocation = new LatLng(39.9334, 32.8597); // Ankara
                                    isUsingDeviceLocation = false;

                                    // Harita hazırsa, konuma git
                                    if (mMap != null) {
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                                    }

                                    // Yakındaki yerleri yükle
                                    loadNearbyPlaces();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting device location", e);
                            Toast.makeText(MapsActivity.this, "Konum alınamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                            // Eğer daha önce hiçbir konum yoksa, varsayılan bir konum ayarla (Örneğin: Ankara)
                            if (currentLocation == null) {
                                Log.d(TAG, "Setting default location after error (Ankara)");
                                currentLocation = new LatLng(39.9334, 32.8597); // Ankara
                                isUsingDeviceLocation = false;

                                // Harita hazırsa, konuma git
                                if (mMap != null) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                                }

                                // Yakındaki yerleri yükle
                                loadNearbyPlaces();
                            }
                        });
            } else {
                Log.w(TAG, "Location permission not granted");

                // Eğer konum izni yoksa, varsayılan bir konum ayarla (Örneğin: Ankara)
                if (currentLocation == null) {
                    Log.d(TAG, "Setting default location (no permission) (Ankara)");
                    currentLocation = new LatLng(39.9334, 32.8597); // Ankara
                    isUsingDeviceLocation = false;

                    // Harita hazırsa, konuma git
                    if (mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                    }

                    // Yakındaki yerleri yükle
                    loadNearbyPlaces();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
            Toast.makeText(this, "Güvenlik hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSearchFunctionality() {
        searchInput = findViewById(R.id.search_input);
        ImageButton searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> performSearch());

        searchInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Lütfen arama kriterini girin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Klavyeyi gizle
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

        // Yükleme göstergesini göster
        placesLoadingProgress.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Aranıyor: " + query, Toast.LENGTH_SHORT).show();

        // Adresle konum ara
        searchLocationByName(query);
    }

    private void searchLocationByName(String locationName) {
        // Geocoder ile adresi koordinatlara çevir
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Koordinatları al
                LatLng searchedLocation = new LatLng(
                        address.getLatitude(),
                        address.getLongitude()
                );

                // Log bilgisi ekle
                Log.d(TAG, "Address found: " + address.getAddressLine(0) +
                        " at " + searchedLocation.latitude + "," + searchedLocation.longitude);

                // Güncel konumu güncelle
                currentLocation = searchedLocation;
                isUsingDeviceLocation = false; // Artık cihaz konumu değil, aranan konum kullanılıyor

                // Harita hazırsa, konuma git ve işaretçi ekle
                if (mMap != null) {
                    // Arama konumu işaretçisini güncelle
                    if (searchLocationMarker != null) {
                        searchLocationMarker.remove();
                    }

                    // Aranan konumu haritada göster
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            currentLocation, DEFAULT_ZOOM));

                    // Aranan konuma işaretçi ekle
                    searchLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLocation)
                            .title(address.getAddressLine(0))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    // Önce yükleme göstergesini gizle
                    placesLoadingProgress.setVisibility(View.GONE);

                    // Yakındaki yerleri yükle - DEBUG mesajı ekle
                    Log.d(TAG, "Loading nearby places from searchLocationByName for: " + currentPlaceType);

                    // Seçilen sekmeye göre yakındaki yerleri yükle
                    loadNearbyPlaces();

                    // Paneli aç
                    expandPanel();
                }
            } else {
                // Konum bulunamadı
                Log.w(TAG, "No location found for: " + locationName);
                Toast.makeText(this, "Konum bulunamadı: " + locationName, Toast.LENGTH_SHORT).show();
                placesLoadingProgress.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            Toast.makeText(this, "Konum arama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            placesLoadingProgress.setVisibility(View.GONE);
        }
    }

    private void loadNearbyPlaces() {
        if (currentLocation == null) {
            Log.w(TAG, "Cannot load places - currentLocation is null");
            Toast.makeText(this, "Konum bilgisi mevcut değil.", Toast.LENGTH_SHORT).show();
            return;
        }

        // UI durumunu güncelleyin
        placesLoadingProgress.setVisibility(View.VISIBLE);
        emptyPlacesView.setVisibility(View.GONE);

        // Mevcut seçilen kategoriyi log'a yazın
        Log.d(TAG, "Loading places of type: " + currentPlaceType +
                " at location: " + currentLocation.latitude + ", " + currentLocation.longitude);

        // Temizlik işlemleri
        clearPlaceMarkers();
        placesAdapter.updatePlaces(new ArrayList<>()); // Listeyi temizle

        // Places servisini çağır
        placesService.findNearbyPlaces(currentLocation, currentPlaceType, new NearbyPlacesService.NearbyPlacesCallback() {
            @Override
            public void onPlacesFound(List<NearbyPlace> places) {
                placesLoadingProgress.setVisibility(View.GONE);

                // Sonuçları log'a yaz
                Log.d(TAG, "Places found: " + places.size());
                if (!places.isEmpty()) {
                    Log.d(TAG, "First place: " + places.get(0).getName() + " at " +
                            places.get(0).getLocation().latitude + ", " + places.get(0).getLocation().longitude);
                }

                // Adaptörü güncelle
                placesAdapter.updatePlaces(places);

                // Haritada göster
                showPlacesOnMap(places);

                // Boş durum kontrolü
                if (places.isEmpty()) {
                    emptyPlacesView.setVisibility(View.VISIBLE);
                    emptyPlacesView.setText("Bu bölgede " + getPlaceTypeDisplayName(currentPlaceType) + " bulunamadı");
                } else {
                    emptyPlacesView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                placesLoadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error loading places: " + errorMessage);

                // Hata durumunu göster
                Toast.makeText(MapsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                placesAdapter.updatePlaces(new ArrayList<>());
                emptyPlacesView.setVisibility(View.VISIBLE);
                emptyPlacesView.setText("Yerler yüklenirken bir hata oluştu: " + errorMessage);
            }
        });
    }

    // Yer türünü görüntüleme adı olarak döndüren yardımcı metot
    private String getPlaceTypeDisplayName(String placeType) {
        switch (placeType) {
            case NearbyPlace.Type.GAS:
                return "benzin istasyonu";
            case NearbyPlace.Type.SERVICE:
                return "servis merkezi";
            case NearbyPlace.Type.WASH:
                return "araç yıkama";
            case NearbyPlace.Type.PARTS:
                return "yedek parça";
            default:
                return "yer";
        }
    }

    private void showPlacesOnMap(List<NearbyPlace> places) {
        if (mMap == null) {
            Log.w(TAG, "Cannot show places on map - map is null");
            return;
        }

        Log.d(TAG, "Showing " + places.size() + " places on map");

        // Sadece yer işaretçilerini temizle (arama işaretçisini koru)
        clearPlaceMarkers();

        // Her yer için işaretçi ekle
        for (NearbyPlace place : places) {
            // İşaretçi seçeneklerini oluştur
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(place.getLocation())
                    .title(place.getName())
                    .snippet(place.getFormattedDistance());

            // Yer tipine göre simge ayarla
            float markerColor;
            switch (place.getType()) {
                case NearbyPlace.Type.GAS:
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    break;
                case NearbyPlace.Type.SERVICE:
                    markerColor = BitmapDescriptorFactory.HUE_BLUE;
                    break;
                case NearbyPlace.Type.WASH:
                    markerColor = BitmapDescriptorFactory.HUE_CYAN;
                    break;
                case NearbyPlace.Type.PARTS:
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                default:
                    markerColor = BitmapDescriptorFactory.HUE_RED;
            }

            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(markerColor));

            // İşaretçiyi haritaya ekle
            Marker marker = mMap.addMarker(markerOptions);

            // Yer ID'sini işaretçi etiketi olarak ayarla
            if (marker != null) {
                marker.setTag(place.getId());
                placeMarkers.put(place.getId(), marker);
                Log.d(TAG, "Added marker for place: " + place.getName() + " at " + place.getLocation().latitude + "," + place.getLocation().longitude);
            } else {
                Log.w(TAG, "Failed to add marker for place: " + place.getName());
            }
        }
    }

    // clearPlaceMarkers metodunu güncelleyelim - sadece yer işaretçilerini temizle, arama işaretçisini değil
    private void clearPlaceMarkers() {
        // Tüm yer işaretçilerini haritadan kaldır
        for (Marker marker : placeMarkers.values()) {
            marker.remove();
        }

        // İşaretçiler haritasını temizle
        placeMarkers.clear();
    }

    @Override
    public void onPlaceClick(NearbyPlace place) {
        // When a place is clicked in the list, center the map on it and show info window
        if (mMap != null) {
            Marker marker = placeMarkers.get(place.getId());
            if (marker != null) {
                // Animate camera to the marker
                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));

                // Show info window
                marker.showInfoWindow();
            }
        }
    }
}