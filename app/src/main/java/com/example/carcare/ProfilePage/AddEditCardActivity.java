package com.example.carcare.ProfilePage;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.carcare.R;
import com.example.carcare.models.CardModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddEditCardActivity extends AppCompatActivity {

    private static final String TAG = "AddEditCardActivity";

    private TextInputEditText etCardName, etCardHolderName, etCardNumber, etCardCvv;
    private AutoCompleteTextView actvExpiryMonth, actvExpiryYear;
    private CheckBox cbMasterpass;
    private Button btnSaveCardForm;
    private TextInputLayout tilCardNumber; // Kart numarasını formatlamak için

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String cardToEditId; // Düzenleme modu için

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_card);

        Toolbar toolbar = findViewById(R.id.toolbar_add_card);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Başlık XML'den geliyor veya düzenleme moduna göre ayarlanacak
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cardToEditId = getIntent().getStringExtra("card_id_to_edit");

        initializeUI();
        setupExpiryDateDropDowns();
        setupCardNumberFormatting();

        if (cardToEditId != null) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Kartı Düzenle");
            loadCardForEditing();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Yeni Kart Ekle");
        }


        btnSaveCardForm.setOnClickListener(v -> saveCard());

        // CVV yardım ikonu
        TextInputLayout tilCvv = findViewById(R.id.et_card_cvv).getRootView().findViewById(R.id.et_card_cvv); // TextInputLayout'ı almak için
        // Bu satır doğru değil, TextInputLayout'ın ID'si olmalı. XML'de CVV'nin TextInputLayout'ına ID verin.
        // Örnek: android:id="@+id/til_card_cvv"
        // tilCvv = findViewById(R.id.til_card_cvv);
        // if (tilCvv != null) {
        //     tilCvv.setEndIconOnClickListener(iconView ->
        //             Toast.makeText(AddEditCardActivity.this, "CVV, kartınızın arkasındaki 3 haneli güvenlik kodudur.", Toast.LENGTH_LONG).show()
        //     );
        // }
    }

    private void initializeUI() {
        etCardName = findViewById(R.id.et_card_name);
        etCardHolderName = findViewById(R.id.et_card_holder_name);
        etCardNumber = findViewById(R.id.et_card_number);
        tilCardNumber = (TextInputLayout) etCardNumber.getParent().getParent(); // TextInputLayout'ı al
        actvExpiryMonth = findViewById(R.id.actv_expiry_month);
        actvExpiryYear = findViewById(R.id.actv_expiry_year);
        etCardCvv = findViewById(R.id.et_card_cvv);
        cbMasterpass = findViewById(R.id.cb_masterpass);
        btnSaveCardForm = findViewById(R.id.btn_save_card_form);
    }

    private void setupExpiryDateDropDowns() {
        // Aylar
        ArrayList<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            months.add(String.format(Locale.getDefault(), "%02d", i));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, months);
        actvExpiryMonth.setAdapter(monthAdapter);

        // Yıllar (örneğin, mevcut yıldan sonraki 10 yıl)
        ArrayList<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 10; i++) {
            years.add(String.valueOf(currentYear + i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, years);
        actvExpiryYear.setAdapter(yearAdapter);
    }

    private void setupCardNumberFormatting() {
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private static final char SPACE_CHAR = ' ';
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Her 4 karakterden sonra boşluk ekle
                if (s.length() > 0 && (s.length() % 5) == 0) {
                    char c = s.charAt(s.length() - 1);
                    // Eğer son eklenen karakter boşluk değilse ve bir önceki de boşluk değilse
                    if (SPACE_CHAR == c || Character.isDigit(c) && TextUtils.split(s.toString(), String.valueOf(SPACE_CHAR)).length <= 3) {
                        // Kullanıcının boşluk girmesini engelleme
                    } else {
                        s.insert(s.length() - 1, String.valueOf(SPACE_CHAR));
                    }
                }
            }
        });
    }


    private void loadCardForEditing() {
        // ... (SavedCardsActivity'deki loadAddressForEditing'e benzer şekilde Firestore'dan kartı çekip alanları doldurun)
        // Örnek:
        // db.collection("users").document(userId).collection("savedCards").document(cardToEditId).get()
        //    .addOnSuccessListener(documentSnapshot -> {
        //        if (documentSnapshot.exists()) {
        //            CardModel card = documentSnapshot.toObject(CardModel.class);
        //            etCardName.setText(card.getCardName());
        //            etCardHolderName.setText(card.getCardHolderName());
        //            etCardNumber.setText(card.getUnmaskedCardNumberForEdit()); // Gerçek numarayı düzenleme için göster
        //            actvExpiryMonth.setText(card.getExpiryMonth(), false);
        //            actvExpiryYear.setText(card.getExpiryYear(), false);
        //            // CVV genellikle tekrar girilir, yüklenmez.
        //            cbMasterpass.setChecked(card.isMasterpassOptIn());
        //        }
        //    });
        Toast.makeText(this, "Kart düzenleme henüz implemente edilmedi.", Toast.LENGTH_SHORT).show();
    }

    private void saveCard() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Kart kaydetmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }

        String cardName = etCardName.getText().toString().trim();
        String cardHolderName = etCardHolderName.getText().toString().trim();
        String cardNumberRaw = etCardNumber.getText().toString().replace(" ", ""); // Boşlukları kaldır
        String expiryMonth = actvExpiryMonth.getText().toString().trim();
        String expiryYear = actvExpiryYear.getText().toString().trim();
        String cvv = etCardCvv.getText().toString().trim();
        boolean masterpassOptIn = cbMasterpass.isChecked();

        if (TextUtils.isEmpty(cardName) || TextUtils.isEmpty(cardHolderName) ||
                TextUtils.isEmpty(cardNumberRaw) || TextUtils.isEmpty(expiryMonth) ||
                TextUtils.isEmpty(expiryYear) || TextUtils.isEmpty(cvv)) {
            Toast.makeText(this, "Lütfen tüm zorunlu alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cardNumberRaw.length() < 13 || cardNumberRaw.length() > 19) { // Basit bir uzunluk kontrolü
            tilCardNumber.setError("Geçersiz kart numarası uzunluğu");
            return;
        } else {
            tilCardNumber.setError(null);
        }

        String maskedCardNumber = "**** **** **** " + (cardNumberRaw.length() > 4 ? cardNumberRaw.substring(cardNumberRaw.length() - 4) : cardNumberRaw);
        String cardType = detectCardType(cardNumberRaw); // Kart tipini algıla

        Map<String, Object> cardData = new HashMap<>();
        cardData.put("cardName", cardName);
        cardData.put("cardHolderName", cardHolderName);
        cardData.put("maskedCardNumber", maskedCardNumber); // Maskelenmiş numara
        cardData.put("lastFourDigits", (cardNumberRaw.length() > 4 ? cardNumberRaw.substring(cardNumberRaw.length() - 4) : cardNumberRaw));
        cardData.put("expiryMonth", expiryMonth);
        cardData.put("expiryYear", expiryYear);
        cardData.put("cardType", cardType); // Visa, Mastercard vb.
        cardData.put("masterpassOptIn", masterpassOptIn);
        cardData.put("isDefault", false); // Yeni eklenen kart varsayılan olmasın (şimdilik)
        // cardData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        String collectionPath = "users/" + currentUser.getUid() + "/savedCards";

        if (cardToEditId != null) {
            // Düzenleme
            db.collection(collectionPath).document(cardToEditId)
                    .set(cardData) // veya .update(cardData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddEditCardActivity.this, "Kart başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditCardActivity.this, "Kart güncellenirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating card", e);
                    });
        } else {
            // Yeni ekleme
            db.collection(collectionPath)
                    .add(cardData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditCardActivity.this, "Kart başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditCardActivity.this, "Kart kaydedilirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error saving card", e);
                    });
        }
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("51") || cardNumber.startsWith("52") || cardNumber.startsWith("53") || cardNumber.startsWith("54") || cardNumber.startsWith("55")) {
            return "MASTERCARD";
        } // Diğer kart tipleri için regex veya kütüphane kullanılabilir
        return "UNKNOWN";
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}