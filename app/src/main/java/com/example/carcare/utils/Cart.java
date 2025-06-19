package com.example.carcare.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.carcare.models.CartItem;
import com.example.carcare.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Cart {
    private static final String TAG = "CartUtil";
    private static Cart instance;
    // Ürün ID'sini anahtar olarak kullanarak CartItem'lara hızlı erişim için Map kullanıyoruz.
    private Map<String, CartItem> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public interface CartChangeListener {
        void onCartChanged();
    }

    private List<CartChangeListener> listeners = new ArrayList<>();

    private Cart() {
        cartItems = new HashMap<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadCartFromFirebase();
    }

    public static synchronized Cart getInstance() {
        if (instance == null) {
            instance = new Cart();
        }
        return instance;
    }

    // LISTENER METODLARI
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
            if (listener != null) listener.onCartChanged();
        }
    }

    // SEPET YÖNETİM METODLARI

    /**
     * Sepete yeni bir ürün ekler veya mevcut ürünün miktarını 1 artırır.
     */
    public void addItem(Product product, Context context) {
        if (product == null || product.getId() == null) {
            Log.e(TAG, "Cannot add null product or product with null ID to cart.");
            return;
        }

        String productId = product.getId();
        if (cartItems.containsKey(productId)) {
            // Ürün zaten sepette, miktarını artır
            CartItem existingItem = cartItems.get(productId);
            existingItem.setQuantity(existingItem.getQuantity() + 1);
        } else {
            // Ürün yeni, sepete 1 adet olarak ekle
            CartItem newItem = new CartItem(product, 1);
            cartItems.put(productId, newItem);
        }

        saveItemToFirebase(cartItems.get(productId));
        Toast.makeText(context, product.getName() + " sepete eklendi", Toast.LENGTH_SHORT).show();
        notifyListeners();
    }

    /**
     * Belirtilen ürünün miktarını 1 artırır.
     */
    public void increaseQuantity(String productId) {
        if (cartItems.containsKey(productId)) {
            CartItem item = cartItems.get(productId);
            item.setQuantity(item.getQuantity() + 1);
            saveItemToFirebase(item);
            notifyListeners();
        }
    }

    /**
     * Belirtilen ürünün miktarını 1 azaltır. Miktar 1 ise ürünü sepetten kaldırır.
     */
    public void decreaseQuantity(String productId, Context context) {
        if (cartItems.containsKey(productId)) {
            CartItem item = cartItems.get(productId);
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                saveItemToFirebase(item);
            } else {
                // Miktar 1 ise ürünü tamamen kaldır
                removeItem(productId, context);
            }
            notifyListeners();
        }
    }

    /**
     * Ürünü miktarından bağımsız olarak sepetten tamamen kaldırır.
     */
    public void removeItem(String productId, Context context) {
        if (cartItems.containsKey(productId)) {
            CartItem removedItem = cartItems.remove(productId);
            deleteItemFromFirebase(productId);
            Toast.makeText(context, removedItem.getProduct().getName() + " sepetten çıkarıldı", Toast.LENGTH_SHORT).show();
            notifyListeners();
        }
    }

    /**
     * Sepetteki tüm ürünleri temizler.
     */
    public void clearCart(Context context) {
        cartItems.clear();
        clearCartInFirebase();
        Toast.makeText(context, "Sepet temizlendi", Toast.LENGTH_SHORT).show();
        notifyListeners();
    }

    public void clearCartWithoutToast(Context context) {
        cartItems.clear();
        clearCartInFirebase();
        // Toast.makeText(...) satırı burada yok.
        notifyListeners();
    }

    // GETTER'LAR
    public List<CartItem> getItems() {
        return new ArrayList<>(cartItems.values());
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getProduct().getFinalPrice() * item.getQuantity();
        }
        return total;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartItem item : cartItems.values()) {
            count += item.getQuantity();
        }
        return count;
    }


    // FIREBASE İŞLEMLERİ
    private void saveItemToFirebase(CartItem item) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return; // Giriş yapılmamışsa Firebase'e kaydetme

        Map<String, Object> cartData = new HashMap<>();
        cartData.put("quantity", item.getQuantity());
        cartData.put("addedAt", FieldValue.serverTimestamp());

        // Doküman ID'si olarak ürün ID'sini kullanıyoruz. Bu, güncellemeyi kolaylaştırır.
        db.collection("users").document(user.getUid())
                .collection("cart").document(item.getProduct().getId())
                .set(cartData) // set metodu, doküman yoksa oluşturur, varsa üzerine yazar.
                .addOnSuccessListener(aVoid -> Log.d(TAG, item.getProduct().getName() + " Firebase'e kaydedildi/güncellendi."))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase'e kaydetme hatası", e));
    }

    private void deleteItemFromFirebase(String productId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .collection("cart").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ürün Firebase'den silindi: " + productId))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase'den silme hatası", e));
    }

    private void clearCartInFirebase() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(aVoid -> Log.d(TAG, "Firebase sepeti temizlendi."));
                });
    }

    private void loadCartFromFirebase() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        cartItems.clear();
                        for (QueryDocumentSnapshot cartDoc : task.getResult()) {
                            String productId = cartDoc.getId();
                            Number quantityNum = (Number) cartDoc.get("quantity");
                            int quantity = (quantityNum != null) ? quantityNum.intValue() : 0;

                            if (quantity > 0) {
                                // Ürünün kendi bilgilerini "products" koleksiyonundan çek
                                db.collection("products").document(productId).get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Product product = productDoc.toObject(Product.class);
                                                if (product != null) {
                                                    product.setId(productDoc.getId()); // ID'yi manuel set etmeyi unutma
                                                    CartItem item = new CartItem(product, quantity);
                                                    cartItems.put(productId, item);
                                                    notifyListeners(); // Her ürün yüklendiğinde arayüzü güncelle
                                                }
                                            } else {
                                                // Ürün artık mevcut değilse, sepetten de sil
                                                deleteItemFromFirebase(productId);
                                            }
                                        });
                            }
                        }
                    } else {
                        Log.e(TAG, "Sepet yüklenirken hata", task.getException());
                    }
                });
    }
}