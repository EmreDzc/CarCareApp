package com.example.carcare.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;


import com.example.carcare.R;
import com.example.carcare.utils.Cart;

public class CheckoutActivity extends AppCompatActivity {

    private EditText inputName, inputCard, inputCVV;
    private Button buttonPurchase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        inputName = findViewById(R.id.inputName);
        inputCard = findViewById(R.id.inputCard);
        inputCVV = findViewById(R.id.inputCVV);
        buttonPurchase = findViewById(R.id.buttonPurchase);

        buttonPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CheckoutActivity.this, StoreActivity.class);
                startActivity(intent);
            }
        });

    }
}
