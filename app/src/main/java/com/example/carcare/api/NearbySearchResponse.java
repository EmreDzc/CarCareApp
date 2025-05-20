package com.example.carcare.api;

import java.util.List;

public class NearbySearchResponse {
    private List<Result> results;
    private String status;

    public List<Result> getResults() {
        return results;
    }

    public String getStatus() {
        return status;
    }

    public static class Result {
        private String place_id;
        private String name;
        private String vicinity;
        private float rating;
        private Geometry geometry;
        private OpeningHours opening_hours;

        public String getPlaceId() {
            return place_id;
        }

        public String getName() {
            return name;
        }

        public String getVicinity() {
            return vicinity;
        }

        public float getRating() {
            return rating;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public OpeningHours getOpeningHours() {
            return opening_hours;
        }
    }

    public static class Geometry {
        private Location location;

        public Location getLocation() {
            return location;
        }
    }

    public static class Location {
        private double lat;
        private double lng;

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }
    }

    public static class OpeningHours {
        private boolean open_now;

        public boolean isOpenNow() {
            return open_now;
        }
    }
}