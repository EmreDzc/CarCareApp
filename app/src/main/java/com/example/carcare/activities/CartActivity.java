package com.example.carcare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.adapters.CartAdapter;
import com.example.carcare.models.CartItem;
import com.example.carcare.utils.Cart;

import java.text.NumberFormat;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartAdapterListener, Cart.CartChangeListener {

    private RecyclerView recyclerViewCart;
    private CartAdapter cartAdapter;
    private TextView textTotalPrice;
    private LinearLayout emptyCartView;
    private Button buttonConfirm, buttonContinueShopping;
    private ImageButton backButton;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));

        initializeViews();
        setupRecyclerView();
        setupEventListeners();

        // Cart singleton'dan gelecek değişiklikleri dinlemek için listener ekliyoruz.
        Cart.getInstance().addCartChangeListener(this);

        updateCartUI();
    }

    private void initializeViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        textTotalPrice = findViewById(R.id.textTotalPrice);
        emptyCartView = findViewById(R.id.emptyCartView);
        buttonConfirm = findViewById(R.id.buttonConfirm);
        buttonContinueShopping = findViewById(R.id.buttonContinueShopping);
        backButton = findViewById(R.id.button_back_to_store); // ID'yi xml'den aldım
    }

    private void setupRecyclerView() {
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, Cart.getInstance().getItems(), this);
        recyclerViewCart.setAdapter(cartAdapter);
    }

    private void setupEventListeners() {
        buttonConfirm.setOnClickListener(v -> {
            // CheckoutActivity'e git
            startActivity(new Intent(CartActivity.this, CheckoutActivity.class));
        });

        buttonContinueShopping.setOnClickListener(v -> {
            // Mağazaya geri dön
            startActivity(new Intent(CartActivity.this, StoreActivity.class));
            finish();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void updateCartUI() {
        // Adapter'daki listeyi güncelle
        cartAdapter.updateItems(Cart.getInstance().getItems());

        // Toplam fiyatı güncelle
        double totalPrice = Cart.getInstance().getTotalPrice();
        textTotalPrice.setText("Total: " + currencyFormat.format(totalPrice));

        // Boş sepet görünümünü kontrol et
        if (Cart.getInstance().getItems().isEmpty()) {
            emptyCartView.setVisibility(View.VISIBLE);
            recyclerViewCart.setVisibility(View.GONE);
            findViewById(R.id.checkoutLayout).setVisibility(View.GONE);
        } else {
            emptyCartView.setVisibility(View.GONE);
            recyclerViewCart.setVisibility(View.VISIBLE);
            findViewById(R.id.checkoutLayout).setVisibility(View.VISIBLE);
        }
    }

    // --- CartAdapterListener Metodları ---
    @Override
    public void onIncreaseClicked(CartItem item) {
        Cart.getInstance().increaseQuantity(item.getProduct().getId());
    }

    @Override
    public void onDecreaseClicked(CartItem item) {
        Cart.getInstance().decreaseQuantity(item.getProduct().getId(), this);
    }

    @Override
    public void onRemoveClicked(CartItem item) {
        Cart.getInstance().removeItem(item.getProduct().getId(), this);
    }

    // --- Cart.CartChangeListener Metodu ---
    @Override
    public void onCartChanged() {
        // Sepette bir değişiklik olduğunda (artırma, azaltma, silme) bu metod tetiklenir
        // ve arayüzü günceller.
        updateCartUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Aktivite yok olduğunda listener'ı kaldırmayı unutmayın
        Cart.getInstance().removeCartChangeListener(this);
    }
}