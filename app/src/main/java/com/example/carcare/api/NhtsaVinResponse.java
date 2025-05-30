// com/example/carcare/api/nhtsa/NhtsaVinResponse.java
package com.example.carcare.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NhtsaVinResponse {
    @SerializedName("Results")
    private List<NhtsaVehicleInfo> results;

    public List<NhtsaVehicleInfo> getResults() {
        return results;
    }
}