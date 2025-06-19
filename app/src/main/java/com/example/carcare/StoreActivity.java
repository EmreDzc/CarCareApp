package com.example.carcare;

import android.app.Activity;
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

import com.example.carcare.ProfilePage.ProfileActivity;
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
    private ImageButton favoritesButton, cartButton;
    private FloatingActionButton fabAdmin; // FAB için tanımlama

    private ActivityResultLauncher<Intent> filterActivityResultLauncher;
    private ActivityResultLauncher<Intent> searchActivityLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        Log.d(TAG, "StoreActivity başlatıldı");
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firestore başlatıldı");

        // FilterActivity için launcher
        filterActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Log.d(TAG, "FilterActivity'den sonuç alındı, filtreler güncelleniyor.");
                        handleFilterResult(); // Yeni metod
                    }
                }
        );

        // SearchActivity için launcher
        searchActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String searchQuery = result.getData().getStringExtra("SEARCH_QUERY");
                        if (searchQuery != null && !searchQuery.isEmpty()) {
                            if (searchBar != null) {
                                searchBar.setText(searchQuery);
                            }
                        }
                    }
                }
        );

        initViews(); // searchBar burada initialize ediliyor
        setupRecyclerView();
        setupEventListeners(); // searchBar listener'ı burada ayarlanıyor
        setupBottomNavigation();
        checkAndShowAdminButton();

        handleIntent(getIntent()); // onCreate'de ilk gelen Intent'i işle
        loadProducts();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent); // Aktivite zaten açıksa yeni Intent'i işle
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            // SearchActivity'den gelen arama sorgusunu işle
            String searchQuery = intent.getStringExtra("SEARCH_QUERY");
            if (searchQuery != null && !searchQuery.isEmpty()) {
                if (searchBar != null) {
                    searchBar.setText(searchQuery);
                    // İmleci metnin sonuna getirmek isteyebilirsiniz:
                    searchBar.setSelection(searchBar.getText().length());
                }
                getIntent().putExtra("SEARCH_QUERY_HANDLED", true);
            }
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        searchBar = findViewById(R.id.search_bar); // EditText
        filterButton = findViewById(R.id.filter_button);
        badgeTextView = findViewById(R.id.cart_badge_text);
        favoritesButton = findViewById(R.id.favorites_button);
        cartButton = findViewById(R.id.cart_button);
        fabAdmin = findViewById(R.id.fab_admin);

        if (badgeTextView == null) {
            Log.e(TAG, "cart_badge_text (TextView) bulunamadı. Rozet güncellenemeyecek.");
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
        // searchBar EditText'ine tıklandığında veya odaklandığında SearchActivity'yi aç
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                Intent intent = new Intent(StoreActivity.this, SearchActivity.class);
                searchActivityLauncher.launch(intent);
            });
            searchBar.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // EditText'e odaklanıldığında da SearchActivity'yi aç
                    Intent intent = new Intent(StoreActivity.this, SearchActivity.class);
                    searchActivityLauncher.launch(intent);
                }
            });

            // Metin değişikliği dinleyicisi (kullanıcı doğrudan yazarsa diye)
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(adapter != null) adapter.filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        filterButton.setOnClickListener(v -> toggleFilters());

        favoritesButton.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(StoreActivity.this, WishlistActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(StoreActivity.this, "You must be logged in to see favorites..", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StoreActivity.this, "You must be logged in to see notifications.", Toast.LENGTH_SHORT).show();
                    return false; // İşlemi iptal et, sayfada kal
                }
            } else if (id == R.id.nav_settings) {
                intent = new Intent(this, ProfileActivity.class);
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
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        if (hasFilters) {
            // Filtreleri kaldır
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.putBoolean("hasFilters", false);
            editor.apply();

            filterButton.setText("Show Filters");
            Toast.makeText(this, "Filters removed", Toast.LENGTH_SHORT).show();
            loadProducts(); // Tüm ürünleri yükle
        } else {
            // FilterActivity'i aç
            Intent intent = new Intent(StoreActivity.this, FilterActivity.class);
            filterActivityResultLauncher.launch(intent);
        }
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
                    showError("An error occurred while loading products: " + e.getMessage());
                });
    }

