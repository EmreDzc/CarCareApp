package com.example.carcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CarActivity extends AppCompatActivity {

    private TextView tvKilometerValue, tvFuelValue, tvEngineTempValue;
    private TextView tvTPFL, tvTPFR, tvTPRL, tvTPRR;
    private TextView tvOilLevelValue;
    private Button btnOpenSite, btnTrafficFineInquiry, btnMotorVehicleFineInquiry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car);

        // Alt navigasyonu bağla
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_dashboard);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(this, StoreActivity.class));
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapsActivity.class));
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });

        // Değer TextView’lerini bağla
        tvKilometerValue   = findViewById(R.id.tvKilometerValue);
        tvFuelValue        = findViewById(R.id.tvFuelValue);
        tvEngineTempValue  = findViewById(R.id.tvEngineTempValue);
        tvTPFL             = findViewById(R.id.tvTPFL);
        tvTPFR             = findViewById(R.id.tvTPFR);
        tvTPRL             = findViewById(R.id.tvTPRL);
        tvTPRR             = findViewById(R.id.tvTPRR);
        tvOilLevelValue    = findViewById(R.id.tvOilLevelValue);

        // Butonları bağla
        btnOpenSite               = findViewById(R.id.btnOpenSite);
        btnTrafficFineInquiry     = findViewById(R.id.btnTrafficFineInquiry);
        btnMotorVehicleFineInquiry= findViewById(R.id.btnMotorVehicleFineInquiry);

        // Örnek değerleri göster
        int kilometer  = 12560, fuel = 75, engineTemp = 90,
                tpFl = 32, tpFr = 31, tpRl = 30, tpRr = 29, oilLevel = 15;

        tvKilometerValue.setText(kilometer + " km");
        tvFuelValue       .setText(fuel + " %");
        tvEngineTempValue .setText(engineTemp + " °C");
        tvTPFL             .setText(tpFl + " PSI");
        tvTPFR             .setText(tpFr + " PSI");
        tvTPRL             .setText(tpRl + " PSI");
        tvTPRR             .setText(tpRr + " PSI");
        tvOilLevelValue   .setText(oilLevel + " %");

        // Buton tıklamaları — hepsi aynı siteyi açıyor
        Intent gov = new Intent(Intent.ACTION_VIEW, Uri.parse("https://turkiye.gov.tr"));
        btnOpenSite              .setOnClickListener(v -> startActivity(gov));
        btnTrafficFineInquiry    .setOnClickListener(v -> startActivity(gov));
        btnMotorVehicleFineInquiry.setOnClickListener(v -> startActivity(gov));
    }
}
