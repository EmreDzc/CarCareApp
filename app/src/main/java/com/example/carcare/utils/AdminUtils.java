package com.example.carcare.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminUtils {
    private static final String TAG = "AdminUtils";
    private static final String COLLECTION_ADMINS = "admins";

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }

    public static void checkAdminStatus(AdminCheckCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.d(TAG, "No user logged in");
            callback.onResult(false);
            return;
        }

        Log.d(TAG, "Checking admin status for user: " + user.getEmail());
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Email bazlı admin kontrolü
        db.collection(COLLECTION_ADMINS)
                .whereEqualTo("email", user.getEmail())
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isAdmin = !queryDocumentSnapshots.isEmpty();
                    Log.d(TAG, "Admin check result: " + isAdmin + " for email: " + user.getEmail());
                    Log.d(TAG, "Documents found: " + queryDocumentSnapshots.size());
                    callback.onResult(isAdmin);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check admin status", e);
                    callback.onResult(false);
                });
    }

    // Kullanıcıyı admin yapma metodu
    public static void makeUserAdmin(String email, AdminCheckCallback callback) {
        Log.d(TAG, "Making user admin: " + email);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        java.util.Map<String, Object> adminData = new java.util.HashMap<>();
        adminData.put("email", email);
        adminData.put("role", "admin");
        adminData.put("isActive", true);
        adminData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection(COLLECTION_ADMINS)
                .add(adminData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "User made admin successfully: " + email);
                    callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to make user admin: " + email, e);
                    callback.onResult(false);
                });
    }

    // Test için - kendini admin yap
    public static void makeCurrentUserAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Making current user admin: " + user.getEmail());
            makeUserAdmin(user.getEmail(), isSuccess -> {
                if (isSuccess) {
                    Log.d(TAG, "Current user is now admin");
                } else {
                    Log.e(TAG, "Failed to make current user admin");
                }
            });
        }
    }
}