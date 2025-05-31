package com.example.carcare.ProfilePage;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carcare.R;

public class AddressActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Diğer click listener'lar
        findViewById(R.id.btn_add_address).setOnClickListener(v -> {
            // Adres ekleme sayfasına git
        });

        findViewById(R.id.btn_add_first_address).setOnClickListener(v -> {
            // İlk adres ekleme sayfasına git
        });
    }
}