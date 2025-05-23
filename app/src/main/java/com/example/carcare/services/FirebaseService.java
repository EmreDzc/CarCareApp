package com.example.carcare.services;

import android.util.Log;
import com.example.carcare.models.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static final String COLLECTION_PRODUCTS = "products";

    private final FirebaseFirestore db;

    public FirebaseService() {
        db = FirebaseFirestore.getInstance();
    }

    public interface ProductsCallback {
        void onProductsLoaded(List<Product> products);
        void onError(Exception e);
    }

    // Tüm ürünleri getir
    public void getAllProducts(ProductsCallback callback) {
        db.collection(COLLECTION_PRODUCTS)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            productList.add(product);
                        }
                        callback.onProductsLoaded(productList);
                    } else {
                        Log.w(TAG, "Error getting products.", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // Kategoriye göre ürünleri getir
    public void getProductsByCategory(String category, ProductsCallback callback) {
        db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("category", category)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            productList.add(product);
                        }
                        callback.onProductsLoaded(productList);
                    } else {
                        Log.w(TAG, "Error getting products by category.", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // Fiyat aralığına göre ürünleri getir
    public void getProductsByPriceRange(double minPrice, double maxPrice, ProductsCallback callback) {
        db.collection(COLLECTION_PRODUCTS)
                .whereGreaterThanOrEqualTo("price", minPrice)
                .whereLessThanOrEqualTo("price", maxPrice)
                .orderBy("price", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            productList.add(product);
                        }
                        callback.onProductsLoaded(productList);
                    } else {
                        Log.w(TAG, "Error getting products by price range.", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }
}