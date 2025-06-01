package com.example.carcare.api;

public class PlaceDetailsResponse {
    private Result result;
    private String status;

    public Result getResult() {
        return result;
    }

    public String getStatus() {
        return status;
    }

    public static class Result {
        private String place_id;
        private String name;
        private String formatted_address;
        private String formatted_phone_number;
        private String website;
        private float rating;
        private int user_ratings_total;
        private OpeningHours opening_hours;
        private Geometry geometry;

        // Getters
        public String getPlaceId() { return place_id; }
        public String getName() { return name; }
        public String getFormattedAddress() { return formatted_address; }
        public String getFormattedPhoneNumber() { return formatted_phone_number; }
        public String getWebsite() { return website; }
        public float getRating() { return rating; }
        public int getUserRatingsTotal() { return user_ratings_total; }
        public OpeningHours getOpeningHours() { return opening_hours; }
        public Geometry getGeometry() { return geometry; }
    }

    public static class OpeningHours {
        private boolean open_now;
        private String[] weekday_text;

        public boolean isOpenNow() { return open_now; }
        public String[] getWeekdayText() { return weekday_text; }
    }

    public static class Geometry {
        private Location location;

        public Location getLocation() { return location; }
    }

    public static class Location {
        private double lat;
        private double lng;

        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }
}