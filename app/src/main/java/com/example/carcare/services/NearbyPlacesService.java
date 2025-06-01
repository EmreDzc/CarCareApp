package com.example.carcare.services;

import android.content.Context;
import android.util.Log;

import com.example.carcare.api.GooglePlacesApi;
import com.example.carcare.api.NearbySearchResponse;
import com.example.carcare.api.RetrofitClient;
import com.example.carcare.models.NearbyPlace;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyPlacesService {
    private static final String TAG = "NearbyPlacesService";
    private static final double SEARCH_RADIUS_KM = 3.0; // 3 km radius

    private Context context;
    private final String PLACES_API_KEY;

    public NearbyPlacesService(Context context, String apiKey) {
        this.context = context;
        this.PLACES_API_KEY = apiKey;
    }

    public interface NearbyPlacesCallback {
        void onPlacesFound(List<NearbyPlace> places);
        void onError(String errorMessage);
    }

    public void findNearbyPlaces(LatLng location, String placeType, NearbyPlacesCallback callback) {
        Log.d(TAG, "Searching for nearby places of type: " + placeType + " at location: " + location.latitude + "," + location.longitude);

        GooglePlacesApi apiService = RetrofitClient.getGooglePlacesClient().create(GooglePlacesApi.class);
        String locationString = location.latitude + "," + location.longitude;
        int radius = (int)(SEARCH_RADIUS_KM * 1000); // km to meters
        String searchType = getNearbySearchType(placeType);

        Log.d(TAG, "Making API call with:");
        Log.d(TAG, "Location: " + locationString);
        Log.d(TAG, "Radius: " + radius);
        Log.d(TAG, "Type: " + searchType);
        Log.d(TAG, "API Key: " + PLACES_API_KEY.substring(0, 10) + "..."); // Güvenlik için sadece ilk 10 karakter

        Call<NearbySearchResponse> call = apiService.getNearbyPlaces(locationString, radius, searchType, PLACES_API_KEY);

        // API URL'sini log'la
        Log.d(TAG, "API URL: " + call.request().url().toString());

        call.enqueue(new Callback<NearbySearchResponse>() {
            @Override
            public void onResponse(Call<NearbySearchResponse> call, Response<NearbySearchResponse> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                Log.d(TAG, "API Response Message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API call successful, status: " + response.body().getStatus());

                    if ("OK".equals(response.body().getStatus())) {
                        List<NearbyPlace> places = convertToNearbyPlaces(response.body(), location, placeType);
                        Log.d(TAG, "Found " + places.size() + " places");
                        callback.onPlacesFound(places);
                    } else {
                        Log.w(TAG, "API returned status: " + response.body().getStatus());
                        callback.onError("API Status: " + response.body().getStatus());
                    }
                } else {
                    // Hata detaylarını oku
                    String errorBody = "Bilinmeyen hata";
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                            Log.e(TAG, "API Error Body: " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }

                    Log.e(TAG, "API response unsuccessful: " + response.code() + " - " + response.message());
                    callback.onError("API yanıtı alınamadı: " + response.code() + " - " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<NearbySearchResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError("API çağrısı başarısız: " + t.getMessage());
            }
        });
    }

    private List<NearbyPlace> convertToNearbyPlaces(NearbySearchResponse response, LatLng userLocation, String placeType) {
        List<NearbyPlace> places = new ArrayList<>();

        if (response.getResults() != null) {
            for (NearbySearchResponse.Result result : response.getResults()) {
                // Sonuçları parse et
                LatLng placeLocation = new LatLng(
                        result.getGeometry().getLocation().getLat(),
                        result.getGeometry().getLocation().getLng()
                );

                // Mesafeyi hesapla
                double distance = calculateDistance(
                        userLocation.latitude, userLocation.longitude,
                        placeLocation.latitude, placeLocation.longitude
                );

                // Açık olup olmadığını kontrol et
                boolean isOpen = false;
                if (result.getOpeningHours() != null) {
                    isOpen = result.getOpeningHours().isOpenNow();
                }

                // Değerlendirme puanını kontrol et
                float rating = 0.0f;
                if (result.getRating() > 0) {
                    rating = result.getRating();
                }

                // Yeni yer oluştur
                NearbyPlace place = new NearbyPlace(
                        result.getPlaceId(),
                        result.getName(),
                        result.getVicinity() != null ? result.getVicinity() : "",
                        placeType,
                        placeLocation,
                        distance,
                        rating,
                        isOpen
                );

                places.add(place);
                Log.d(TAG, "Added place: " + place.getName() + " at distance " + place.getFormattedDistance());
            }

            // Mesafeye göre sırala
            places.sort((p1, p2) -> Double.compare(p1.getDistance(), p2.getDistance()));
        }

        return places;
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Earth's radius in km
        final double R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private String getNearbySearchType(String placeType) {
        switch (placeType) {
            case NearbyPlace.Type.GAS:
                return "gas_station";
            case NearbyPlace.Type.SERVICE:
                return "car_repair";
            case NearbyPlace.Type.WASH:
                return "car_wash";
            case NearbyPlace.Type.PARTS:
                return "car_dealer";
            default:
                return "gas_station";
        }
    }
}