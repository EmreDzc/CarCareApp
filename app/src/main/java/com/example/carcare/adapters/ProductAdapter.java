package com.example.carcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.R;
import com.example.carcare.ProductDetailActivity;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private static final String TAG = "ProductAdapter";
    private Context context;
    private List<Product> productList; // Görüntülenecek liste (filtrelenmiş)
    private List<Product> originalProductList; // Orijinal tam liste
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView cartBadgeText;

    public ProductAdapter(Context context, List<Product> productList, TextView cartBadge) {
        this.context = context;
        this.productList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
        this.originalProductList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.cartBadgeText = cartBadge;

        Log.d(TAG, "ProductAdapter oluşturuldu. Ürün sayısı: " + this.productList.size());
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        if (productList == null || position < 0 || position >= productList.size()) {
            Log.e(TAG, "Invalid position or productList is null/empty. Position: " + position);
            return;
        }

        Product product = productList.get(position);
        if (product == null) {
            Log.e(TAG, "Product at position " + position + " is null.");
            bindEmptyProduct(holder);
            return;
        }

        bindProduct(holder, product);
    }

    private void bindEmptyProduct(ProductViewHolder holder) {
        holder.productName.setText("Ürün Yüklenemedi");
        holder.productPrice.setText("");
        holder.ratingBarStars.setRating(0);
        holder.textNumericAvgRating.setVisibility(View.GONE);
        holder.textReviewCount.setVisibility(View.GONE);
        holder.productBrand.setVisibility(View.GONE);
        holder.productImage.setImageResource(R.drawable.placeholder_image);
        holder.addToCartButton.setEnabled(false);
    }

    private void bindProduct(ProductViewHolder holder, Product product) {
        // Marka bilgisi
        if (!TextUtils.isEmpty(product.getBrand())) {
            holder.productBrand.setText(product.getBrand());
            holder.productBrand.setVisibility(View.VISIBLE);
        } else {
            holder.productBrand.setVisibility(View.GONE);
        }

        // Ürün adı
        holder.productName.setText(product.getName() != null ? product.getName() : "İsimsiz Ürün");

        // Fiyat bilgisi
        bindPrice(holder, product);

        // Ürün resmi
        bindImage(holder, product);

        // Rating bilgileri
        bindRating(holder, product);

        // Event listener'lar
        setupEventListeners(holder, product);

        // Favori durumu kontrol
        checkFavoriteStatus(holder.favoriteButton, product);
    }

    private void bindPrice(ProductViewHolder holder, Product product) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));

        if (product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
            // İndirimli fiyat varsa
            holder.productPrice.setText(currencyFormat.format(product.getDiscountPrice()));
            holder.productPrice.setTextColor(context.getResources().getColor(R.color.orange_primary));

            // Orijinal fiyatı göstermek için ek TextView ekleyebilirsiniz
            // holder.originalPriceTextView.setText(currencyFormat.format(product.getPrice()));
            // holder.originalPriceTextView.setPaintFlags(holder.originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // holder.originalPriceTextView.setVisibility(View.VISIBLE);
        } else {
            // Normal fiyat
            holder.productPrice.setText(currencyFormat.format(product.getPrice()));
            holder.productPrice.setTextColor(context.getResources().getColor(R.color.orange_primary));

            // holder.originalPriceTextView.setVisibility(View.GONE);
        }
    }

    private void bindImage(ProductViewHolder holder, Product product) {
        String imageBase64 = product.getImageBase64();
        if (!TextUtils.isEmpty(imageBase64)) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedString)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(holder.productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode error for product: " + product.getName(), e);
                holder.productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void bindRating(ProductViewHolder holder, Product product) {
        holder.ratingBarStars.setRating(product.getAverageRating());

        if (product.getTotalReviews() > 0) {
            // Değerlendirme var
            holder.textNumericAvgRating.setText(String.format(Locale.US, "%.1f", product.getAverageRating()));
            holder.textReviewCount.setText(String.format(Locale.getDefault(), "(%d)", product.getTotalReviews()));
            holder.textNumericAvgRating.setVisibility(View.VISIBLE);
            holder.textReviewCount.setVisibility(View.VISIBLE);
            holder.layoutRatingInfo.setVisibility(View.VISIBLE);
        } else {
            // Değerlendirme yok
            holder.textNumericAvgRating.setVisibility(View.GONE);
            holder.textReviewCount.setVisibility(View.GONE);
            // RatingBar'ı göster ama 0 yıldız olarak (tasarımınıza göre ayarlayın)
            holder.layoutRatingInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupEventListeners(ProductViewHolder holder, Product product) {
        holder.addToCartButton.setOnClickListener(v -> addToCart(product));
        holder.addToCartButton.setEnabled(true);

        holder.itemView.setOnClickListener(v -> openProductDetail(product));
        holder.favoriteButton.setOnClickListener(v -> toggleFavorite(holder.favoriteButton, product));
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    /**
     * Adapter'daki listeyi tamamen günceller (StoreActivity'den çağrılır)
     */
    public void updateList(List<Product> newList) {
        Log.d(TAG, "updateList çağrıldı. Yeni liste boyutu: " + (newList != null ? newList.size() : 0));

        if (newList == null) {
            this.productList.clear();
            this.originalProductList.clear();
        } else {
            this.productList.clear();
            this.productList.addAll(newList);
            this.originalProductList.clear();
            this.originalProductList.addAll(newList);
        }
        notifyDataSetChanged();

        Log.d(TAG, "Liste güncellendi. Görüntülenen ürün sayısı: " + getItemCount());
    }

    /**
     * Arama filtreleme (sadece arama çubuğu için kullanılır)
     * StoreActivity'deki diğer filtreler (kategori, fiyat vs.) updateList() ile gelir
     */
    public void filter(String searchText) {
        Log.d(TAG, "filter() çağrıldı. Arama metni: '" + searchText + "'");

        if (originalProductList == null) {
            Log.w(TAG, "originalProductList null, filtreleme yapılamıyor");
            return;
        }

        List<Product> filteredList = new ArrayList<>();

        if (TextUtils.isEmpty(searchText)) {
            // Arama metni boşsa orijinal listeyi göster
            filteredList.addAll(originalProductList);
        } else {
            String searchPattern = searchText.toLowerCase(Locale.getDefault()).trim();

            for (Product product : originalProductList) {
                if (product == null) continue;

                if (isProductMatchingSearch(product, searchPattern)) {
                    filteredList.add(product);
                }
            }
        }

        productList.clear();
        productList.addAll(filteredList);
        notifyDataSetChanged();

        Log.d(TAG, "Filtreleme tamamlandı. Sonuç: " + filteredList.size() + " ürün");
    }

    /**
     * Ürünün arama kriterlerine uyup uymadığını kontrol eder
     */
    private boolean isProductMatchingSearch(Product product, String searchPattern) {
        // Ürün adında ara
        if (product.getName() != null &&
                product.getName().toLowerCase(Locale.getDefault()).contains(searchPattern)) {
            return true;
        }

        // Marka adında ara
        if (product.getBrand() != null &&
                product.getBrand().toLowerCase(Locale.getDefault()).contains(searchPattern)) {
            return true;
        }

        // Açıklamada ara
        if (product.getDescription() != null &&
                product.getDescription().toLowerCase(Locale.getDefault()).contains(searchPattern)) {
            return true;
        }

        // Kategori adında ara (kategori mapping için)
        if (product.getCategory() != null) {
            String categoryDisplay = getCategoryDisplayName(product.getCategory());
            if (categoryDisplay.toLowerCase(Locale.getDefault()).contains(searchPattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Veritabanındaki kategori kodunu kullanıcı dostu isme çevirir
     */
    private String getCategoryDisplayName(String categoryCode) {
        if (categoryCode == null) return "";

        switch (categoryCode.toLowerCase()) {
            case "motor_oil": return "Motor Yağları";
            case "filters": return "Filtreler";
            case "brake_parts": return "Fren Parçaları";
            case "tires": return "Lastikler";
            case "batteries": return "Aküler";
            case "cleaning": return "Temizlik Ürünleri";
            case "tools": return "Araçlar ve Takımlar";
            case "accessories": return "Aksesuar";
            case "lights": return "Aydınlatma";
            case "electronics": return "Elektronik";
            default: return categoryCode;
        }
    }

    private void addToCart(Product product) {
        if (product == null) {
            Log.w(TAG, "addToCart: product is null");
            return;
        }

        try {
            Cart.getInstance().addItem(product, context);
            updateCartBadge();

            String productName = product.getName() != null ? product.getName() : "Ürün";
            Toast.makeText(context, productName + " sepete eklendi", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Ürün sepete eklendi: " + productName);
        } catch (Exception e) {
            Log.e(TAG, "Sepete ekleme hatası", e);
            Toast.makeText(context, "Sepete eklenirken hata oluştu", Toast.LENGTH_SHORT).show();
        }
    }

    private void openProductDetail(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            Toast.makeText(context, "Ürün bilgileri yüklenemedi.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "openProductDetail: Product or Product ID is null.");
            return;
        }

        try {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            context.startActivity(intent);

            Log.d(TAG, "ProductDetailActivity açıldı: " + product.getId());
        } catch (Exception e) {
            Log.e(TAG, "ProductDetailActivity açılırken hata", e);
            Toast.makeText(context, "Ürün detayı açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite(ImageButton favoriteBtn, Product product) {
        if (product == null) {
            Log.w(TAG, "toggleFavorite: product is null");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String productId = product.getId();

        if (TextUtils.isEmpty(productId)) {
            Toast.makeText(context, "Ürün ID bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Butonu geçici olarak devre dışı bırak
        favoriteBtn.setEnabled(false);

        db.collection("users").document(userId).collection("favorites").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String productName = product.getName() != null ? product.getName() : "Ürün";

                    if (documentSnapshot.exists()) {
                        // Favorilerden çıkar
                        db.collection("users").document(userId).collection("favorites").document(productId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                                    favoriteBtn.setEnabled(true);
                                    Toast.makeText(context, productName + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Favoriden çıkarma hatası", e);
                                    favoriteBtn.setEnabled(true);
                                });
                    } else {
                        // Favorilere ekle
                        Map<String, Object> favoriteMap = createFavoriteMap(product);

                        db.collection("users").document(userId).collection("favorites").document(productId)
                                .set(favoriteMap)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite);
                                    favoriteBtn.setEnabled(true);
                                    Toast.makeText(context, productName + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Favoriye ekleme hatası", e);
                                    favoriteBtn.setEnabled(true);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Favori durumu kontrol hatası", e);
                    favoriteBtn.setEnabled(true);
                });
    }

    private Map<String, Object> createFavoriteMap(Product product) {
        Map<String, Object> favoriteMap = new HashMap<>();
        favoriteMap.put("productId", product.getId());

        if (product.getName() != null) {
            favoriteMap.put("name", product.getName());
        }

        double price = product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice();
        favoriteMap.put("price", price);

        if (product.getImageBase64() != null) {
            favoriteMap.put("imageBase64", product.getImageBase64());
        }

        if (product.getBrand() != null) {
            favoriteMap.put("brand", product.getBrand());
        }

        favoriteMap.put("addedAt", FieldValue.serverTimestamp());

        return favoriteMap;
    }

    private void checkFavoriteStatus(ImageButton favoriteBtn, Product product) {
        if (product == null) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(product.getId())) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    favoriteBtn.setImageResource(documentSnapshot.exists() ?
                            R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Favori durumu kontrol hatası", e);
                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                });
    }

    private void updateCartBadge() {
        if (cartBadgeText != null) {
            try {
                int itemCount = Cart.getInstance().getItems().size();
                cartBadgeText.setVisibility(itemCount > 0 ? View.VISIBLE : View.GONE);
                if (itemCount > 0) {
                    cartBadgeText.setText(String.valueOf(itemCount));
                }

                Log.d(TAG, "Cart badge güncellendi: " + itemCount);
            } catch (Exception e) {
                Log.e(TAG, "Cart badge güncelleme hatası", e);
            }
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productBrand, textNumericAvgRating, textReviewCount;
        RatingBar ratingBarStars;
        Button addToCartButton;
        ImageButton favoriteButton;
        LinearLayout layoutRatingInfo;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name_card);
            productPrice = itemView.findViewById(R.id.product_price_card);
            productBrand = itemView.findViewById(R.id.product_brand_card);

            layoutRatingInfo = itemView.findViewById(R.id.layout_rating_info_card);
            textNumericAvgRating = itemView.findViewById(R.id.text_numeric_avg_rating_card);
            ratingBarStars = itemView.findViewById(R.id.rating_bar_stars_card);
            textReviewCount = itemView.findViewById(R.id.text_review_count_card);

            addToCartButton = itemView.findViewById(R.id.btn_add_to_cart_card);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
        }
    }
}