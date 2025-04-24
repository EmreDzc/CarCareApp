package com.example.carcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GooglePlacesApi {
    @GET("maps/api/place/nearbysearch/json")
    Call<NearbySearchResponse> getNearbyPlaces(
            @Query("location") String location,  // Ör: "41.0082,28.9784"
            @Query("radius") int radius,          // Ör: 1500 (metre cinsinden)
            @Query("type") String type,           // Ör: "gas_station"
            @Query("key") String apiKey           // API anahtarınız
    );
}
