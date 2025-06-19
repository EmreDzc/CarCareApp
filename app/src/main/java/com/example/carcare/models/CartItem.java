package com.example.carcare.models;

public class CartItem {
    private Product product;
    private int quantity;
    private String cartDocId; // Firestore'daki doküman ID'si

    // Firestore için boş constructor gerekli
    public CartItem() {}

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCartDocId() {
        return cartDocId;
    }

    public void setCartDocId(String cartDocId) {
        this.cartDocId = cartDocId;
    }
}