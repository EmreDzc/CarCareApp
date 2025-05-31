package com.example.carcare.ProfilePage.address; // Paket adınızı güncelleyin

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
// AutoCompleteTextView'leri normal TextInputEditText ile değiştirdik, bu yüzden importu kaldırabilir veya bırakabilirsiniz.
// import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.example.carcare.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEditAddressActivity extends AppCompatActivity {

    private static final String TAG = "AddEditAddressActivity";

    // Harita ile ilgili değişkenler kaldırıldı
    // private GoogleMap mMap;
    // private FusedLocationProviderClient fusedLocationClient;
    // private LatLng currentSelectedLatLng;

    // Form elemanları
    private TextInputEditText editTextProvince, editTextDistrict, editTextNeighborhood, editTextStreet,
            editTextBuildingNo, editTextFloorNo, editTextDoorNo,
            editTextAddressDescription, editTextAddressTitle;
    private TextInputEditText editTextName, editTextSurname, editTextPhone;
    private Button btnSaveAddress;
    private TextInputLayout layoutName, layoutSurname, layoutPhone;
    private View personalInfoTitleView; // Kişisel bilgi başlığını da gizlemek için

    private String addressType; // "delivery" veya "billing"
    private String addressToEditId; // Düzenleme modu için adres ID'si

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_address);

        Toolbar toolbar = findViewById(R.id.toolbar_add_address);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Intent'ten verileri al
        addressType = getIntent().getStringExtra("address_type");
        addressToEditId = getIntent().getStringExtra("address_id_to_edit"); // Düzenleme için

        if (addressType == null) {
            addressType = "delivery"; // Varsayılan
            Log.w(TAG, "Address type not provided, defaulting to delivery.");
        }

        initializeUI();
        setupToolbarTitle(); // Başlığı ayarla

        if (addressToEditId != null) {
            // Düzenleme modu, adresi yükle
            loadAddressForEditing();
        }

        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void initializeUI() {
        editTextAddressTitle = findViewById(R.id.edit_text_address_title);
        editTextProvince = findViewById(R.id.edit_text_province);
        // AutoCompleteTextView'leri TextInputEditText ile değiştirdik, ID'leri aynı
        editTextDistrict = findViewById(R.id.edit_text_district);
        editTextNeighborhood = findViewById(R.id.edit_text_neighborhood);
        editTextStreet = findViewById(R.id.edit_text_street);
        editTextBuildingNo = findViewById(R.id.edit_text_building_no);
        editTextFloorNo = findViewById(R.id.edit_text_floor_no);
        editTextDoorNo = findViewById(R.id.edit_text_door_no);
        editTextAddressDescription = findViewById(R.id.edit_text_address_description);

        editTextName = findViewById(R.id.edit_text_name);
        editTextSurname = findViewById(R.id.edit_text_surname);
        editTextPhone = findViewById(R.id.edit_text_phone);
        btnSaveAddress = findViewById(R.id.btn_save_address);

        layoutName = findViewById(R.id.layout_name);
        layoutSurname = findViewById(R.id.layout_surname);
        layoutPhone = findViewById(R.id.layout_phone);
        personalInfoTitleView = findViewById(R.id.text_view_personal_info_title);

        // Adres tipine göre kişisel bilgi alanlarını göster/gizle
        if ("billing".equalsIgnoreCase(addressType)) {
            layoutName.setVisibility(View.GONE);
            layoutSurname.setVisibility(View.GONE);
            layoutPhone.setVisibility(View.GONE);
            personalInfoTitleView.setVisibility(View.GONE);
        } else { // delivery
            layoutName.setVisibility(View.VISIBLE);
            layoutSurname.setVisibility(View.VISIBLE);
            layoutPhone.setVisibility(View.VISIBLE);
            personalInfoTitleView.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbarTitle() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title;
            if (addressToEditId != null) { // Düzenleme modu
                title = addressType.equals("delivery") ? "Teslimat Adresini Düzenle" : "Fatura Adresini Düzenle";
            } else { // Ekleme modu
                title = addressType.equals("delivery") ? "Teslimat Adresi Ekle" : "Fatura Adresi Ekle";
            }
            getSupportActionBar().setTitle(title);
        }
    }

    private void loadAddressForEditing() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || addressToEditId == null) return;

        String collectionPath = "users/" + currentUser.getUid() +
                (addressType.equals("delivery") ? "/deliveryAddresses" : "/billingAddresses");

        db.collection(collectionPath).document(addressToEditId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editTextAddressTitle.setText(documentSnapshot.getString("title"));
                        editTextProvince.setText(documentSnapshot.getString("province"));
                        editTextDistrict.setText(documentSnapshot.getString("district"));
                        editTextNeighborhood.setText(documentSnapshot.getString("neighborhood"));
                        editTextStreet.setText(documentSnapshot.getString("street"));
                        editTextBuildingNo.setText(documentSnapshot.getString("buildingNo"));
                        editTextFloorNo.setText(documentSnapshot.getString("floorNo"));
                        editTextDoorNo.setText(documentSnapshot.getString("doorNo"));
                        editTextAddressDescription.setText(documentSnapshot.getString("description"));

                        if ("delivery".equalsIgnoreCase(addressType)) {
                            editTextName.setText(documentSnapshot.getString("recipientName"));
                            editTextSurname.setText(documentSnapshot.getString("recipientSurname"));
                            editTextPhone.setText(documentSnapshot.getString("recipientPhone"));
                        }
                    } else {
                        Toast.makeText(this, "Düzenlenecek adres bulunamadı.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Adres yüklenirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading address for edit", e);
                    finish();
                });
    }


    private void saveAddress() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Adres kaydetmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = editTextAddressTitle.getText().toString().trim();
        String province = editTextProvince.getText().toString().trim();
        String district = editTextDistrict.getText().toString().trim(); // Artık normal EditText
        String neighborhood = editTextNeighborhood.getText().toString().trim(); // Artık normal EditText
        String street = editTextStreet.getText().toString().trim();
        String buildingNo = editTextBuildingNo.getText().toString().trim();
        String floorNo = editTextFloorNo.getText().toString().trim();
        String doorNo = editTextDoorNo.getText().toString().trim();
        String description = editTextAddressDescription.getText().toString().trim();

        String name = "";
        String surname = "";
        String phone = "";

        if ("delivery".equalsIgnoreCase(addressType)) {
            name = editTextName.getText().toString().trim();
            surname = editTextSurname.getText().toString().trim();
            phone = editTextPhone.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname) || TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Lütfen ad, soyad ve telefon bilgilerini girin.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(province) || TextUtils.isEmpty(district) ||
                TextUtils.isEmpty(neighborhood) || TextUtils.isEmpty(street) || TextUtils.isEmpty(buildingNo)) {
            Toast.makeText(this, "Lütfen adres başlığı ve zorunlu adres alanlarını doldurun.", Toast.LENGTH_LONG).show();
            return;
        }

        // Harita kaldırıldığı için currentSelectedLatLng kontrolü ve kaydı kaldırıldı.

        Map<String, Object> addressData = new HashMap<>();
        addressData.put("title", title);
        addressData.put("province", province);
        addressData.put("district", district);
        addressData.put("neighborhood", neighborhood);
        addressData.put("street", street);
        addressData.put("buildingNo", buildingNo);
        if (!TextUtils.isEmpty(floorNo)) addressData.put("floorNo", floorNo); // Opsiyonel alanlar
        if (!TextUtils.isEmpty(doorNo)) addressData.put("doorNo", doorNo);   // Opsiyonel alanlar
        if (!TextUtils.isEmpty(description)) addressData.put("description", description); // Opsiyonel alanlar
        addressData.put("addressType", addressType);

        if ("delivery".equalsIgnoreCase(addressType)) {
            addressData.put("recipientName", name);
            addressData.put("recipientSurname", surname);
            addressData.put("recipientPhone", phone);
        }
        // addressData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp()); // Zaman damgası

        String collectionPath = "users/" + currentUser.getUid() +
                (addressType.equals("delivery") ? "/deliveryAddresses" : "/billingAddresses");

        if (addressToEditId != null) {
            // Düzenleme modu: Mevcut dokümanı güncelle
            db.collection(collectionPath).document(addressToEditId)
                    .set(addressData) // set() ile üzerine yaz veya merge() ile birleştir
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddEditAddressActivity.this, "Adres başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditAddressActivity.this, "Adres güncellenirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating address", e);
                    });
        } else {
            // Ekleme modu: Yeni doküman oluştur
            db.collection(collectionPath)
                    .add(addressData) // .add() otomatik ID oluşturur
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditAddressActivity.this, "Adres başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Address saved with ID: " + documentReference.getId());
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditAddressActivity.this, "Adres kaydedilirken hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error saving address", e);
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Geri butonuna basıldığında bir önceki ekrana dön
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}