package com.example.carcare.services;

import android.util.Log;
import com.example.carcare.models.Product; // Güncellenmiş Product modelini kullandığınızdan emin olun
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
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = document.toObject(Product.class);
                                product.setId(document.getId()); // ID'yi manuel olarak ata
                                // Product modeli artık 'imageBase64' bekliyor.
                                // toObject(Product.class) alan adları eşleşiyorsa bunu doğru şekilde eşleştirmeli.
                                productList.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Döküman Product'a dönüştürülürken hata: " + document.getId(), e);
                            }
                        }
                        callback.onProductsLoaded(productList);
                    } else {
                        Log.w(TAG, "Ürünler alınırken hata.", task.getException());
                        callback.onError(task.getException() != null ? task.getException() : new Exception("Bilinmeyen ürün yükleme hatası"));
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
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = document.toObject(Product.class);
                                product.setId(document.getId());
                                productList.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Döküman Product'a dönüştürülürken hata (kategoriye göre): " + document.getId(), e);
                            }
                        }
                        callback.onProductsLoaded(productList);
                    } else {
                        Log.w(TAG, "Kategoriye göre ürünler alınırken hata.", task.getException());
                        callback.onError(task.getException() != null ? task.getException() : new Exception("Bilinmeyen kategoriye göre ürün yükleme hatası"));
                    }
                });
    }

    // Fiyat aralığına göre ürünleri getir
    public void getProductsByPriceRange(double minPrice, double maxPrice, ProductsCallback callback) {
        Query query = db.collection(COLLECTION_PRODUCTS);

        if (minPrice > 0) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice);
        }
        if (maxPrice < Double.MAX_VALUE) { // Float.MAX_VALUE yerine Double.MAX_VALUE daha uygun olabilir
            query = query.whereLessThanOrEqualTo("price", maxPrice);
        }
        // Fiyat aralığı sorgularında ilk orderBy alanı fiyat olmalıdır.
        query = query.orderBy("price", Query.Direction.ASCENDING);
        // İkincil sıralama eklenebilir: .orderBy("name", Query.Direction.ASCENDING);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Product> productList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        productList.add(product);
                    } catch (Exception e) {
                        Log.e(TAG, "Döküman Product'a dönüştürülürken hata (fiyata göre): " + document.getId(), e);
                    }
                }
                callback.onProductsLoaded(productList);
            } else {
                Log.w(TAG, "Fiyat aralığına göre ürünler alınırken hata.", task.getException());
                callback.onError(task.getException() != null ? task.getException() : new Exception("Bilinmeyen fiyata göre ürün yükleme hatası"));
            }
        });
    }
}