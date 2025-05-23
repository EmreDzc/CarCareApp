package com.example.carcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.FilterActivity;
import com.example.carcare.R;
import com.example.carcare.activities.CartActivity;
import com.example.carcare.adapters.ProductAdapter;
import com.example.carcare.models.Product;
import com.example.carcare.utils.AdminUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
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
    private boolean filtersVisible = false;
    private ProgressBar progressBar;
    private TextView errorText;
    private FirebaseFirestore db;
    private TextView badgeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        // Firebase Firestore başlat
        db = FirebaseFirestore.getInstance();

        // Views
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        searchBar = findViewById(R.id.search_bar);
        badgeTextView = findViewById(R.id.cart_badge_text);

        // RecyclerView ayarla
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Ürün listesi başlat
        products = new ArrayList<>();
        adapter = new ProductAdapter(this, products, badgeTextView);
        recyclerView.setAdapter(adapter);

        // Arama çubuğu dinleyicisi
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filtre butonu ayarla
        filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFilters();
            }
        });

        // Üst çubuk butonları ayarla
        ImageButton favoritesButton = findViewById(R.id.favorites_button);
        favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Wishlist sayfasına git
                Intent intent = new Intent(StoreActivity.this, WishlistActivity.class);
                startActivity(intent);
            }
        });

        ImageButton cartButton = findViewById(R.id.cart_button);
        cartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sepet butonuna tıklama işlemi
                Intent intent = new Intent(StoreActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        // Alt navigasyon menüsü
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_store);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(StoreActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                // Zaten buradayız
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

        // Sepet rozeti güncelleme
        updateCartBadge();

        // Ürünleri Firebase'den yükle
        loadProducts();

        checkAndShowAdminButton();
        addAdminManually();

    }

    private void addAdminManually() {
        AdminUtils.makeUserAdmin("youremail@gmail.com", isSuccess -> {
            if (isSuccess) {
                Toast.makeText(this, "Admin eklendi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Admin eklenemedi", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void checkAndShowAdminButton() {
        AdminUtils.checkAdminStatus(isAdmin -> {
            if (isAdmin) {
                runOnUiThread(() -> {
                    com.google.android.material.floatingactionbutton.FloatingActionButton fabAdmin =
                            findViewById(R.id.fab_admin);
                    if (fabAdmin != null) {
                        fabAdmin.setVisibility(View.VISIBLE);
                        fabAdmin.setOnClickListener(v -> {
                            Intent intent = new Intent(StoreActivity.this, AdminProductActivity.class);
                            startActivity(intent);
                        });
                    }
                });
            }
        });
    }

    private void toggleFilters() {
        Intent intent = new Intent(StoreActivity.this, FilterActivity.class);
        startActivity(intent);
    }

    private void loadProducts() {
        showLoading(true);

        // Filtre ayarlarını kontrol et
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        if (hasFilters) {
            // Filtreleri uygula
            applyFilters();
        } else {
            // Tüm ürünleri getir
            db.collection("products")
                    .orderBy("name")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        showLoading(false);
                        products.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Product product = document.toObject(Product.class);
                            products.add(product);
                        }

                        if (products.isEmpty()) {
                            showError("Henüz ürün bulunmamaktadır");
                        } else {
                            adapter.updateList(products);
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
                        Log.e(TAG, "Error loading products", e);
                    });
        }
    }

    private void applyFilters() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        String searchText = prefs.getString("searchText", "");
        float minPrice = prefs.getFloat("minPrice", 0f);
        float maxPrice = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        String categories = prefs.getString("categories", "");
        String sortBy = prefs.getString("sortBy", "Relevance");

        // Kategori listesini ayır
        List<String> categoryList = new ArrayList<>();
        if (!categories.isEmpty()) {
            categoryList = Arrays.asList(categories.split(","));
        }

        // Firestore sorgusu oluştur
        Query query = db.collection("products");

        // Fiyat filtresi uygula
        query = query.whereGreaterThanOrEqualTo("price", minPrice)
                .whereLessThanOrEqualTo("price", maxPrice);

        // Kategori filtresi (eğer seçili kategoriler varsa)
        if (!categoryList.isEmpty()) {
            query = query.whereIn("category", categoryList);
        }

        // Sıralama uygula
        if (sortBy.equals("Artan Fiyat")) {
            query = query.orderBy("price", Query.Direction.ASCENDING);
        } else if (sortBy.equals("Azalan Fiyat")) {
            query = query.orderBy("price", Query.Direction.DESCENDING);
        } else {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        // Sorguyu çalıştır
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    products.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        products.add(product);
                    }

                    // Arama filtresi uygula
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
                        adapter.updateList(products);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
                    Log.e(TAG, "Error loading filtered products", e);
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void updateCartBadge() {
        int itemCount = com.example.carcare.utils.Cart.getInstance().getItems().size();
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
        // Filtrelerin uygulanıp uygulanmadığını kontrol et
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        // Filtre butonunun yazısını güncelle
        if (hasFilters) {
            filterButton.setText("Hide Filters");
        } else {
            filterButton.setText("Show Filters");
        }

        // Sepet rozetini güncelle
        updateCartBadge();

        // Filtrelenmişse ürünleri yeniden yükle
        if (hasFilters && products.isEmpty()) {
            loadProducts();
        }
    }
}