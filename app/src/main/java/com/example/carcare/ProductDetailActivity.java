package com.example.carcare;

import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.adapters.ReviewAdapter;
import com.example.carcare.models.Product;
import com.example.carcare.models.Review;
import com.example.carcare.utils.Cart;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    private ImageView productImage;
    private TextView productName, productPrice, productDescription, productBrand,
            productModelCode, productSeller,
            productColorText, productStockStatus, toolbarTitle, productDiscountPrice,
            productWarrantyInfo, productShippingInfo, productReturnPolicy;

    private RatingBar productRatingMain;
    private TextView productReviewCountMain;

    private Button addToCartButton;
    private ImageButton backButton, favoriteButton;
    private ProgressBar progressBar;
    private LinearLayout productSpecificationsLayout, productColorsLayout, productSizesLayout;
    private ChipGroup chipGroupSizes, chipGroupTags;
    private Toolbar toolbar;

    private TextView titleDescription, titleSpecifications, titleShippingInfo;
    private View dividerAfterDescription, dividerAfterSpecs, dividerBeforeReviews;

    private RatingBar ratingBarSubmit;
    private TextInputEditText editTextReviewComment;
    private Button btnSubmitReview;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList; // Bu aktivitenin ana review listesi
    private TextView textReviewsTitle, textNoReviews;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(this, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
        loadProductDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        productImage = findViewById(R.id.product_detail_image);
        productBrand = findViewById(R.id.product_detail_brand);
        productName = findViewById(R.id.product_detail_name);
        productModelCode = findViewById(R.id.product_detail_model_code);
        productPrice = findViewById(R.id.product_detail_price);
        productDiscountPrice = findViewById(R.id.product_detail_discount_price);
        productDescription = findViewById(R.id.product_detail_description);
        productSeller = findViewById(R.id.product_detail_seller);

        productRatingMain = findViewById(R.id.product_detail_rating_main);
        productReviewCountMain = findViewById(R.id.product_detail_review_count_main);

        productColorsLayout = findViewById(R.id.product_colors_layout);
        productColorText = findViewById(R.id.product_detail_color);
        productSizesLayout = findViewById(R.id.product_sizes_layout);
        chipGroupSizes = findViewById(R.id.chip_group_sizes);
        productStockStatus = findViewById(R.id.product_detail_stock_status);
        productSpecificationsLayout = findViewById(R.id.product_specifications_layout);
        productWarrantyInfo = findViewById(R.id.product_detail_warranty_info);
        productShippingInfo = findViewById(R.id.product_detail_shipping_info);
        productReturnPolicy = findViewById(R.id.product_detail_return_policy);
        chipGroupTags = findViewById(R.id.chip_group_tags);

        titleDescription = findViewById(R.id.title_description);
        titleSpecifications = findViewById(R.id.title_specifications);
        titleShippingInfo = findViewById(R.id.title_shipping_info);
        dividerAfterDescription = findViewById(R.id.divider_after_description);
        dividerAfterSpecs = findViewById(R.id.divider_after_specs);
        dividerBeforeReviews = findViewById(R.id.divider_before_reviews);

        addToCartButton = findViewById(R.id.btn_add_to_cart_detail);
        backButton = findViewById(R.id.btn_back);
        favoriteButton = findViewById(R.id.btn_favorite_detail);
        progressBar = findViewById(R.id.progress_bar_detail);

        ratingBarSubmit = findViewById(R.id.rating_bar_submit);
        editTextReviewComment = findViewById(R.id.edit_text_review_comment);
        btnSubmitReview = findViewById(R.id.btn_submit_review);
        recyclerViewReviews = findViewById(R.id.recycler_view_reviews);
        textReviewsTitle = findViewById(R.id.text_reviews_title);
        textNoReviews = findViewById(R.id.text_no_reviews);

        reviewList = new ArrayList<>(); // Aktivitenin listesini initialize et
        reviewAdapter = new ReviewAdapter(this, reviewList); // Adapter'a bu listeyi (başlangıçta boş) ver
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(reviewAdapter);
        recyclerViewReviews.setNestedScrollingEnabled(false);
        Log.d(TAG, "Views initialized.");
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        addToCartButton.setOnClickListener(v -> {
            if (currentProduct != null) {
                Cart.getInstance().addItem(currentProduct, this);
                Toast.makeText(this, currentProduct.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
            }
        });
        favoriteButton.setOnClickListener(v -> { if (currentProduct != null) toggleFavorite(currentProduct); });
        btnSubmitReview.setOnClickListener(v -> submitReview());
        Log.d(TAG, "Listeners setup.");
    }

    private void loadProductDetails() {
        Log.d(TAG, "Loading product details for ID: " + productId);
        showLoading(true);
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Product document found: " + documentSnapshot.getId());
                        currentProduct = documentSnapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(documentSnapshot.getId());
                            updateUI(currentProduct);
                            checkFavoriteStatus(currentProduct);
                            loadReviews();
                        } else {
                            showError("Ürün bilgileri dönüştürülemedi.");
                            showLoading(false);
                        }
                    } else {
                        showError("Ürün bulunamadı.");
                        showLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Ürün yükleme hatası", e);
                    showError("Hata: " + e.getMessage());
                });
    }

    private void updateUI(Product product) {
        if (product == null) { Log.e(TAG, "updateUI called with null product."); return; }
        toolbarTitle.setText(product.getName() != null ? product.getName() : "Ürün Detayı");
        updateTextViewVisibility(productBrand, product.getBrand(), "");
        productName.setText(product.getName() != null ? product.getName() : "N/A");
        updateTextViewVisibility(productModelCode, product.getModelCode(), "Model: ");

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        if (product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
            productPrice.setText(currencyFormat.format(product.getPrice()));
            productPrice.setPaintFlags(productPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            productPrice.setTextColor(ContextCompat.getColor(this, R.color.grey_medium));
            productDiscountPrice.setText(currencyFormat.format(product.getDiscountPrice()));
            productDiscountPrice.setVisibility(View.VISIBLE);
        } else {
            productPrice.setText(currencyFormat.format(product.getPrice()));
            productPrice.setPaintFlags(productPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            productPrice.setTextColor(ContextCompat.getColor(this, R.color.orange_primary));
            productDiscountPrice.setVisibility(View.GONE);
        }

        if (productRatingMain != null) { productRatingMain.setRating(product.getAverageRating()); }
        if (productReviewCountMain != null) { productReviewCountMain.setText(String.format(Locale.getDefault(), "(%d)", product.getTotalReviews()));}

        updateTextViewVisibility(productSeller, product.getSellerName(), "Satıcı: ");

        if (!TextUtils.isEmpty(product.getColor())) {
            productColorsLayout.setVisibility(View.VISIBLE);
            productColorText.setText(product.getColor());
        } else { productColorsLayout.setVisibility(View.GONE); }

        updateChipGroup(chipGroupSizes, product.getSizes(), productSizesLayout);

        if (product.getStock() > 0) {
            productStockStatus.setText(String.format(Locale.getDefault(), "Stokta: %d adet", product.getStock()));
            productStockStatus.setTextColor(ContextCompat.getColor(this, R.color.green_dark));
            addToCartButton.setEnabled(true); addToCartButton.setText("Sepete Ekle");
        } else {
            productStockStatus.setText("Tükendi");
            productStockStatus.setTextColor(ContextCompat.getColor(this, R.color.red_dark));
            addToCartButton.setEnabled(false); addToCartButton.setText("Tükendi");
        }
        updateSectionVisibility(titleDescription, productDescription, product.getDescription(), dividerAfterDescription);
        if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()) {
            titleSpecifications.setVisibility(View.VISIBLE);
            productSpecificationsLayout.setVisibility(View.VISIBLE);
            if (dividerAfterSpecs != null) dividerAfterSpecs.setVisibility(View.VISIBLE);
            productSpecificationsLayout.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Map.Entry<String, String> entry : product.getSpecifications().entrySet()) {
                View specView = inflater.inflate(R.layout.item_specification, productSpecificationsLayout, false);
                ((TextView) specView.findViewById(R.id.spec_key)).setText(entry.getKey());
                ((TextView) specView.findViewById(R.id.spec_value)).setText(entry.getValue());
                productSpecificationsLayout.addView(specView);
            }
        } else {
            titleSpecifications.setVisibility(View.GONE);
            productSpecificationsLayout.setVisibility(View.GONE);
            if (dividerAfterSpecs != null) dividerAfterSpecs.setVisibility(View.GONE);
        }
        boolean hasShippingInfoSection = !TextUtils.isEmpty(product.getShippingInfo()) || !TextUtils.isEmpty(product.getWarrantyInfo()) || !TextUtils.isEmpty(product.getReturnPolicy());
        titleShippingInfo.setVisibility(hasShippingInfoSection ? View.VISIBLE : View.GONE);
        updateTextViewVisibility(productShippingInfo, product.getShippingInfo(), "");
        updateTextViewVisibility(productWarrantyInfo, product.getWarrantyInfo(), "");
        updateTextViewVisibility(productReturnPolicy, product.getReturnPolicy(), "");
        updateChipGroup(chipGroupTags, product.getTags(), null);
        if (!TextUtils.isEmpty(product.getImageBase64())) {
            try {
                byte[] decodedString = Base64.decode(product.getImageBase64(), Base64.DEFAULT);
                Glide.with(this).load(decodedString).placeholder(R.drawable.placeholder_image).error(R.drawable.error_image).into(productImage);
            } catch (IllegalArgumentException e) { Log.e(TAG, "Base64 decode error for product image", e); productImage.setImageResource(R.drawable.error_image); }
        } else { productImage.setImageResource(R.drawable.placeholder_image); }
        Log.d(TAG, "UI updated for product: " + (product.getName() != null ? product.getName() : "N/A"));
    }

    private void loadReviews() {
        Log.d(TAG, "Loading reviews for product ID: " + productId);
        db.collection("products").document(productId).collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20) // İsteğe bağlı: Çok fazla yorum varsa sayfalama veya limit ekleyin
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> loadedReviews = new ArrayList<>(); // Her zaman yeni bir liste oluştur
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Review review = doc.toObject(Review.class);
                            if (review != null) {
                                review.setId(doc.getId());
                                loadedReviews.add(review);
                            }
                        }
                    }
                    Log.d(TAG, "Reviews loaded from Firestore: " + loadedReviews.size());

                    // Aktivitenin ana reviewList'ini bu yeni yüklenenlerle güncelle
                    this.reviewList.clear();
                    this.reviewList.addAll(loadedReviews);

                    // Adapter'ı güncelle (adapter içindeki liste de güncellenmiş olacak)
                    reviewAdapter.updateReviews(this.reviewList);

                    // UI görünürlüğünü güncelle (aktivitedeki güncel reviewList'e göre)
                    updateReviewsUIVisibility();

                    showLoading(false); // Tüm yüklemeler (ürün + yorumlar) bitti
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    Toast.makeText(this, "Değerlendirmeler yüklenemedi.", Toast.LENGTH_SHORT).show();
                    // Hata durumunda da UI'ı güncelle
                    this.reviewList.clear(); // Hata varsa listeyi boşalt
                    reviewAdapter.updateReviews(this.reviewList);
                    updateReviewsUIVisibility();
                    showLoading(false);
                });
    }

    private void updateReviewsUIVisibility() {
        // Bu metod aktivitedeki `this.reviewList`'i kontrol etmeli
        Log.d(TAG, "updateReviewsUIVisibility - Current reviewList size: " + (this.reviewList != null ? this.reviewList.size() : "null"));
        if (this.reviewList == null || this.reviewList.isEmpty()) {
            textNoReviews.setVisibility(View.VISIBLE);
            recyclerViewReviews.setVisibility(View.GONE);
            textReviewsTitle.setVisibility(View.GONE);
        } else {
            textNoReviews.setVisibility(View.GONE);
            recyclerViewReviews.setVisibility(View.VISIBLE);
            textReviewsTitle.setVisibility(View.VISIBLE);
        }
        if (dividerBeforeReviews != null) dividerBeforeReviews.setVisibility(View.VISIBLE);
    }


    private void submitReview() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Değerlendirme yapmak için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }
        float ratingValue = ratingBarSubmit.getRating();
        if (ratingValue == 0) {
            Toast.makeText(this, "Lütfen ürüne puan verin (1-5 yıldız).", Toast.LENGTH_SHORT).show();
            return;
        }
        String comment = editTextReviewComment.getText() != null ? editTextReviewComment.getText().toString().trim() : "";
        showLoading(true);
        Log.d(TAG, "Loading state: " + true); // Log eklendi
        String userId = firebaseUser.getUid();
        String userName = firebaseUser.getDisplayName();
        if (TextUtils.isEmpty(userName)) {
            String email = firebaseUser.getEmail();
            userName = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Anonim Kullanıcı";
        }
        Review newReview = new Review(userId, userName, ratingValue, comment);
        Log.d(TAG, "Submitting review: User=" + userName + ", Rating=" + ratingValue + ", Comment=" + comment);
        db.collection("products").document(productId).collection("reviews")
                .add(newReview)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Review added successfully: " + documentReference.getId());
                    ratingBarSubmit.setRating(0);
                    if (editTextReviewComment.getText() != null) editTextReviewComment.getText().clear();
                    updateProductRatingStats(); // Bu metod loadReviews ve showLoading(false) çağıracak
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting review", e);
                    Toast.makeText(ProductDetailActivity.this, "Değerlendirme gönderilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void updateProductRatingStats() {
        Log.d(TAG, "Updating product rating stats for product ID: " + productId);
        db.collection("products").document(productId).collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "queryDocumentSnapshots is null while updating rating stats.");
                        updateProductDocument(0, 0.0f);
                        return;
                    }
                    double totalRatingSum = 0;
                    int reviewCount = queryDocumentSnapshots.size(); // Bu doğru, toplam döküman sayısı
                    Log.d(TAG, "Fetched " + reviewCount + " reviews for stat calculation.");

                    if (reviewCount == 0) {
                        Log.d(TAG, "No reviews found, resetting stats to 0.");
                        updateProductDocument(0, 0.0f);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Review r = doc.toObject(Review.class);
                        if (r != null) {
                            totalRatingSum += r.getRating();
                        }
                    }
                    float averageRating = (float) (totalRatingSum / reviewCount);
                    averageRating = Math.round(averageRating * 10.0f) / 10.0f;
                    Log.d(TAG, "Calculated stats: TotalReviews=" + reviewCount + ", AverageRating=" + averageRating);
                    updateProductDocument(reviewCount, averageRating);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching reviews for stat update", e);
                    showLoading(false); // Hata durumunda loading'i kapat
                });
    }

    private void updateProductDocument(int totalReviews, float averageRating) {
        Log.d(TAG, "Updating product document: TotalReviews=" + totalReviews + ", AverageRating=" + averageRating);
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalReviews", totalReviews);
        updates.put("averageRating", averageRating);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("products").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Product rating stats updated successfully in Firestore.");
                    if (currentProduct != null) { // currentProduct'ı da güncelle
                        currentProduct.setTotalReviews(totalReviews);
                        currentProduct.setAverageRating(averageRating);
                        // updateUI(currentProduct); // UI'ı burada güncellemek yerine loadReviews tetikleyecek
                    }
                    loadReviews(); // En son yorumları ve güncel ürün bilgisini (UI'da) yükle
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating product document with new stats", e);
                    showLoading(false);
                });
    }

    // Diğer yardımcı metodlar (updateTextViewVisibility, updateSectionVisibility, updateChipGroup, toggleFavorite, checkFavoriteStatus, showLoading, showError) aynı kalacak
    // ... (Bu metodları bir önceki yanıttan kopyalayabilirsiniz)
    private void updateTextViewVisibility(TextView textView, String text, String prefix) {
        if (textView == null) return;
        if (!TextUtils.isEmpty(text)) {
            textView.setText(prefix + text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }
    private void updateSectionVisibility(TextView title, TextView content, String text, View divider) {
        boolean hasContent = !TextUtils.isEmpty(text);
        if (title != null) title.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        if (content != null) {
            content.setText(hasContent ? text : "");
            content.setVisibility(hasContent ? View.VISIBLE : View.GONE);
        }
        if (divider != null) divider.setVisibility(hasContent ? View.VISIBLE : View.GONE);
    }
    private void updateChipGroup(ChipGroup chipGroup, List<String> items, View parentLayout) {
        if (chipGroup == null) return;
        chipGroup.removeAllViews();
        boolean hasItems = items != null && !items.isEmpty();

        if (parentLayout != null) parentLayout.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        chipGroup.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        if (hasItems) {
            for (String item : items) {
                if (item == null || item.trim().isEmpty()) continue;
                Chip chip = new Chip(this);
                chip.setText(item.trim());
                chip.setChipBackgroundColorResource(R.color.grey_light);
                chip.setTextColor(ContextCompat.getColor(this, R.color.grey_dark));
                chipGroup.addView(chip);
            }
        }
    }
    private void toggleFavorite(Product product) {
        if (product == null) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show(); return; }
        String userId = user.getUid();
        String prodId = product.getId();
        if (TextUtils.isEmpty(prodId)) { Toast.makeText(this, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show(); return; }

        db.collection("users").document(userId).collection("favorites").document(prodId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> favoriteData = new HashMap<>();
                    if (documentSnapshot.exists()) {
                        db.collection("users").document(userId).collection("favorites").document(prodId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                    Toast.makeText(this, (product.getName() != null ? product.getName() : "Ürün") + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        favoriteData.put("productId", prodId);
                        if (product.getName() != null) favoriteData.put("name", product.getName());
                        favoriteData.put("price", product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice());
                        if (product.getImageBase64() != null) favoriteData.put("imageBase64", product.getImageBase64());
                        favoriteData.put("addedAt", FieldValue.serverTimestamp());
                        db.collection("users").document(userId).collection("favorites").document(prodId).set(favoriteData)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite);
                                    Toast.makeText(this, (product.getName() != null ? product.getName() : "Ürün") + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                });
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Error toggling favorite: " + e.getMessage()));
    }
    private void checkFavoriteStatus(Product product) {
        if (product == null) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(product.getId())) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border); return;
        }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId()).get()
                .addOnSuccessListener(documentSnapshot -> favoriteButton.setImageResource(documentSnapshot.exists() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking favorite status: " + e.getMessage());
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                });
    }
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "Loading state: " + isLoading);
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error displayed: " + message);
    }
}