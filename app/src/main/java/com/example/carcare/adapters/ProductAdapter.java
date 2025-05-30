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
    private List<Product> productList;
    private List<Product> productListFull; // Filtreleme için orijinal liste
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView cartBadgeText;

    public ProductAdapter(Context context, List<Product> productList, TextView cartBadge) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.productListFull = new ArrayList<>(this.productList);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.cartBadgeText = cartBadge;
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
            holder.productName.setText("Ürün Yüklenemedi");
            holder.productPrice.setText("");
            holder.ratingBarStars.setRating(0);
            holder.textNumericAvgRating.setVisibility(View.GONE);
            holder.textReviewCount.setVisibility(View.GONE);
            holder.productImage.setImageResource(R.drawable.placeholder_image);
            return;
        }

        if (!TextUtils.isEmpty(product.getBrand())) {
            holder.productBrand.setText(product.getBrand());
            holder.productBrand.setVisibility(View.VISIBLE);
        } else {
            holder.productBrand.setVisibility(View.GONE);
        }

        holder.productName.setText(product.getName() != null ? product.getName() : "İsimsiz Ürün");

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        if (product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
            holder.productPrice.setText(currencyFormat.format(product.getDiscountPrice()));
            // Orijinal fiyatı da göstermek isterseniz:
            // holder.originalPriceTextView.setText(currencyFormat.format(product.getPrice()));
            // holder.originalPriceTextView.setPaintFlags(holder.originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // holder.originalPriceTextView.setVisibility(View.VISIBLE);
        } else {
            holder.productPrice.setText(currencyFormat.format(product.getPrice()));
            // holder.originalPriceTextView.setVisibility(View.GONE);
        }


        String imageBase64 = product.getImageBase64();
        if (!TextUtils.isEmpty(imageBase64)) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedString)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode error for product: " + product.getName(), e);
                holder.productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Değerlendirme Bilgileri
        holder.ratingBarStars.setRating(product.getAverageRating());
        if (product.getTotalReviews() > 0) {
            holder.textNumericAvgRating.setText(String.format(Locale.US, "%.1f", product.getAverageRating()));
            holder.textReviewCount.setText(String.format(Locale.getDefault(), "(%d)", product.getTotalReviews()));
            holder.textNumericAvgRating.setVisibility(View.VISIBLE);
            holder.textReviewCount.setVisibility(View.VISIBLE);
            holder.layoutRatingInfo.setVisibility(View.VISIBLE);
        } else {
            // Hiç değerlendirme yoksa, sayısal puanı ve yorum sayısını gizle.
            // Yıldızlar 0 olarak zaten ayarlandı (product.getAverageRating() 0 olacağı için).
            holder.textNumericAvgRating.setVisibility(View.GONE);
            holder.textReviewCount.setVisibility(View.GONE);
            // layoutRatingInfo'yu gizlemeyin, böylece 0 yıldız görünür (tasarımınıza göre).
            // Eğer yıldızlar da gizlenecekse: holder.layoutRatingInfo.setVisibility(View.GONE);
            // Mevcut item_product.xml'de bu elemanlar zaten gone olduğu için,
            // sadece totalReviews > 0 ise VISIBLE yapılması yeterli. RatingBar hep visible kalabilir.
        }


        holder.addToCartButton.setOnClickListener(v -> addToCart(product));
        holder.itemView.setOnClickListener(v -> openProductDetail(product));
        holder.favoriteButton.setOnClickListener(v -> toggleFavorite(holder.favoriteButton, product));
        checkFavoriteStatus(holder.favoriteButton, product);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateList(List<Product> newList) {
        if (newList == null) {
            this.productList.clear();
            this.productListFull.clear();
        } else {
            this.productList.clear();
            this.productList.addAll(newList);
            this.productListFull.clear();
            this.productListFull.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void filter(String text) {
        List<Product> filteredList = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            filteredList.addAll(productListFull);
        } else {
            String filterPattern = text.toLowerCase(Locale.getDefault()).trim();
            for (Product product : productListFull) {
                if (product != null) {
                    boolean nameMatch = product.getName() != null && product.getName().toLowerCase(Locale.getDefault()).contains(filterPattern);
                    // boolean brandMatch = product.getBrand() != null && product.getBrand().toLowerCase(Locale.getDefault()).contains(filterPattern);
                    if (nameMatch /* || brandMatch */) {
                        filteredList.add(product);
                    }
                }
            }
        }
        productList.clear();
        productList.addAll(filteredList);
        notifyDataSetChanged();
    }

    private void addToCart(Product product) {
        if (product == null) return;
        Cart.getInstance().addItem(product, context);
        updateCartBadge();
        Toast.makeText(context, (product.getName() != null ? product.getName() : "Ürün") + " sepete eklendi", Toast.LENGTH_SHORT).show();
    }

    private void openProductDetail(Product product) {
        if (product == null || product.getId() == null || product.getId().isEmpty()) {
            Toast.makeText(context, "Ürün bilgileri yüklenemedi.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "openProductDetail: Product or Product ID is null.");
            return;
        }
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        context.startActivity(intent);
    }

    private void toggleFavorite(ImageButton favoriteBtn, Product product) {
        if (product == null) return;
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
        db.collection("users").document(userId).collection("favorites").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        db.collection("users").document(userId).collection("favorites").document(productId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                                    Toast.makeText(context, (product.getName() != null ? product.getName() : "Ürün") + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Map<String, Object> favoriteMap = new HashMap<>();
                        favoriteMap.put("productId", productId);
                        if (product.getName() != null) favoriteMap.put("name", product.getName());
                        favoriteMap.put("price", product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice());
                        if (product.getImageBase64() != null) favoriteMap.put("imageBase64", product.getImageBase64());
                        favoriteMap.put("addedAt", FieldValue.serverTimestamp());
                        db.collection("users").document(userId).collection("favorites").document(productId)
                                .set(favoriteMap)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite);
                                    Toast.makeText(context, (product.getName() != null ? product.getName() : "Ürün") + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error toggling favorite", e));
    }

    private void checkFavoriteStatus(ImageButton favoriteBtn, Product product) {
        if (product == null) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(product.getId())) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            return;
        }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    favoriteBtn.setImageResource(documentSnapshot.exists() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking favorite status", e);
                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                });
    }

    private void updateCartBadge() {
        if (cartBadgeText != null) {
            int itemCount = Cart.getInstance().getItems().size();
            cartBadgeText.setVisibility(itemCount > 0 ? View.VISIBLE : View.GONE);
            if (itemCount > 0) {
                cartBadgeText.setText(String.valueOf(itemCount));
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