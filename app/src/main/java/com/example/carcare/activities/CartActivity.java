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
import com.example.carcare.models.Product; // Gerekirse Product import
import com.example.carcare.utils.Cart;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class CartActivity extends AppCompatActivity implements Cart.CartChangeListener {

    private RecyclerView recyclerView;
    private TextView totalPriceText; // emptyCartMsg kaldırıldı, emptyCartView kullanılıyor
    private Button confirmButton, continueShoppingButton;
    private CartAdapter adapter;
    private View emptyCartView; // LinearLayout veya FrameLayout gibi bir container

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerViewCart);
        totalPriceText = findViewById(R.id.textTotalPrice);
        confirmButton = findViewById(R.id.buttonConfirm);
        emptyCartView = findViewById(R.id.emptyCartView); // R.id.textEmptyCart yerine bu view'ı kullan
        continueShoppingButton = findViewById(R.id.buttonContinueShopping); // Layout'ta olduğundan emin ol

        Cart.getInstance().addCartChangeListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // CartAdapter'a this (Cart.CartChangeListener) callback'ini geçiyoruz.
        adapter = new CartAdapter(Cart.getInstance().getItems(), this, this);
        recyclerView.setAdapter(adapter);

        updateTotalPrice();
        checkEmptyCart();

        confirmButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                if (!Cart.getInstance().getItems().isEmpty()) {
                    Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Sepetiniz boş!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_LONG).show();
                // Örnek: Giriş sayfasına yönlendirme
                // Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                // startActivity(intent);
            }
        });

        if (continueShoppingButton != null) {
            continueShoppingButton.setOnClickListener(v -> finish());
        }

        ImageButton backToStore = findViewById(R.id.button_back_to_store); // Layout'ta olduğundan emin ol
        if (backToStore != null) {
            backToStore.setOnClickListener(v -> finish());
        }
    }

    @Override
    public void onCartChanged() {
        // CartAdapter kendi listesini Cart singleton'ından alıyorsa ve
        // Cart'taki değişikliklerde CartAdapter.notifyDataSetChanged() çağrılıyorsa
        // burada adapter.notifyDataSetChanged() gerekmeyebilir.
        // Ancak, adapter'a yeni bir liste geçirmek daha güvenli olabilir.
        if (adapter != null) {
            adapter.updateCartItems(Cart.getInstance().getItems()); // Adapter'a yeni listeyi ver
        }
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
            if (emptyCartView != null) emptyCartView.setVisibility(View.VISIBLE);
            confirmButton.setEnabled(false);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyCartView != null) emptyCartView.setVisibility(View.GONE);
            confirmButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cart.getInstance().removeCartChangeListener(this);
    }
}