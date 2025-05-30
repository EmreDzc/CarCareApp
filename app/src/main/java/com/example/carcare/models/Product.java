package com.example.carcare.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@IgnoreExtraProperties
public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String imageBase64;
    private String category; // Araç parçası kategorisi (örn: Fren Sistemi, Motor Yağı, Aksesuar)
    private int stock;

    private String brand; // Marka (örn: Bosch, Mobil, Sonax)
    private String modelCode; // Ürünün spesifik model/parça numarası
    private String sellerName;
    private Map<String, String> specifications;
    private List<String> tags = new ArrayList<>();
    private double discountPrice;
    private boolean isFeatured;
    private String warrantyInfo;
    private String shippingInfo;
    private String returnPolicy;

    private float averageRating;
    private int totalReviews;

    @ServerTimestamp private Date createdAt;
    @ServerTimestamp private Date updatedAt;

    @Exclude private String cartItemId;

    public Product() {
        this.tags = new ArrayList<>();
        this.averageRating = 0.0f;
        this.totalReviews = 0;
    }

    // --- Getter ve Setter'lar ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
    public List<String> getTags() { return tags == null ? new ArrayList<>() : tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(double discountPrice) { this.discountPrice = discountPrice; }
    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }
    public String getWarrantyInfo() { return warrantyInfo; }
    public void setWarrantyInfo(String warrantyInfo) { this.warrantyInfo = warrantyInfo; }
    public String getShippingInfo() { return shippingInfo; }
    public void setShippingInfo(String shippingInfo) { this.shippingInfo = shippingInfo; }
    public String getReturnPolicy() { return returnPolicy; }
    public void setReturnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public float getAverageRating() { return averageRating; }
    public void setAverageRating(float averageRating) { this.averageRating = averageRating; }
    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
    @Exclude public String getCartItemId() { return cartItemId; }
    @Exclude public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }
}