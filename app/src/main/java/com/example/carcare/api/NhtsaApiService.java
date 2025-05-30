// com/example/carcare/api/NhtsaApiService.java
package com.example.carcare.api; // Paket adı doğru

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NhtsaApiService {
    @GET("vehicles/DecodeVinValuesExtended/{vin}") // API endpoint'inin base URL'den sonraki kısmı
    Call<NhtsaVinResponse> getVehicleDetailsByVin(
            @Path("vin") String vin,         // URL'deki {vin} kısmını doldurur
            @Query("format") String format    // URL'ye ?format=json gibi bir query parametresi ekler
    );
}