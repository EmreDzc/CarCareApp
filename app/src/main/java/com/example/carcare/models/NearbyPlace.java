package com.example.carcare.models;

import com.google.android.gms.maps.model.LatLng;

public class NearbyPlace {
    private String id;
    private String name;
    private String address;
    private String type; // "gas", "service", "wash", "parts"
    private LatLng location;
    private double distance; // in kilometers
    private float rating;
    private boolean isOpen;

    public NearbyPlace(String id, String name, String address, String type,
                       LatLng location, double distance, float rating, boolean isOpen) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.type = type;
        this.location = location;
        this.distance = distance;
        this.rating = rating;
        this.isOpen = isOpen;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getType() { return type; }
    public LatLng getLocation() { return location; }
    public double getDistance() { return distance; }
    public float getRating() { return rating; }
    public boolean isOpen() { return isOpen; }

    // For UI display
    public String getFormattedDistance() {
        if (distance < 1.0) {
            return String.format("%.0f m", distance * 1000);
        } else {
            return String.format("%.1f km", distance);
        }
    }

    // Type enumeration for code readability
    public static class Type {
        public static final String GAS = "gas";
        public static final String SERVICE = "service";
        public static final String WASH = "wash";
        public static final String PARTS = "parts";
    }
}