package com.example.carcare.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminUtils {
    private static final String COLLECTION_ADMINS = "admins";

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }

    public static void checkAdminStatus(AdminCheckCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Email bazlı admin kontrolü
        db.collection(COLLECTION_ADMINS)
                .whereEqualTo("email", user.getEmail())
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isAdmin = !queryDocumentSnapshots.isEmpty();
                    callback.onResult(isAdmin);
                })
                .addOnFailureListener(e -> {
                    callback.onResult(false);
                });
    }

    // Kullanıcıyı admin yapma metodu
    public static void makeUserAdmin(String email, AdminCheckCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        java.util.Map<String, Object> adminData = new java.util.HashMap<>();
        adminData.put("email", email);
        adminData.put("role", "admin");
        adminData.put("isActive", true);
        adminData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COLLECTION_ADMINS)
                .add(adminData)
                .addOnSuccessListener(documentReference -> {
                    callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    callback.onResult(false);
                });
    }
}