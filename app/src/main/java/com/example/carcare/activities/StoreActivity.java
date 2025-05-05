package com.example.carcare.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.adapters.ProductAdapter;
import com.example.carcare.models.Product;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.view.MenuItem;
import com.example.carcare.CarActivity;
import com.example.carcare.MapsActivity;
import com.example.carcare.NotificationActivity;
import com.example.carcare.SettingsActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;





import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);


        recyclerView = findViewById(R.id.recyclerViewProducts);

        EditText searchBar = findViewById(R.id.search_bar);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        products = getProductList();
        adapter = new ProductAdapter(this, products);
        recyclerView.setAdapter(adapter);

        ImageButton cartButton = findViewById(R.id.cart_button);
        cartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StoreActivity.this, CartActivity.class);
                startActivity(intent);
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);


        bottomNavigationView.setSelectedItemId(R.id.nav_store);


        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(StoreActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                // We're already here
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(StoreActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(StoreActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(StoreActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });


    }

    private List<Product> getProductList() {
        List<Product> list = new ArrayList<>();
        list.add(new Product("1", "CRD Deluxe Tire Plug Kit", "A kit for tire problems", 7.25, R.drawable.tire_plug_kit));
        list.add(new Product("2", "Goodyear 215/60 R16", "Summer tires", 108.80, R.drawable.goodyear_tire));
        list.add(new Product("3", "Oyunzu Car Accessory", "Atatürk portrait", 3.50, R.drawable.ataturk_portrait));
        list.add(new Product("4", "Mann W 712/94", "Oil filter", 12.99, R.drawable.oil_filter));
        return list;
    }
}
