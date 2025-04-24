package com.example.carcare.api;

import java.util.List;

public class NearbySearchResponse {
    private List<PlaceResult> results;
    // API'den dönebilecek diğer alanlar (örneğin status, next_page_token vb.) eklenebilir

    public List<PlaceResult> getResults() {
        return results;
    }

    public void setResults(List<PlaceResult> results) {
        this.results = results;
    }

    // --- Inner Class: PlaceResult ---
    public static class PlaceResult {
        private String name;
        private String vicinity; // Adres bilgisi
        private Geometry geometry;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVicinity() {
            return vicinity;
        }

        public void setVicinity(String vicinity) {
            this.vicinity = vicinity;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }
    }

    // --- Inner Class: Geometry ---
    public static class Geometry {
        private LocationData location;

        public LocationData getLocation() {
            return location;
        }

        public void setLocation(LocationData location) {
            this.location = location;
        }
    }

    // --- Inner Class: LocationData ---
    public static class LocationData {
        private double lat;
        private double lng;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }
}
