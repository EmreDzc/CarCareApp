package com.example.carcare.ProfilePage.address;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.carcare.R;
import com.example.carcare.models.AddressModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEditAddressActivity extends AppCompatActivity {

    private static final String TAG = "AddEditAddressActivity";

    private TextInputEditText editTextAddressTitle, editTextProvince, editTextDistrict,
            editTextNeighborhood, editTextStreet, editTextBuildingNo, editTextFloorNo,
            editTextDoorNo, editTextAddressDescription, editTextName, editTextSurname,
            editTextPhone;

    private Button btnSaveAddress;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String addressToEditId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_address);

        Toolbar toolbar = findViewById(R.id.toolbar_add_address);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        addressToEditId = getIntent().getStringExtra("address_id_to_edit");

        initializeViews();

        if (addressToEditId != null) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Address");
            loadAddressForEditing();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Add New Address");
        }

        btnSaveAddress.setOnClickListener(v -> saveAddress());
    }

    private void initializeViews() {
        editTextAddressTitle = findViewById(R.id.edit_text_address_title);
        editTextProvince = findViewById(R.id.edit_text_province);
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
    }

    private void loadAddressForEditing() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || addressToEditId == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("deliveryAddresses")
                .document(addressToEditId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AddressModel address = documentSnapshot.toObject(AddressModel.class);
                        if (address != null) {
                            populateFields(address);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Adres yüklenirken hata", e);
                    Toast.makeText(this, "An error occurred while loading the address.", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFields(AddressModel address) {
        editTextAddressTitle.setText(address.getTitle());
        editTextProvince.setText(address.getProvince());
        editTextDistrict.setText(address.getDistrict());
        editTextNeighborhood.setText(address.getNeighborhood());
        editTextStreet.setText(address.getStreet());
        editTextBuildingNo.setText(address.getBuildingNo());
        editTextFloorNo.setText(address.getFloorNo());
        editTextDoorNo.setText(address.getDoorNo());
        editTextAddressDescription.setText(address.getDescription());
        editTextName.setText(address.getRecipientName());
        editTextSurname.setText(address.getRecipientSurname());
        editTextPhone.setText(address.getRecipientPhone());
    }

    private void saveAddress() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must log in to save an address.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateForm()) {
            return;
        }

        Map<String, Object> addressData = new HashMap<>();
        addressData.put("title", editTextAddressTitle.getText().toString().trim());
        addressData.put("province", editTextProvince.getText().toString().trim());
        addressData.put("district", editTextDistrict.getText().toString().trim());
        addressData.put("neighborhood", editTextNeighborhood.getText().toString().trim());
        addressData.put("street", editTextStreet.getText().toString().trim());
        addressData.put("buildingNo", editTextBuildingNo.getText().toString().trim());
        addressData.put("floorNo", editTextFloorNo.getText().toString().trim());
        addressData.put("doorNo", editTextDoorNo.getText().toString().trim());
        addressData.put("description", editTextAddressDescription.getText().toString().trim());
        addressData.put("recipientName", editTextName.getText().toString().trim());
        addressData.put("recipientSurname", editTextSurname.getText().toString().trim());
        addressData.put("recipientPhone", editTextPhone.getText().toString().trim());
        addressData.put("addressType", "delivery"); // Teslimat adresi
        addressData.put("isDefaultAddress", false); // Yeni adres varsayılan değil

        String collectionPath = "users/" + user.getUid() + "/deliveryAddresses";

        if (addressToEditId != null) {
            // Düzenleme
            db.collection(collectionPath).document(addressToEditId)
                    .set(addressData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddEditAddressActivity.this, "Address updated successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditAddressActivity.this, "Error while updating address: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating address", e);
                    });
        } else {
            // Yeni ekleme
            db.collection(collectionPath)
                    .add(addressData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditAddressActivity.this, "Address saved successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditAddressActivity.this, "Error while saving address: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error saving address", e);
                    });
        }
    }

    private boolean validateForm() {
        boolean valid = true;

        if (TextUtils.isEmpty(editTextAddressTitle.getText())) {
            editTextAddressTitle.setError("Address header required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextProvince.getText())) {
            editTextProvince.setError("Province required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextDistrict.getText())) {
            editTextDistrict.setError("District required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextNeighborhood.getText())) {
            editTextNeighborhood.setError("Neighborhood required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextStreet.getText())) {
            editTextStreet.setError("Street/Avenue required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextBuildingNo.getText())) {
            editTextBuildingNo.setError("House number required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextName.getText())) {
            editTextName.setError("Name required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextSurname.getText())) {
            editTextSurname.setError("Surname required");
            valid = false;
        }

        if (TextUtils.isEmpty(editTextPhone.getText())) {
            editTextPhone.setError("Phone number required");
            valid = false;
        }

        return valid;
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
