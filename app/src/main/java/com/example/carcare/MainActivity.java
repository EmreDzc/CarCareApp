package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Eğer kullanıcı giriş yapmamışsa, LoginActivity'ye yönlendir.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Kullanıcı giriş yapmışsa MainActivity layout'unu yükle.
        setContentView(R.layout.activity_main);

        // "Open Settings" butonunu tanımla ve SettingsActivity'ye yönlendir.
        Button btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnOpenSettings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // "Notifications" butonunu tanımla ve NotificationActivity'ye yönlendir.
        Button btnGoToNotification = findViewById(R.id.btn_go_to_notification);
        btnGoToNotification.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
    }
}
