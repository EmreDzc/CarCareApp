package com.example.carcare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.adapters.CartAdapter;
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class CartActivity extends AppCompatActivity implements Cart.CartChangeListener {

    private RecyclerView recyclerView;
    private TextView totalPriceText, emptyCartMsg;
    private Button confirmButton, continueShoppingButton;
    private CartAdapter adapter;
    private View emptyCartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerViewCart);
        totalPriceText = findViewById(R.id.textTotalPrice);
        confirmButton = findViewById(R.id.buttonConfirm);
        emptyCartMsg = findViewById(R.id.textEmptyCart);
        emptyCartView = findViewById(R.id.emptyCartView);
        continueShoppingButton = findViewById(R.id.buttonContinueShopping);

        // Sepeti dinle
        Cart.getInstance().addCartChangeListener(this);

        // RecyclerView'ı ayarla
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(Cart.getInstance().getItems(), this, this);
        recyclerView.setAdapter(adapter);

        // Toplam fiyatı güncelle
        updateTotalPrice();

        // Boş sepet kontrolü
        checkEmptyCart();

        // Sepeti onayla butonu
        confirmButton.setOnClickListener(v -> {
            // Kullanıcı giriş kontrolü
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Sepet boş değilse devam et
                if (!Cart.getInstance().getItems().isEmpty()) {
                    Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Sepetiniz boş!", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Giriş yapmamış kullanıcıyı login ekranına yönlendir
                Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_LONG).show();
                // Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                // startActivity(intent);
            }
        });

        // Alışverişe Devam Et butonu
        continueShoppingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sepet sayfasını kapat ve Store sayfasına dön
                finish();
            }
        });

        // Geri butonu
        ImageButton backToStore = findViewById(R.id.button_back_to_store);
        backToStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onCartChanged() {
        // Sepet değiştiğinde UI'ı güncelle
        updateTotalPrice();
        checkEmptyCart();
    }

    private void updateTotalPrice() {
        double total = Cart.getInstance().getTotalPrice();
        totalPriceText.setText(String.format(Locale.US, "Toplam: $%.2f", total));
    }

    private void checkEmptyCart() {
        if (Cart.getInstance().getItems().isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyCartView.setVisibility(View.VISIBLE);
            confirmButton.setEnabled(false);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyCartView.setVisibility(View.GONE);
            confirmButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Activity kapanırken listener'ı kaldır
        Cart.getInstance().removeCartChangeListener(this);
    }
}