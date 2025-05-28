package com.example.carcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
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

    private static final String TAG = "ProductAdapter";
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

        // Base64 string'den resim yükleme
        String imageBase64 = product.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedString) // Byte array'i yükle
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode hatası, ürün: " + product.getName(), e);
                holder.productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Rasgele derecelendirme (gerçek uygulamada Firebase'den alınabilir)
        float rating = 3.5f + (random.nextFloat() * 1.5f);
        int reviewCount = 50 + random.nextInt(150);
        holder.productRating.setRating(rating);
        holder.productReviewCount.setText(String.format(Locale.getDefault(),"(%d inceleme)", reviewCount));

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
        this.productList.clear();
        this.productList.addAll(newList);
        this.productListFull.clear(); // Filtreleme için tam listeyi de güncelle
        this.productListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        productList.clear();
        if (text.isEmpty()) {
            productList.addAll(productListFull);
        } else {
            text = text.toLowerCase(Locale.getDefault());
            for (Product product : productListFull) {
                boolean nameMatch = product.getName() != null && product.getName().toLowerCase(Locale.getDefault()).contains(text);
                boolean descriptionMatch = product.getDescription() != null && product.getDescription().toLowerCase(Locale.getDefault()).contains(text);
                boolean categoryMatch = product.getCategory() != null && product.getCategory().toLowerCase(Locale.getDefault()).contains(text);
                if (nameMatch || descriptionMatch || categoryMatch) {
                    productList.add(product);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void addToCart(Product product) {
        Cart.getInstance().addItem(product, context);
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
        String userId = user.getUid();
        String productId = product.getId();
        if (productId == null) {
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
                                    Toast.makeText(context, product.getName() + " favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Map<String, Object> favoriteMap = new HashMap<>();
                        favoriteMap.put("productId", productId);
                        favoriteMap.put("addedAt", FieldValue.serverTimestamp());
                        db.collection("users").document(userId).collection("favorites").document(productId)
                                .set(favoriteMap)
                                .addOnSuccessListener(aVoid -> {
                                    favoriteBtn.setImageResource(R.drawable.ic_favorite);
                                    Toast.makeText(context, product.getName() + " favorilere eklendi", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void checkFavoriteStatus(ImageButton favoriteBtn, Product product) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || product.getId() == null) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            return;
        }
        db.collection("users").document(user.getUid()).collection("favorites").document(product.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    favoriteBtn.setImageResource(documentSnapshot.exists() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                })
                .addOnFailureListener(e -> favoriteBtn.setImageResource(R.drawable.ic_favorite_border));
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
        TextView productName, productPrice, productReviewCount;
        RatingBar productRating;
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