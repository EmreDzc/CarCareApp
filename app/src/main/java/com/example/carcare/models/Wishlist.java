package com.example.carcare.models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
    public class Wishlist {
        private String id;
        private String userId;
        private String productId;
        private String productName;
        // private String productImageUrl; // KALDIRILDI
        private String productImageBase64; // EKLENDİ
        private double productPrice;
        private Date addedAt;

        // Boş constructor (Firebase için)
        public Wishlist() {}

        // Getter ve Setter'lar
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        // productImageBase64 için Getter ve Setter
        public String getProductImageBase64() { return productImageBase64; }
        public void setProductImageBase64(String productImageBase64) { this.productImageBase64 = productImageBase64; }

        public double getProductPrice() { return productPrice; }
        public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

        public Date getAddedAt() { return addedAt; }
        public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }
    }