package com.example.carcare.adapters;

import android.content.Context;
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
import com.example.carcare.models.Wishlist;
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

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

        // Ürün bilgilerini göster
        holder.productName.setText(item.getProductName());
        holder.productPrice.setText(String.format(Locale.US, "$%.2f", item.getProductPrice()));

        // Eklenme tarihini göster
        if (item.getAddedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            holder.addedDate.setText("Added on " + dateFormat.format(item.getAddedAt()));
        }

        // Ürün resmini yükle
        if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getProductImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Sepete Ekle butonu
        holder.addToCartButton.setOnClickListener(v -> {
            // Product nesnesini oluştur
            Product product = new Product();
            product.setId(item.getProductId());
            product.setName(item.getProductName());
            product.setPrice(item.getProductPrice());
            product.setImageUrl(item.getProductImageUrl());

            // Sepete ekle
            Cart.getInstance().addItem(product, context);
        });

        // Favorilerden Çıkar butonu
        holder.removeButton.setOnClickListener(v -> {
            removeFromWishlist(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return wishlistItems.size();
    }

    private void removeFromWishlist(Wishlist item, int position) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .collection("favorites").document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    wishlistItems.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, wishlistItems.size());
                    Toast.makeText(context, "Favorilerden çıkarıldı", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, addedDate;
        Button addToCartButton;
        ImageButton removeButton;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            addedDate = itemView.findViewById(R.id.addedDate);
            addToCartButton = itemView.findViewById(R.id.btnAddToCart);
            removeButton = itemView.findViewById(R.id.btnRemove);
        }
    }
}