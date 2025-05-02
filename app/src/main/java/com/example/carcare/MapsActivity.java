package com.example.carcare;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.carcare.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Benzin istasyonu modeli
class GasStation {
    private String name;
    private LatLng location;
    private double distance; // kilometre cinsinden

    public GasStation(String name, LatLng location, double distance) {
        this.name = name;
        this.location = location;
        this.distance = distance;
    }

    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return location;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}

// RecyclerView Adapter'ı
class GasStationAdapter extends RecyclerView.Adapter<GasStationAdapter.GasStationViewHolder> {

    private List<GasStation> stations;
    private Context context;
    private OnStationClickListener listener;

    public interface OnStationClickListener {
        void onStationClick(GasStation station);
    }

    public GasStationAdapter(Context context, List<GasStation> stations, OnStationClickListener listener) {
        this.context = context;
        this.stations = stations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GasStationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.gas_station_item, parent, false);
        return new GasStationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GasStationViewHolder holder, int position) {
        GasStation station = stations.get(position);
        holder.textStationName.setText(station.getName());
        holder.textStationDistance.setText(String.format(Locale.getDefault(), "%.1f km uzaklıkta", station.getDistance()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStationClick(station);
            }
        });

        holder.btnNavigate.setOnClickListener(v -> {
            // Google Maps navigasyonunu başlat
            LatLng location = station.getLocation();
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location.latitude + "," + location.longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public void updateStations(List<GasStation> newStations) {
        this.stations = newStations;
        notifyDataSetChanged();
    }

    static class GasStationViewHolder extends RecyclerView.ViewHolder {
        TextView textStationName;
        TextView textStationDistance;
        ImageButton btnNavigate;

        public GasStationViewHolder(@NonNull View itemView) {
            super(itemView);
            textStationName = itemView.findViewById(R.id.text_station_name);
            textStationDistance = itemView.findViewById(R.id.text_station_distance);
            btnNavigate = itemView.findViewById(R.id.btn_navigate);
        }
    }
}


class CarWash {
    private String name;
    private LatLng location;
    private double distance; // kilometre cinsinden

    public CarWash(String name, LatLng location, double distance) {
        this.name = name;
        this.location = location;
        this.distance = distance;
    }

    public String getName() {
        return name;
    }

    public LatLng getLocation() {
        return location;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
// RecyclerView Adapter for car washes
class CarWashAdapter extends RecyclerView.Adapter<CarWashAdapter.CarWashViewHolder> {

    private List<CarWash> carWashes;
    private Context context;
    private OnCarWashClickListener listener;

    public interface OnCarWashClickListener {
        void onCarWashClick(CarWash carWash);
    }

    public CarWashAdapter(Context context, List<CarWash> carWashes, OnCarWashClickListener listener) {
        this.context = context;
        this.carWashes = carWashes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarWashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.car_wash_item, parent, false);
        return new CarWashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarWashViewHolder holder, int position) {
        CarWash carWash = carWashes.get(position);
        holder.textCarWashName.setText(carWash.getName());
        holder.textCarWashDistance.setText(String.format(Locale.getDefault(), "%.1f km uzaklıkta", carWash.getDistance()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCarWashClick(carWash);
            }
        });

        holder.btnNavigate.setOnClickListener(v -> {
            // Google Maps navigasyonunu başlat
            LatLng location = carWash.getLocation();
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location.latitude + "," + location.longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carWashes.size();
    }

    public void updateCarWashes(List<CarWash> newCarWashes) {
        this.carWashes = newCarWashes;
        notifyDataSetChanged();
    }

    static class CarWashViewHolder extends RecyclerView.ViewHolder {
        TextView textCarWashName;
        TextView textCarWashDistance;
        ImageButton btnNavigate;

        public CarWashViewHolder(@NonNull View itemView) {
            super(itemView);
            textCarWashName = itemView.findViewById(R.id.text_carwash_name);
            textCarWashDistance = itemView.findViewById(R.id.text_carwash_distance);
            btnNavigate = itemView.findViewById(R.id.btn_navigate);
        }
    }
}

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GasStationAdapter.OnStationClickListener,
        CarWashAdapter.OnCarWashClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    // API anahtarınızı buraya ekleyin (Google Cloud Console'dan aldığınız geçerli anahtar)
    private final String API_KEY = "AIzaSyAycvep_5exb1QAjfNbMyabQ8t-0yUVnq0";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;

    // Arama için gerekli view elemanları
    private EditText searchEditText;
    private Button searchButton;
    private RequestQueue requestQueue;

    // BottomSheet bileşenleri
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private RecyclerView recyclerView;

    // Gas Station bileşenleri
    private GasStationAdapter gasStationAdapter;
    private List<GasStation> gasStationList = new ArrayList<>();

    // Car Wash bileşenleri
    private CarWashAdapter carWashAdapter;
    private List<CarWash> carWashList = new ArrayList<>();

    // Kategori butonları
    private Button btnGasStations;
    private Button btnCarWash;

    // Aktif kategori (varsayılan: benzin istasyonları)
    private boolean isGasStationActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Konum servisini başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Volley RequestQueue oluştur
        requestQueue = Volley.newRequestQueue(this);

        // Arama UI elemanlarını tanımla
        searchEditText = binding.editAddress;
        searchButton = binding.btnSearch;

        // BottomSheet ayarları
        View bottomSheet = binding.bottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // RecyclerView ayarları
        recyclerView = findViewById(R.id.recycler_gas_stations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gas Station adapter'ı
        gasStationAdapter = new GasStationAdapter(this, gasStationList, this);

        // Car Wash adapter'ı
        carWashAdapter = new CarWashAdapter(this, carWashList, this);

        // Varsayılan adapter'ı ayarla
        recyclerView.setAdapter(gasStationAdapter);

        // Kategori butonlarını tanımla
        btnGasStations = findViewById(R.id.btn_gas_stations);
        btnCarWash = findViewById(R.id.btn_car_wash);

        // Buton tıklama olaylarını ayarla
        setupCategoryButtons();

        // Arama butonuna tıklandığında çalışacak kod
        searchButton.setOnClickListener(v -> {
            String searchAddress = searchEditText.getText().toString();
            if (!searchAddress.isEmpty()) {
                geocodeAddress(searchAddress);
            } else {
                Toast.makeText(MapsActivity.this, "Lütfen bir adres girin", Toast.LENGTH_SHORT).show();
            }
        });

        // Map fragment'ini elde edip hazır olduğunda çağrılması için set ediyoruz.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupCategoryButtons() {
        btnGasStations.setOnClickListener(v -> {
            isGasStationActive = true;
            recyclerView.setAdapter(gasStationAdapter);
            updateButtonStates();

            // Haritayı temizle ve benzin istasyonlarını göster
            mMap.clear();
            if (currentLocation != null) {
                addMarkerOnMap(currentLocation.latitude, currentLocation.longitude,
                        "Bulunduğunuz Konum", BitmapDescriptorFactory.HUE_AZURE);

                // Benzin istasyonlarını haritaya ekle
                for (GasStation station : gasStationList) {
                    LatLng location = station.getLocation();
                    addGasStationMarker(location.latitude, location.longitude, station.getName());
                }
            }
        });

        btnCarWash.setOnClickListener(v -> {
            isGasStationActive = false;
            recyclerView.setAdapter(carWashAdapter);
            updateButtonStates();

            // Haritayı temizle ve araç yıkama yerlerini göster
            mMap.clear();
            if (currentLocation != null) {
                addMarkerOnMap(currentLocation.latitude, currentLocation.longitude,
                        "Bulunduğunuz Konum", BitmapDescriptorFactory.HUE_AZURE);

                // Araç yıkama yerlerini haritaya ekle
                for (CarWash carWash : carWashList) {
                    LatLng location = carWash.getLocation();
                    addCarWashMarker(location.latitude, location.longitude, carWash.getName());
                }
            }
        });

        // Varsayılan buton durumlarını ayarla
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (isGasStationActive) {
            btnGasStations.setAlpha(1.0f);
            btnCarWash.setAlpha(0.5f);
        } else {
            btnGasStations.setAlpha(0.5f);
            btnCarWash.setAlpha(1.0f);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Konum izinlerini kontrol et
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            currentLocation = userLocation;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));

                            mMap.clear();
                            addMarkerOnMap(userLocation.latitude, userLocation.longitude,
                                    "Bulunduğunuz Konum", BitmapDescriptorFactory.HUE_AZURE);

                            searchNearbyGasStations(userLocation);
                            searchNearbyCarWashes(userLocation);
                        } else {
                            LatLng istanbul = new LatLng(41.0082, 28.9784);
                            currentLocation = istanbul;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 14));
                            addMarkerOnMap(istanbul.latitude, istanbul.longitude,
                                    "İstanbul", BitmapDescriptorFactory.HUE_AZURE);
                            searchNearbyGasStations(istanbul);
                            searchNearbyCarWashes(istanbul);
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                LatLng istanbul = new LatLng(41.0082, 28.9784);
                currentLocation = istanbul;
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 14));
                addMarkerOnMap(istanbul.latitude, istanbul.longitude,
                        "İstanbul", BitmapDescriptorFactory.HUE_AZURE);
                searchNearbyGasStations(istanbul);
                searchNearbyCarWashes(istanbul);
            }
        }
    }

    private void addMarkerOnMap(double lat, double lng, String name, float markerColor) {
        LatLng position = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
    }

    private void addGasStationMarker(double lat, double lng, String name) {
        addMarkerOnMap(lat, lng, name, BitmapDescriptorFactory.HUE_RED);
    }

    private void addCarWashMarker(double lat, double lng, String name) {
        addMarkerOnMap(lat, lng, name, BitmapDescriptorFactory.HUE_BLUE);
    }

    private double calculateDistance(LatLng start, LatLng end) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(end.latitude - start.latitude);
        double dLng = Math.toRadians(end.longitude - start.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }

    private void searchNearbyGasStations(LatLng location) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + location.latitude + "," + location.longitude +
                "&radius=3000" +
                "&type=gas_station" +
                "&key=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            Log.d("MapsActivity", "Bulunan benzin istasyonu sayısı: " + results.length());

                            gasStationList.clear();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject station = results.getJSONObject(i);
                                JSONObject geometry = station.getJSONObject("geometry");
                                JSONObject locationObj = geometry.getJSONObject("location");
                                double lat = locationObj.getDouble("lat");
                                double lng = locationObj.getDouble("lng");
                                String name = station.getString("name");

                                LatLng stationLocation = new LatLng(lat, lng);
                                double distance = calculateDistance(currentLocation, stationLocation);

                                gasStationList.add(new GasStation(name, stationLocation, distance));

                                // Eğer benzin istasyonları seçiliyse haritaya ekle
                                if (isGasStationActive) {
                                    addGasStationMarker(lat, lng, name);
                                }
                            }

                            Collections.sort(gasStationList, new Comparator<GasStation>() {
                                @Override
                                public int compare(GasStation s1, GasStation s2) {
                                    return Double.compare(s1.getDistance(), s2.getDistance());
                                }
                            });

                            gasStationAdapter.updateStations(gasStationList);

                            if (isGasStationActive && !gasStationList.isEmpty()) {
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            }

                        } catch (JSONException e) {
                            Log.e("MapsActivity", "JSON ayrıştırma hatası: ", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MapsActivity", "Volley isteği hatası: ", error);
                        Toast.makeText(MapsActivity.this, "Benzin istasyonları aranamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(request);
    }

    private void searchNearbyCarWashes(LatLng location) {
        // Google Places API'de "car_wash" doğrudan bir tür değil, bu yüzden "car_repair" ve keyword olarak "car wash" kullanıyoruz
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + location.latitude + "," + location.longitude +
                "&radius=3000" +
                "&type=car_repair" +
                "&keyword=car%20wash" +
                "&key=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            Log.d("MapsActivity", "Bulunan araç yıkama yeri sayısı: " + results.length());

                            carWashList.clear();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject carWash = results.getJSONObject(i);
                                JSONObject geometry = carWash.getJSONObject("geometry");
                                JSONObject locationObj = geometry.getJSONObject("location");
                                double lat = locationObj.getDouble("lat");
                                double lng = locationObj.getDouble("lng");
                                String name = carWash.getString("name");

                                LatLng carWashLocation = new LatLng(lat, lng);
                                double distance = calculateDistance(currentLocation, carWashLocation);

                                carWashList.add(new CarWash(name, carWashLocation, distance));

                                // Eğer araç yıkama seçiliyse haritaya ekle
                                if (!isGasStationActive) {
                                    addCarWashMarker(lat, lng, name);
                                }
                            }

                            Collections.sort(carWashList, new Comparator<CarWash>() {
                                @Override
                                public int compare(CarWash c1, CarWash c2) {
                                    return Double.compare(c1.getDistance(), c2.getDistance());
                                }
                            });

                            carWashAdapter.updateCarWashes(carWashList);

                            if (!isGasStationActive && !carWashList.isEmpty()) {
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            }

                        } catch (JSONException e) {
                            Log.e("MapsActivity", "JSON ayrıştırma hatası: ", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MapsActivity", "Volley isteği hatası: ", error);
                        Toast.makeText(MapsActivity.this, "Araç yıkama yerleri aranamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        requestQueue.add(request);
    }

    private void geocodeAddress(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, "UTF-8");
            String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "address=" + encodedAddress +
                    "&key=" + API_KEY;

            JsonObjectRequest geocodeRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String status = response.getString("status");

                                if (status.equals("OK")) {
                                    JSONArray results = response.getJSONArray("results");
                                    JSONObject result = results.getJSONObject(0);
                                    JSONObject geometry = result.getJSONObject("geometry");
                                    JSONObject location = geometry.getJSONObject("location");

                                    double lat = location.getDouble("lat");
                                    double lng = location.getDouble("lng");
                                    String formattedAddress = result.getString("formatted_address");

                                    currentLocation = new LatLng(lat, lng);
                                    mMap.clear();
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14));
                                    addMarkerOnMap(lat, lng, formattedAddress, BitmapDescriptorFactory.HUE_GREEN);
                                    searchNearbyGasStations(currentLocation);
                                    searchNearbyCarWashes(currentLocation);

                                    Toast.makeText(MapsActivity.this, "Konum bulundu: " + formattedAddress, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MapsActivity.this, "Adres bulunamadı: " + status, Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {
                                Log.e("MapsActivity", "Geocoding JSON ayrıştırma hatası: ", e);
                                Toast.makeText(MapsActivity.this, "Adres ayrıştırılamadı", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("MapsActivity", "Geocoding hatası: ", error);
                            Toast.makeText(MapsActivity.this, "Adres aranamadı: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            requestQueue.add(geocodeRequest);

        } catch (UnsupportedEncodingException e) {
            Log.e("MapsActivity", "URL encoding hatası: ", e);
            Toast.makeText(this, "Adres formatında hata", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStationClick(GasStation station) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getLocation(), 16));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onCarWashClick(CarWash carWash) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(carWash.getLocation(), 16));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
}