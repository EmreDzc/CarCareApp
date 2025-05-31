package com.example.carcare.ProfilePage;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.carcare.R;

public class SavedCardsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_cards);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Diğer click listener'lar
        findViewById(R.id.btn_add_card).setOnClickListener(v -> {
            // Kart ekleme sayfasına git
        });

        findViewById(R.id.btn_add_first_card).setOnClickListener(v -> {
            // İlk kart ekleme sayfasına git
        });
    }
}