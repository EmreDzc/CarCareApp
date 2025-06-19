package com.example.carcare.adapters;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carcare.R;
import com.example.carcare.models.CartItem;
import com.example.carcare.models.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private CartAdapterListener listener;
    private NumberFormat currencyFormat;

    // Aktivitenin tıklamaları dinlemesi için bir arayüz (interface)
    public interface CartAdapterListener {
        void onIncreaseClicked(CartItem item);
        void onDecreaseClicked(CartItem item);
        void onRemoveClicked(CartItem item);
    }

    public CartAdapter(Context context, List<CartItem> cartItems, CartAdapterListener listener) {
        this.context = context;
        this.cartItems = new ArrayList<>(cartItems); // Kopyasını oluşturmak daha güvenli
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_cart.xml layout'unu inflate ediyoruz
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        Product product = cartItem.getProduct();

        holder.textCartItemName.setText(product.getName());
        holder.textCartItemPrice.setText(currencyFormat.format(product.getFinalPrice()));
        holder.textQuantity.setText(String.valueOf(cartItem.getQuantity()));

        // Ürün resmi
        String imageBase64 = product.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
            Glide.with(context)
                    .load(decodedString)
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.imageCartItem);
        } else {
            holder.imageCartItem.setImageResource(R.drawable.placeholder_image);
        }

        // Tıklama olaylarını listener aracılığıyla aktiviteye iletiyoruz
        holder.buttonIncrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseClicked(cartItem);
            }
        });

        holder.buttonDecrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseClicked(cartItem);
            }
        });

        holder.buttonRemoveItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(cartItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    // Sepet güncellendiğinde listeyi yenilemek için
    public void updateItems(List<CartItem> newItems) {
        this.cartItems.clear();
        this.cartItems.addAll(newItems);
        notifyDataSetChanged();
    }

    // ViewHolder sınıfı
    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCartItem;
        TextView textCartItemName, textCartItemPrice, textQuantity;
        ImageButton buttonIncrease, buttonDecrease, buttonRemoveItem;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCartItem = itemView.findViewById(R.id.imageCartItem);
            textCartItemName = itemView.findViewById(R.id.textCartItemName);
            textCartItemPrice = itemView.findViewById(R.id.textCartItemPrice);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            buttonIncrease = itemView.findViewById(R.id.buttonIncrease);
            buttonDecrease = itemView.findViewById(R.id.buttonDecrease);
            buttonRemoveItem = itemView.findViewById(R.id.buttonRemoveItem);
        }
    }
}