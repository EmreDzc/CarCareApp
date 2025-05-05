package com.example.carcare.models;

public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private int imageResId;

    public Product(String id, String name, String description, double price, int imageResId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getImageResId() { return imageResId; }
}
