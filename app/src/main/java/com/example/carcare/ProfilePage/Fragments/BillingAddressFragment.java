package com.example.carcare.ProfilePage.Fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.carcare.ProfilePage.address.AddEditAddressActivity; // AddEditAddressActivity'ye import
import com.example.carcare.R;
import com.example.carcare.models.AddressModel;
import com.example.carcare.adapters.AddressAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BillingAddressFragment extends Fragment implements AddressAdapter.AddressInteractionListener {

    private static final String TAG = "BillingAddressFrag";

    private RecyclerView recyclerViewBillingAddresses;
    private AddressAdapter addressAdapter;
    private List<AddressModel> addressList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Button btnAddNewAddressPage;
    private LinearLayout layoutNoAddress;
    private Button btnAddFirstAddressPage;

    public BillingAddressFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_billing_address, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewBillingAddresses = view.findViewById(R.id.recycler_view_billing_addresses); // XML'deki yeni ID
        btnAddNewAddressPage = view.findViewById(R.id.btn_add_new_billing_address);
        layoutNoAddress = view.findViewById(R.id.layout_no_billing_address);
        btnAddFirstAddressPage = view.findViewById(R.id.btn_add_first_billing_address);

        addressList = new ArrayList<>();
        // AddressAdapter'ı bu fragment için de kullanıyoruz, listener olarak 'this' veriyoruz
        addressAdapter = new AddressAdapter(getContext(), addressList, this);

        recyclerViewBillingAddresses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewBillingAddresses.setAdapter(addressAdapter);

        // Adres Ekle butonlarına tıklama olayı
        View.OnClickListener addAddressListener = v -> {
            Intent intent = new Intent(getActivity(), AddEditAddressActivity.class);
            intent.putExtra("address_type", "billing"); // Adres tipini "billing" olarak gönder
            startActivity(intent);
        };

        btnAddNewAddressPage.setOnClickListener(addAddressListener);
        btnAddFirstAddressPage.setOnClickListener(addAddressListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAddresses(); // Sayfa her göründüğünde adresleri yeniden yükle
    }

    private void loadAddresses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            layoutNoAddress.setVisibility(View.VISIBLE);
            recyclerViewBillingAddresses.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Log in to see addresses.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        // Firestore'da fatura adreslerinin olduğu koleksiyon yolu
        db.collection("users").document(userId).collection("billingAddresses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        addressList.clear();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                AddressModel address = document.toObject(AddressModel.class);
                                address.setDocumentId(document.getId());
                                addressList.add(address);
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                        }
                        addressAdapter.notifyDataSetChanged();

                        if (addressList.isEmpty()) {
                            layoutNoAddress.setVisibility(View.VISIBLE);
                            recyclerViewBillingAddresses.setVisibility(View.GONE);
                            btnAddNewAddressPage.setVisibility(View.GONE);
                        } else {
                            layoutNoAddress.setVisibility(View.GONE);
                            recyclerViewBillingAddresses.setVisibility(View.VISIBLE);
                            btnAddNewAddressPage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.w(TAG, "Error getting billing documents.", task.getException());
                        Toast.makeText(getContext(), "Error loading billing addresses.", Toast.LENGTH_SHORT).show();
                        layoutNoAddress.setVisibility(View.VISIBLE);
                        recyclerViewBillingAddresses.setVisibility(View.GONE);
                    }
                });
    }

    // AddressAdapter.AddressInteractionListener metodları
    @Override
    public void onEditAddress(AddressModel address) {
        Intent intent = new Intent(getActivity(), AddEditAddressActivity.class);
        intent.putExtra("address_type", "billing"); // Adres tipini doğru gönder
        intent.putExtra("address_id_to_edit", address.getDocumentId());
        startActivity(intent);
    }

    @Override
    public void onDeleteAddress(AddressModel address) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || address.getDocumentId() == null) {
            Toast.makeText(getContext(), "Billing address could not be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Billing Address")
                .setMessage(address.getTitle() + " Are you sure you want to delete the billing address titled?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(currentUser.getUid())
                            .collection("billingAddresses").document(address.getDocumentId()) // Doğru koleksiyon yolu
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "The billing address was deleted successfully.", Toast.LENGTH_SHORT).show();
                                loadAddresses(); // Listeyi yenile
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error while deleting billing address: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
