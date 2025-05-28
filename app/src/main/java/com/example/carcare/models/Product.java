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
    private String category;
    private int stock;

    private String brand;
    private String modelCode;
    private String sellerName;
    // private float rating; // BU ALANI KULLANMIYORUZ, averageRating ALANI VAR
    // private int reviewCount; // BU ALANI KULLANMIYORUZ, totalReviews ALANI VAR
    private String color;
    private List<String> sizes = new ArrayList<>();
    private Map<String, String> specifications;
    private List<String> tags = new ArrayList<>();
    private double discountPrice;
    private boolean isFeatured;
    private String warrantyInfo;
    private String shippingInfo;
    private String returnPolicy;

    private float averageRating; // Ortalama kullanıcı puanı
    private int totalReviews;    // Toplam değerlendirme sayısı

    @ServerTimestamp private Date createdAt;
    @ServerTimestamp private Date updatedAt;

    @Exclude private String cartItemId;

    public Product() {
        // Firebase için boş constructor
        this.sizes = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.averageRating = 0.0f; // Varsayılan değer
        this.totalReviews = 0;     // Varsayılan değer
    }

    // Constructor'ı güncelleyebilirsiniz veya sadece setter'ları kullanabilirsiniz.
    // Eğer constructor kullanıyorsanız, averageRating ve totalReviews'ı da ekleyin.

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
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getSizes() { return sizes == null ? new ArrayList<>() : sizes; } // Null check
    public void setSizes(List<String> sizes) { this.sizes = sizes; }
    public Map<String, String> getSpecifications() { return specifications; }
    public void setSpecifications(Map<String, String> specifications) { this.specifications = specifications; }
    public List<String> getTags() { return tags == null ? new ArrayList<>() : tags; } // Null check
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