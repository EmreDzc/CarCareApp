package com.example.carcare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;


import com.example.carcare.R;
import com.example.carcare.adapters.CartAdapter;
import com.example.carcare.utils.Cart;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView totalPriceText;
    private Button confirmButton;
    private CartAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerViewCart);
        totalPriceText = findViewById(R.id.textTotalPrice);
        confirmButton = findViewById(R.id.buttonConfirm);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(Cart.getInstance().getItems(), this, this::updateTotalPrice);
        recyclerView.setAdapter(adapter);

        updateTotalPrice();

        confirmButton.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });

        ImageButton backToStore = findViewById(R.id.button_back_to_store);
        backToStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this, StoreActivity.class);
                startActivity(intent);
            }
        });

    }

    private void updateTotalPrice() {
        double total = Cart.getInstance().getTotalPrice();
        totalPriceText.setText("Total: $" + String.format("%.2f", total));
    }
}
