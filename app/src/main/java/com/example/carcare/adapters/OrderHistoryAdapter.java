package com.example.carcare.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.models.Order;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private OnOrderClickListener listener;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderHistoryAdapter(Context context, List<Order> orderList, OnOrderClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr", "TR"));
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Sipariş tarihi
        if (order.getOrderDate() != null) {
            holder.textOrderDate.setText(dateFormat.format(order.getOrderDate()));
        } else {
            holder.textOrderDate.setText("Tarih Bilinmiyor");
        }

        // Toplam fiyat
        holder.textTotalPrice.setText("Toplam: " + currencyFormat.format(order.getTotalAmount()));

        // Durum
        holder.textOrderStatus.setText(order.getStatus() != null ? order.getStatus() : "Durum Bilinmiyor");

        // Durum rengi
        try {
            holder.textOrderStatus.setTextColor(Color.parseColor(order.getStatusColor()));
        } catch (IllegalArgumentException e) {
            holder.textOrderStatus.setTextColor(Color.parseColor("#757575"));
        }

        // Sipariş numarası (eğer görünür yapmak istiyorsanız)
        // holder.textOrderNumber.setText(order.getOrderNumber());

        // Ürün sayısı
        int itemCount = order.getItemCount();
        holder.textItemCount.setText(itemCount + " ürün teslim edildi");

        // İlk birkaç ürünün resimlerini göster
        displayProductImages(holder.layoutProductImages, order.getItems());

        // Detaylar butonu
        holder.textDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });

        // Kartın tamamına tıklama
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });

        // Değerlendirme butonu (isteğe bağlı)
        if (order.getStatus() != null && order.getStatus().equals("Teslim Edildi")) {
            holder.textReview.setVisibility(View.VISIBLE);
            holder.textReview.setOnClickListener(v -> {
                // Değerlendirme sayfasına yönlendir
                // Intent reviewIntent = new Intent(context, ReviewActivity.class);
                // reviewIntent.putExtra("ORDER_ID", order.getId());
                // context.startActivity(reviewIntent);
            });
        } else {
            holder.textReview.setVisibility(View.GONE);
        }
    }

    private void displayProductImages(LinearLayout layout, List<Map<String, Object>> items) {
        layout.removeAllViews();

        if (items == null || items.isEmpty()) {
            return;
        }

        // Maksimum 4 ürün resmi göster
        int maxImages = Math.min(items.size(), 4);

        for (int i = 0; i < maxImages; i++) {
            ImageView imageView = new ImageView(context);

            // Layout parametreleri
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
            params.setMargins(8, 0, 8, 0);
            imageView.setLayoutParams(params);

            // Resim özellikleri
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(Color.parseColor("#F5F5F5"));

            // Placeholder resim (gerçek implementasyonda ürün resmini yükleyin)
            imageView.setImageResource(R.drawable.placeholder_image);

            // Base64 resim yükleme (eğer items'da imageBase64 varsa)
            // String imageBase64 = (String) items.get(i).get("imageBase64");
            // if (imageBase64 != null && !imageBase64.isEmpty()) {
            //     // Glide ile resim yükle
            // }

            layout.addView(imageView);
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textOrderDate, textTotalPrice, textOrderStatus, textItemCount, textDetails, textReview;
        LinearLayout layoutProductImages;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderDate = itemView.findViewById(R.id.text_order_date);
            textTotalPrice = itemView.findViewById(R.id.text_total_price);
            textOrderStatus = itemView.findViewById(R.id.text_order_status);
            textItemCount = itemView.findViewById(R.id.text_item_count);
            textDetails = itemView.findViewById(R.id.text_details);
            textReview = itemView.findViewById(R.id.text_review);
            layoutProductImages = itemView.findViewById(R.id.layout_product_images);
        }
    }
}