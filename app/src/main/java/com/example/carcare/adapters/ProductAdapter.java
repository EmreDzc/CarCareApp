package com.example.carcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private List<Product> productListFull;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Random random;
    private TextView cartBadgeText;

    public ProductAdapter(Context context, List<Product> productList, TextView cartBadge) {
        this.context = context;
        this.productList = productList;
        this.productListFull = new ArrayList<>(productList);
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.random = new Random();
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
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.US, "$%.2f", product.getPrice()));

        // Firebase Storage'dan görsel yükleme
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Rasgele derecelendirme (gerçek uygulamada Firebase'den alınabilir)
        float rating = 3.5f + (random.nextFloat() * 1.5f);
        int reviewCount = 50 + random.nextInt(150);
        holder.productRating.setRating(rating);
        holder.productReviewCount.setText(String.format("(%d reviews)", reviewCount));

        // Sepete Ekle butonuna tıklama işlemi
        holder.addToCartButton.setOnClickListener(v -> {
            addToCart(product);
        });

        // Ürüne tıklama
        holder.itemView.setOnClickListener(v -> {
            openProductDetail(product);
        });

        // Favori butonuna tıklama işlemi
        holder.favoriteButton.setOnClickListener(v -> {
            toggleFavorite(holder.favoriteButton, product);
        });

        // Favori durumunu Firebase'den kontrol et
        checkFavoriteStatus(holder.favoriteButton, product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        productList.clear();
        productList.addAll(newList);
        productListFull.clear();
        productListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        productList.clear();
        if (text.isEmpty()) {
            productList.addAll(productListFull);
        } else {
            text = text.toLowerCase();
            for (Product product : productListFull) {
                if (product.getName().toLowerCase().contains(text) ||
                        product.getDescription().toLowerCase().contains(text) ||
                        (product.getCategory() != null && product.getCategory().toLowerCase().contains(text))) {
                    productList.add(product);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void addToCart(Product product) {
        // Sepete ürün ekle
        Cart.getInstance().addItem(product, context);

        // Sepet rozetini güncelle
        updateCartBadge();
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        context.startActivity(intent);
    }

    private void toggleFavorite(ImageButton favoriteBtn, Product product) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kullanıcı favorilerini kontrol et
        String userId = user.getUid();
        db.collection("users").document(userId)
                .collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Favori zaten var, kaldır
                        db.collection("users").document(userId)
                                .collection("favorites").document(product.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                                    Toast.makeText(context, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Favorilere ekle
                        Map<String, Object> favoriteMap = new HashMap<>();
                        favoriteMap.put("productId", product.getId());
                        favoriteMap.put("addedAt", FieldValue.serverTimestamp());

                        db.collection("users").document(userId)
                                .collection("favorites").document(product.getId())
                                .set(favoriteMap)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite);
                                    Toast.makeText(context, "Favorilere eklendi", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void checkFavoriteStatus(ImageButton favoriteBtn, Product product) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        favoriteBtn.setImageResource(R.drawable.ic_favorite);
                    } else {
                        favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                    }
                })
                .addOnFailureListener(e -> {
                    favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                });
    }

    private void updateCartBadge() {
        if (cartBadgeText != null) {
            int itemCount = Cart.getInstance().getItems().size();
            if (itemCount > 0) {
                cartBadgeText.setVisibility(View.VISIBLE);
                cartBadgeText.setText(String.valueOf(itemCount));
            } else {
                cartBadgeText.setVisibility(View.GONE);
            }
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
        RatingBar productRating;
        TextView productReviewCount;
        Button addToCartButton;
        ImageButton favoriteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productRating = itemView.findViewById(R.id.product_rating);
            productReviewCount = itemView.findViewById(R.id.product_review_count);
            addToCartButton = itemView.findViewById(R.id.btn_add_to_cart);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
        }
    }
}