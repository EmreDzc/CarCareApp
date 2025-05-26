package com.example.carcare.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;

import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.example.carcare.adapters.OrderSummaryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    // Shipping Information Fields
    private EditText inputFullName, inputEmail, inputPhone, inputAddress,
            inputCity, inputState, inputZipCode;

    // Payment Fields
    private EditText inputCardNumber, inputExpiry, inputCVC;
    private RadioGroup paymentRadioGroup;
    private LinearLayout creditCardSection;

    // Order Summary
    private RecyclerView recyclerOrderItems;
    private TextView textSubtotal, textTax, textTotal;

    // Action Buttons
    private Button buttonPay;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OrderSummaryAdapter orderAdapter;

    private static final double TAX_RATE = 0.08; // 8% tax
    private DecimalFormat priceFormat = new DecimalFormat("$#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // View elemanlarını tanımla
        initializeViews();

        // Order summary'yi ayarla
        setupOrderSummary();

        // Event listener'ları ayarla
        setupEventListeners();

        // Totalleri hesapla
        calculateTotals();
    }

    private void initializeViews() {
        // Shipping Information
        inputFullName = findViewById(R.id.inputFullName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputAddress = findViewById(R.id.inputAddress);
        inputCity = findViewById(R.id.inputCity);
        inputState = findViewById(R.id.inputState);
        inputZipCode = findViewById(R.id.inputZipCode);

        // Payment Information
        inputCardNumber = findViewById(R.id.inputCardNumber);
        inputExpiry = findViewById(R.id.inputExpiry);
        inputCVC = findViewById(R.id.inputCVC);
        paymentRadioGroup = findViewById(R.id.paymentRadioGroup);
        creditCardSection = findViewById(R.id.creditCardSection);

        // Order Summary
        recyclerOrderItems = findViewById(R.id.recyclerOrderItems);
        textSubtotal = findViewById(R.id.textSubtotal);
        textTax = findViewById(R.id.textTax);
        textTotal = findViewById(R.id.textTotal);

        // Actions
        buttonPay = findViewById(R.id.buttonPay);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupOrderSummary() {
        // RecyclerView setup
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderSummaryAdapter(Cart.getInstance().getItems());
        recyclerOrderItems.setAdapter(orderAdapter);
    }

    private void setupEventListeners() {
        // Payment method değişimi
        paymentRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCreditCard) {
                creditCardSection.setVisibility(View.VISIBLE);
            } else {
                creditCardSection.setVisibility(View.GONE);
            }
        });

        // Pay butonuna tıklama
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

        buttonPay.setText("Pay " + priceFormat.format(total));
    }

    private boolean validateForm() {
        boolean valid = true;

        // Shipping Information Validation
        if (TextUtils.isEmpty(inputFullName.getText().toString().trim())) {
            inputFullName.setError("Full name is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputEmail.getText().toString().trim()) ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString().trim()).matches()) {
            inputEmail.setError("Valid email is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputPhone.getText().toString().trim())) {
            inputPhone.setError("Phone number is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputAddress.getText().toString().trim())) {
            inputAddress.setError("Address is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputCity.getText().toString().trim())) {
            inputCity.setError("City is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputState.getText().toString().trim())) {
            inputState.setError("State is required");
            valid = false;
        }

        if (TextUtils.isEmpty(inputZipCode.getText().toString().trim())) {
            inputZipCode.setError("ZIP code is required");
            valid = false;
        }

        // Payment Method Validation (sadece kredi kartı seçiliyse)
        if (paymentRadioGroup.getCheckedRadioButtonId() == R.id.radioCreditCard) {
            if (TextUtils.isEmpty(inputCardNumber.getText().toString().trim()) ||
                    inputCardNumber.getText().toString().trim().length() < 16) {
                inputCardNumber.setError("Valid card number is required");
                valid = false;
            }

            if (TextUtils.isEmpty(inputExpiry.getText().toString().trim()) ||
                    !inputExpiry.getText().toString().trim().matches("\\d{2}/\\d{2}")) {
                inputExpiry.setError("Valid expiry date is required (MM/YY)");
                valid = false;
            }

            if (TextUtils.isEmpty(inputCVC.getText().toString().trim()) ||
                    inputCVC.getText().toString().trim().length() < 3) {
                inputCVC.setError("Valid CVC is required");
                valid = false;
            }
        }

        // Cart validation
        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void processOrder() {
        // Kullanıcı giriş kontrolü
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            return;
        }

        // Yükleniyor göster
        progressBar.setVisibility(View.VISIBLE);
        buttonPay.setEnabled(false);

        // Sipariş verilerini hazırla
        Map<String, Object> order = new HashMap<>();
        order.put("userId", user.getUid());

        // Shipping Information
        order.put("fullName", inputFullName.getText().toString().trim());
        order.put("email", inputEmail.getText().toString().trim());
        order.put("phone", inputPhone.getText().toString().trim());
        order.put("address", inputAddress.getText().toString().trim());
        order.put("city", inputCity.getText().toString().trim());
        order.put("state", inputState.getText().toString().trim());
        order.put("zipCode", inputZipCode.getText().toString().trim());

        // Payment Information
        String paymentMethod = (paymentRadioGroup.getCheckedRadioButtonId() == R.id.radioCreditCard)
                ? "Credit Card" : "Cash on Delivery";
        order.put("paymentMethod", paymentMethod);

        // Order Details
        double subtotal = Cart.getInstance().getTotalPrice();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        order.put("subtotal", subtotal);
        order.put("tax", tax);
        order.put("totalAmount", total);
        order.put("orderDate", FieldValue.serverTimestamp());
        order.put("status", "Order Received");

        // Sipariş öğelerini hazırla
        List<Map<String, Object>> items = new ArrayList<>();
        for (Product product : Cart.getInstance().getItems()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("price", product.getPrice());
            item.put("quantity", 1); // Assuming quantity is 1 for now
            items.add(item);
        }
        order.put("items", items);

        // Siparişi Firestore'a ekle
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    // Sepeti temizle
                    Cart.getInstance().clearCart(this);

                    // Yükleniyor gizle
                    progressBar.setVisibility(View.GONE);

                    // Sipariş tamamlandı mesajı göster
                    showOrderCompleteDialog(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Hata durumunda
                    progressBar.setVisibility(View.GONE);
                    buttonPay.setEnabled(true);
                    Toast.makeText(this, "Order processing error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showOrderCompleteDialog(String orderId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Order Complete")
                .setMessage("Your order has been successfully placed. Order ID: " + orderId)
                .setPositiveButton("Continue Shopping", (dialog, which) -> {
                    // Mağaza sayfasına dön
                    Intent intent = new Intent(CheckoutActivity.this, StoreActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}