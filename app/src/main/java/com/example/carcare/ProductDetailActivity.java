package com.example.carcare;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    private ImageView productImage;
    private TextView productName, productPrice, productDescription, productCategory, productStockStatus;
    private RatingBar productRating;
    private Button addToCartButton;
    private ImageButton backButton, favoriteButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
        loadProductDetails();
    }

    private void initViews() {
        productImage = findViewById(R.id.product_detail_image);
        productName = findViewById(R.id.product_detail_name);
        productPrice = findViewById(R.id.product_detail_price);
        productDescription = findViewById(R.id.product_detail_description);
        productCategory = findViewById(R.id.product_detail_category);
        productStockStatus = findViewById(R.id.product_detail_stock);
        productRating = findViewById(R.id.product_detail_rating);
        addToCartButton = findViewById(R.id.btn_add_to_cart_detail);
        backButton = findViewById(R.id.btn_back);
        favoriteButton = findViewById(R.id.btn_favorite_detail);
        progressBar = findViewById(R.id.progress_bar_detail);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        addToCartButton.setOnClickListener(v -> {
            if (currentProduct != null) {
                Cart.getInstance().addItem(currentProduct, this);
                Toast.makeText(this, currentProduct.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
            }
        });
        favoriteButton.setOnClickListener(v -> {
            if (currentProduct != null) {
                toggleFavorite(currentProduct);
            }
        });
    }

    private void loadProductDetails() {
        showLoading(true);
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(documentSnapshot.getId()); // ID'yi ata
                            updateUI(currentProduct);
                            checkFavoriteStatus(currentProduct);
                        } else {
                            showError("Ürün bilgileri dönüştürülemedi");
                        }
                    } else {
                        showError("Ürün bulunamadı");
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Ürün yüklenirken hata", e);
                    showError("Ürün yüklenirken hata: " + e.getMessage());
                });
    }

    private void updateUI(Product product) {
        productName.setText(product.getName());
        productPrice.setText(String.format(Locale.US, "$%.2f", product.getPrice()));
        productDescription.setText(product.getDescription());

        if (product.getCategory() != null && !product.getCategory().isEmpty()) {
            String categoryText = product.getCategory().substring(0, 1).toUpperCase() + product.getCategory().substring(1).toLowerCase();
            productCategory.setText(categoryText);
        } else {
            productCategory.setText("Genel");
        }

        if (product.getStock() > 0) {
            productStockStatus.setText(String.format(Locale.getDefault(),"Stokta var (%d adet)", product.getStock()));
            productStockStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            addToCartButton.setEnabled(true);
        } else {
            productStockStatus.setText("Tükendi");
            productStockStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            addToCartButton.setEnabled(false);
        }

        productRating.setRating(4.0f); // Örnek, gerekirse Firebase'den alınmalı

        // Base64 string'den resim yükleme
        String imageBase64 = product.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedString)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode hatası, ürün: " + product.getName(), e);
                productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void toggleFavorite(Product product) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();
        String prodId = product.getId();

        if (prodId == null || prodId.isEmpty()) {
            Toast.makeText(this, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).collection("favorites").document(prodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        db.collection("users").document(userId).collection("favorites").document(prodId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                    Toast.makeText(this, product.getName() + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Map<String, Object> favoriteData = new HashMap<>();
                        favoriteData.put("productId", prodId);
                        favoriteData.put("addedAt", FieldValue.serverTimestamp());
                        db.collection("users").document(userId).collection("favorites").document(prodId)
                                .set(favoriteData)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite);
                                    Toast.makeText(this, product.getName() + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void checkFavoriteStatus(Product product) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || product.getId() == null || product.getId().isEmpty()) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            return;
        }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    favoriteButton.setImageResource(documentSnapshot.exists() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                })
                .addOnFailureListener(e -> favoriteButton.setImageResource(R.drawable.ic_favorite_border));
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        int contentVisibility = isLoading ? View.INVISIBLE : View.VISIBLE;
        productImage.setVisibility(contentVisibility);
        productName.setVisibility(contentVisibility);
        productPrice.setVisibility(contentVisibility);
        productDescription.setVisibility(contentVisibility);
        productCategory.setVisibility(contentVisibility);
        productStockStatus.setVisibility(contentVisibility);
        productRating.setVisibility(contentVisibility);
        addToCartButton.setVisibility(contentVisibility);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Hata durumunda aktiviteyi sonlandırmak yerine kullanıcıya bilgi vermek daha iyi olabilir.
        // finish();
    }
}