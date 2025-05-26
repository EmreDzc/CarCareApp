package com.example.carcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.activities.CartActivity;
import com.example.carcare.adapters.ProductAdapter;
import com.example.carcare.models.Product;
import com.example.carcare.utils.AdminUtils;
import com.example.carcare.utils.Cart;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    private static final String TAG = "StoreActivity";

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> products;
    private Button filterButton;
    private EditText searchBar;
    private ProgressBar progressBar;
    private TextView errorText;
    private FirebaseFirestore db;
    private TextView badgeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        Log.d(TAG, "StoreActivity başlatıldı");

        // Firebase Firestore başlat
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firestore initialized");

        // Views'ları başlat
        initViews();

        // RecyclerView ayarla
        setupRecyclerView();

        // Event listeners ayarla
        setupEventListeners();

        // Bottom navigation ayarla
        setupBottomNavigation();

        // Sepet rozeti güncelleme
        updateCartBadge();

        // Ürünleri Firebase'den yükle
        loadProducts();

        // Admin butonu kontrol et
        checkAndShowAdminButton();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        searchBar = findViewById(R.id.search_bar);
        badgeTextView = findViewById(R.id.cart_badge_text);
        filterButton = findViewById(R.id.filter_button);

        Log.d(TAG, "Views initialized");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        products = new ArrayList<>();

        Log.d(TAG, "RecyclerView layout manager set");

        // RecyclerView'in görünür olup olmadığını kontrol et
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "RecyclerView layout - Width: " + recyclerView.getWidth() +
                        ", Height: " + recyclerView.getHeight());
                Log.d(TAG, "RecyclerView visibility: " + recyclerView.getVisibility());
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        Log.d(TAG, "RecyclerView setup complete");
    }

    private void setupEventListeners() {
        // Arama çubuğu dinleyicisi
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filtre butonu
        filterButton.setOnClickListener(v -> toggleFilters());

        // Üst çubuk butonları
        ImageButton favoritesButton = findViewById(R.id.favorites_button);
        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, WishlistActivity.class);
            startActivity(intent);
        });

        ImageButton cartButton = findViewById(R.id.cart_button);
        cartButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, CartActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_store);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(StoreActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(StoreActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(StoreActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(StoreActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    private void checkAndShowAdminButton() {
        AdminUtils.checkAdminStatus(isAdmin -> {
            Log.d(TAG, "Is admin: " + isAdmin);
            if (isAdmin) {
                runOnUiThread(() -> {
                    com.google.android.material.floatingactionbutton.FloatingActionButton fabAdmin =
                            findViewById(R.id.fab_admin);
                    if (fabAdmin != null) {
                        Log.d(TAG, "FAB found, making visible");
                        fabAdmin.setVisibility(View.VISIBLE);
                        fabAdmin.setOnClickListener(v -> {
                            Intent intent = new Intent(StoreActivity.this, AdminProductActivity.class);
                            startActivity(intent);
                        });
                    } else {
                        Log.e(TAG, "FAB not found in layout");
                    }
                });
            } else {
                Log.d(TAG, "User is not admin");
            }
        });
    }

    private void toggleFilters() {
        Intent intent = new Intent(StoreActivity.this, FilterActivity.class);
        startActivity(intent);
    }

    private void loadProducts() {
        Log.d(TAG, "loadProducts() çağrıldı");
        showLoading(true);

        // Filtre ayarlarını kontrol et
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        if (hasFilters) {
            Log.d(TAG, "Filtreler uygulanıyor");
            applyFilters();
        } else {
            Log.d(TAG, "Tüm ürünler getiriliyor");
            getAllProducts();
        }
    }

    private void getAllProducts() {
        Log.d(TAG, "Firebase'den tüm ürünler isteniyor");

        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase sorgusu başarılı. Döküman sayısı: " + queryDocumentSnapshots.size());
                    showLoading(false);
                    products.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d(TAG, "Processing document: " + document.getId());

                            // Manuel veri çekme
                            String id = document.getId();
                            String name = document.getString("name");
                            String description = document.getString("description");
                            String imageUrl = document.getString("imageUrl");
                            String category = document.getString("category");

                            // Price ve stock için güvenli çekim
                            Object priceObj = document.get("price");
                            Object stockObj = document.get("stock");

                            double price = 0;
                            long stock = 0;

                            if (priceObj instanceof Number) {
                                price = ((Number) priceObj).doubleValue();
                            }

                            if (stockObj instanceof Number) {
                                stock = ((Number) stockObj).longValue();
                            }

                            Log.d(TAG, "Product data - Name: " + name + ", Price: " + price + ", ImageUrl: " + imageUrl);

                            // Manuel Product oluşturma
                            Product product = new Product();
                            product.setId(id);
                            product.setName(name);
                            product.setDescription(description);
                            product.setPrice(price);
                            product.setImageUrl(imageUrl);
                            product.setCategory(category);
                            product.setStock((int) stock);

                            products.add(product);
                            Log.d(TAG, "Product added: " + product.getName());

                        } catch (Exception e) {
                            Log.e(TAG, "Ürün dönüştürülürken hata: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Toplam ürün sayısı: " + products.size());

                    if (products.isEmpty()) {
                        showError("Henüz ürün bulunmamaktadır");
                    } else {
                        hideError();

                        Log.d(TAG, "Creating new adapter with " + products.size() + " products");

                        // YENİ ADAPTER OLUŞTUR
                        adapter = new ProductAdapter(this, products, badgeTextView);
                        recyclerView.setAdapter(adapter);

                        Log.d(TAG, "New adapter set. Item count: " + adapter.getItemCount());

                        // Verification
                        recyclerView.post(() -> {
                            Log.d(TAG, "=== POST-UPDATE VERIFICATION ===");
                            Log.d(TAG, "Adapter item count: " + adapter.getItemCount());
                            Log.d(TAG, "RecyclerView child count: " + recyclerView.getChildCount());
                            Log.d(TAG, "RecyclerView dimensions: " + recyclerView.getWidth() + "x" + recyclerView.getHeight());
                            Log.d(TAG, "RecyclerView visibility: " + recyclerView.getVisibility());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase sorgusu başarısız", e);
                    showLoading(false);
                    showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
                });
    }

    private void applyFilters() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        String searchText = prefs.getString("searchText", "");
        float minPrice = prefs.getFloat("minPrice", 0f);
        float maxPrice = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        String categories = prefs.getString("categories", "");
        String sortBy = prefs.getString("sortBy", "Relevance");

        List<String> categoryList = new ArrayList<>();
        if (!categories.isEmpty()) {
            categoryList = Arrays.asList(categories.split(","));
        }

        Query query = db.collection("products");

        // Fiyat filtresi
        if (maxPrice != Float.MAX_VALUE) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice)
                    .whereLessThanOrEqualTo("price", maxPrice);
        }

        // Kategori filtresi
        if (!categoryList.isEmpty()) {
            query = query.whereIn("category", categoryList);
        }

        // Sıralama
        if (sortBy.equals("Artan Fiyat")) {
            query = query.orderBy("price", Query.Direction.ASCENDING);
        } else if (sortBy.equals("Azalan Fiyat")) {
            query = query.orderBy("price", Query.Direction.DESCENDING);
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    products.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Manuel parsing (getAllProducts ile aynı)
                            String id = document.getId();
                            String name = document.getString("name");
                            String description = document.getString("description");
                            String imageUrl = document.getString("imageUrl");
                            String category = document.getString("category");

                            Object priceObj = document.get("price");
                            Object stockObj = document.get("stock");

                            double price = 0;
                            long stock = 0;

                            if (priceObj instanceof Number) {
                                price = ((Number) priceObj).doubleValue();
                            }

                            if (stockObj instanceof Number) {
                                stock = ((Number) stockObj).longValue();
                            }

                            Product product = new Product();
                            product.setId(id);
                            product.setName(name);
                            product.setDescription(description);
                            product.setPrice(price);
                            product.setImageUrl(imageUrl);
                            product.setCategory(category);
                            product.setStock((int) stock);

                            products.add(product);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing filtered product: " + document.getId(), e);
                        }
                    }

                    // Arama filtresi
                    if (!searchText.isEmpty()) {
                        List<Product> filteredList = new ArrayList<>();
                        for (Product product : products) {
                            if (product.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                                    product.getDescription().toLowerCase().contains(searchText.toLowerCase())) {
                                filteredList.add(product);
                            }
                        }
                        products = filteredList;
                    }

                    if (products.isEmpty()) {
                        showError("Filtrelenmiş sonuç bulunamadı");
                    } else {
                        hideError();

                        Log.d(TAG, "Creating filtered adapter with " + products.size() + " products");

                        // Filtered results için de yeni adapter oluştur
                        adapter = new ProductAdapter(this, products, badgeTextView);
                        recyclerView.setAdapter(adapter);

                        Log.d(TAG, "Filtered adapter set. Item count: " + adapter.getItemCount());
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
                    Log.e(TAG, "Error loading filtered products", e);
                });
    }

    private void showLoading(boolean isLoading) {
        Log.d(TAG, "showLoading called with: " + isLoading);

        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "After showLoading - RecyclerView visibility: " + recyclerView.getVisibility());
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        Log.e(TAG, "Hata gösteriliyor: " + message);
    }

    private void hideError() {
        Log.d(TAG, "hideError called");
        errorText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        Log.d(TAG, "RecyclerView visibility after hideError: " + recyclerView.getVisibility());
        Log.d(TAG, "ErrorText visibility after hideError: " + errorText.getVisibility());

        // RecyclerView'in layout parametrelerini kontrol et
        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        Log.d(TAG, "RecyclerView LayoutParams - Width: " + params.width + ", Height: " + params.height);
    }

    private void updateCartBadge() {
        int itemCount = Cart.getInstance().getItems().size();
        if (itemCount > 0) {
            badgeTextView.setVisibility(View.VISIBLE);
            badgeTextView.setText(String.valueOf(itemCount));
        } else {
            badgeTextView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() çağrıldı");

        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        if (hasFilters) {
            filterButton.setText("Hide Filters");
        } else {
            filterButton.setText("Show Filters");
        }

        updateCartBadge();

        if (hasFilters && (products == null || products.isEmpty())) {
            loadProducts();
        }
    }
}