package com.example.carcare.models;

import com.example.carcare.R;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

// FireStore yapısına uygun olması için IgnoreExtraProperties ekleniyor
@IgnoreExtraProperties
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private String category;
    private int stock;

    // Sepet işlemleri için ekstra alan (Firestore'a kaydedilmeyen)
    @Exclude
    private String cartItemId;

    // Firebase için boş constructor gerekli
    public Product() {
    }

    public Product(String id, String name, String description, double price, String imageUrl, String category, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.stock = stock;
    }

    // Getter ve setter metodları
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    // Ürün görseli için uyumluluk metodu
    @Exclude
    public int getImageResId() {
        // Yerel kaynak ID yerine artık Firebase Storage URL kullanılıyor
        // Eğer eski kod hala bu metodu kullanıyorsa çakışma olmasın diye placeholder döndür
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