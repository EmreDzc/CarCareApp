package com.example.carcare.models;

import com.example.carcare.R;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    // private String imageUrl; // KALDIRILDI
    private String imageBase64; // EKLENDİ: Base64 kodlanmış resim string'i için
    private String category;
    private int stock;

    @Exclude
    private String cartItemId;

    public Product() {
        // Firebase için boş constructor gerekli
    }

    // Constructor güncellendi
    public Product(String id, String name, String description, double price, String imageBase64, String category, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageBase64 = imageBase64; // GÜNCELLENDİ
        this.category = category;
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // imageBase64 için Getter ve Setter
    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Exclude
    public String getCartItemId() {
        return cartItemId;
    }

    @Exclude
    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
    }

    @Exclude
    public int getImageResId() {
        // Bu metod artık ürün resimleri için doğrudan kullanılmıyor.
        // Genel bir placeholder döndür.
        return R.drawable.placeholder_image;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}