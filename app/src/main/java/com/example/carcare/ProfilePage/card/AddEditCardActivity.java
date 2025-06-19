package com.example.carcare.ProfilePage.card;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.carcare.R;
import com.example.carcare.models.CardModel;
import com.example.carcare.utils.CardSecurityUtils;
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
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cardToEditId = getIntent().getStringExtra("card_id_to_edit");

        initializeUI();
        setupExpiryDateDropDowns();
        setupCardNumberFormatting();

        if (cardToEditId != null) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Card");
            loadCardForEditing();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Add New Card");
        }

        btnSaveCardForm.setOnClickListener(v -> saveCard());
    }

    private void initializeUI() {
        etCardName = findViewById(R.id.et_card_name);
        etCardHolderName = findViewById(R.id.et_card_holder_name);
        etCardNumber = findViewById(R.id.et_card_number);
        tilCardNumber = findViewById(R.id.til_card_number); // XML'de tanımlanan ID
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
            private boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String original = s.toString();
                String digitsOnly = original.replaceAll("[^\\d]", "");

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digitsOnly.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digitsOnly.charAt(i));
                }

                if (!original.equals(formatted.toString())) {
                    s.replace(0, s.length(), formatted.toString());
                }
                isFormatting = false;
            }
        });
    }

    private void loadCardForEditing() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || cardToEditId == null) {
            Toast.makeText(this, "Card could not be loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("savedCards").document(cardToEditId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CardModel card = documentSnapshot.toObject(CardModel.class);
                        if (card != null) {
                            etCardName.setText(card.getCardName());
                            etCardHolderName.setText(card.getCardHolderName());

                            // DİKKAT: Ham kart numarasını gösteremiyoruz çünkü hashlenmiş
                            // Sadece son 4 haneyi göster
                            etCardNumber.setText("**** **** **** " + card.getLastFourDigits());
                            etCardNumber.setEnabled(false); // Düzenlemede kart numarası değiştirilemez
                            etCardNumber.setHint("For security reasons, the card number cannot be changed.");

                            actvExpiryMonth.setText(card.getExpiryMonth(), false);
                            actvExpiryYear.setText(card.getExpiryYear(), false);

                            // CVV'yi düzenlemede tekrar girmesini iste
                            etCardCvv.setText("");
                            etCardCvv.setHint("Re-enter CVV for security");

                            cbMasterpass.setChecked(card.isMasterpassOptIn());

                            // Kullanıcıyı bilgilendir
                            Toast.makeText(AddEditCardActivity.this,
                                    "For security reasons, card number and CVV must be entered again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(AddEditCardActivity.this, "Card not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditCardActivity.this, "Error loading card: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading card", e);
                });
    }

    private void saveCard() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must log in to register a card.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Please fill in all mandatory fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Düzenleme modunda kart numarası değiştirilemez kontrolü
        if (cardToEditId != null && cardNumberRaw.contains("*")) {
            Toast.makeText(this, "The card number cannot be changed in edit mode.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cardNumberRaw.length() < 13 || cardNumberRaw.length() > 19) {
            if (tilCardNumber != null) {
                tilCardNumber.setError("Invalid card number length");
            }
            return;
        } else {
            if (tilCardNumber != null) {
                tilCardNumber.setError(null);
            }
        }

        // GÜVENLİK: Kart bilgilerini hashle
        String hashedCardNumber = CardSecurityUtils.hashCardNumber(cardNumberRaw);
        String lastFourDigits = CardSecurityUtils.getLastFourDigits(cardNumberRaw);
        String maskedCardNumber = CardSecurityUtils.createMaskedCardNumber(lastFourDigits);
        String hashedCvv = CardSecurityUtils.hashCvv(cvv); // CVV'yi de hashle
        String cardType = detectCardType(cardNumberRaw);

        Map<String, Object> cardData = new HashMap<>();
        cardData.put("cardName", cardName);
        cardData.put("cardHolderName", cardHolderName);
        cardData.put("hashedCardNumber", hashedCardNumber); // YENİ: Hashlenmiş numara
        cardData.put("maskedCardNumber", maskedCardNumber); // Görüntüleme için
        cardData.put("lastFourDigits", lastFourDigits);
        cardData.put("expiryMonth", expiryMonth);
        cardData.put("expiryYear", expiryYear);
        cardData.put("cardType", cardType);
        cardData.put("hashedCvv", hashedCvv); // YENİ: Hashlenmiş CVV
        cardData.put("masterpassOptIn", masterpassOptIn);
        cardData.put("isDefault", false);

        String collectionPath = "users/" + currentUser.getUid() + "/savedCards";

        if (cardToEditId != null) {
            // Düzenleme
            db.collection(collectionPath).document(cardToEditId)
                    .set(cardData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddEditCardActivity.this, "Card updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditCardActivity.this, "Error while updating card: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating card", e);
                    });
        } else {
            // Yeni ekleme
            db.collection(collectionPath)
                    .add(cardData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditCardActivity.this, "Card registered successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditCardActivity.this, "Error while registering card: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error saving card", e);
                    });
        }

        // Güvenlik: Hassas verileri bellekten temizle
        cardNumberRaw = null;
        cvv = null;
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber.startsWith("4")) {
            return "VISA";
        } else if (cardNumber.startsWith("51") || cardNumber.startsWith("52") ||
                cardNumber.startsWith("53") || cardNumber.startsWith("54") ||
                cardNumber.startsWith("55")) {
            return "MASTERCARD";
        }
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
