package com.example.carcare.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Review {
    private String id; // Firestore tarafından otomatik atanacak
    private String userId;
    private String userName; // Kullanıcının görünen adı
    private float rating; // 1.0 - 5.0 arası
    private String comment;
    @ServerTimestamp
    private Date timestamp;

    public Review() {
        // Firebase için boş constructor
    }

    public Review(String userId, String userName, float rating, String comment) {
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
    }

    // --- Getter ve Setterlar ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}