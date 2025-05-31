package com.example.carcare.ProfilePage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.carcare.CarActivity;
import com.example.carcare.LoginActivity;
import com.example.carcare.MapsActivity;
import com.example.carcare.NotificationActivity;
import com.example.carcare.OrderHistoryActivity;
import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.WishlistActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Tema tercihini kontrol et ve uygula
        SharedPreferences themePref = newBase.getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePref.getBoolean("isDarkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "ProfileActivity created");
        setupBottomNavigation();
        setupCardClickListeners();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(ProfileActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(ProfileActivity.this, StoreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(ProfileActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    startActivity(new Intent(ProfileActivity.this, NotificationActivity.class));
                } else {
                    Toast.makeText(ProfileActivity.this, "Bildirimleri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true; // Zaten buradayız
            }
            return false;
        });
    }

    private void setupCardClickListeners() {
        // Hesap Ayarları - DÜZELTME
        MaterialCardView cardAccountSettings = findViewById(R.id.card_account_settings);
        cardAccountSettings.setOnClickListener(v -> {
            Log.d(TAG, "Hesap Ayarları kartına tıklandı");
            try {
                Intent intent = new Intent(ProfileActivity.this, AccountSettingsActivity.class);
                startActivity(intent);
                Log.d(TAG, "AccountSettingsActivity başlatıldı");
            } catch (Exception e) {
                Log.e(TAG, "AccountSettingsActivity başlatılamadı", e);
                Toast.makeText(this, "Sayfa açılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Siparişlerim
        MaterialCardView cardOrders = findViewById(R.id.card_orders);
        cardOrders.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, "Siparişleri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            }
        });

        // Adreslerim
        MaterialCardView cardAddresses = findViewById(R.id.card_addresses);
        cardAddresses.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Geçici olarak toast göster - AddressActivity henüz hazır değil
                Toast.makeText(ProfileActivity.this, "Adresler sayfası yakında eklenecek", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileActivity.this, "Adresleri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            }
        });

        // Favorilerim
        MaterialCardView cardFavorites = findViewById(R.id.card_favorites);
        cardFavorites.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(ProfileActivity.this, WishlistActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(ProfileActivity.this, "Favorileri görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            }
        });

        // Kayıtlı Kartlarım
        MaterialCardView cardSavedCards = findViewById(R.id.card_saved_cards);
        cardSavedCards.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Geçici olarak toast göster - SavedCardsActivity henüz hazır değil
                Toast.makeText(ProfileActivity.this, "Kayıtlı kartlar sayfası yakında eklenecek", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileActivity.this, "Kayıtlı kartları görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            }
        });

        // Çıkış Yap - Direkt logout (eski methodunuz gibi)
        MaterialCardView cardLogout = findViewById(R.id.card_logout);
        cardLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        // Eski logout metodunuzun aynısı
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(ProfileActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}