package com.example.carcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.ProductDetailActivity;
import com.example.carcare.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.OrderItemViewHolder> {

    private Context context;
    private List<Map<String, Object>> orderItems;
    private NumberFormat currencyFormat;

    public OrderDetailAdapter(Context context, List<Map<String, Object>> orderItems) {
        this.context = context;
        this.orderItems = orderItems;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        Map<String, Object> item = orderItems.get(position);

        // Ürün adı
        String productName = (String) item.get("productName");
        holder.textProductName.setText(productName != null ? productName : "Ürün Adı Bilinmiyor");

        // Fiyat
        Object priceObj = item.get("price");
        if (priceObj instanceof Number) {
            double price = ((Number) priceObj).doubleValue();
            holder.textProductPrice.setText(currencyFormat.format(price));
        } else {
            holder.textProductPrice.setText("Fiyat Bilinmiyor");
        }

        // Miktar
        Object quantityObj = item.get("quantity");
        if (quantityObj instanceof Number) {
            int quantity = ((Number) quantityObj).intValue();
            holder.textProductQuantity.setText("Adet: " + quantity);
        } else {
            holder.textProductQuantity.setText("Adet: 1");
        }

        // Ürün resmi (placeholder)
        holder.imageProduct.setImageResource(R.drawable.placeholder_image);

        // Eğer Base64 resim verisi varsa yüklenebilir
        String imageBase64 = (String) item.get("imageBase64");
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            // Burada Glide ile Base64'den resim yükleyebilirsiniz
            // Glide.with(context)
            //     .load(Base64.decode(imageBase64, Base64.DEFAULT))
            //     .placeholder(R.drawable.placeholder_image)
            //     .into(holder.imageProduct);
        }

        // ÖNEMLİ: Ürünü değerlendir butonu - ProductDetailActivity'ye yönlendir
        holder.textReviewProduct.setOnClickListener(v -> {
            String productId = (String) item.get("productId");
            if (productId != null && !productId.isEmpty()) {
                // ProductDetailActivity'ye git ve değerlendirme bölümüne odaklan
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", productId);
                intent.putExtra("FOCUS_REVIEW", true); // Değerlendirme bölümüne odaklanmak için
                intent.putExtra("FROM_ORDER", true); // Sipariş sayfasından geldiğini belirtmek için
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Ürün ID bulunamadı", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView textProductName, textProductPrice, textProductQuantity, textReviewProduct;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.image_order_product);
            textProductName = itemView.findViewById(R.id.text_order_product_name);
            textProductPrice = itemView.findViewById(R.id.text_order_product_price);
            textProductQuantity = itemView.findViewById(R.id.text_order_product_quantity);
            textReviewProduct = itemView.findViewById(R.id.text_review_product);
        }
    }
}