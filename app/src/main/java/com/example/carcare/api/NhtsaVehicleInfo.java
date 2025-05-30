// com/example/carcare/api/nhtsa/NhtsaVehicleInfo.java
package com.example.carcare.api;

import com.google.gson.annotations.SerializedName;

public class NhtsaVehicleInfo {
    @SerializedName("Make")
    private String make;

    @SerializedName("Model")
    private String model;

    @SerializedName("ModelYear")
    private String modelYear;

    @SerializedName("Manufacturer")
    private String manufacturer;

    @SerializedName("VehicleType")
    private String vehicleType;

    public String getMake() {
        return make != null ? make.trim() : null;
    }

    public String getModel() {
        return model != null ? model.trim() : null;
    }

    public String getModelYear() {
        // Bazen "2017.0" gibi gelebilir, ".0" kısmını temizleyelim
        if (modelYear != null) {
            String year = modelYear.trim();
            if (year.endsWith(".0")) {
                return year.substring(0, year.length() - 2);
            }
            return year;
        }
        return null;
    }

    public String getManufacturer() {
        return manufacturer != null ? manufacturer.trim() : null;
    }

    public String getVehicleType() {
        return vehicleType != null ? vehicleType.trim() : null;
    }
}