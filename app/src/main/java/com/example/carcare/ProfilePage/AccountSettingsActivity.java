package com.example.carcare.ProfilePage;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carcare.LoginActivity;
import com.example.carcare.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccountSettingsActivity";

    private EditText editNameFromUi;
    private EditText editSurnameFromUi;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_account_settings);


            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            progressDialog = new ProgressDialog(this);
            // progressDialog.setMessage("Lütfen bekleyin..."); // Mesajı her işlem öncesi set edelim
            progressDialog.setCancelable(false);

            editNameFromUi = findViewById(R.id.edit_name);
            editSurnameFromUi = findViewById(R.id.edit_surname);

            setupBackButton();
            setupProfileForm();
            setupPasswordForm();
            setupDeleteAccountForm();

            Log.d(TAG, "onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Bir hata oluştu, lütfen tekrar deneyin.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupBackButton() {
        final ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        } else {
            Log.w(TAG, "btn_back not found in layout");
        }
    }

    private void setupProfileForm() {
        final Button btnSaveProfile = findViewById(R.id.btn_save_profile);
        loadUserProfile(); // Uses class members editNameFromUi, editSurnameFromUi

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> {
                if (editNameFromUi == null || editSurnameFromUi == null) {
                    Log.e(TAG, "Profile EditTexts are null in onClickListener.");
                    Toast.makeText(this, "Profil alanları yüklenemedi.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String namePart = editNameFromUi.getText().toString().trim();
                String surnamePart = editSurnameFromUi.getText().toString().trim();

                if (namePart.isEmpty()) {
                    Toast.makeText(this, "İsim alanı boş olamaz.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String fullNameToSave = namePart;
                if (!surnamePart.isEmpty()) {
                    fullNameToSave += " " + surnamePart;
                }
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(this, "Kullanıcı girişi yapılmamış.", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String userId = currentUser.getUid();
                Map<String, Object> profileUpdates = new HashMap<>();
                profileUpdates.put("fullName", fullNameToSave);

                progressDialog.setMessage("Profil güncelleniyor...");
                progressDialog.show();

                String finalFullNameToSave = fullNameToSave;
                db.collection("users").document(userId)
                        .set(profileUpdates, SetOptions.merge())
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Profile updated successfully with fullName: '" + finalFullNameToSave + "' for user: " + userId);
                                Toast.makeText(AccountSettingsActivity.this, "Profil başarıyla güncellendi.", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "Profile update failed for user: " + userId, task.getException());
                                Toast.makeText(AccountSettingsActivity.this, "Profil güncelleme başarısız: " + (task.getException() != null ? task.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        } else {
            Log.w(TAG, "btn_save_profile not found in layout");
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                String fullNameFromDb = documentSnapshot.getString("fullName");
                                if (fullNameFromDb != null && !fullNameFromDb.isEmpty()) {
                                    String nameToDisplay = "";
                                    String surnameToDisplay = "";
                                    int lastSpaceIndex = fullNameFromDb.lastIndexOf(' ');
                                    if (lastSpaceIndex > 0 && lastSpaceIndex < fullNameFromDb.length() - 1) {
                                        nameToDisplay = fullNameFromDb.substring(0, lastSpaceIndex);
                                        surnameToDisplay = fullNameFromDb.substring(lastSpaceIndex + 1);
                                    } else {
                                        nameToDisplay = fullNameFromDb;
                                    }
                                    if (editNameFromUi != null) editNameFromUi.setText(nameToDisplay);
                                    if (editSurnameFromUi != null) editSurnameFromUi.setText(surnameToDisplay);
                                    Log.d(TAG, "User profile loaded.");
                                } else {
                                    Log.d(TAG, "fullName is null or empty in Firestore for user: " + userId);
                                }
                            } else {
                                Log.d(TAG, "No profile document found for user: " + userId);
                            }
                        } else {
                            Log.e(TAG, "Failed to load user profile data for user " + userId, task.getException());
                            // Toast.makeText(this, "Profil bilgileri yüklenemedi.", Toast.LENGTH_SHORT).show(); // Kullanıcıyı rahatsız etmeyebiliriz
                        }
                    });
        }
    }

    private void setupPasswordForm() {
        // DİKKAT: XML'deki ID'ler ile eşleştiğinden emin olun!
        final EditText editCurrentPassword = findViewById(R.id.edit_current_password);
        final EditText editNewPassword = findViewById(R.id.edit_new_password);
        final EditText editConfirmNewPassword = findViewById(R.id.edit_confirm_new_password); // *** DÜZELTİLMİŞ ID ***
        final Button btnChangePassword = findViewById(R.id.btn_change_password);

        // Ekstra null kontrolleri (teşhis için iyi, üretimde kaldırılabilir eğer ID'ler kesin doğruysa)
        if (editCurrentPassword == null || editNewPassword == null || editConfirmNewPassword == null || btnChangePassword == null) {
            Log.e(TAG, "One or more views in setupPasswordForm are null. Check XML IDs and findViewById calls.");
            if (editCurrentPassword == null) Log.e(TAG, "editCurrentPassword is NULL");
            if (editNewPassword == null) Log.e(TAG, "editNewPassword is NULL");
            if (editConfirmNewPassword == null) Log.e(TAG, "editConfirmNewPassword (R.id.edit_confirm_new_password) is NULL"); // Düzeltilmiş ID ile log
            if (btnChangePassword == null) Log.e(TAG, "btnChangePassword is NULL");
            Toast.makeText(this, "Şifre değiştirme formu yüklenirken hata oluştu.", Toast.LENGTH_LONG).show();
            return;
        }

        btnChangePassword.setOnClickListener(v -> {
            String currentPasswordStr = editCurrentPassword.getText().toString().trim();
            String newPasswordStr = editNewPassword.getText().toString().trim();
            String confirmPasswordStr = editConfirmNewPassword.getText().toString().trim(); // *** DÜZELTİLMİŞ EditText KULLANIMI ***

            if (currentPasswordStr.isEmpty() || newPasswordStr.isEmpty() || confirmPasswordStr.isEmpty()) {
                Toast.makeText(this, "Tüm şifre alanlarını doldurun", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPasswordStr.equals(confirmPasswordStr)) {
                Toast.makeText(this, "Yeni şifreler eşleşmiyor", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPasswordStr.length() < 6) {
                Toast.makeText(this, "Yeni şifre en az 6 karakter olmalı.", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                if (user.getEmail() == null) {
                    Toast.makeText(this, "Şifre değişikliği için kullanıcı e-postası gerekli.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "User email is null, cannot re-authenticate for password change.");
                    return;
                }
                progressDialog.setMessage("Şifre değiştiriliyor...");
                progressDialog.show();
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPasswordStr);
                user.reauthenticate(credential)
                        .addOnCompleteListener(reauthTask -> {
                            if (reauthTask.isSuccessful()) {
                                user.updatePassword(newPasswordStr)
                                        .addOnCompleteListener(updatePasswordTask -> {
                                            progressDialog.dismiss();
                                            if (updatePasswordTask.isSuccessful()) {
                                                Toast.makeText(AccountSettingsActivity.this, "Şifre başarıyla değiştirildi.", Toast.LENGTH_SHORT).show();
                                                if(editCurrentPassword != null) editCurrentPassword.setText("");
                                                if(editNewPassword != null) editNewPassword.setText("");
                                                if(editConfirmNewPassword != null) editConfirmNewPassword.setText(""); // Düzeltilmiş
                                            } else {
                                                Log.e(TAG, "Password update failed: ", updatePasswordTask.getException());
                                                Toast.makeText(AccountSettingsActivity.this, "Şifre değiştirilemedi: " + (updatePasswordTask.getException() != null ? updatePasswordTask.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } else {
                                progressDialog.dismiss();
                                Log.w(TAG, "Re-authentication failed: ", reauthTask.getException());
                                Toast.makeText(AccountSettingsActivity.this, "Mevcut şifre yanlış veya yeniden kimlik doğrulama başarısız: " + (reauthTask.getException() != null ? reauthTask.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Kullanıcı girişi yapılmamış.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDeleteAccountForm() {
        final Button btnDeleteAccount = findViewById(R.id.btn_delete_account);
        // XML'deki edit_delete_password_confirm bu metodda doğrudan kullanılmıyor, dialog içinde yenisi oluşturuluyor.

        if (btnDeleteAccount == null) {
            Log.w(TAG, "btn_delete_account not found in layout");
            return;
        }

        btnDeleteAccount.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsActivity.this);
            builder.setTitle("Hesabı Silmeyi Onayla");
            builder.setMessage("⚠️ Uyarı: Hesabınızı kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.\n\nDevam etmek için lütfen mevcut şifrenizi girin.");

            final EditText inputPasswordDialog = new EditText(AccountSettingsActivity.this);
            inputPasswordDialog.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            inputPasswordDialog.setHint("Mevcut Şifreniz");
            // Dialog için layout ve margin ayarları
            LinearLayout layout = new LinearLayout(AccountSettingsActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = (int) (16 * getResources().getDisplayMetrics().density); // dp to px
            params.setMargins(margin, 0, margin, 0);
            inputPasswordDialog.setLayoutParams(params);
            layout.addView(inputPasswordDialog);
            builder.setView(layout);

            builder.setPositiveButton("HESABI SİL", (dialog, which) -> {
                String password = inputPasswordDialog.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(AccountSettingsActivity.this, "Lütfen şifrenizi girin.", Toast.LENGTH_SHORT).show();
                    return;
                }
                proceedWithAccountDeletion(password);
            });
            builder.setNegativeButton("İPTAL", (dialog, which) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void proceedWithAccountDeletion(String password) {
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(AccountSettingsActivity.this, "Kullanıcı bulunamadı. Lütfen tekrar giriş yapın.", Toast.LENGTH_LONG).show();
            return;
        }
        if (user.getEmail() == null) {
            Toast.makeText(AccountSettingsActivity.this, "Hesap silme işlemi için kullanıcı e-postası gerekli.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User email is null, cannot re-authenticate for account deletion.");
            return;
        }

        progressDialog.setMessage("Hesabınız siliniyor...");
        progressDialog.show();

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d(TAG, "User re-authenticated successfully for deletion.");
                        db.collection("users").document(user.getUid())
                                .delete()
                                .addOnCompleteListener(firestoreDeleteTask -> {
                                    if (firestoreDeleteTask.isSuccessful()) {
                                        Log.d(TAG, "User data deleted from Firestore.");
                                        user.delete()
                                                .addOnCompleteListener(authDeleteTask -> {
                                                    progressDialog.dismiss();
                                                    if (authDeleteTask.isSuccessful()) {
                                                        Log.d(TAG, "User account deleted from Firebase Auth.");
                                                        Toast.makeText(AccountSettingsActivity.this, "Hesabınız başarıyla silindi.", Toast.LENGTH_LONG).show();
                                                        Intent intent = new Intent(AccountSettingsActivity.this, LoginActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Log.e(TAG, "Failed to delete user account from Auth: ", authDeleteTask.getException());
                                                        Toast.makeText(AccountSettingsActivity.this, "Hesap silinemedi (Auth): " + (authDeleteTask.getException() != null ? authDeleteTask.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Log.e(TAG, "Failed to delete user data from Firestore: ", firestoreDeleteTask.getException());
                                        Toast.makeText(AccountSettingsActivity.this, "Kullanıcı verileri silinemedi (Firestore): " + (firestoreDeleteTask.getException() != null ? firestoreDeleteTask.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Log.w(TAG, "User re-authentication failed for deletion: ", reauthTask.getException());
                        Toast.makeText(AccountSettingsActivity.this, "Şifre doğrulama başarısız: " + (reauthTask.getException() != null ? reauthTask.getException().getMessage() : "Bilinmeyen hata"), Toast.LENGTH_LONG).show();
                    }
                });
    }
}