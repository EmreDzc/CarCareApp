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
import android.widget.Toast; // Toast için import eklendi

import androidx.activity.result.ActivityResultLauncher; // FilterActivity için eklendi
import androidx.activity.result.contract.ActivityResultContracts; // FilterActivity için eklendi
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.activities.CartActivity;
import com.example.carcare.adapters.ProductAdapter;
import com.example.carcare.models.Product;
import com.example.carcare.utils.AdminUtils;
import com.example.carcare.utils.Cart;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // FAB için import
import com.google.firebase.auth.FirebaseAuth; // FirebaseAuth için import eklendi
import com.google.firebase.auth.FirebaseUser; // FirebaseUser için import eklendi
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
    private ImageButton favoritesButton, cartButton, profileButton; // profileButton eklendi
    private FloatingActionButton fabAdmin; // FAB için tanımlama

    // FilterActivity'den sonuç almak için Launcher
    private ActivityResultLauncher<Intent> filterActivityResultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        Log.d(TAG, "StoreActivity başlatıldı");
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firestore başlatıldı");

        // FilterActivity'den sonucu almak için launcher'ı başlat
        filterActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        // Filtreler uygulandı veya temizlendi, ürünleri yeniden yükle
                        Log.d(TAG, "FilterActivity'den sonuç alındı, ürünler yeniden yükleniyor.");
                        loadProducts();
                    }
                }
        );

        initViews();
        setupRecyclerView();
        setupEventListeners();
        setupBottomNavigation();
        // updateCartBadge(); // onResume içinde çağrılıyor
        // loadProducts(); // onResume içinde çağrılıyor
        checkAndShowAdminButton();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        searchBar = findViewById(R.id.search_bar);
        filterButton = findViewById(R.id.filter_button);
        badgeTextView = findViewById(R.id.cart_badge_text);
        favoritesButton = findViewById(R.id.favorites_button);
        cartButton = findViewById(R.id.cart_button);
        profileButton = findViewById(R.id.profile_button); // profileButton ID'si ile eşleştirildi
        fabAdmin = findViewById(R.id.fab_admin); // FAB ID'si ile eşleştirildi

        if (badgeTextView == null) {
            Log.e(TAG, "cart_badge_text (TextView) bulunamadı. Rozet güncellenemeyecek.");
        }
        if (profileButton == null) {
            Log.w(TAG, "profile_button bulunamadı.");
        }
        if (fabAdmin == null) {
            Log.w(TAG, "fab_admin bulunamadı.");
        }
        Log.d(TAG, "View'lar başlatıldı");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "RecyclerView ayarlanıyor");
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        products = new ArrayList<>();
        Log.d(TAG, "RecyclerView ayarları tamamlandı");
    }

    private void setupEventListeners() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(adapter != null) adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        filterButton.setOnClickListener(v -> toggleFilters());

        favoritesButton.setOnClickListener(v -> {
            // WishlistActivity'e gitmeden önce kullanıcı girişi kontrolü
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(StoreActivity.this, WishlistActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(StoreActivity.this, "Favorileri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
                // Opsiyonel: LoginActivity'e yönlendir
            }
        });

        cartButton.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, CartActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_store); // Başlangıçta Store seçili
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_dashboard) { // bottom_nav_menu.xml'deki ID'ler
                intent = new Intent(this, CarActivity.class);
            } else if (id == R.id.nav_store) {
                return true; // Zaten buradayız
            } else if (id == R.id.nav_map) {
                intent = new Intent(this, MapsActivity.class);
            } else if (id == R.id.nav_notifications) {
                // Bildirimler için kullanıcı girişi kontrolü
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    intent = new Intent(this, NotificationActivity.class);
                } else {
                    Toast.makeText(StoreActivity.this, "Bildirimleri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
                    return false; // İşlemi iptal et, sayfada kal
                }
            } else if (id == R.id.nav_settings) {
                intent = new Intent(this, SettingsActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0); // Geçiş animasyonunu kaldır
                return true;
            }
            return false;
        });
    }

    private void checkAndShowAdminButton() {
        AdminUtils.checkAdminStatus(isAdmin -> {
            Log.d(TAG, "Admin mi: " + isAdmin);
            if (isAdmin && fabAdmin != null) {
                runOnUiThread(() -> {
                    Log.d(TAG, "FAB admin için görünür yapılıyor");
                    fabAdmin.setVisibility(View.VISIBLE);
                    fabAdmin.setOnClickListener(v -> {
                        Intent intent = new Intent(StoreActivity.this, AdminProductActivity.class);
                        startActivity(intent);
                    });
                });
            } else if (fabAdmin != null) {
                runOnUiThread(() -> fabAdmin.setVisibility(View.GONE)); // Admin değilse gizle
            }
        });
    }

    private void toggleFilters() {
        // FilterActivity'i sonuç için başlat
        Intent intent = new Intent(StoreActivity.this, FilterActivity.class);
        filterActivityResultLauncher.launch(intent);
    }


    private void loadProducts() {
        Log.d(TAG, "loadProducts() çağrıldı");
        showLoading(true);

        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        if (hasFilters) {
            Log.d(TAG, "Filtreler uygulanıyor");
            applyFiltersFromPrefs();
        } else {
            Log.d(TAG, "Tüm ürünler getiriliyor");
            fetchAllProducts();
        }
    }

    // StoreActivity.java içinde bulunan parseProductFromDocument metodu (zaten doğru)
    private Product parseProductFromDocument(QueryDocumentSnapshot document) {
        if (document == null) return null;
        try {
            String id = document.getId();
            String name = document.getString("name");
            String description = document.getString("description");
            String imageBase64 = document.getString("imageBase64");
            String category = document.getString("category");
            String brand = document.getString("brand");

            double price = 0;
            if (document.get("price") instanceof Number) {
                price = document.getDouble("price");
            }

            double discountPrice = 0;
            if (document.get("discountPrice") instanceof Number) {
                discountPrice = document.getDouble("discountPrice");
            }

            long stock = 0;
            if (document.get("stock") instanceof Number) {
                stock = document.getLong("stock");
            }

            // Rating bilgilerini al
            float averageRating = 0.0f;
            if (document.get("averageRating") instanceof Number) {
                averageRating = document.getDouble("averageRating").floatValue();
            }

            int totalReviews = 0;
            if (document.get("totalReviews") instanceof Number) {
                totalReviews = document.getLong("totalReviews").intValue();
            }

            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setDiscountPrice(discountPrice);
            product.setImageBase64(imageBase64);
            product.setCategory(category);
            product.setBrand(brand);
            product.setStock((int) stock);
            product.setAverageRating(averageRating); // Rating bilgisini set et
            product.setTotalReviews(totalReviews);   // Review sayısını set et

            // Diğer alanları da parse edebilirsiniz (modelCode, sellerName vb.)
            // product.setModelCode(document.getString("modelCode"));
            // ...

            return product;
        } catch (Exception e) {
            Log.e(TAG, "Ürün parse edilirken hata: " + document.getId(), e);
            return null;
        }
    }


    private void fetchAllProducts() {
        Log.d(TAG, "Firebase'den tüm ürünler isteniyor");
        db.collection("products")
                .orderBy("name", Query.Direction.ASCENDING) // Ada göre sırala
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase sorgusu başarılı. Döküman sayısı: " + queryDocumentSnapshots.size());
                    products.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = parseProductFromDocument(document);
                        if (product != null) products.add(product);
                    }
                    Log.d(TAG, "Toplam ürün sayısı: " + products.size());
                    updateProductDisplay();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase sorgusu başarısız", e);
                    showLoading(false);
                    showError("Ürünler yüklenirken hata oluştu: " + e.getMessage());
                });
    }

    private void applyFiltersFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        float minPrice = prefs.getFloat("minPrice", 0f);
        float maxPrice = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        String categoriesStr = prefs.getString("categories", "");
        String sortBy = prefs.getString("sortBy", "Relevance");

        List<String> categoryList = new ArrayList<>();
        if (categoriesStr != null && !categoriesStr.isEmpty()) {
            categoryList.addAll(Arrays.asList(categoriesStr.split(",")));
        }

        Query query = db.collection("products");

        // Fiyat ve Kategori filtrelerini uygula
        boolean hasPriceFilter = false;
        if (minPrice > 0) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice);
            hasPriceFilter = true;
        }
        if (maxPrice < Float.MAX_VALUE) {
            query = query.whereLessThanOrEqualTo("price", maxPrice);
            hasPriceFilter = true;
        }
        if (!categoryList.isEmpty()) {
            // Firestore 'whereIn' sorgusu için liste 10 elemandan fazla olamaz.
            // Eğer çok fazla kategori seçilirse, bu sorguyu bölmeniz gerekebilir.
            // Şimdilik basit tutuyoruz.
            query = query.whereIn("category", categoryList);
        }

        // Sıralama
        // Eğer fiyat filtresi varsa ve sıralama "Artan/Azalan Fiyat" değilse,
        // Firestore birden fazla inequality ve farklı bir orderBy alanı ile sorun yaşayabilir.
        // Bu durumda, ilk orderBy alanı fiyat olmalı ya da sorguyu basitleştirmelisiniz.
        if ("Artan Fiyat".equals(sortBy)) {
            query = query.orderBy("price", Query.Direction.ASCENDING);
        } else if ("Azalan Fiyat".equals(sortBy)) {
            query = query.orderBy("price", Query.Direction.DESCENDING);
        } else { // "Relevance" (veya varsayılan)
            if (hasPriceFilter) {
                // Fiyat filtresi varsa ve sıralama fiyat değilse, Firestore'un composite index'e ihtiyacı olabilir.
                // Veya basitlik için, sadece ada göre sıralayıp fiyat aralığını kodda filtreleyebilirsiniz (performans düşebilir).
                // Şimdilik, fiyat filtresi varsa ve sıralama belirtilmemişse fiyata göre sıralıyoruz.
                query = query.orderBy("price", Query.Direction.ASCENDING).orderBy("name", Query.Direction.ASCENDING);
            } else {
                query = query.orderBy("name", Query.Direction.ASCENDING);
            }
        }


        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    products.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = parseProductFromDocument(document);
                        if (product != null) products.add(product);
                    }
                    Log.d(TAG, "Filtrelenmiş ürün sayısı: " + products.size());
                    updateProductDisplay();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Filtrelenmiş ürünler yüklenirken hata", e);
                    showLoading(false);
                    showError("Filtrelenmiş ürünler yüklenirken hata: " + e.getMessage());
                });
    }


    private void updateProductDisplay() {
        if (products.isEmpty()) {
            showError("Filtreyle eşleşen ürün bulunamadı veya henüz ürün yok.");
        } else {
            hideError();
            if (adapter == null) {
                adapter = new ProductAdapter(this, products, badgeTextView);
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "Yeni adapter oluşturuldu: " + products.size() + " ürün.");
            } else {
                adapter.updateList(new ArrayList<>(products));
                Log.d(TAG, "Adapter güncellendi: " + products.size() + " ürün.");
            }
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            errorText.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        Log.e(TAG, "Hata gösteriliyor: " + message);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
        Log.d(TAG, "Hata gizlendi.");
    }

    private void updateCartBadge() {
        if (badgeTextView == null) return;
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
        updateCartBadge();

        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);
        if (filterButton != null) {
            filterButton.setText(hasFilters ? "Filtreleri Kaldır" : "Filtrele");
        }
        loadProducts(); // Her onResume'da ürünleri yeniden yükle (filtreler değişmiş olabilir)
    }
}