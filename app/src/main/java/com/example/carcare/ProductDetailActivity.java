package com.example.carcare;

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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    private ImageView productImage;
    private TextView productName, productPrice, productDescription, productBrand,
            productModelCode, productSeller,
            productStockStatus, toolbarTitle, productDiscountPrice,
            productWarrantyInfo, productShippingInfo, productReturnPolicy;

    // Ürünün ana rating ve yorum sayısı gösterimi
    private RatingBar productRatingMain;
    private TextView productReviewCountMain;

    private Button addToCartButton, buyNowButton;
    private ImageButton backButton, favoriteButton;
    private ProgressBar progressBar;
    private LinearLayout productSpecificationsLayout;
    private ChipGroup  chipGroupTags;
    private Toolbar toolbar;

    private TextView titleDescription, titleSpecifications, titleShippingInfo;
    private View dividerAfterDescription, dividerAfterSpecs, dividerBeforeReviews;

    // Yorum gönderme ve listeleme UI elemanları
    private RatingBar ratingBarSubmit;
    private TextInputEditText editTextReviewComment;
    private Button btnSubmitReview;
    private RecyclerView recyclerViewReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList; // Bu aktivitenin ana review listesi
    private TextView textReviewsTitle, textNoReviews; // Yorumlar başlığı ve "yorum yok" mesajı

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

        // Ürünün ana rating ve yorum sayısı
        productRatingMain = findViewById(R.id.product_detail_rating_main);
        productReviewCountMain = findViewById(R.id.product_detail_review_count_main);

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
        dividerBeforeReviews = findViewById(R.id.divider_before_reviews); // Yorumlardan önceki ayırıcı

        addToCartButton = findViewById(R.id.btn_add_to_cart_detail);
        buyNowButton = findViewById(R.id.btn_buy_now_detail);
        buyNowButton = findViewById(R.id.btn_buy_now_detail);
        favoriteButton = findViewById(R.id.btn_favorite_detail);
        progressBar = findViewById(R.id.progress_bar_detail);

        // Yorum UI elemanları
        ratingBarSubmit = findViewById(R.id.rating_bar_submit);
        editTextReviewComment = findViewById(R.id.edit_text_review_comment);
        btnSubmitReview = findViewById(R.id.btn_submit_review);
        recyclerViewReviews = findViewById(R.id.recycler_view_reviews);
        textReviewsTitle = findViewById(R.id.text_reviews_title);
        textNoReviews = findViewById(R.id.text_no_reviews);

        reviewList = new ArrayList<>();
        Log.d(TAG, "reviewList initialized with size: " + reviewList.size());

        if (recyclerViewReviews != null) {
            reviewAdapter = new ReviewAdapter(this, reviewList);

            // *** YENİ: Yatay LinearLayoutManager Ayarlama ***
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerViewReviews.setLayoutManager(layoutManager);

            recyclerViewReviews.setAdapter(reviewAdapter);
            recyclerViewReviews.setNestedScrollingEnabled(false); // ScrollView içinde düzgün çalışması için

            // *** İSTEĞE BAĞLI: SnapHelper ekleme (her seferinde bir öğeyi ortalar) ***
            // SnapHelper snapHelper = new PagerSnapHelper();
            // snapHelper.attachToRecyclerView(recyclerViewReviews);

            Log.d(TAG, "RecyclerView and adapter initialized successfully with HORIZONTAL layout.");
            Log.d(TAG, "Initial adapter item count: " + reviewAdapter.getItemCount());
        } else {
            Log.e(TAG, "recyclerViewReviews is NULL! Check R.id.recycler_view_reviews in layout");
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerViewReviews.setLayoutManager(layoutManager);

        // Null check'ler
        if (textReviewsTitle == null) Log.e(TAG, "textReviewsTitle is NULL!");
        if (textNoReviews == null) Log.e(TAG, "textNoReviews is NULL!");
        if (ratingBarSubmit == null) Log.e(TAG, "ratingBarSubmit is NULL!");
        if (btnSubmitReview == null) Log.e(TAG, "btnSubmitReview is NULL!");

        Log.d(TAG, "Views initialized.");
    }

    private void setupListeners() {
        // Back Button tanımlaması - bu satırı ekleyin
        backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Sepete Ekle Butonu
        if (addToCartButton != null) {
            addToCartButton.setOnClickListener(v -> {
                if (currentProduct != null) {
                    Cart.getInstance().addItem(currentProduct, this);
                    Toast.makeText(this, currentProduct.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
                    // İsteğe bağlı: Sepet ikonundaki sayacı güncelle
                } else {
                    Toast.makeText(this, "Ürün bilgisi yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Şimdi Al Butonu - Bu kısmı güncelleyin
        if (buyNowButton != null) {
            buyNowButton.setOnClickListener(v -> {
                if (currentProduct != null) {
                    // Kullanıcı giriş kontrolü
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Satın almak için lütfen giriş yapın", Toast.LENGTH_LONG).show();
                        // İsteğe bağlı: Login sayfasına yönlendir
                        // Intent loginIntent = new Intent(this, LoginActivity.class);
                        // startActivity(loginIntent);
                        return;
                    }

                    // Stok kontrolü
                    if (currentProduct.getStock() <= 0) {
                        Toast.makeText(this, "Bu ürün şu anda stokta yok", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Sepeti temizle ve sadece bu ürünü ekle
                    Cart.getInstance().clearCartWithoutToast(this);
                    Cart.getInstance().addItem(currentProduct, this);

                    // CheckoutActivity'e git
                    Intent intent = new Intent(ProductDetailActivity.this, CheckoutActivity.class);
                    intent.putExtra("DIRECT_BUY", true); // Direkt satın alma olduğunu belirt
                    intent.putExtra("PRODUCT_ID", currentProduct.getId()); // Ürün ID'sini geç
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Ürün bilgisi yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        favoriteButton.setOnClickListener(v -> {
            if (currentProduct != null) toggleFavorite(currentProduct);
        });

        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> submitReview());
        }
        Log.d(TAG, "Listeners setup.");
    }

    private void loadProductDetails() {
        Log.d(TAG, "Loading product details for ID: " + productId);
        showLoading(true); // Yüklemeyi başlat
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
                            loadReviews(); // Yorumları yükle
                        } else {
                            showError("Ürün bilgileri dönüştürülemedi.");
                            showLoading(false); // Hata durumunda gizle
                        }
                    } else {
                        showError("Ürün bulunamadı.");
                        showLoading(false); // Hata durumunda gizle
                    }

                    if (currentProduct != null) { // Sadece ürün başarıyla yüklendiyse spinner'ı kaldır.

                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false); // Her türlü hatada gizle
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
            productPrice.setPaintFlags(productPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); // Üstü çizili bayrağını kaldır
            productPrice.setTextColor(ContextCompat.getColor(this, R.color.orange_primary)); // Varsayılan fiyat rengi
            productDiscountPrice.setVisibility(View.GONE);
        }

        // Ürünün ana rating ve yorum sayısını güncelle
        if (productRatingMain != null) { productRatingMain.setRating(product.getAverageRating()); }
        if (productReviewCountMain != null) { productReviewCountMain.setText(String.format(Locale.getDefault(), "(%d)", product.getTotalReviews()));}


        updateTextViewVisibility(productSeller, product.getSellerName(), "Satıcı: ");

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
                    Log.d(TAG, "Firestore review query success. Document count: " +
                            (queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : "null"));

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            try {
                                Review review = doc.toObject(Review.class);
                                if (review != null) {
                                    review.setId(doc.getId());
                                    loadedReviews.add(review);
                                } else {
                                    Log.w(TAG, "Parsed review object is null for document: " + doc.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing review document: " + doc.getId(), e);
                            }
                        }
                    }
                    Log.d(TAG, "Total reviews parsed into loadedReviews: " + loadedReviews.size());

                    if (this.reviewList == null) this.reviewList = new ArrayList<>();
                    this.reviewList.clear();
                    this.reviewList.addAll(loadedReviews);
                    Log.d(TAG, "Activity's this.reviewList updated with size: " + this.reviewList.size());

                    if (reviewAdapter != null) {
                        Log.d(TAG, "Calling reviewAdapter.updateReviews with loadedReviews list of size " + loadedReviews.size());
                        reviewAdapter.updateReviews(loadedReviews);
                        Log.d(TAG, "After adapter update - getItemCount: " + reviewAdapter.getItemCount());
                    } else {
                        Log.e(TAG, "ReviewAdapter is null! Cannot update reviews.");
                    }

                    updateReviewsUIVisibility();
                    showLoading(false); // Yorumlar BAŞARIYLA yüklendikten sonra ProgressBar'ı kapat
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    Toast.makeText(this, "Değerlendirmeler yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    if (reviewAdapter != null) {
                        reviewAdapter.updateReviews(new ArrayList<>()); // Hata durumunda boş liste ile güncelle
                    }
                    updateReviewsUIVisibility();
                    showLoading(false); // Yorum yükleme HATASINDA ProgressBar'ı kapat
                });
    }


    private void updateReviewsHeader() {
        // Bu metod, yorumlar bölümünün başlığını (textReviewsTitle) günceller.
        // Örneğin: "Ürün Değerlendirmeleri ⭐ 4.5 • 10 Değerlendirme"
        if (currentProduct != null && textReviewsTitle != null) {
            String headerText;
            if (currentProduct.getTotalReviews() > 0) {
                headerText = String.format(Locale.getDefault(),
                        "Ürün Değerlendirmeleri ⭐ %.1f • %d Değerlendirme",
                        currentProduct.getAverageRating(),
                        currentProduct.getTotalReviews());
            } else {
                headerText = "Ürün Değerlendirmeleri"; // Henüz yorum yoksa
            }
            textReviewsTitle.setText(headerText);
            Log.d(TAG, "Review header updated: " + headerText);
        } else {
            Log.w(TAG, "Cannot update review header, currentProduct or textReviewsTitle is null.");
        }
    }

    private void updateReviewsUIVisibility() {
        // Bu metod, yorum listesinin (RecyclerView), "yorum yok" mesajının (textNoReviews)
        // ve yorumlar başlığının (textReviewsTitle) görünürlüğünü ayarlar.
        int adapterItemCount = (reviewAdapter != null) ? reviewAdapter.getItemCount() : 0;
        Log.d(TAG, "updateReviewsUIVisibility - Adapter item count: " + adapterItemCount);

        boolean hasReviews = adapterItemCount > 0;

        if (textReviewsTitle != null) {
            // Başlık her zaman görünebilir veya sadece yorum varsa görünebilir.
            // Tasarıma göre yorum yoksa başlığı da gizleyebilirsiniz.
            // Şimdilik, yorum yoksa bile "Ürün Değerlendirmeleri" başlığı kalsın diyeceğiz
            // ama içeriği updateReviewsHeader ile güncellenecek.
            textReviewsTitle.setVisibility(View.VISIBLE); // Başlık hep görünsün
            updateReviewsHeader(); // Başlığın içeriğini güncelle (yorum sayısı vb.)
        }

        if (textNoReviews != null) {
            textNoReviews.setVisibility(hasReviews ? View.GONE : View.VISIBLE);
            Log.d(TAG, "textNoReviews visibility: " + (hasReviews ? "GONE" : "VISIBLE"));
        }

        if (recyclerViewReviews != null) {
            recyclerViewReviews.setVisibility(hasReviews ? View.VISIBLE : View.GONE);
            Log.d(TAG, "recyclerViewReviews visibility: " + (hasReviews ? "VISIBLE" : "GONE"));
        }

        // Yorumlardan önceki ayırıcı çizgi
        if(dividerBeforeReviews != null) {
            // Yorum alanı her zaman görüneceği için (yorum olsa da olmasa da "yorum yok" mesajı vs.)
            // ayırıcı çizgi de hep görünebilir.
            dividerBeforeReviews.setVisibility(View.VISIBLE);
        }
    }


    private void submitReview() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Değerlendirme yapmak için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            // İsteğe bağlı: Giriş ekranına yönlendirme
            return;
        }

        if (ratingBarSubmit == null || editTextReviewComment == null) {
            Log.e(TAG, "Review submission UI elements are null.");
            Toast.makeText(this, "Bir hata oluştu, lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        float ratingValue = ratingBarSubmit.getRating();
        if (ratingValue == 0) { // Hiç puan verilmemişse
            Toast.makeText(this, "Lütfen ürüne puan verin (1-5 yıldız).", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = editTextReviewComment.getText() != null ?
                editTextReviewComment.getText().toString().trim() : "";
        // Yorum zorunlu değilse bu kontrolü kaldırabilirsiniz.
        // if (TextUtils.isEmpty(comment)) {
        //     Toast.makeText(this, "Lütfen yorumunuzu yazın.", Toast.LENGTH_SHORT).show();
        //     return;
        // }

        showLoading(true); // Yorum gönderilirken progressBar göster

        String userId = firebaseUser.getUid();
        String userName = firebaseUser.getDisplayName();
        if (TextUtils.isEmpty(userName)) { // Kullanıcı adı yoksa e-postadan türet
            String email = firebaseUser.getEmail();
            userName = email != null && email.contains("@") ?
                    email.substring(0, email.indexOf('@')) : "Anonim Kullanıcı";
        }

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("userId", userId);
        reviewData.put("userName", userName);
        reviewData.put("rating", ratingValue); // float olarak
        reviewData.put("comment", comment);
        reviewData.put("timestamp", FieldValue.serverTimestamp()); // Sunucu zaman damgası

        Log.d(TAG, "Submitting review: User=" + userName + ", Rating=" + ratingValue +
                ", Comment=" + comment + " for ProductID: " + productId);

        db.collection("products").document(productId).collection("reviews")
                .add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Review added successfully with ID: " + documentReference.getId());

                    // Formu temizle
                    ratingBarSubmit.setRating(0);
                    if (editTextReviewComment.getText() != null) {
                        editTextReviewComment.getText().clear();
                    }
                    Toast.makeText(this, "Değerlendirmeniz başarıyla gönderildi!",
                            Toast.LENGTH_SHORT).show();

                    // Ürünün genel rating istatistiklerini güncelle
                    updateProductRatingStats();
                    // showLoading(false) burada değil, updateProductRatingStats -> updateProductDocument -> loadReviews sonrası
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting review", e);
                    Toast.makeText(this, "Değerlendirme gönderilemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false); // Hata durumunda progressBar'ı kapat
                });
    }

    private void updateProductRatingStats() {
        Log.d(TAG, "Updating product rating stats for product ID: " + productId);
        db.collection("products").document(productId).collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "queryDocumentSnapshots is null while updating rating stats.");
                        updateProductDocument(0, 0.0f); // Hata veya boş durum
                        return;
                    }

                    double totalRatingSum = 0;
                    int reviewCount = queryDocumentSnapshots.size(); // Toplam yorum sayısı
                    Log.d(TAG, "Fetched " + reviewCount + " reviews for stat calculation.");

                    if (reviewCount == 0) {
                        Log.d(TAG, "No reviews found for product, resetting stats to 0.");
                        updateProductDocument(0, 0.0f);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review r = doc.toObject(Review.class); // Review modeline parse et
                        if (r != null) {
                            totalRatingSum += r.getRating();
                        } else {
                            // Firestore'dan gelen rating double olabilir, doğrudan da alınabilir
                            Object ratingObj = doc.get("rating");
                            if (ratingObj instanceof Number) {
                                totalRatingSum += ((Number) ratingObj).doubleValue();
                            }
                        }
                    }

                    float averageRating = (reviewCount > 0) ? (float) (totalRatingSum / reviewCount) : 0.0f;
                    averageRating = Math.round(averageRating * 10.0f) / 10.0f; // Tek ondalık basamağa yuvarla

                    Log.d(TAG, "Calculated stats: TotalReviews=" + reviewCount + ", AverageRating=" + averageRating);
                    updateProductDocument(reviewCount, averageRating);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching reviews for stat update", e);
                    // Hata durumunda bile UI'ı bir şekilde sonlandırmak gerekebilir.
                    showLoading(false); // Eğer submitReview'dan geliyorsa loading'i kapat
                });
    }

    private void updateProductDocument(int totalReviews, float averageRating) {
        Log.d(TAG, "Updating product document: ProductID=" + productId +
                ", TotalReviews=" + totalReviews + ", AverageRating=" + averageRating);
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalReviews", totalReviews);
        updates.put("averageRating", averageRating);
        updates.put("updatedAt", FieldValue.serverTimestamp()); // Son güncellenme zamanı

        db.collection("products").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Product rating stats updated successfully in Firestore for ProductID: " + productId);
                    if (currentProduct != null) { // Aktivitedeki currentProduct nesnesini de güncelle
                        currentProduct.setTotalReviews(totalReviews);
                        currentProduct.setAverageRating(averageRating);

                        // Ürünün ana rating ve yorum sayısını UI'da direkt güncelle
                        if (productRatingMain != null) productRatingMain.setRating(currentProduct.getAverageRating());
                        if (productReviewCountMain != null) productReviewCountMain.setText(String.format(Locale.getDefault(), "(%d)", currentProduct.getTotalReviews()));
                    }
                    // Yorum listesini ve yorum başlığını yenilemek için loadReviews çağır.
                    // Bu, yeni eklenen yorumun da listede görünmesini sağlar.
                    loadReviews();
                    showLoading(false); // Tüm işlemler bittiğinde progressBar'ı kapat
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating product document with new stats for ProductID: " + productId, e);
                    showLoading(false); // Hata durumunda progressBar'ı kapat
                });
    }


    // --- Diğer Yardımcı Metodlar (Zaten Projenizde Mevcut) ---
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
                chip.setChipBackgroundColorResource(R.color.grey_light); // Renk resource'larınızdan
                chip.setTextColor(ContextCompat.getColor(this, R.color.grey_dark)); // Renk resource'larınızdan
                chipGroup.addView(chip);
            }
        }
    }
    private void toggleFavorite(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            Log.w(TAG, "toggleFavorite: Product or product ID is null.");
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show(); return; }

        String userId = user.getUid();
        String prodId = product.getId();

        db.collection("users").document(userId).collection("favorites").document(prodId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) { // Favorilerde varsa kaldır
                        db.collection("users").document(userId).collection("favorites").document(prodId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                    Toast.makeText(this, (product.getName() != null ? product.getName() : "Ürün") + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error removing favorite", e));
                    } else { // Favorilerde yoksa ekle
                        Map<String, Object> favoriteData = new HashMap<>();
                        favoriteData.put("productId", prodId);
                        if (product.getName() != null) favoriteData.put("name", product.getName());
                        favoriteData.put("price", product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice());
                        if (product.getImageBase64() != null) favoriteData.put("imageBase64", product.getImageBase64());
                        favoriteData.put("addedAt", FieldValue.serverTimestamp());

                        db.collection("users").document(userId).collection("favorites").document(prodId).set(favoriteData)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite); // Dolu kalp
                                    Toast.makeText(this, (product.getName() != null ? product.getName() : "Ürün") + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error adding favorite", e));
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Error checking favorite before toggle: " + e.getMessage()));
    }
    private void checkFavoriteStatus(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border); // Varsayılan
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border); // Kullanıcı yoksa boş kalp
            return;
        }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        favoriteButton.setImageResource(R.drawable.ic_favorite); // Favorilerdeyse dolu kalp
                    } else {
                        favoriteButton.setImageResource(R.drawable.ic_favorite_border); // Değilse boş kalp
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking favorite status: " + e.getMessage());
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border); // Hata durumunda boş kalp
                });
    }
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "Loading state set to: " + isLoading);
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error displayed: " + message);
    }
    // updateProductRating metodu artık updateProductRatingStats ve updateProductDocument içinde ele alınıyor.
}