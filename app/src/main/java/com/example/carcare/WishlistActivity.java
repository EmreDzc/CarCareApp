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

import com.example.carcare.adapters.WishlistAdapter;
import com.example.carcare.models.Wishlist; // Güncellenmiş Wishlist modelini import ettiğinizden emin olun
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;


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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewWishlist);
        progressBar = findViewById(R.id.progressBar);
        emptyMessage = findViewById(R.id.emptyMessage);
        backButton = findViewById(R.id.btn_back);

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        wishlistItems = new ArrayList<>();
        adapter = new WishlistAdapter(this, wishlistItems);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showEmptyMessage("Lütfen önce giriş yapın.");
            return;
        }
        showLoading(true);

        db.collection("users").document(user.getUid())
                .collection("favorites")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showLoading(false);
                        showEmptyMessage("Favori listeniz boş.");
                        wishlistItems.clear(); // Önceki verileri temizle
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    List<Wishlist> newWishlistItems = new ArrayList<>();
                    Set<String> processedProductIds = new HashSet<>();
                    List<String> duplicateFavoriteDocsToDelete = new ArrayList<>();
                    List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>> productTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot favDoc : queryDocumentSnapshots) {
                        String productId = favDoc.getString("productId");
                        String favoriteDocId = favDoc.getId(); // Favori belgesinin kendi ID'si

                        if (productId == null || productId.isEmpty()) {
                            Log.w(TAG, "Favori öğesi geçersiz productId içeriyor: " + favoriteDocId);
                            // Bu hatalı favori kaydını silmeyi düşünebilirsiniz.
                            // duplicateFavoriteDocsToDelete.add(favoriteDocId);
                            continue;
                        }

                        if (processedProductIds.contains(productId)) {
                            Log.d(TAG, "Duplicate favorite entry found for productId: " + productId + ", docId: " + favoriteDocId + ". Marking for deletion.");
                            duplicateFavoriteDocsToDelete.add(favoriteDocId);
                            continue;
                        }
                        processedProductIds.add(productId);

                        // Ürün bilgilerini productID kullanarak products koleksiyonundan al
                        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> productTask =
                                db.collection("products").document(productId).get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Wishlist item = new Wishlist();
                                                item.setId(favoriteDocId); // Favori belgesinin ID'si, silme işlemi için önemli
                                                item.setUserId(user.getUid());
                                                item.setProductId(productId);
                                                item.setProductName(productDoc.getString("name"));
                                                // Resim Base64 olarak alınıyor
                                                item.setProductImageBase64(productDoc.getString("imageBase64"));
                                                Double price = productDoc.getDouble("price");
                                                item.setProductPrice(price != null ? price : 0.0);
                                                item.setAddedAt(favDoc.getDate("addedAt"));
                                                newWishlistItems.add(item);
                                            } else {
                                                Log.w(TAG, "Favorilerde listelenen ürün bulunamadı (products): " + productId + ". Bu favori kaydı siliniyor.");
                                                // Eğer ürün silinmişse, bu favori kaydını da sil.
                                                duplicateFavoriteDocsToDelete.add(favoriteDocId);
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Ürün detayı alınırken hata: " + productId, e));
                        productTasks.add(productTask);
                    }

                    // Tüm ürün getirme işlemleri tamamlandığında UI'ı güncelle
                    com.google.android.gms.tasks.Tasks.whenAllComplete(productTasks)
                            .addOnCompleteListener(allTasks -> {
                                showLoading(false);
                                wishlistItems.clear();
                                wishlistItems.addAll(newWishlistItems);
                                adapter.notifyDataSetChanged(); // Adapter'ı güncelle

                                if (wishlistItems.isEmpty() && queryDocumentSnapshots.size() > 0 && duplicateFavoriteDocsToDelete.size() == queryDocumentSnapshots.size()) {
                                    // Tüm favoriler ya duplikeydi ya da karşılık gelen ürün yoktu
                                    showEmptyMessage("Favori listenizdeki ürünler artık mevcut değil veya hatalı kayıtlar temizlendi.");
                                } else if (wishlistItems.isEmpty()) {
                                    showEmptyMessage("Favori listeniz boş.");
                                } else {
                                    emptyMessage.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                }

                                if (!duplicateFavoriteDocsToDelete.isEmpty()) {
                                    deleteFavoriteRecords(user.getUid(), duplicateFavoriteDocsToDelete);
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyMessage("Favoriler yüklenirken bir hata oluştu.");
                    Log.e(TAG, "Favoriler yüklenemedi", e);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFavoriteRecords(String userId, List<String> documentIdsToDelete) {
        if (documentIdsToDelete.isEmpty()) return;

        WriteBatch batch = db.batch();
        for (String docId : documentIdsToDelete) {
            Log.d(TAG, "Batch delete için favori kaydı ekleniyor: " + docId);
            batch.delete(db.collection("users").document(userId).collection("favorites").document(docId));
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, documentIdsToDelete.size() + " adet favori kaydı başarıyla silindi."))
                .addOnFailureListener(e -> Log.e(TAG, "Favori kayıtları silinirken hata", e));
    }


    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            emptyMessage.setVisibility(View.GONE);
        }
    }

    private void showEmptyMessage(String message) {
        emptyMessage.setText(message);
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }
}