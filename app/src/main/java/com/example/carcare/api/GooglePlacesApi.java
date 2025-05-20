package com.example.carcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GooglePlacesApi {
    @GET("place/nearbysearch/json")
    Call<NearbySearchResponse> getNearbyPlaces(
            @Query("location") String location,
            @Query("radius") int radius,
            @Query("type") String type,
            @Query("key") String apiKey
    );
}