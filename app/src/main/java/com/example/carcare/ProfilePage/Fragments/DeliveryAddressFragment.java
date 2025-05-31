package com.example.carcare.ProfilePage.Fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import com.example.carcare.ProfilePage.address.AddEditAddressActivity; // Yeni Activity'miz
import com.example.carcare.R;
import com.example.carcare.models.AddressModel; // YENİ MODEL SINIFI
import com.example.carcare.adapters.AddressAdapter; // YENİ ADAPTER SINIFI
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeliveryAddressFragment extends Fragment implements AddressAdapter.AddressInteractionListener {

    private static final String TAG = "DeliveryAddressFrag";

    private RecyclerView recyclerViewDeliveryAddresses;
    private AddressAdapter addressAdapter;
    private List<AddressModel> addressList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Button btnAddNewAddressPage; // Layout'taki butona göre ID'yi güncelleyin
    private LinearLayout layoutNoAddress;
    private Button btnAddFirstAddressPage; // Layout'taki butona göre ID'yi güncelleyin

    public DeliveryAddressFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery_address, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewDeliveryAddresses = view.findViewById(R.id.recycler_view_delivery_addresses); // XML'inize ekleyin
        btnAddNewAddressPage = view.findViewById(R.id.btn_add_new_delivery_address);
        layoutNoAddress = view.findViewById(R.id.layout_no_delivery_address);
        btnAddFirstAddressPage = view.findViewById(R.id.btn_add_first_delivery_address);

        addressList = new ArrayList<>();
        addressAdapter = new AddressAdapter(getContext(), addressList, this); // 'this' listener için

        recyclerViewDeliveryAddresses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDeliveryAddresses.setAdapter(addressAdapter);

        View.OnClickListener addAddressListener = v -> {
            Intent intent = new Intent(getActivity(), AddEditAddressActivity.class);
            intent.putExtra("address_type", "delivery");
            startActivity(intent);
        };

        btnAddNewAddressPage.setOnClickListener(addAddressListener);
        btnAddFirstAddressPage.setOnClickListener(addAddressListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAddresses(); // Sayfa her göründüğünde adresleri yeniden yükle (yeni eklenenleri görmek için)
    }

    private void loadAddresses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            layoutNoAddress.setVisibility(View.VISIBLE);
            recyclerViewDeliveryAddresses.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Adresleri görmek için giriş yapın.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("deliveryAddresses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        addressList.clear(); // Önceki listeyi temizle
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                AddressModel address = document.toObject(AddressModel.class);
                                address.setDocumentId(document.getId()); // Firestore doküman ID'sini modele ekle
                                addressList.add(address);
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                        }
                        addressAdapter.notifyDataSetChanged();

                        if (addressList.isEmpty()) {
                            layoutNoAddress.setVisibility(View.VISIBLE);
                            recyclerViewDeliveryAddresses.setVisibility(View.GONE);
                            btnAddNewAddressPage.setVisibility(View.GONE); // Adres yoksa bu butonu gizle
                        } else {
                            layoutNoAddress.setVisibility(View.GONE);
                            recyclerViewDeliveryAddresses.setVisibility(View.VISIBLE);
                            btnAddNewAddressPage.setVisibility(View.VISIBLE); // Adres varsa bu butonu göster
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(getContext(), "Adresler yüklenirken hata oluştu.", Toast.LENGTH_SHORT).show();
                        layoutNoAddress.setVisibility(View.VISIBLE);
                        recyclerViewDeliveryAddresses.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onEditAddress(AddressModel address) {
        Intent intent = new Intent(getActivity(), AddEditAddressActivity.class);
        intent.putExtra("address_type", "delivery");
        intent.putExtra("address_id_to_edit", address.getDocumentId());
        // Düzenleme modunda diğer adres bilgilerini de gönderebilirsiniz, ama ID yeterli, oradan çekilecek.
        startActivity(intent);
    }

    @Override
    public void onDeleteAddress(AddressModel address) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || address.getDocumentId() == null) {
            Toast.makeText(getContext(), "Adres silinemedi.", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Adresi Sil")
                .setMessage(address.getTitle() + " başlıklı adresi silmek istediğinizden emin misiniz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    db.collection("users").document(currentUser.getUid())
                            .collection("deliveryAddresses").document(address.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Adres başarıyla silindi.", Toast.LENGTH_SHORT).show();
                                loadAddresses(); // Listeyi yenile
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Adres silinirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("İptal", null)
                .show();
    }
}