package com.example.carcare.adapters;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.example.carcare.models.Wishlist; // Güncellenmiş Wishlist modelini import ettiğinizden emin olun
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {
    private static final String TAG = "WishlistAdapter";
    private Context context;
    private List<Wishlist> wishlistItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public WishlistAdapter(Context context, List<Wishlist> wishlistItems) {
        this.context = context;
        this.wishlistItems = wishlistItems;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Wishlist item = wishlistItems.get(position);
        if (item == null) {
            Log.e(TAG, "Wishlist item at position " + position + " is null.");
            return;
        }

        holder.productName.setText(item.getProductName());
        holder.productPrice.setText(String.format(Locale.US, "$%.2f", item.getProductPrice()));

        if (item.getAddedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            holder.addedDate.setText("Eklendi: " + dateFormat.format(item.getAddedAt()));
        } else {
            holder.addedDate.setText("Tarih bilgisi yok");
        }

        // Base64 string'den resim yükleme
        String imageBase64 = item.getProductImageBase64(); // Wishlist modelinden al
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedString)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.productImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode hatası, ürün: " + item.getProductName(), e);
                holder.productImage.setImageResource(R.drawable.error_image);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.addToCartButton.setOnClickListener(v -> {
            Product product = new Product();
            product.setId(item.getProductId());
            product.setName(item.getProductName());
            product.setPrice(item.getProductPrice());
            product.setImageBase64(item.getProductImageBase64()); // imageBase64'ü ata
            // product.setCategory(...); // Gerekirse kategori ve stok bilgisi de Wishlist item'ında olmalı veya product'tan çekilmeli
            // product.setStock(...);

            if (product.getId() == null || product.getId().isEmpty()) {
                Toast.makeText(context, "Ürün bilgisi eksik, sepete eklenemedi.", Toast.LENGTH_SHORT).show();
                return;
            }
            Cart.getInstance().addItem(product, context);
            Toast.makeText(context, item.getProductName() + " sepete eklendi.", Toast.LENGTH_SHORT).show();
        });

        holder.removeButton.setOnClickListener(v -> {
            removeFromWishlist(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return wishlistItems != null ? wishlistItems.size() : 0;
    }

    private void removeFromWishlist(Wishlist item, int position) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Lütfen önce giriş yapın.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (item.getId() == null || item.getId().isEmpty()) {
            Toast.makeText(context, "Favori öğe ID'si bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("favorites").document(item.getId()) // Wishlist item'ın Firestore'daki ID'si
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < wishlistItems.size()) {
                        wishlistItems.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, wishlistItems.size()); // Önemli
                        Toast.makeText(context, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Favorilerden çıkarılırken geçersiz pozisyon: " + position);
                        // Listeyi yeniden yükle veya hata mesajı göster
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Favorilerden çıkarılırken hata", e);
                    Toast.makeText(context, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void updateWishlistItems(List<Wishlist> newItems) {
        this.wishlistItems.clear();
        this.wishlistItems.addAll(newItems);
        notifyDataSetChanged();
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, addedDate;
        Button addToCartButton;
        ImageButton removeButton;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage); // Layout ID'lerini kontrol et
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            addedDate = itemView.findViewById(R.id.addedDate);
            addToCartButton = itemView.findViewById(R.id.btnAddToCart);
            removeButton = itemView.findViewById(R.id.btnRemove); // Layout'ta bu ID ile bir ImageButton olmalı
        }
    }
}