package com.example.carcare.services;

import android.content.Context;
import android.util.Log;

import com.example.carcare.api.GooglePlacesApi;
import com.example.carcare.api.NearbySearchResponse;
import com.example.carcare.api.RetrofitClient;
import com.example.carcare.models.NearbyPlace;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyPlacesService {
    private static final String TAG = "NearbyPlacesService";
    private static final double SEARCH_RADIUS_KM = 3.0; // 3 km radius

    private Context context;
    private PlacesClient placesClient;
    private final String PLACES_API_KEY;


    public NearbyPlacesService(Context context, String apiKey) {
        this.context = context;
        this.PLACES_API_KEY = apiKey;

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey);
        }

        placesClient = Places.createClient(context);
    }

    public interface NearbyPlacesCallback {
        void onPlacesFound(List<NearbyPlace> places);
        void onError(String errorMessage);
    }

    /**
     * Yakındaki yerleri bulan ana metot
     * @param location Arama yapılacak konum
     * @param placeType Yer türü (NearbyPlace.Type'da tanımlı)
     * @param callback Sonuçları almak için callback
     */
    public void findNearbyPlaces(LatLng location, String placeType, NearbyPlacesCallback callback) {
        Log.d(TAG, "Searching for nearby places of type: " + placeType + " at location: " + location.latitude + "," + location.longitude);
        findRealNearbyPlaces(location, placeType, callback);
    }

    /**
     * Google Places API'yi kullanarak yakındaki yerleri arayan metot
     */
    private void findRealNearbyPlaces(LatLng location, String placeType, NearbyPlacesCallback callback) {
        // Places API ile Retrofit kullanımı
        GooglePlacesApi apiService = RetrofitClient.getClient().create(GooglePlacesApi.class);
        String locationString = location.latitude + "," + location.longitude;
        int radius = (int)(SEARCH_RADIUS_KM * 1000); // km to meters

        Log.d(TAG, "Making API call with location: " + locationString + ", radius: " + radius + ", type: " + getNearbySearchType(placeType));

        apiService.getNearbyPlaces(locationString, radius, getNearbySearchType(placeType), PLACES_API_KEY)
                .enqueue(new Callback<NearbySearchResponse>() {
                    @Override
                    public void onResponse(Call<NearbySearchResponse> call, Response<NearbySearchResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "API call successful, status: " + response.body().getStatus());
                            List<NearbyPlace> places = convertToNearbyPlaces(response.body(), location, placeType);
                            Log.d(TAG, "Found " + places.size() + " places");
                            callback.onPlacesFound(places);
                        } else {
                            Log.e(TAG, "API response unsuccessful: " + (response.errorBody() != null ? response.errorBody().toString() : "No error body"));
                            callback.onError("API yanıtı alınamadı: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<NearbySearchResponse> call, Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        callback.onError("API çağrısı başarısız: " + t.getMessage());
                    }
                });
    }

    /**
     * API yanıtını NearbyPlace nesnelerine dönüştüren metot
     */
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

    /**
     * Location autocomplete için istek oluşturan yardımcı metot
     */
    private com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.Builder
    createNearbySearchRequest(LatLng location, double radiusKm, String placeType) {

        // Calculate bounds for the search area (roughly within radiusKm)
        RectangularBounds bounds = createBoundsWithRadiusKm(location, radiusKm);

        // Build the request
        return com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setTypesFilter(Arrays.asList(placeType))
                .setCountries("TR") // Turkey
                .setSessionToken(com.google.android.libraries.places.api.model.AutocompleteSessionToken.newInstance());
    }

    /**
     * Belirli bir yarıçapta sınırlar oluşturan yardımcı metot
     */
    private RectangularBounds createBoundsWithRadiusKm(LatLng center, double radiusKm) {
        // Approximately degrees per km
        double latDegPerKm = 1.0 / 110.574;
        double lngDegPerKm = 1.0 / (111.320 * Math.cos(Math.toRadians(center.latitude)));

        double latDelta = radiusKm * latDegPerKm;
        double lngDelta = radiusKm * lngDegPerKm;

        LatLng southwest = new LatLng(
                center.latitude - latDelta,
                center.longitude - lngDelta);

        LatLng northeast = new LatLng(
                center.latitude + latDelta,
                center.longitude + lngDelta);

        return RectangularBounds.newInstance(southwest, northeast);
    }

    /**
     * Test için merkez etrafında rastgele konum üreten metot
     */
    private LatLng getRandomLocationNearby(LatLng center, double radiusKm) {
        double latDegPerKm = 1.0 / 110.574;
        double lngDegPerKm = 1.0 / (111.320 * Math.cos(Math.toRadians(center.latitude)));

        double randomDistance = Math.random() * radiusKm;
        double randomAngle = Math.random() * 2 * Math.PI;

        double latDelta = randomDistance * latDegPerKm * Math.sin(randomAngle);
        double lngDelta = randomDistance * lngDegPerKm * Math.cos(randomAngle);

        return new LatLng(
                center.latitude + latDelta,
                center.longitude + lngDelta);
    }

    /**
     * İki nokta arasındaki mesafeyi hesaplayan Haversine formülü
     */
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

    /**
     * Uygulama içi yer türünü Google Places API türüne dönüştüren metot
     */
    private String getNearbySearchType(String placeType) {
        switch (placeType) {
            case NearbyPlace.Type.GAS:
                return "gas_station";
            case NearbyPlace.Type.SERVICE:
                return "car_repair";
            case NearbyPlace.Type.WASH:
                return "car_wash";
            case NearbyPlace.Type.PARTS:
                return "car_dealer"; // No specific type for parts, using dealer as approximation
            default:
                return "gas_station";
        }
    }
}