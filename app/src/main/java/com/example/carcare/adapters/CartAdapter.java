package com.example.carcare.adapters;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
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
    private static final String TAG = "CartAdapter";
    private List<Product> cartItems;
    private Context context;
    private Cart.CartChangeListener onCartUpdated; // Bu listener, Cart sınıfının içindeki bir interface olmalı

    public CartAdapter(List<Product> items, Context ctx, Cart.CartChangeListener updateCallback) {
        this.cartItems = items;
        this.context = ctx;
        this.onCartUpdated = updateCallback;

        // Eğer Cart singleton ise ve global değişiklikleri dinliyorsa, bu gerekli olabilir.
        // Ancak, adapter genellikle kendi listesindeki değişikliklere notifyDataSetChanged ile tepki verir.
        // Cart.getInstance().addCartChangeListener(this::notifyDataSetChanged);
        // Ya da daha spesifik olarak, Cart sınıfından gelen callback'i kullan
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
        if (product == null) {
            Log.e(TAG, "Product at position " + position + " is null.");
            return;
        }

        holder.name.setText(product.getName());
        holder.price.setText(String.format(Locale.US, "$%.2f", product.getPrice()));

        // Base64 string'den resim yükleme
        String imageBase64 = product.getImageBase64();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Glide.with(context)
                        .load(decodedString) // Byte array'i yükle
                        .placeholder(R.drawable.placeholder_image) // drawable içinde olmalı
                        .error(R.drawable.error_image) // drawable içinde olmalı
                        .into(holder.image);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decode hatası, ürün: " + product.getName(), e);
                holder.image.setImageResource(R.drawable.error_image);
            }
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image);
        }

        holder.remove.setOnClickListener(v -> {
            Product productToRemove = cartItems.get(holder.getAdapterPosition()); // Her zaman güncel pozisyonu al
            Cart.getInstance().removeItem(productToRemove, context);
            // notifyDataSetChanged() Cart sınıfındaki listener aracılığıyla çağrılabilir
            // veya doğrudan burada çağrılabilir. Eğer Cart global listener'a sahipse,
            // aşağıdaki satır gereksiz olabilir.
            // cartItems.remove(productToRemove); // Yerel listeyi de güncellemek önemli
            // notifyItemRemoved(holder.getAdapterPosition());
            // notifyItemRangeChanged(holder.getAdapterPosition(), cartItems.size());

            if (onCartUpdated != null) {
                onCartUpdated.onCartChanged(); // Bu metod genellikle total price gibi şeyleri günceller
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    // Bu metod, Cart sınıfındaki listener yönetimini basitleştirmek için eklendi.
    // Eğer Cart sınıfı listener'ları yönetiyorsa, bu gerekli olmayabilir.
    public void updateCartItems(List<Product> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    // `onDetachedFromRecyclerView` içindeki listener kaldırma işlemi
    // Cart sınıfındaki listener ekleme/kaldırma mekanizmasına bağlıdır.
    // Eğer CartAdapter constructor'ında `Cart.getInstance().addCartChangeListener(this::notifyDataSetChanged);`
    // gibi bir şey varsa, burada da `Cart.getInstance().removeCartChangeListener(this::notifyDataSetChanged);` olmalı.
    // Mevcut kodunuzda `onCartUpdated` callback'ini Cart'a kaydediyorsunuz.
    // Bu `onCartUpdated` callback'i adapter'a özgü bir referanssa (lambda değilse) kaldırılabilir.
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Eğer Cart.getInstance() bir listener listesi tutuyorsa ve
        // bu adapter'a özel bir listener eklediyseniz, onu burada kaldırın.
        // Örnek: Cart.getInstance().removeCartChangeListener(onCartUpdated);
        // Eğer onCartUpdated, CartActivity'nin bir metoduysa ve CartActivity
        // kendi listener'ını yönetiyorsa, bu satır burada olmamalıdır.
        // Şu anki yapıda CartActivity Cart'a listener olarak kendini ekliyor ve
        // CartAdapter'a da bir callback veriyor. Bu biraz karmaşık.
        // İdeal olan, CartActivity'nin Cart'ı dinlemesi ve CartAdapter'a sadece listeyi vermesidir.
        // Adapter listeyi güncellediğinde `notifyDataSetChanged` yapar.
    }
}