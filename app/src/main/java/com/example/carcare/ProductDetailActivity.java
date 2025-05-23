package com.example.carcare;

import android.os.Bundle;
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
import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName, productPrice, productDescription, productCategory, productStockStatus;
    private RatingBar productRating;
    private Button addToCartButton;
    private ImageButton backButton, favoriteButton;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Ürün ID'sini al
        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null) {
            Toast.makeText(this, "Ürün bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // FireStore başlat
        db = FirebaseFirestore.getInstance();

        // View elemanlarını tanımla
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

        // Geri butonu
        backButton.setOnClickListener(v -> finish());

        // Ürün bilgilerini yükle
        loadProductDetails();

        // Sepete Ekle butonu
        addToCartButton.setOnClickListener(v -> {
            if (currentProduct != null) {
                Cart.getInstance().addItem(currentProduct, this);
                Toast.makeText(this, currentProduct.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
            }
        });

        // Favori butonu - ProductAdapter içindeki toggleFavorite metoduna benzer şekilde
        favoriteButton.setOnClickListener(v -> {
            if (currentProduct != null) {
                // ProductAdapter'daki toggleFavorite metodunu kullanın
            }
        });
    }

    private void loadProductDetails() {
        showLoading(true);

        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(Product.class);

                        if (currentProduct != null) {
                            // UI'ı güncelle
                            updateUI(currentProduct);
                        } else {
                            showError("Ürün bilgileri yüklenemedi");
                        }
                    } else {
                        showError("Ürün bulunamadı");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Ürün yüklenirken hata: " + e.getMessage());
                });
    }

    private void updateUI(Product product) {
        productName.setText(product.getName());
        productPrice.setText(String.format(Locale.US, "$%.2f", product.getPrice()));
        productDescription.setText(product.getDescription());

        if (product.getCategory() != null) {
            String categoryText = product.getCategory().substring(0, 1).toUpperCase() + product.getCategory().substring(1);
            productCategory.setText(categoryText);
        } else {
            productCategory.setText("Genel");
        }

        // Stok durumu
        if (product.getStock() > 0) {
            productStockStatus.setText("Stokta var");
            productStockStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            addToCartButton.setEnabled(true);
        } else {
            productStockStatus.setText("Tükendi");
            productStockStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            addToCartButton.setEnabled(false);
        }

// Derecelendirme (gerçek uygulamada Firebase'den ortalama derecelendirme alınabilir)
        productRating.setRating(4.0f);

        // Ürün görseli
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(productImage);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Favori durumunu kontrol et
        checkFavoriteStatus();
    }

    private void checkFavoriteStatus() {
        // ProductAdapter'daki checkFavoriteStatus metodunu kullanın
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            productImage.setVisibility(View.INVISIBLE);
            productName.setVisibility(View.INVISIBLE);
            productPrice.setVisibility(View.INVISIBLE);
            productDescription.setVisibility(View.INVISIBLE);
            productCategory.setVisibility(View.INVISIBLE);
            productStockStatus.setVisibility(View.INVISIBLE);
            productRating.setVisibility(View.INVISIBLE);
            addToCartButton.setVisibility(View.INVISIBLE);
        } else {
            productImage.setVisibility(View.VISIBLE);
            productName.setVisibility(View.VISIBLE);
            productPrice.setVisibility(View.VISIBLE);
            productDescription.setVisibility(View.VISIBLE);
            productCategory.setVisibility(View.VISIBLE);
            productStockStatus.setVisibility(View.VISIBLE);
            productRating.setVisibility(View.VISIBLE);
            addToCartButton.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}