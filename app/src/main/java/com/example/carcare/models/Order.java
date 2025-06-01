package com.example.carcare.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class Order {
    private String id;
    @Exclude private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String paymentMethod;
    private double subtotal;
    private double tax;
    private double totalAmount;
    private String status;
    private List<Map<String, Object>> items;
    @ServerTimestamp private Date orderDate;
    private Date estimatedDeliveryDate;
    private String trackingNumber;
    private String shippingCompany;
    private Date cancelledDate;
    private String statusColor;



    public Order() {
        // Firestore için boş constructor gerekli
    }

    // Getter ve Setter metodları
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    @Exclude
    public String getUserId() { return userId; }
    @Exclude
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public void setEstimatedDeliveryDate(Date estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getShippingCompany() { return shippingCompany; }
    public void setShippingCompany(String shippingCompany) { this.shippingCompany = shippingCompany; }

    // Yardımcı metodlar
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public String getOrderNumber() {
        if (id != null && id.length() >= 8) {
            return "#" + id.substring(id.length() - 8).toUpperCase();
        }
        return "#" + (id != null ? id : "UNKNOWN");
    }

    public String getStatusColor() {
        switch (status != null ? status : "Bilinmiyor") {
            case "Sipariş Alındı":
                return "#FF9800"; // Orange
            case "Hazırlanıyor":
                return "#2196F3"; // Blue
            case "Kargoda":
                return "#9C27B0"; // Purple
            case "Teslim Edildi":
                return "#4CAF50"; // Green
            case "İptal Edildi":
                return "#F44336"; // Red
            default:
                return "#757575"; // Grey
        }
    }

    public Date getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(Date cancelledDate) {
        this.cancelledDate = cancelledDate;
    }
}