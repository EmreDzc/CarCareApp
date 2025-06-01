// com/example/carcare/api/RetrofitClient.java
package com.example.carcare.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Google Places API için
    private static final String GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static Retrofit googlePlacesRetrofit = null;

    // NHTSA vPIC API için
    private static final String NHTSA_VPIC_BASE_URL = "https://vpic.nhtsa.dot.gov/api/";
    private static Retrofit nhtsaRetrofit = null;

    public static Retrofit getGooglePlacesClient() {
        if (googlePlacesRetrofit == null) {
            googlePlacesRetrofit = new Retrofit.Builder()
                    .baseUrl(GOOGLE_PLACES_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return googlePlacesRetrofit;
    }

    public static Retrofit getNhtsaClient() {
        if (nhtsaRetrofit == null) {
            nhtsaRetrofit = new Retrofit.Builder()
                    .baseUrl(NHTSA_VPIC_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return nhtsaRetrofit;
    }
}