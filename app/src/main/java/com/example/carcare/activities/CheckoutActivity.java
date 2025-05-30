package com.example.carcare.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // ImageButton olarak değiştirildi
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import java.util.Calendar;
import java.util.Date;

import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.example.carcare.adapters.OrderSummaryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";

    private EditText inputFullName, inputEmail, inputPhone, inputAddress,
            inputCity, inputState, inputZipCode;
    private EditText inputCardNumber, inputExpiry, inputCVC;
    private RadioGroup paymentRadioGroup;
    private LinearLayout creditCardSection;
    private RecyclerView recyclerOrderItems;
    private TextView textSubtotal, textTax, textTotal;
    private Button buttonPay;
    private ProgressBar progressBar;
    private ImageButton backButton; // Değişiklik: Button yerine ImageButton


    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OrderSummaryAdapter orderAdapter;

    private static final double TAX_RATE = 0.08;
    private DecimalFormat priceFormat = new DecimalFormat("$0.00", new DecimalFormatSymbols(Locale.US));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Direkt satın alma kontrolü
        boolean isDirectBuy = getIntent().getBooleanExtra("DIRECT_BUY", false);
        String productId = getIntent().getStringExtra("PRODUCT_ID");

        if (isDirectBuy && productId != null) {
            Log.d(TAG, "Direkt satın alma modu aktif, Product ID: " + productId);
        }

        // Sepet boş kontrolü
        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Sepetiniz boş. Mağazaya yönlendiriliyorsunuz.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
        setupOrderSummary();
        setupEventListeners();
        calculateTotals();
    }

    private void initializeViews() {
        inputFullName = findViewById(R.id.inputFullName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputAddress = findViewById(R.id.inputAddress);
        inputCity = findViewById(R.id.inputCity);
        inputState = findViewById(R.id.inputState);
        inputZipCode = findViewById(R.id.inputZipCode);

        inputCardNumber = findViewById(R.id.inputCardNumber);
        inputExpiry = findViewById(R.id.inputExpiry);
        inputCVC = findViewById(R.id.inputCVC);
        paymentRadioGroup = findViewById(R.id.paymentRadioGroup);
        creditCardSection = findViewById(R.id.creditCardSection);

        recyclerOrderItems = findViewById(R.id.recyclerOrderItems);
        textSubtotal = findViewById(R.id.textSubtotal);
        textTax = findViewById(R.id.textTax);
        textTotal = findViewById(R.id.textTotal);

        buttonPay = findViewById(R.id.buttonPay);
        progressBar = findViewById(R.id.progressBar);
        // Değişiklik: XML'deki ID ile eşleştirildi
        backButton = findViewById(R.id.button_back_to_cart);

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.w(TAG, "Geri butonu (button_back_to_cart) layout'ta bulunamadı.");
        }
    }

    private void setupOrderSummary() {
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderSummaryAdapter(Cart.getInstance().getItems());
        recyclerOrderItems.setAdapter(orderAdapter);
    }

    private void setupEventListeners() {
        paymentRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCreditCard) {
                creditCardSection.setVisibility(View.VISIBLE);
            } else {
                creditCardSection.setVisibility(View.GONE);
            }
        });

        buttonPay.setOnClickListener(v -> {
            if (validateForm()) {
                processOrder();
            }
        });
    }

    private void calculateTotals() {
        double subtotal = Cart.getInstance().getTotalPrice();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        textSubtotal.setText(priceFormat.format(subtotal));
        textTax.setText(priceFormat.format(tax));
        textTotal.setText(priceFormat.format(total));
        buttonPay.setText("Öde " + priceFormat.format(total));
    }

    private boolean validateForm() {
        boolean valid = true;

        if (TextUtils.isEmpty(inputFullName.getText())) {
            inputFullName.setError("Tam ad gerekli.");
            valid = false;
        }
        if (TextUtils.isEmpty(inputAddress.getText())) {
            inputAddress.setError("Adres gerekli.");
            valid = false;
        }
        if (TextUtils.isEmpty(inputEmail.getText()) || !android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText()).matches()) {
            inputEmail.setError("Geçerli e-posta gerekli.");
            valid = false;
        }

        if (paymentRadioGroup.getCheckedRadioButtonId() == R.id.radioCreditCard) {
            if (TextUtils.isEmpty(inputCardNumber.getText()) || inputCardNumber.getText().length() < 13) {
                inputCardNumber.setError("Geçerli kart numarası gerekli.");
                valid = false;
            }
            if (TextUtils.isEmpty(inputExpiry.getText()) || !inputExpiry.getText().toString().matches("\\d{2}/\\d{2}")) {
                inputExpiry.setError("AA/YY formatında geçerli son kullanma tarihi gerekli.");
                valid = false;
            }
            if (TextUtils.isEmpty(inputCVC.getText()) || inputCVC.getText().length() < 3) {
                inputCVC.setError("Geçerli CVC gerekli.");
                valid = false;
            }
        }

        // Sepet kontrolü - Bu kısmı güncelledik
        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Sepetiniz boş! Mağazaya yönlendiriliyorsunuz.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            valid = false;
        }

        return valid;
    }


    private void processOrder() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonPay.setEnabled(false);

        // Sipariş verilerini hazırla
        Map<String, Object> orderData = new HashMap<>();
        // userId artık gerekli değil çünkü zaten kullanıcının alt koleksiyonunda
        orderData.put("fullName", inputFullName.getText().toString().trim());
        orderData.put("email", inputEmail.getText().toString().trim());
        orderData.put("phone", inputPhone.getText().toString().trim());
        orderData.put("address", inputAddress.getText().toString().trim());
        orderData.put("city", inputCity.getText().toString().trim());
        orderData.put("state", inputState.getText().toString().trim());
        orderData.put("zipCode", inputZipCode.getText().toString().trim());

        String paymentMethod = (paymentRadioGroup.getCheckedRadioButtonId() == R.id.radioCreditCard)
                ? "Credit Card" : "Cash on Delivery";
        orderData.put("paymentMethod", paymentMethod);

        // Fiyat hesaplamaları
        double subtotal = Cart.getInstance().getTotalPrice();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;
        orderData.put("subtotal", subtotal);
        orderData.put("tax", tax);
        orderData.put("totalAmount", total);

        // Sipariş tarihi ve durumu
        orderData.put("orderDate", FieldValue.serverTimestamp());
        orderData.put("status", "Sipariş Alındı");

        // Kargo ve teslimat bilgileri
        orderData.put("shippingCompany", "CarCare Express");
        orderData.put("trackingNumber", generateTrackingNumber());

        // Tahmini teslimat tarihi (3-5 gün sonra)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 4);
        orderData.put("estimatedDeliveryDate", new Date(calendar.getTimeInMillis()));

        // Sipariş öğelerini hazırla
        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (Product product : Cart.getInstance().getItems()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("price", product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice());
            item.put("quantity", 1);
            item.put("imageBase64", product.getImageBase64());
            orderItems.add(item);
        }
        orderData.put("items", orderItems);

        // DEĞİŞİKLİK: Kullanıcının alt koleksiyonuna kaydet
        db.collection("users")
                .document(user.getUid())
                .collection("orders")  // Subcollection
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();

                    // Sepeti temizle
                    Cart.getInstance().clearCart(this);

                    progressBar.setVisibility(View.GONE);

                    // Başarı diyaloğu göster
                    showOrderCompleteDialog(orderId);

                    Log.d(TAG, "Sipariş başarıyla oluşturuldu: " + orderId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonPay.setEnabled(true);
                    Log.e(TAG, "Sipariş işlenirken hata", e);
                    Toast.makeText(this, "Sipariş işlenirken hata: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // Tracking numarası oluşturma (aynı kalacak)
    private String generateTrackingNumber() {
        return String.valueOf(System.currentTimeMillis()).substring(3);
    }

    private void showOrderCompleteDialog(String orderId) {
        new AlertDialog.Builder(this)
                .setTitle("Sipariş Tamamlandı")
                .setMessage("Siparişiniz başarıyla alındı. Sipariş ID: " + orderId)
                .setPositiveButton("Alışverişe Devam Et", (dialog, which) -> {
                    Intent intent = new Intent(CheckoutActivity.this, StoreActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .setCancelable(false)
                .show();
    }
}