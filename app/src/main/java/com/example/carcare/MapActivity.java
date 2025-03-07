package com.example.carcare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private EditText editAddress;
    private Button btnSearch;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // RecyclerView (Bottom Sheet) alanları
    private RecyclerView rvPlaces;
    private PlacesAdapter placesAdapter;
    private List<PlaceModel> placeList = new ArrayList<>();

    // Bottom Sheet Behavior
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Marker for current location
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Arama barı elemanları
        editAddress = findViewById(R.id.edit_address);
        btnSearch = findViewById(R.id.btn_search);

        // Bottom Sheet ve RecyclerView ayarı
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        bottomSheetBehavior.setPeekHeight(screenHeight / 3);

        rvPlaces = findViewById(R.id.rv_places);
        rvPlaces.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new PlacesAdapter();
        rvPlaces.setAdapter(placesAdapter);

        // Liste öğesine tıklanınca haritada ilgili konuma zoom yap
        placesAdapter.setOnPlaceClickListener(new OnPlaceClickListener() {
            @Override
            public void onPlaceClick(PlaceModel place) {
                LatLng latLng = new LatLng(place.getLat(), place.getLng());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // Harita Fragment'ı alıp haritayı yükle
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Harita yüklenemedi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Konum istemcisi
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Arama butonuna tıklama
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = editAddress.getText().toString();
                if (!address.isEmpty()) {
                    searchAddress(address);
                } else {
                    Toast.makeText(MapActivity.this, "Lütfen adres giriniz", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Konum güncelleme isteği ayarı
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        // Konum callback ayarı
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null && mMap != null) {
                        updateMapLocation(location);
                        // Yakındaki benzin istasyonlarını getir
                        fetchNearbyGasStations(location);
                        break; // İlk geçerli konumu kullan
                    }
                }

                // Konum güncellemesini durdur
                fusedLocationClient.removeLocationUpdates(this);
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Harita ayarları
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Haritada benzin istasyonu seçildiğinde bottom sheet durumunu güncelle
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null && marker.getTag() instanceof Integer) {
                int position = (int) marker.getTag();
                if (position >= 0 && position < placeList.size()) {
                    // RecyclerView'de ilgili öğeye kaydır
                    rvPlaces.scrollToPosition(position);
                    // Bottom sheet'i genişlet
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
            return false; // false döndürerek varsayılan davranışı da çalıştır
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED && mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    getDeviceLocation();
                }
            } else {
                Toast.makeText(this, "Konum izni verilmedi. Benzin istasyonları gösterilemeyecek.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Cihazın mevcut konumunu alır; null dönerse aktif güncelleme başlatır.
    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    updateMapLocation(location);
                    fetchNearbyGasStations(location);
                } else {
                    // Konum alınamadıysa, güncel konum isteği yap
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Konum alınamadı: " + e.getMessage());
                Toast.makeText(MapActivity.this, "Konum alınamadı, tekrar deneniyor...", Toast.LENGTH_SHORT).show();
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Konum izni hatası: " + e.getMessage());
            Toast.makeText(this, "Konum izni hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Harita konumunu günceller
    private void updateMapLocation(Location location) {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

        // Mevcut konum marker'ını ekle
        TextView bottomSheetTitle = findViewById(R.id.tv_bottom_sheet_title);
        if (bottomSheetTitle != null) {
            bottomSheetTitle.setText("Yakınlardaki Benzin İstasyonları");
        }
    }

    // Girilen adresi geocode edip haritada gösterir
    private void searchAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address result = addresses.get(0);
                LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                currentLocation = latLng; // Aranan konum şimdi mevcut konum olsun
                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Aradığınız Konum")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                Location tempLocation = new Location("");
                tempLocation.setLatitude(result.getLatitude());
                tempLocation.setLongitude(result.getLongitude());
                fetchNearbyGasStations(tempLocation);

                // Bottom sheet başlığını güncelle
                TextView bottomSheetTitle = findViewById(R.id.tv_bottom_sheet_title);
                if (bottomSheetTitle != null) {
                    bottomSheetTitle.setText(address + " yakınındaki benzin istasyonları");
                }
            } else {
                Toast.makeText(this, "Adres bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } catch(IOException e) {
            Log.e(TAG, "Adres arama hatası: " + e.getMessage());
            Toast.makeText(this, "Adres araması yapılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Retrofit kullanarak yakınlardaki benzin istasyonlarını getirir.
    private void fetchNearbyGasStations(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String locationStr = lat + "," + lng;
        int radius = 5000; // 5 km - arttırıldı
        String type = "gas_station";

        // AndroidManifest.xml dosyasından Google Maps API anahtarınızı kullanın
        // Bu örnekte, meta-data kısmında tanımladığınız API anahtarını kullanıyoruz
        String apiKey = "AIzaSyAwVxJdi9hYUBSu9g07lLeP5B8d_pUHobo";

        // Loading göster
        showLoading(true);

        GooglePlacesApi apiService = RetrofitClient.getClient().create(GooglePlacesApi.class);
        apiService.getNearbyPlaces(locationStr, radius, type, apiKey)
                .enqueue(new Callback<NearbySearchResponse>() {
                    @Override
                    public void onResponse(Call<NearbySearchResponse> call, Response<NearbySearchResponse> response) {
                        showLoading(false);

                        if(response.isSuccessful() && response.body() != null) {
                            List<NearbySearchResponse.PlaceResult> results = response.body().getResults();

                            if (results.isEmpty()) {
                                Toast.makeText(MapActivity.this, "Bu bölgede benzin istasyonu bulunamadı", Toast.LENGTH_SHORT).show();
                                placeList.clear();
                                placesAdapter.setPlaceList(placeList);
                                return;
                            }

                            List<PlaceModel> placeModelList = new ArrayList<>();
                            for (NearbySearchResponse.PlaceResult result : results) {
                                double resultLat = result.getGeometry().getLocation().getLat();
                                double resultLng = result.getGeometry().getLocation().getLng();
                                String name = result.getName();
                                String vicinity = result.getVicinity();
                                placeModelList.add(new PlaceModel(name, vicinity, resultLat, resultLng));
                            }
                            updateUIWithPlaces(placeModelList);

                            // Bottom sheet'i göster
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        } else {
                            Log.e(TAG, "API cevabı başarısız: " + response.code());
                            Toast.makeText(MapActivity.this, "Benzin istasyonları alınamadı. Kod: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<NearbySearchResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "API hatası: " + t.getMessage());
                        Toast.makeText(MapActivity.this, "Benzin istasyonları getirilemedi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Loading göster/gizle
    private void showLoading(boolean isLoading) {
        View loadingView = findViewById(R.id.loading_view);
        if (loadingView != null) {
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    // Gelen yer verilerini haritaya marker olarak ekler ve RecyclerView'e aktarır.
    private void updateUIWithPlaces(List<PlaceModel> newPlaces) {
        mMap.clear();
        placeList = newPlaces;
        placesAdapter.setPlaceList(placeList);

        // Mevcut konum marker'ı ekle
        if (currentLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Mevcut Konum")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Benzin istasyonlarını ekle
        for (int i = 0; i < placeList.size(); i++) {
            PlaceModel place = placeList.get(i);
            LatLng latLng = new LatLng(place.getLat(), place.getLng());

            // Benzin istasyonu ikonunu ekle
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(place.getName())
                    .snippet(place.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            // Marker'a index tag'i ekle (RecyclerView ile eşleştirmek için)
            mMap.addMarker(markerOptions).setTag(i);
        }

        // Benzin istasyonu sayısını göster
        Toast.makeText(this, placeList.size() + " benzin istasyonu bulundu", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Uygulamadan çıkıldığında konum güncellemelerini durdur
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // --- Inner Class: PlaceModel ---
    public class PlaceModel {
        private String name;
        private String address;
        private double lat;
        private double lng;

        public PlaceModel(String name, String address, double lat, double lng) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }

        public String getName() { return name; }
        public String getAddress() { return address; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }

    // --- Inner Interface: OnPlaceClickListener ---
    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceModel place);
    }

    // --- Inner Class: PlacesAdapter ---
    public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {
        private List<PlaceModel> placeList = new ArrayList<>();
        private OnPlaceClickListener listener;

        public void setPlaceList(List<PlaceModel> list) {
            this.placeList = list;
            notifyDataSetChanged();
        }

        public void setOnPlaceClickListener(OnPlaceClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_place, parent, false);
            return new PlaceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
            PlaceModel place = placeList.get(position);
            holder.tvName.setText(place.getName());
            holder.tvAddress.setText(place.getAddress());

            // Kaç km uzakta olduğunu hesapla ve göster
            if (currentLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.latitude, currentLocation.longitude,
                        place.getLat(), place.getLng(),
                        results
                );
                float distanceInMeters = results[0];
                String distance = String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000);
                holder.tvDistance.setText(distance);
                holder.tvDistance.setVisibility(View.VISIBLE);
            } else {
                holder.tvDistance.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });
        }

        @Override
        public int getItemCount() {
            return placeList.size();
        }

        public class PlaceViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress, tvDistance;

            public PlaceViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_place_name);
                tvAddress = itemView.findViewById(R.id.tv_place_address);
                tvDistance = itemView.findViewById(R.id.tv_place_distance);
            }
        }
    }

    // =============================================
    // Aşağıda RetrofitClient, GooglePlacesApi ve NearbySearchResponse inner sınıfları tanımlanmıştır.
    // =============================================

    public static class RetrofitClient {
        private static Retrofit retrofit = null;
        private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place/";

        public static Retrofit getClient() {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            return retrofit;
        }
    }

    public interface GooglePlacesApi {
        @GET("nearbysearch/json")
        Call<NearbySearchResponse> getNearbyPlaces(
                @Query("location") String location,
                @Query("radius") int radius,
                @Query("type") String type,
                @Query("key") String key
        );
    }

    public static class NearbySearchResponse {
        private List<PlaceResult> results;
        private String status;

        public List<PlaceResult> getResults() {
            return results != null ? results : new ArrayList<>();
        }

        public String getStatus() {
            return status;
        }

        public static class PlaceResult {
            private String name;
            private String vicinity;
            private Geometry geometry;

            public String getName() {
                return name;
            }

            public String getVicinity() {
                return vicinity;
            }

            public Geometry getGeometry() {
                return geometry;
            }
        }

        public static class Geometry {
            private LocationData location;

            public LocationData getLocation() {
                return location;
            }
        }

        public static class LocationData {
            private double lat;
            private double lng;

            public double getLat() {
                return lat;
            }

            public double getLng() {
                return lng;
            }
        }
    }
}