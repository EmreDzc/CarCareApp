package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<Product> cartItems;
    private Context context;
    private Cart.CartChangeListener onCartUpdated;

    public CartAdapter(List<Product> items, Context ctx, Cart.CartChangeListener updateCallback) {
        this.cartItems = items;
        this.context = ctx;
        this.onCartUpdated = updateCallback;

        // Sepet değişikliklerini dinlemek için listener ekle
        Cart.getInstance().addCartChangeListener(() -> notifyDataSetChanged());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, price;
        public ImageView image;
        public Button remove;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.textCartItemName);
            price = view.findViewById(R.id.textCartItemPrice);
            image = view.findViewById(R.id.imageCartItem);
            remove = view.findViewById(R.id.buttonRemove);
        }
    }

    @NonNull
    @Override
    public CartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = cartItems.get(position);
        holder.name.setText(product.getName());
        holder.price.setText(String.format(Locale.US, "$%.2f", product.getPrice()));

        // Firebase Storage'dan ürün resmini yükle
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image);
        }

        holder.remove.setOnClickListener(v -> {
            Cart.getInstance().removeItem(product, context);
            if (onCartUpdated != null) {
                onCartUpdated.onCartChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Adapter silindiğinde listener'ı kaldır
        Cart.getInstance().removeCartChangeListener(onCartUpdated);
    }
}