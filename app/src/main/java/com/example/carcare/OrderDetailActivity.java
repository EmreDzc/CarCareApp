package com.example.carcare;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.adapters.OrderDetailAdapter;
import com.example.carcare.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailActivity";

    private TextView textOrderNumber, textOrderDate, textOrderStatus, textEstimatedDelivery;
    private TextView textTrackingNumber, textShippingCompany;
    private TextView textFullName, textAddress, textPhone, textEmail;
    private TextView textSubtotal, textTax, textTotal, textPaymentMethod;
    private RecyclerView recyclerOrderItems;
    private ProgressBar progressBar;
    private ImageButton backButton;
    private Button btnCancelOrder;
    private CardView cardCancelButton;

    private OrderDetailAdapter orderItemsAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String orderId;
    private Order currentOrder;

    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Sipariş ID bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ÖNEMLİ: İlk önce Firebase'i initialize et
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        // ÖNEMLİ: Firebase initialize edildikten sonra çağır
        loadOrderDetails();
    }

    private void initViews() {
        // Toolbar
        backButton = findViewById(R.id.btn_back_order_detail);

        // Order Info
        textOrderNumber = findViewById(R.id.text_order_number);
        textOrderDate = findViewById(R.id.text_order_date);
        textOrderStatus = findViewById(R.id.text_order_status);
        textEstimatedDelivery = findViewById(R.id.text_estimated_delivery);
        textTrackingNumber = findViewById(R.id.text_tracking_number);
        textShippingCompany = findViewById(R.id.text_shipping_company);

        // Delivery Address
        textFullName = findViewById(R.id.text_delivery_name);
        textAddress = findViewById(R.id.text_delivery_address);
        textPhone = findViewById(R.id.text_delivery_phone);
        textEmail = findViewById(R.id.text_delivery_email);

        // Payment Info
        textSubtotal = findViewById(R.id.text_order_subtotal);
        textTax = findViewById(R.id.text_order_tax);
        textTotal = findViewById(R.id.text_order_total);
        textPaymentMethod = findViewById(R.id.text_payment_method);

        // Cancel Button
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        cardCancelButton = findViewById(R.id.card_cancel_button);

        // Items RecyclerView
        recyclerOrderItems = findViewById(R.id.recycler_order_items);
        progressBar = findViewById(R.id.progress_bar_order_detail);

        // Date ve Currency formatları
        dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr", "TR"));
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));

        // Back button listener
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Cancel button listener
        if (btnCancelOrder != null) {
            btnCancelOrder.setOnClickListener(v -> showCancelConfirmationDialog());
        }

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sayfa her görünür olduğunda veriyi yenile
        Log.d(TAG, "onResume - Refreshing order data");
        if (orderId != null && !orderId.isEmpty()) {
            loadOrderDetails();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Activity restart olduğunda veriyi yenile
        Log.d(TAG, "onRestart - Refreshing order data");
        if (orderId != null && !orderId.isEmpty()) {
            loadOrderDetails();
        }
    }

    private void setupRecyclerView() {
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadOrderDetails() {
        // Null check ekle
        if (auth == null) {
            Log.e(TAG, "FirebaseAuth is null!");
            showError("Firebase Auth hatası");
            finish();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showError("Kullanıcı giriş yapmamış");
            finish();
            return;
        }

        showLoading(true);

        Log.d(TAG, "Loading order details for orderId: " + orderId + ", userId: " + user.getUid());

        // Kullanıcının alt koleksiyonundan sipariş detayını çek
        db.collection("users")
                .document(user.getUid())
                .collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firestore query successful. Document exists: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        currentOrder = documentSnapshot.toObject(Order.class);
                        if (currentOrder != null) {
                            currentOrder.setId(documentSnapshot.getId());
                            currentOrder.setUserId(user.getUid());
                            updateUI();
                            Log.d(TAG, "Order loaded successfully: " + currentOrder.getOrderNumber());
                        } else {
                            showError("Sipariş bilgileri okunamadı");
                        }
                    } else {
                        showError("Sipariş bulunamadı");
                        Log.w(TAG, "Order document does not exist for orderId: " + orderId);
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading order details", e);
                    showError("Sipariş yüklenirken hata: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void updateUI() {
        if (currentOrder == null) {
            Log.w(TAG, "updateUI called with null currentOrder");
            return;
        }

        try {
            // Order Info
            textOrderNumber.setText("Sipariş No: " + currentOrder.getOrderNumber());

            if (currentOrder.getOrderDate() != null) {
                textOrderDate.setText("Sipariş Tarihi: " + dateFormat.format(currentOrder.getOrderDate()));
            } else {
                textOrderDate.setText("Sipariş Tarihi: Bilinmiyor");
            }

            String status = currentOrder.getStatus() != null ? currentOrder.getStatus() : "Durum Bilinmiyor";
            textOrderStatus.setText(status);

            try {
                textOrderStatus.setTextColor(Color.parseColor(currentOrder.getStatusColor()));
            } catch (IllegalArgumentException e) {
                textOrderStatus.setTextColor(Color.parseColor("#757575"));
            }

            // Cancel button visibility - only show if order can be cancelled
            updateCancelButtonVisibility();

            // Tracking info
            if (currentOrder.getEstimatedDeliveryDate() != null) {
                textEstimatedDelivery.setText("Tahmini Teslimat: " + dateFormat.format(currentOrder.getEstimatedDeliveryDate()));
                textEstimatedDelivery.setVisibility(View.VISIBLE);
            } else {
                textEstimatedDelivery.setVisibility(View.GONE);
            }

            if (currentOrder.getTrackingNumber() != null && !currentOrder.getTrackingNumber().isEmpty()) {
                textTrackingNumber.setText("Teslimat No: " + currentOrder.getTrackingNumber());
                textTrackingNumber.setVisibility(View.VISIBLE);
            } else {
                textTrackingNumber.setVisibility(View.GONE);
            }

            if (currentOrder.getShippingCompany() != null && !currentOrder.getShippingCompany().isEmpty()) {
                textShippingCompany.setText("Kargo Firması: " + currentOrder.getShippingCompany());
                textShippingCompany.setVisibility(View.VISIBLE);
            } else {
                textShippingCompany.setVisibility(View.GONE);
            }

            // Delivery Address
            textFullName.setText("Alıcı: " + (currentOrder.getFullName() != null ? currentOrder.getFullName() : ""));

            String fullAddress = "";
            if (currentOrder.getAddress() != null) fullAddress += currentOrder.getAddress();
            if (currentOrder.getCity() != null) fullAddress += "\n" + currentOrder.getCity();
            if (currentOrder.getState() != null) fullAddress += "/" + currentOrder.getState();
            if (currentOrder.getZipCode() != null) fullAddress += " " + currentOrder.getZipCode();
            textAddress.setText(fullAddress);

            if (currentOrder.getPhone() != null && !currentOrder.getPhone().isEmpty()) {
                textPhone.setText(currentOrder.getPhone());
                textPhone.setVisibility(View.VISIBLE);
            } else {
                textPhone.setVisibility(View.GONE);
            }

            if (currentOrder.getEmail() != null && !currentOrder.getEmail().isEmpty()) {
                textEmail.setText(currentOrder.getEmail());
                textEmail.setVisibility(View.VISIBLE);
            } else {
                textEmail.setVisibility(View.GONE);
            }

            // Payment Info
            textSubtotal.setText("Ara Toplam: " + currencyFormat.format(currentOrder.getSubtotal()));
            textTax.setText("Kargo: " + currencyFormat.format(currentOrder.getTax()));
            textTotal.setText("Toplam: " + currencyFormat.format(currentOrder.getTotalAmount()));
            textPaymentMethod.setText("Ödeme: " + (currentOrder.getPaymentMethod() != null ? currentOrder.getPaymentMethod() : "Bilinmiyor"));

            // Order Items
            if (currentOrder.getItems() != null && !currentOrder.getItems().isEmpty()) {
                orderItemsAdapter = new OrderDetailAdapter(this, currentOrder.getItems());
                recyclerOrderItems.setAdapter(orderItemsAdapter);
                Log.d(TAG, "Order items loaded: " + currentOrder.getItems().size() + " items");
            } else {
                Log.w(TAG, "No items found in order");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            showError("UI güncellenirken hata oluştu");
        }
    }

    private void updateCancelButtonVisibility() {
        if (currentOrder == null || cardCancelButton == null) return;

        String status = currentOrder.getStatus();

        // İptal butonu sadece belirli durumlarda görünür olsun
        boolean canCancel = status != null &&
                !status.toLowerCase().contains("iptal") &&
                !status.toLowerCase().contains("teslim") &&
                !status.toLowerCase().contains("kargo") &&
                !status.toLowerCase().contains("gönderildi") &&
                !status.toLowerCase().contains("hazırlanıyor") &&
                !status.toLowerCase().contains("cancelled");

        cardCancelButton.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        Log.d(TAG, "Cancel button visibility updated. Status: " + status + ", Can cancel: " + canCancel);
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sipariş İptali")
                .setMessage("Bu siparişi iptal etmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet, İptal Et", (dialog, which) -> {
                    cancelOrder();
                })
                .setNegativeButton("Hayır", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void cancelOrder() {
        if (currentOrder == null || auth.getCurrentUser() == null) {
            showError("Sipariş iptal edilemedi");
            return;
        }

        showLoading(true);

        // Sipariş durumunu güncelle
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "İptal Edildi");
        updates.put("statusColor", "#F44336"); // Kırmızı renk
        updates.put("cancelledDate", new Date());

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("orders")
                .document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order cancelled successfully");
                    Toast.makeText(this, "Sipariş başarıyla iptal edildi", Toast.LENGTH_SHORT).show();

                    // Local order object'i güncelle
                    currentOrder.setStatus("İptal Edildi");

                    // UI'ı hemen güncelle
                    updateUI();

                    // İptal butonu gizle
                    if (cardCancelButton != null) {
                        cardCancelButton.setVisibility(View.GONE);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cancelling order", e);
                    showError("Sipariş iptal edilirken hata oluştu: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error shown to user: " + message);
    }
}