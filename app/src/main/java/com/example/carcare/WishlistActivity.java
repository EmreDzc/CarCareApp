package com.example.carcare;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.adapters.WishlistAdapter;
import com.example.carcare.models.Wishlist;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WishlistActivity extends AppCompatActivity {
    private static final String TAG = "WishlistActivity";


    private RecyclerView recyclerView;
    private WishlistAdapter adapter;
    private List<Wishlist> wishlistItems;
    private ProgressBar progressBar;
    private TextView emptyMessage;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // View elemanlarını tanımla
        recyclerView = findViewById(R.id.recyclerViewWishlist);
        progressBar = findViewById(R.id.progressBar);
        emptyMessage = findViewById(R.id.emptyMessage);
        backButton = findViewById(R.id.btn_back);

        // Geri butonu
        backButton.setOnClickListener(v -> finish());

        // RecyclerView ayarla
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        wishlistItems = new ArrayList<>();
        adapter = new WishlistAdapter(this, wishlistItems);
        recyclerView.setAdapter(adapter);

        // Wishlist verilerini yükle
        loadWishlist();
    }

    private void loadWishlist() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showEmptyMessage("Lütfen önce giriş yapın");
            return;
        }

        showLoading(true);

        db.collection("users").document(user.getUid())
                .collection("favorites")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    wishlistItems.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyMessage("Favori listeniz boş");
                        return;
                    }

                    // Duplikat kontrolü için Set kullan
                    Set<String> processedProductIds = new HashSet<>();
                    List<String> documentsToDelete = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productId = document.getString("productId");

                        // Eğer bu productId daha önce işlendiyse, fazla olanları sil
                        if (processedProductIds.contains(productId)) {
                            documentsToDelete.add(document.getId());
                            continue;
                        }

                        processedProductIds.add(productId);

                        // Ürün bilgilerini getir
                        db.collection("products").document(productId)
                                .get()
                                .addOnSuccessListener(productDoc -> {
                                    if (productDoc.exists()) {
                                        Wishlist wishlistItem = new Wishlist();
                                        wishlistItem.setId(document.getId());
                                        wishlistItem.setUserId(user.getUid());
                                        wishlistItem.setProductId(productId);
                                        wishlistItem.setProductName(productDoc.getString("name"));
                                        wishlistItem.setProductImageUrl(productDoc.getString("imageUrl"));
                                        wishlistItem.setProductPrice(productDoc.getDouble("price"));
                                        wishlistItem.setAddedAt(document.getDate("addedAt"));

                                        wishlistItems.add(wishlistItem);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }

                    // Duplikat kayıtları sil
                    deleteDuplicateRecords(user.getUid(), documentsToDelete);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyMessage("Favoriler yüklenirken hata oluştu");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteDuplicateRecords(String userId, List<String> documentIds) {
        for (String docId : documentIds) {
            db.collection("users").document(userId)
                    .collection("favorites").document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Duplicate record deleted: " + docId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting duplicate: " + docId, e);
                    });
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
    }

    private void showEmptyMessage(String message) {
        emptyMessage.setText(message);
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sayfa geri geldiğinde wishlist'i yenile
        loadWishlist();
    }
}