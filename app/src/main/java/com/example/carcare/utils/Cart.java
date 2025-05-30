package com.example.carcare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.carcare.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cart {
    private static Cart instance;
    private List<Product> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Sepet değişikliklerini dinlemek için callback
    public interface CartChangeListener {
        void onCartChanged();
    }

    private List<CartChangeListener> listeners = new ArrayList<>();

    private Cart() {
        cartItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Kullanıcı giriş yapmışsa sepeti Firebase'den yükle
        loadCartFromFirebase();
    }

    public static synchronized Cart getInstance() {
        if (instance == null) {
            instance = new Cart();
        }
        return instance;
    }

    public void addCartChangeListener(CartChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeCartChangeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (CartChangeListener listener : listeners) {
            listener.onCartChanged();
        }
    }

    public void clearCartWithoutToast(Context context) {
        // Yerel listeyi temizle
        cartItems.clear();

        // Firebase'den temizle (kullanıcı giriş yapmışsa)
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("cart")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Tüm belgeleri sil - DocumentSnapshot kullan
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            document.getReference().delete();
                        }
                        // Toast mesajı yok
                    });
        } else {
            // Kullanıcı giriş yapmamışsa yerel verileri temizle
            context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            // Toast mesajı yok
        }

        // Değişikliği dinleyicilere bildir
        notifyListeners();
    }

    public void addItem(Product product, Context context) {
        // Ürünü yerel listeye ekle
        cartItems.add(product);

        // Firebase'e kaydet (kullanıcı giriş yapmışsa)
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("addedAt", FieldValue.serverTimestamp());

            db.collection("users").document(user.getUid())
                    .collection("cart").add(item)
                    .addOnSuccessListener(documentReference -> {
                        // Başarılı olduğunda ID'yi güncelle
                        product.setCartItemId(documentReference.getId());
                        Toast.makeText(context, product.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Hata durumunda kullanıcıyı bilgilendir
                        Toast.makeText(context, "Sepete eklenirken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Kullanıcı giriş yapmamışsa yerel olarak kaydet
            saveCartToPrefs(context);
            Toast.makeText(context, product.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
        }

        // Değişikliği dinleyicilere bildir
        notifyListeners();
    }

    public void removeItem(Product product, Context context) {
        // Ürünü yerel listeden kaldır
        cartItems.remove(product);

        // Firebase'den kaldır (kullanıcı giriş yapmışsa)
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && product.getCartItemId() != null) {
            db.collection("users").document(user.getUid())
                    .collection("cart").document(product.getCartItemId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, product.getName() + " sepetten çıkarıldı", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Sepetten çıkarılırken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Kullanıcı giriş yapmamışsa yerel olarak kaydet
            saveCartToPrefs(context);
            Toast.makeText(context, product.getName() + " sepetten çıkarıldı", Toast.LENGTH_SHORT).show();
        }

        // Değişikliği dinleyicilere bildir
        notifyListeners();
    }

    public List<Product> getItems() {
        return cartItems;
    }

    public void clearCart(Context context) {
        // Yerel listeyi temizle
        cartItems.clear();

        // Firebase'den temizle (kullanıcı giriş yapmışsa)
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("cart")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Tüm belgeleri sil - DocumentSnapshot kullan
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            document.getReference().delete();
                        }
                        Toast.makeText(context, "Sepet temizlendi", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Kullanıcı giriş yapmamışsa yerel verileri temizle
            context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            Toast.makeText(context, "Sepet temizlendi", Toast.LENGTH_SHORT).show();
        }

        // Değişikliği dinleyicilere bildir
        notifyListeners();
    }

    public double getTotalPrice() {
        double total = 0;
        for (Product p : cartItems) {
            total += p.getPrice();
        }
        return total;
    }

    // Firebase'den sepeti yükle
    private void loadCartFromFirebase() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .collection("cart")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        cartItems.clear();

                        // Her bir sepet öğesi için ürün bilgilerini al - DocumentSnapshot kullan
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String productId = document.getString("productId");

                            if (productId != null) {
                                // Ürün bilgilerini getir
                                db.collection("products").document(productId)
                                        .get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Product product = productDoc.toObject(Product.class);
                                                if (product != null) {
                                                    // Sepet öğesi ID'sini ayarla
                                                    product.setCartItemId(document.getId());
                                                    cartItems.add(product);

                                                    // Değişikliği dinleyicilere bildir
                                                    notifyListeners();
                                                }
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    // Yerel depolamaya sepeti kaydet (giriş yapmayan kullanıcılar için)
    private void saveCartToPrefs(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Sepetteki ürün ID'lerini kaydet
        StringBuilder sb = new StringBuilder();
        for (Product p : cartItems) {
            sb.append(p.getId()).append(",");
        }

        editor.putString("cart_items", sb.toString());
        editor.apply();
    }

    // Yerel depolamadan sepeti yükle (giriş yapmayan kullanıcılar için)
    public void loadCartFromPrefs(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("CartPrefs", Context.MODE_PRIVATE);
        String itemsString = prefs.getString("cart_items", "");

        if (!itemsString.isEmpty()) {
            String[] productIds = itemsString.split(",");

            for (String id : productIds) {
                if (!id.isEmpty()) {
                    // Ürün bilgilerini Firebase'den al
                    db.collection("products").document(id)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Product product = documentSnapshot.toObject(Product.class);
                                    if (product != null && !cartItems.contains(product)) {
                                        cartItems.add(product);

                                        // Değişikliği dinleyicilere bildir
                                        notifyListeners();
                                    }
                                }
                            });
                }
            }
        }
    }
}