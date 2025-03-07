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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carcare.api.GooglePlacesApi;
import com.example.carcare.api.RetrofitClient;
import com.example.carcare.api.NearbySearchResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map); // Yukarıdaki XML tasarımınız

        // Arama barı elemanları (CardView içindeki EditText ve Button)
        editAddress = findViewById(R.id.edit_address);
        btnSearch = findViewById(R.id.btn_search);

        // Bottom Sheet ve RecyclerView ayarlaması
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
        // Dinamik olarak peekHeight ayarlaması (örneğin, ekran yüksekliğinin üçte biri)
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // Harita Fragment’ı alıp haritayı yükle
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        // Konum güncelleme isteği
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        // Konum callback ayarı
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null && mMap != null) {
                    updateMapLocation(location);
                    fusedLocationClient.removeLocationUpdates(this);
                    // Demo veya gerçek API çağrısıyla, yakındaki benzin istasyonlarını getir
                    fetchNearbyGasStations(location);
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getDeviceLocation();
                }
            } else {
                Toast.makeText(this, "Konum izni verilmedi.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Cihazın mevcut konumunu alır; null dönerse aktif güncelleme başlatır.
    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    updateMapLocation(location);
                    fetchNearbyGasStations(location);
                } else {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Haritayı günceller: Kamera konumunu ayarlar ve marker ekler.
    private void updateMapLocation(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Şu an buradasınız"));
    }

    // Girilen adresi geocode edip haritada gösterir.
    // Aynı zamanda, arama sonucunda elde edilen konumun çevresindeki benzin istasyonlarını getirmek için fetchNearbyGasStations çağrılır.
    private void searchAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address result = addresses.get(0);
                LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                mMap.addMarker(new MarkerOptions().position(latLng).title("Aradığınız Konum"));
                // Arama sonucu koordinatları ile yakındaki yerleri getir:
                Location tempLocation = new Location("");
                tempLocation.setLatitude(result.getLatitude());
                tempLocation.setLongitude(result.getLongitude());
                fetchNearbyGasStations(tempLocation);
            } else {
                Toast.makeText(this, "Adres bulunamadı", Toast.LENGTH_SHORT).show();
            }
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Yakındaki benzin istasyonlarını getirir.
    // Gerçek uygulamada Google Places Nearby Search API çağrısı yapılacaktır.
    // Aşağıdaki örnekte Retrofit kullanılarak API çağrısı gerçekleştirilmiştir.
    private void fetchNearbyGasStations(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String locationStr = lat + "," + lng;
        int radius = 1500; // metre cinsinden
        String type = "gas_station";
        String apiKey = "AIzaSyAwVxJdi9hYUBSu9g07lLeP5B8d_pUHobo"; // API anahtarınızı buraya ekleyin

        GooglePlacesApi apiService = RetrofitClient.getClient().create(GooglePlacesApi.class);
        apiService.getNearbyPlaces(locationStr, radius, type, apiKey)
                .enqueue(new retrofit2.Callback<NearbySearchResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<NearbySearchResponse> call, retrofit2.Response<NearbySearchResponse> response) {
                        if(response.isSuccessful() && response.body() != null) {
                            List<NearbySearchResponse.PlaceResult> results = response.body().getResults();
                            List<PlaceModel> placeModelList = new ArrayList<>();
                            for (NearbySearchResponse.PlaceResult result : results) {
                                double resultLat = result.getGeometry().getLocation().getLat();
                                double resultLng = result.getGeometry().getLocation().getLng();
                                String name = result.getName();
                                String vicinity = result.getVicinity();
                                // Inner PlaceModel ile uyumlu hale getiriyoruz
                                placeModelList.add(new PlaceModel(name, vicinity, resultLat, resultLng));
                            }
                            updateUIWithPlaces(placeModelList);
                        } else {
                            Toast.makeText(MapActivity.this, "Yerler alınamadı.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<NearbySearchResponse> call, Throwable t) {
                        Toast.makeText(MapActivity.this, "Hata: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Gelen yer verilerini haritaya marker olarak ekler ve RecyclerView'e aktarır.
    private void updateUIWithPlaces(List<PlaceModel> newPlaces) {
        mMap.clear();
        placeList = newPlaces;
        placesAdapter.setPlaceList(placeList);
        for (PlaceModel place : placeList) {
            LatLng latLng = new LatLng(place.getLat(), place.getLng());
            mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
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
            TextView tvName, tvAddress;
            public PlaceViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_place_name);
                tvAddress = itemView.findViewById(R.id.tv_place_address);
            }
        }
    }
}
