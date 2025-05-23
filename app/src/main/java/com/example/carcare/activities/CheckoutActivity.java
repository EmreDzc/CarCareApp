package com.example.carcare.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private EditText inputName, inputCard, inputCVV, inputExpiry, inputAddress;
    private Button buttonPurchase, buttonCancel;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // View elemanlarını tanımla
        inputName = findViewById(R.id.inputName);
        inputCard = findViewById(R.id.inputCard);
        inputCVV = findViewById(R.id.inputCVV);
        inputExpiry = findViewById(R.id.inputExpiry);
        inputAddress = findViewById(R.id.inputAddress);
        buttonPurchase = findViewById(R.id.buttonPurchase);
        buttonCancel = findViewById(R.id.buttonCancel);
        progressBar = findViewById(R.id.progressBar);

        // İptal butonuna tıklama
        buttonCancel.setOnClickListener(v -> finish());

        // Satın al butonuna tıklama
        buttonPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    processOrder();
                }
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String name = inputName.getText().toString().trim();
        String card = inputCard.getText().toString().trim();
        String cvv = inputCVV.getText().toString().trim();
        String expiry = inputExpiry.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();

        // Kart sahibi adı kontrolü
        if (TextUtils.isEmpty(name)) {
            inputName.setError("Kart sahibinin adını girin");
            valid = false;
        } else {
            inputName.setError(null);
        }

        // Kart numarası kontrolü
        if (TextUtils.isEmpty(card) || card.length() < 16) {
            inputCard.setError("Geçerli bir kart numarası girin");
            valid = false;
        } else {
            inputCard.setError(null);
        }

        // CVV kontrolü
        if (TextUtils.isEmpty(cvv) || cvv.length() < 3) {
            inputCVV.setError("Geçerli bir CVV girin");
            valid = false;
        } else {
            inputCVV.setError(null);
        }

        // Son kullanma tarihi kontrolü
        if (TextUtils.isEmpty(expiry) || !expiry.matches("\\d{2}/\\d{2}")) {
            inputExpiry.setError("Geçerli bir tarih girin (AA/YY)");
            valid = false;
        } else {
            inputExpiry.setError(null);
        }

        // Adres kontrolü
        if (TextUtils.isEmpty(address)) {
            inputAddress.setError("Teslimat adresini girin");
            valid = false;
        } else {
            inputAddress.setError(null);
        }

        // Sepet kontrolü
        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Sepetiniz boş!", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void processOrder() {
        // Kullanıcı giriş kontrolü
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_LONG).show();
            return;
        }

        // Yükleniyor göster
        progressBar.setVisibility(View.VISIBLE);
        buttonPurchase.setEnabled(false);

        // Sipariş verilerini hazırla
        Map<String, Object> order = new HashMap<>();
        order.put("userId", user.getUid());
        order.put("name", inputName.getText().toString().trim());
        order.put("address", inputAddress.getText().toString().trim());
        order.put("totalAmount", Cart.getInstance().getTotalPrice());
        order.put("orderDate", FieldValue.serverTimestamp());
        order.put("status", "Sipariş Alındı");

        // Sipariş öğelerini hazırla
        List<Map<String, Object>> items = new ArrayList<>();
        for (Product product : Cart.getInstance().getItems()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("price", product.getPrice());
            items.add(item);
        }
        order.put("items", items);

        // Siparişi Firestore'a ekle
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    // Sepeti temizle
                    Cart.getInstance().clearCart(this);

                    // Yükleniyor göster
                    progressBar.setVisibility(View.GONE);

                    // Sipariş tamamlandı mesajı göster
                    showOrderCompleteDialog(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Hata durumunda
                    progressBar.setVisibility(View.GONE);
                    buttonPurchase.setEnabled(true);
                    Toast.makeText(this, "Sipariş işlenirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showOrderCompleteDialog(String orderId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sipariş Tamamlandı")
                .setMessage("Siparişiniz başarıyla alındı. Sipariş numaranız: " + orderId)
                .setPositiveButton("Alışverişe Devam Et", (dialog, which) -> {
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