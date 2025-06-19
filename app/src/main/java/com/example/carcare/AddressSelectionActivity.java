package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.adapters.AddressSelectionAdapter;
import com.example.carcare.models.AddressModel;
import com.example.carcare.ProfilePage.address.AddEditAddressActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddressSelectionActivity extends AppCompatActivity implements AddressSelectionAdapter.OnAddressSelectedListener {

    private static final String TAG = "AddressSelectionActivity";
    private static final int REQUEST_ADD_ADDRESS = 1003;

    private RecyclerView recyclerViewAddresses;
    private LinearLayout layoutNoAddresses;
    private Button buttonAddNewAddress;
    private AddressSelectionAdapter addressAdapter;
    private List<AddressModel> addressList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedAddressId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_selection);

        // Toolbar ayarla
        Toolbar toolbar = findViewById(R.id.toolbar_address_selection);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Address");
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Mevcut seçili adres ID'sini al
        selectedAddressId = getIntent().getStringExtra("selected_address_id");

        initializeViews();
        setupRecyclerView();
        loadAddresses();
    }

    private void initializeViews() {
        recyclerViewAddresses = findViewById(R.id.recycler_view_addresses);
        layoutNoAddresses = findViewById(R.id.layout_no_addresses);
        buttonAddNewAddress = findViewById(R.id.button_add_new_address);

        buttonAddNewAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditAddressActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ADDRESS);
        });
    }

    private void setupRecyclerView() {
        addressList = new ArrayList<>();
        addressAdapter = new AddressSelectionAdapter(addressList, selectedAddressId, this);
        recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAddresses.setAdapter(addressAdapter);
    }

    private void loadAddresses() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoAddressesState();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("deliveryAddresses")
                .orderBy("title")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    addressList.clear();
                    List<AddressModel> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AddressModel address = document.toObject(AddressModel.class);
                        address.setDocumentId(document.getId());
                        tempList.add(address);
                    }

                    // Varsayılan adresleri önce sırala
                    tempList.sort((a, b) -> {
                        if (a.isDefaultAddress() && !b.isDefaultAddress()) return -1;
                        if (!a.isDefaultAddress() && b.isDefaultAddress()) return 1;
                        return a.getTitle().compareTo(b.getTitle());
                    });

                    addressList.addAll(tempList);
                    addressAdapter.notifyDataSetChanged();
                    updateUIVisibility();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading addresses", e);
                    Toast.makeText(this, "An error occurred while loading addresses.", Toast.LENGTH_SHORT).show();
                    showNoAddressesState();
                });
    }

    private void updateUIVisibility() {
        if (addressList.isEmpty()) {
            showNoAddressesState();
        } else {
            layoutNoAddresses.setVisibility(View.GONE);
            recyclerViewAddresses.setVisibility(View.VISIBLE);
        }
    }

    private void showNoAddressesState() {
        layoutNoAddresses.setVisibility(View.VISIBLE);
        recyclerViewAddresses.setVisibility(View.GONE);
    }

    @Override
    public void onAddressSelected(AddressModel address) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_address_id", address.getDocumentId());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_ADDRESS && resultCode == RESULT_OK) {
            loadAddresses();
        }
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