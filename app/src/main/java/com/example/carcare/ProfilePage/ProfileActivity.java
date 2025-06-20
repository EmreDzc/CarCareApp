package com.example.carcare.ProfilePage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.carcare.AIAssistantActivity; // Yeni eklediğiniz activity'nin importu

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.carcare.CarActivity;
import com.example.carcare.LoginActivity;
import com.example.carcare.MapsActivity;
import com.example.carcare.NotificationActivity;
import com.example.carcare.OrderHistoryActivity;
import com.example.carcare.ProfilePage.address.AddressActivity;
import com.example.carcare.ProfilePage.card.SavedCardsActivity;
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
                    Toast.makeText(ProfileActivity.this, "You must be logged in to see notifications.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }

    private void setupCardClickListeners() {


        // Hesap Ayarları
        MaterialCardView cardAccountSettings = findViewById(R.id.card_account_settings);
        cardAccountSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ProfileActivity.this, AccountSettingsActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Page could not be opened: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                Toast.makeText(ProfileActivity.this, "You must log in to see orders..", Toast.LENGTH_SHORT).show();
            }
        });

        // Adreslerim - DÜZELTME: AddressActivity'yi başlat
        MaterialCardView cardAddresses = findViewById(R.id.card_addresses);
        cardAddresses.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    // Artık Toast yerine AddressActivity'yi başlatıyoruz
                    Intent intent = new Intent(ProfileActivity.this, AddressActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Addresses page could not be opened: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ProfileActivity.this, "You must log in to see the addresses.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ProfileActivity.this, "You must be logged in to see favorites..", Toast.LENGTH_SHORT).show();
            }
        });

        // Kayıtlı Kartlarım - DÜZELTME: SavedCardsActivity'yi başlat (veya geçici olarak Toast)
        MaterialCardView cardSavedCards = findViewById(R.id.card_saved_cards);
        cardSavedCards.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                try {
                    // SavedCardsActivity hazır olduğunda bu satırı aktif edin:
                    Intent intent = new Intent(ProfileActivity.this, SavedCardsActivity.class);
                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(this, "The saved cards page could not be opened.: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ProfileActivity.this, "You must log in to see saved cards.", Toast.LENGTH_SHORT).show();
            }
        });

        // YENİ EKLENECEK KISIM:
        MaterialCardView cardAiAssistant = findViewById(R.id.card_ai_assistant);
        cardAiAssistant.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ProfileActivity.this, AIAssistantActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Assistant page could not be opened: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Çıkış Yap
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