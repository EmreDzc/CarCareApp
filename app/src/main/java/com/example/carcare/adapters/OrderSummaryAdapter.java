package com.example.carcare.adapters;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.bumptech.glide.Glide;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import com.example.carcare.models.CartItem;


public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.OrderSummaryViewHolder> {
    private static final String TAG = "OrderSummaryAdapter";
    private DecimalFormat priceFormat = new DecimalFormat("$0.00", new DecimalFormatSymbols(Locale.US));
    private List<CartItem> cartItems;


    public OrderSummaryAdapter(List<CartItem> cartItems, Context context) {
        this.cartItems = cartItems; // Yeni
    }

    @NonNull
    @Override
    public OrderSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_summary, parent, false);
        return new OrderSummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderSummaryViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position); // Değişti
        Product product = cartItem.getProduct(); // Yeni

        if (product == null) {
            Log.e(TAG, "Product at position " + position + " is null.");
            return;
        }

        holder.textProductName.setText(product.getName());
        // Miktar artık dinamik olarak alınıyor!
        holder.textQuantity.setText("Adet: " + cartItem.getQuantity());
        // Fiyat olarak ürünün son fiyatını ve adetini çarpıyoruz.
        double itemTotalPrice = product.getFinalPrice() * cartItem.getQuantity();
        holder.textPrice.setText(priceFormat.format(itemTotalPrice));

        String imageBase64 = product.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(holder.itemView.getContext())
                        .load(decodedString)
                        .placeholder(R.drawable.ic_product_placeholder)
                        .error(R.drawable.ic_product_placeholder)
                        .into(holder.imageProduct);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode hatası, ürün: " + product.getName(), e);
                holder.imageProduct.setImageResource(R.drawable.ic_product_placeholder);
            }
        } else {
            holder.imageProduct.setImageResource(R.drawable.ic_product_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0; // Değişti
    }

    public static class OrderSummaryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView textProductName;
        TextView textQuantity;
        TextView textPrice;

        public OrderSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.imageProduct);
            textProductName = itemView.findViewById(R.id.textProductName);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            textPrice = itemView.findViewById(R.id.textPrice);
        }
    }
}