package com.example.carcare;

import android.app.Activity;
import android.content.Intent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.activities.CartActivity;
import com.example.carcare.activities.CheckoutActivity;
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

    // Toolbar Elemanları
    private LinearLayout layoutSearchBarDetail;
    private ImageButton btnToolbarCart; // ID XML'dekiyle eşleşmeli: btn_cart_toolbar
    private TextView cartBadgeToolbar;
    private ImageButton btnToolbarShare; // ID XML'dekiyle eşleşmeli: btn_share_detail
    private ImageButton btnToolbarBack; // ID XML'dekiyle eşleşmeli: btn_back

    // Sayfa İçeriği Elemanları
    private ImageView productImage;
    private TextView productName, productBrand, productModelCode, productSeller, productStockStatus;
    private TextView productPriceContent; // Sayfa içindeki ana fiyat gösterimi
    private RatingBar productRatingMain;
    private TextView productReviewCountMain;
    private TextView productDescription;
    private LinearLayout productSpecificationsLayout;
    private ChipGroup chipGroupTags;
    private TextView productWarrantyInfo, productShippingInfo, productReturnPolicy;
    private TextView titleDescription, titleSpecifications, titleShippingInfo;
    private View dividerAfterDescription, dividerAfterSpecs, dividerBeforeReviews;

    // BottomAppBar Elemanları
    private TextView bottomBarProductPrice;
    private TextView bottomBarFreeShippingText;
    private Button addToCartButton, buyNowButton;

    // Diğer Elemanlar
    private ImageButton favoriteButton; // Sayfa içi favori
    private ProgressBar progressBar;
    private Toolbar toolbar;

    // Yorum Bölümü
    private RatingBar ratingBarSubmit;
    private TextInputEditText editTextReviewComment;
    private Button btnSubmitReview;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private TextView textReviewsTitle, textNoReviews;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String productId;
    private Product currentProduct;
    private ActivityResultLauncher<Intent> searchActivityLauncherDetail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(this, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean focusReview = getIntent().getBooleanExtra("FOCUS_REVIEW", false);
        boolean fromOrder = getIntent().getBooleanExtra("FROM_ORDER", false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // SearchActivity için ActivityResultLauncher'ı initialize et
        searchActivityLauncherDetail = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String searchQuery = result.getData().getStringExtra("SEARCH_QUERY");
                        if (searchQuery != null && !searchQuery.isEmpty()) {
                            // ProductDetail'den arama yapıldığında direkt StoreActivity'ye git
                            // ve arama sorgusunu StoreActivity'ye ilet.
                            Intent storeIntent = new Intent(ProductDetailActivity.this, StoreActivity.class);
                            storeIntent.putExtra("SEARCH_QUERY", searchQuery);
                            // StoreActivity zaten açıksa yenisini başlatmak yerine onu öne getir ve üzerindekileri temizle
                            storeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(storeIntent);
                            // ProductDetailActivity'yi kapatmak isteğe bağlı, kullanıcı geri gelmek isteyebilir
                            // finish();
                        }
                    }
                }
        );

        initViews();
        setupToolbarActions(); // layoutSearchBarDetail listener'ı burada ayarlanıyor
        setupListeners();
        loadProductDetails(); // Bu metod içinde loadReviews çağrılacak ve showLoading yönetilecek

        if (focusReview && fromOrder) {
            findViewById(android.R.id.content).post(this::scrollToReviewSection);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadgeToolbar();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true); // Başlığı göstereceğiz
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Kendi geri butonumuzu kullanıyoruz
            getSupportActionBar().setTitle(""); // Başlangıçta boş, updateUI'da set edilecek
        }

        // Toolbar elemanları
        btnToolbarBack = findViewById(R.id.btn_back);
        layoutSearchBarDetail = findViewById(R.id.layout_search_bar_detail);
        btnToolbarCart = findViewById(R.id.btn_cart_toolbar);
        cartBadgeToolbar = findViewById(R.id.cart_badge_toolbar);
        btnToolbarShare = findViewById(R.id.btn_share_detail);

        // Sayfa içeriği
        productImage = findViewById(R.id.product_detail_image);
        favoriteButton = findViewById(R.id.btn_favorite_detail);
        productBrand = findViewById(R.id.product_detail_brand);
        productName = findViewById(R.id.product_detail_name);
        productModelCode = findViewById(R.id.product_detail_model_code);
        productRatingMain = findViewById(R.id.product_detail_rating_main);
        productReviewCountMain = findViewById(R.id.product_detail_review_count_main);
        productSeller = findViewById(R.id.product_detail_seller);
        productPriceContent = findViewById(R.id.product_detail_price_content);
        productStockStatus = findViewById(R.id.product_detail_stock_status);
        productDescription = findViewById(R.id.product_detail_description);
        productSpecificationsLayout = findViewById(R.id.product_specifications_layout);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        productWarrantyInfo = findViewById(R.id.product_detail_warranty_info);
        productShippingInfo = findViewById(R.id.product_detail_shipping_info);
        productReturnPolicy = findViewById(R.id.product_detail_return_policy);

        titleDescription = findViewById(R.id.title_description);
        titleSpecifications = findViewById(R.id.title_specifications);
        titleShippingInfo = findViewById(R.id.title_shipping_info);
        dividerAfterDescription = findViewById(R.id.divider_after_description);
        dividerAfterSpecs = findViewById(R.id.divider_after_specs);
        dividerBeforeReviews = findViewById(R.id.divider_before_reviews);

        bottomBarProductPrice = findViewById(R.id.bottom_bar_product_price);
        bottomBarFreeShippingText = findViewById(R.id.bottom_bar_free_shipping_text);
        addToCartButton = findViewById(R.id.btn_add_to_cart_detail);
        buyNowButton = findViewById(R.id.btn_buy_now_detail);

        ratingBarSubmit = findViewById(R.id.rating_bar_submit);
        editTextReviewComment = findViewById(R.id.edit_text_review_comment);
        btnSubmitReview = findViewById(R.id.btn_submit_review);
        recyclerViewReviews = findViewById(R.id.recycler_view_reviews);
        textReviewsTitle = findViewById(R.id.text_reviews_title);
        textNoReviews = findViewById(R.id.text_no_reviews);

        progressBar = findViewById(R.id.progress_bar_detail);

        reviewList = new ArrayList<>();
        if (recyclerViewReviews != null) {
            reviewAdapter = new ReviewAdapter(this, reviewList);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerViewReviews.setLayoutManager(layoutManager);
            recyclerViewReviews.setAdapter(reviewAdapter);
            recyclerViewReviews.setNestedScrollingEnabled(false);
        }
        Log.d(TAG, "Views initialized.");
    }

    private void setupToolbarActions() {
        if (btnToolbarBack != null) {
            btnToolbarBack.setOnClickListener(v -> finish());
        }

        if (layoutSearchBarDetail != null) {
            layoutSearchBarDetail.setOnClickListener(v -> {
                // SearchActivity'yi ActivityResultLauncher ile başlat
                Intent intent = new Intent(ProductDetailActivity.this, SearchActivity.class);
                searchActivityLauncherDetail.launch(intent);
            });
        }

        if (btnToolbarCart != null) {
            btnToolbarCart.setOnClickListener(v -> {
                Intent cartIntent = new Intent(this, CartActivity.class);
                startActivity(cartIntent);
            });
        }

        if (btnToolbarShare != null) {
            btnToolbarShare.setOnClickListener(v -> {
                if (currentProduct != null && currentProduct.getName() != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    String productLink = "https://www.example.com/product/" + currentProduct.getId(); // Gerçek link yapınızla değiştirin
                    String shareBody = "Şu ürüne göz at: " + currentProduct.getName() + "\n" + productLink;
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentProduct.getName());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(shareIntent, "Şununla paylaş:"));
                } else {
                    Toast.makeText(this, "Paylaşılacak ürün bilgisi yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupListeners() {
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> {
                if (currentProduct != null) toggleFavorite(currentProduct);
            });
        }

        if (addToCartButton != null) {
            addToCartButton.setOnClickListener(v -> {
                if (currentProduct != null) {
                    if (currentProduct.getStock() > 0) {
                        Cart.getInstance().addItem(currentProduct, this);
                        Toast.makeText(this, currentProduct.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
                        updateCartBadgeToolbar();
                    } else {
                        Toast.makeText(this, "Ürün stokta yok", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Ürün bilgisi yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (buyNowButton != null) {
            buyNowButton.setOnClickListener(v -> {
                if (currentProduct != null) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Satın almak için lütfen giriş yapın", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (currentProduct.getStock() > 0) {
                        Cart.getInstance().clearCartWithoutToast(this);
                        Cart.getInstance().addItem(currentProduct, this);
                        updateCartBadgeToolbar();
                        Intent intent = new Intent(ProductDetailActivity.this, CheckoutActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Ürün stokta yok", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Ürün bilgisi yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> submitReview());
        }
        Log.d(TAG, "Listeners setup.");
    }

    private void loadProductDetails() {
        Log.d(TAG, "Loading product details for ID: " + productId);
        showLoading(true);
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(product.getName() != null ? product.getName() : "Ürün Detayı");
        }

        updateTextViewVisibility(productBrand, product.getBrand(), "");
        if (productName != null) productName.setText(product.getName() != null ? product.getName() : "N/A");
        updateTextViewVisibility(productModelCode, product.getModelCode(), ""); // "Model: " prefixi XML'de varsa kalabilir.

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String formattedPrice = currencyFormat.format(product.getPrice()); // Sadece ana fiyatı alıyoruz

        if (productPriceContent != null) {
            productPriceContent.setText(formattedPrice);
        }
        if (bottomBarProductPrice != null) {
            bottomBarProductPrice.setText(formattedPrice);
        }

        if (bottomBarFreeShippingText != null) {
            // Kargo bedava koşulunu buraya ekleyin (örn: product.isFreeShipping() veya product.getPrice() > X)
            boolean isFreeShippingActually = product.getPrice() > 0; // Örnek koşul, fiyat varsa kargo bedava
            isFreeShippingActually = false; // Ya da her zaman gizli tutmak için
            bottomBarFreeShippingText.setVisibility(isFreeShippingActually ? View.VISIBLE : View.GONE);
            if(isFreeShippingActually){
                bottomBarFreeShippingText.setText("Kargo Bedava");
            }
        }

        if (productRatingMain != null) productRatingMain.setRating(product.getAverageRating());
        if (productReviewCountMain != null) productReviewCountMain.setText(String.format(Locale.getDefault(), "(%d)", product.getTotalReviews()));
        updateTextViewVisibility(productSeller, product.getSellerName(), "Seller: ");

        if (product.getStock() > 0) {
            if (productStockStatus != null) {
                productStockStatus.setText(String.format(Locale.getDefault(), "In Stock: %d Piece", product.getStock()));
                productStockStatus.setTextColor(ContextCompat.getColor(this, R.color.green_dark));
            }
            if (addToCartButton != null) addToCartButton.setEnabled(true);
            if (buyNowButton != null) buyNowButton.setEnabled(true);
        } else {
            if (productStockStatus != null) {
                productStockStatus.setText("Tükendi");
                productStockStatus.setTextColor(ContextCompat.getColor(this, R.color.red_dark));
            }
            if (addToCartButton != null) addToCartButton.setEnabled(false);
            if (buyNowButton != null) buyNowButton.setEnabled(false);
        }

        updateSectionVisibility(titleDescription, productDescription, product.getDescription(), dividerAfterDescription);
        if (product.getSpecifications() != null && !product.getSpecifications().isEmpty()) {
            if (titleSpecifications != null) titleSpecifications.setVisibility(View.VISIBLE);
            if (productSpecificationsLayout != null) {
                productSpecificationsLayout.setVisibility(View.VISIBLE);
                productSpecificationsLayout.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(this);
                for (Map.Entry<String, String> entry : product.getSpecifications().entrySet()) {
                    View specView = inflater.inflate(R.layout.item_specification, productSpecificationsLayout, false);
                    ((TextView) specView.findViewById(R.id.spec_key)).setText(entry.getKey());
                    ((TextView) specView.findViewById(R.id.spec_value)).setText(entry.getValue());
                    productSpecificationsLayout.addView(specView);
                }
            }
            if (dividerAfterSpecs != null) dividerAfterSpecs.setVisibility(View.VISIBLE);
        } else {
            if (titleSpecifications != null) titleSpecifications.setVisibility(View.GONE);
            if (productSpecificationsLayout != null) productSpecificationsLayout.setVisibility(View.GONE);
            if (dividerAfterSpecs != null) dividerAfterSpecs.setVisibility(View.GONE);
        }

        boolean hasShippingInfoSection = !TextUtils.isEmpty(product.getShippingInfo()) || !TextUtils.isEmpty(product.getWarrantyInfo()) || !TextUtils.isEmpty(product.getReturnPolicy());
        if (titleShippingInfo != null) titleShippingInfo.setVisibility(hasShippingInfoSection ? View.VISIBLE : View.GONE);
        updateTextViewVisibility(productShippingInfo, product.getShippingInfo(), "");
        updateTextViewVisibility(productWarrantyInfo, product.getWarrantyInfo(), "");
        updateTextViewVisibility(productReturnPolicy, product.getReturnPolicy(), "");
        if (chipGroupTags != null) updateChipGroup(chipGroupTags, product.getTags(), null);

        if (!TextUtils.isEmpty(product.getImageBase64())) {
            try {
                byte[] decodedString = Base64.decode(product.getImageBase64(), Base64.DEFAULT);
                if (productImage != null) Glide.with(this).load(decodedString).placeholder(R.drawable.placeholder_image).error(R.drawable.error_image).into(productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode error", e);
                if (productImage != null) productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            if (productImage != null) productImage.setImageResource(R.drawable.placeholder_image);
        }
        Log.d(TAG, "UI updated for product: " + (product.getName() != null ? product.getName() : "N/A"));
    }

    private void loadReviews() {
        if (TextUtils.isEmpty(productId)) {
            Log.e(TAG, "loadReviews: Product ID is empty.");
            showLoading(false);
            return;
        }
        Log.d(TAG, "Loading reviews for product ID: " + productId);

        db.collection("products").document(productId).collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> loadedReviews = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            try {
                                Review review = doc.toObject(Review.class);
                                if (review != null) {
                                    review.setId(doc.getId());
                                    loadedReviews.add(review);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing review document: " + doc.getId(), e);
                            }
                        }
                    }
                    Log.d(TAG, "Total reviews parsed: " + loadedReviews.size());

                    if (this.reviewList == null) this.reviewList = new ArrayList<>();
                    this.reviewList.clear();
                    this.reviewList.addAll(loadedReviews);

                    if (reviewAdapter != null) {
                        reviewAdapter.updateReviews(this.reviewList); // this.reviewList'i kullanmak daha doğru
                    }
                    updateReviewsUIVisibility();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    if (reviewAdapter != null) reviewAdapter.updateReviews(new ArrayList<>());
                    updateReviewsUIVisibility();
                    showLoading(false);
                });
    }

    private void submitReview() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Değerlendirme yapmak için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ratingBarSubmit == null || editTextReviewComment == null) {
            Toast.makeText(this, "Bir hata oluştu.", Toast.LENGTH_SHORT).show(); return;
        }
        float ratingValue = ratingBarSubmit.getRating();
        if (ratingValue == 0) {
            Toast.makeText(this, "Lütfen ürüne puan verin.", Toast.LENGTH_SHORT).show(); return;
        }
        String comment = editTextReviewComment.getText() != null ? editTextReviewComment.getText().toString().trim() : "";
        showLoading(true);
        String userId = firebaseUser.getUid();
        String userName = firebaseUser.getDisplayName();
        if (TextUtils.isEmpty(userName)) {
            String email = firebaseUser.getEmail();
            userName = email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Anonim Kullanıcı";
        }
        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("userId", userId);
        reviewData.put("userName", userName);
        reviewData.put("rating", ratingValue);
        reviewData.put("comment", comment);
        reviewData.put("timestamp", FieldValue.serverTimestamp());
        db.collection("products").document(productId).collection("reviews").add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    if (ratingBarSubmit!=null) ratingBarSubmit.setRating(0);
                    if (editTextReviewComment.getText() != null) editTextReviewComment.getText().clear();
                    Toast.makeText(this, "Değerlendirmeniz gönderildi!", Toast.LENGTH_SHORT).show();
                    updateProductRatingStats();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Değerlendirme gönderilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }


    private void updateProductRatingStats() {
        db.collection("products").document(productId).collection("reviews").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null) {
                        updateProductDocument(0, 0.0f); return;
                    }
                    double totalRatingSum = 0;
                    int reviewCount = queryDocumentSnapshots.size();
                    if (reviewCount == 0) {
                        updateProductDocument(0, 0.0f); return;
                    }
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Object ratingObj = doc.get("rating");
                        if (ratingObj instanceof Number) {
                            totalRatingSum += ((Number) ratingObj).doubleValue();
                        }
                    }
                    float averageRating = (reviewCount > 0) ? (float) (totalRatingSum / reviewCount) : 0.0f;
                    averageRating = Math.round(averageRating * 10.0f) / 10.0f;
                    updateProductDocument(reviewCount, averageRating);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching reviews for stat update", e);
                    showLoading(false);
                });
    }

    private void updateProductDocument(int totalReviews, float averageRating) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalReviews", totalReviews);
        updates.put("averageRating", averageRating);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("products").document(productId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (currentProduct != null) {
                        currentProduct.setTotalReviews(totalReviews);
                        currentProduct.setAverageRating(averageRating);
                        if (productRatingMain != null) productRatingMain.setRating(currentProduct.getAverageRating());
                        if (productReviewCountMain != null) productReviewCountMain.setText(String.format(Locale.getDefault(), "(%d)", currentProduct.getTotalReviews()));
                    }
                    loadReviews(); // Bu da sonunda showLoading(false) çağıracak
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating product document stats", e);
                    showLoading(false);
                });
    }


    private void updateReviewsHeader() {
        if (currentProduct != null && textReviewsTitle != null) {
            String headerText = (currentProduct.getTotalReviews() > 0) ?
                    String.format(Locale.getDefault(), "Ürün Değerlendirmeleri ⭐ %.1f • %d Değerlendirme", currentProduct.getAverageRating(), currentProduct.getTotalReviews()) :
                    "Product Reviews";
            textReviewsTitle.setText(headerText);
        }
    }

    private void updateReviewsUIVisibility() {
        int adapterItemCount = (reviewAdapter != null) ? reviewAdapter.getItemCount() : 0;
        boolean hasReviews = adapterItemCount > 0;
        if (textReviewsTitle != null) {
            textReviewsTitle.setVisibility(View.VISIBLE);
            updateReviewsHeader();
        }
        if (textNoReviews != null) textNoReviews.setVisibility(hasReviews ? View.GONE : View.VISIBLE);
        if (recyclerViewReviews != null) recyclerViewReviews.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
        if (dividerBeforeReviews != null) dividerBeforeReviews.setVisibility(View.VISIBLE);
    }

    private void updateCartBadgeToolbar() {
        if (cartBadgeToolbar == null) return;
        int itemCount = Cart.getInstance().getItems().size();
        cartBadgeToolbar.setVisibility(itemCount > 0 ? View.VISIBLE : View.GONE);
        if (itemCount > 0) cartBadgeToolbar.setText(String.valueOf(itemCount));
    }

    private void scrollToReviewSection() {
        NestedScrollView scrollView = findViewById(R.id.nested_scroll_view);
        View reviewSectionAnchor = findViewById(R.id.rating_bar_submit);
        if (scrollView != null && reviewSectionAnchor != null) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, reviewSectionAnchor.getTop() - (scrollView.getHeight() / 4))));
        }
    }

    private void updateTextViewVisibility(TextView textView, String text, String prefix) {
        if (textView == null) return;
        textView.setVisibility(!TextUtils.isEmpty(text) ? View.VISIBLE : View.GONE);
        if (!TextUtils.isEmpty(text)) textView.setText(prefix + text);
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
                if (TextUtils.isEmpty(item)) continue;
                Chip chip = new Chip(this);
                chip.setText(item.trim());
                chip.setChipBackgroundColorResource(R.color.grey_light);
                chip.setTextColor(ContextCompat.getColor(this, R.color.grey_dark));
                chipGroup.addView(chip);
            }
        }
    }

    private void toggleFavorite(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { Toast.makeText(this, "Lütfen giriş yapın.", Toast.LENGTH_SHORT).show(); return; }
        String userId = user.getUid(), prodId = product.getId();
        db.collection("users").document(userId).collection("favorites").document(prodId).get()
                .addOnSuccessListener(docSnap -> {
                    if (docSnap.exists()) {
                        db.collection("users").document(userId).collection("favorites").document(prodId).delete()
                                .addOnSuccessListener(aVoid -> { if(favoriteButton!=null) favoriteButton.setImageResource(R.drawable.ic_favorite_border); Toast.makeText(this, "Favorilerden çıkarıldı.", Toast.LENGTH_SHORT).show(); });
                    } else {
                        Map<String,Object> favData = new HashMap<>();
                        favData.put("productId", prodId); favData.put("name", product.getName()); favData.put("price", product.getPrice());
                        if(product.getImageBase64()!=null) favData.put("imageBase64", product.getImageBase64());
                        favData.put("addedAt", FieldValue.serverTimestamp());
                        db.collection("users").document(userId).collection("favorites").document(prodId).set(favData)
                                .addOnSuccessListener(aVoid -> { if(favoriteButton!=null) favoriteButton.setImageResource(R.drawable.ic_favorite); Toast.makeText(this, "Favorilere eklendi.", Toast.LENGTH_SHORT).show(); });
                    }
                });
    }

    private void checkFavoriteStatus(Product product) {
        if (product == null || favoriteButton == null || TextUtils.isEmpty(product.getId())) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { favoriteButton.setImageResource(R.drawable.ic_favorite_border); return; }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId()).get()
                .addOnSuccessListener(docSnap -> favoriteButton.setImageResource(docSnap.exists() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border))
                .addOnFailureListener(e -> favoriteButton.setImageResource(R.drawable.ic_favorite_border));
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}