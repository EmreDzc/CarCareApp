package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_main.xml dosyanızı setContentView ile bağlayın
        setContentView(R.layout.activity_main);

        // SettingsActivity'ye geçiş yapacak butonu tanımlıyoruz
        Button btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnOpenSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }
}