// StoreActivity.java

    private void applyFiltersFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);

        // Filtre değerlerini al
        String searchText = prefs.getString("searchText", "").toLowerCase(); // Arama metnini küçük harfe çevir
        float minPrice = prefs.getFloat("minPrice", 0f);
        float maxPrice = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        String categoriesStr = prefs.getString("categories", "");
        String sortBy = prefs.getString("sortBy", "Relevance");

        Log.d(TAG, "Applying filters - Search: " + searchText + ", MinPrice: " + minPrice +
                ", MaxPrice: " + maxPrice + ", Categories: " + categoriesStr + ", Sort: " + sortBy);

        // Kategori listesini oluştur
        List<String> categoryList = new ArrayList<>();
        if (!categoriesStr.isEmpty()) {
            String[] categories = categoriesStr.split(",");
            for (String category : categories) {
                categoryList.add(category.trim());
            }
        }

        // Firebase sorgusu başlat
        Query query = db.collection("products");
        boolean needsClientSideFiltering = false;

        // Fiyat aralığı filtresinin uygulanıp uygulanmadığını kontrol etmek için bir bayrak
        boolean hasPriceRangeFilter = (minPrice > 0 || maxPrice < Float.MAX_VALUE);

        // Kategori filtresi (Firebase'de "whereIn" ile)
        if (!categoryList.isEmpty() && categoryList.size() <= 10) {
            query = query.whereIn("category", categoryList);
        } else if (!categoryList.isEmpty()) {
            // Eğer 10'dan fazla kategori seçilirse, bu filtrelemeyi uygulama içinde yapacağız.
            needsClientSideFiltering = true;
        }

        // Fiyat filtreleri (Firebase'de "range" ile)
        if (minPrice > 0) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice);
        }
        if (maxPrice < Float.MAX_VALUE) {
            query = query.whereLessThanOrEqualTo("price", maxPrice);
        }
        switch (sortBy) {
            case "Artan Fiyat":
                query = query.orderBy("price", Query.Direction.ASCENDING);
                break;
            case "Azalan Fiyat":
                query = query.orderBy("price", Query.Direction.DESCENDING);
                break;
            case "En Yeni":
                if (hasPriceRangeFilter) {
                    // Önce fiyat filtresi yapılan alana göre sırala
                    query = query.orderBy("price", Query.Direction.ASCENDING);
                }
                // Sonra asıl istenen alana göre sırala
                query = query.orderBy("createdAt", Query.Direction.DESCENDING);
                break;
            case "En Yüksek Puan":
                if (hasPriceRangeFilter) {
                    query = query.orderBy("price", Query.Direction.ASCENDING);
                }
                query = query.orderBy("averageRating", Query.Direction.DESCENDING);
                break;
            default: // Relevance (Ada göre sıralama)
                if (hasPriceRangeFilter) {
                    query = query.orderBy("price", Query.Direction.ASCENDING);
                }
                query = query.orderBy("name", Query.Direction.ASCENDING);
                break;
        }


        // Firebase sorgusunu çalıştır
        final boolean finalNeedsClientSideFiltering = needsClientSideFiltering;
        final List<String> finalCategoryList = categoryList;
        final String finalSearchText = searchText; // Zaten küçük harfe çevrildi

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase query successful. Documents: " + queryDocumentSnapshots.size());
                    products.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = parseProductFromDocument(document);
                        if (product != null) {
                            boolean shouldInclude = true;

                            // Eğer 10'dan fazla kategori seçildiyse, filtrelemeyi burada yap
                            if (finalNeedsClientSideFiltering && !finalCategoryList.isEmpty()) {
                                shouldInclude = finalCategoryList.contains(product.getCategory());
                            }

                            // Arama metni filtresi (her zaman uygulama içinde yapılır)
                            if (shouldInclude && !finalSearchText.isEmpty()) {
                                String productName = product.getName() != null ? product.getName().toLowerCase() : "";
                                String productDescription = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
                                String productBrand = product.getBrand() != null ? product.getBrand().toLowerCase() : "";

                                shouldInclude = productName.contains(finalSearchText) ||
                                        productDescription.contains(finalSearchText) ||
                                        productBrand.contains(finalSearchText);
                            }

                            if (shouldInclude) {
                                products.add(product);
                            }
                        }
                    }

                    Log.d(TAG, "Filtered products count: " + products.size());
                    updateProductDisplay();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Filtered products query failed", e);
                    showLoading(false);
                    // Hata mesajını doğrudan göster
                    showError("Error loading filtered products:\n" + e.getMessage());
                });
    }


    private void handleFilterResult() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        boolean hasFilters = prefs.getBoolean("hasFilters", false);

        // Filtre butonunun metnini güncelle
        if (filterButton != null) {
            filterButton.setText(hasFilters ? "Hide Filters" : "Show Filters");
        }

        // Ürünleri yeniden yükle
        loadProducts();
    }


    private void updateProductDisplay() {
        if (products.isEmpty()) {
            showError("No products were found matching the filter or there are no products yet.");
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
            // Not: "Hide/Show Filters" veya "Filtreleri Kaldır/Filtrele" gibi bir mantık kullanın
            filterButton.setText(hasFilters ? "REMOVE FILTERS" : "FILTER");
        }
    }
}