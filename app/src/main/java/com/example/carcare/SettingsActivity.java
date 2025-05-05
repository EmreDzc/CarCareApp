package com.example.carcare;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.carcare.activities.StoreActivity;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    // Edit Profile alanları
    private EditText editName, editSurname, editEmail, editPhone, editReauthPassword;
    // Vehicle Appointment & Insurance alanları
    private EditText editNextMaintenanceDate, editTrafficInsuranceDate, editCarInsuranceDate;
    // Delete Account alanları
    private EditText editConfirmPassword;
    private Button btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings); // Başlangıçta Store seçili olsun

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(SettingsActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(SettingsActivity.this, StoreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(SettingsActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(SettingsActivity.this, NotificationActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });

        // ============================================================
        // 1. Collapsible Edit Profile Bölümü (Güncellenmiş)
        // ============================================================
        LinearLayout layoutEditProfileHeader = findViewById(R.id.layout_edit_profile_header);
        final LinearLayout layoutEditProfileDetails = findViewById(R.id.layout_edit_profile_details);
        final ImageView imgToggle = findViewById(R.id.img_toggle);
        layoutEditProfileHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutEditProfileDetails.getVisibility() == View.GONE) {
                    layoutEditProfileDetails.setVisibility(View.VISIBLE);
                    imgToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutEditProfileDetails.setVisibility(View.GONE);
                    imgToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        // Edit Profile alanlarını tanımla (Email ve reauth alanları kaldırıldı)
        editName = findViewById(R.id.edit_name);
        editSurname = findViewById(R.id.edit_surname);
        editPhone = findViewById(R.id.edit_phone);
        Button btnSaveProfile = findViewById(R.id.btn_save_profile);

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editName.getText().toString().trim();
                String surname = editSurname.getText().toString().trim();
                String phone = editPhone.getText().toString().trim();

                if (name.isEmpty() || surname.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "Name and Surname cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // FirebaseAuth üzerinden oturum açmış kullanıcıyı alıyoruz.
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(SettingsActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Firestore güncellemesi: "name", "surname" ve "phone" alanları güncellenecek
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> profileUpdates = new HashMap<>();
                profileUpdates.put("name", name);
                profileUpdates.put("surname", surname);
                profileUpdates.put("phone", phone);

                db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .set(profileUpdates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(SettingsActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SettingsActivity.this, "Profile update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
        // ============================================================
        // 2. Collapsible Vehicle Appointment & Insurance Bölümü
        // ============================================================
        LinearLayout layoutVehicleHeader = findViewById(R.id.layout_vehicle_appointment_header);
        final LinearLayout layoutVehicleDetails = findViewById(R.id.layout_vehicle_appointment_details);
        final ImageView imgVehicleToggle = findViewById(R.id.img_vehicle_toggle);
        layoutVehicleHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutVehicleDetails.getVisibility() == View.GONE) {
                    layoutVehicleDetails.setVisibility(View.VISIBLE);
                    imgVehicleToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutVehicleDetails.setVisibility(View.GONE);
                    imgVehicleToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });
        editNextMaintenanceDate = findViewById(R.id.edit_next_maintenance_date);
        editTrafficInsuranceDate = findViewById(R.id.edit_traffic_insurance_date);
        editCarInsuranceDate = findViewById(R.id.edit_car_insurance_date);
        Button btnSaveVehicleAppointment = findViewById(R.id.btn_save_vehicle_appointment);
        btnSaveVehicleAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String maintenanceDate = editNextMaintenanceDate.getText().toString();
                String trafficDate = editTrafficInsuranceDate.getText().toString();
                String carInsuranceDate = editCarInsuranceDate.getText().toString();
                Toast.makeText(SettingsActivity.this, "Vehicle appointment details saved.", Toast.LENGTH_SHORT).show();
            }
        });

        // ============================================================
        // 3. Collapsible Upload Picture Bölümü
        // ============================================================
        LinearLayout layoutUploadPictureHeader = findViewById(R.id.layout_upload_picture_header);
        final LinearLayout layoutUploadPictureDetails = findViewById(R.id.layout_upload_picture_details);
        final ImageView imgUploadToggle = findViewById(R.id.img_upload_toggle);
        layoutUploadPictureHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutUploadPictureDetails.getVisibility() == View.GONE) {
                    layoutUploadPictureDetails.setVisibility(View.VISIBLE);
                    imgUploadToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutUploadPictureDetails.setVisibility(View.GONE);
                    imgUploadToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });
        Button btnChoosePicture = findViewById(R.id.btn_choose_picture);
        btnChoosePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            }
        });

        // ============================================================
        // 4. Collapsible Dark Mode Bölümü
        // ============================================================
        LinearLayout layoutDarkModeHeader = findViewById(R.id.layout_dark_mode_header);
        final LinearLayout layoutDarkModeDetails = findViewById(R.id.layout_dark_mode_details);
        final ImageView imgDarkModeToggle = findViewById(R.id.img_dark_mode_toggle);
        layoutDarkModeHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutDarkModeDetails.getVisibility() == View.GONE) {
                    layoutDarkModeDetails.setVisibility(View.VISIBLE);
                    imgDarkModeToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutDarkModeDetails.setVisibility(View.GONE);
                    imgDarkModeToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });
        Switch switchDarkMode = findViewById(R.id.switch_dark_mode);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchDarkMode.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(SettingsActivity.this, "Dark Mode On", Toast.LENGTH_SHORT).show();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(SettingsActivity.this, "Dark Mode Off", Toast.LENGTH_SHORT).show();
            }
            recreate();
        });

        // ============================================================
        // 5. Collapsible FAQ Bölümü
        // ============================================================
        LinearLayout layoutFaqHeader = findViewById(R.id.layout_faq_header);
        final LinearLayout layoutFaqDetails = findViewById(R.id.layout_faq_details);
        final ImageView imgFaqToggle = findViewById(R.id.img_faq_toggle);
        layoutFaqHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutFaqDetails.getVisibility() == View.GONE) {
                    layoutFaqDetails.setVisibility(View.VISIBLE);
                    imgFaqToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutFaqDetails.setVisibility(View.GONE);
                    imgFaqToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });

        // ============================================================
        // 6. Collapsible Log Out Bölümü
        // ============================================================
        LinearLayout layoutLogoutHeader = findViewById(R.id.layout_logout_header);
        final LinearLayout layoutLogoutDetails = findViewById(R.id.layout_logout_details);
        final ImageView imgLogoutToggle = findViewById(R.id.img_logout_toggle);
        layoutLogoutHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutLogoutDetails.getVisibility() == View.GONE) {
                    layoutLogoutDetails.setVisibility(View.VISIBLE);
                    imgLogoutToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutLogoutDetails.setVisibility(View.GONE);
                    imgLogoutToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });
        Button btnConfirmLogout = findViewById(R.id.btn_confirm_logout);
        btnConfirmLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(SettingsActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // ============================================================
        // 7. Collapsible Delete Account Bölümü
        // ============================================================
        LinearLayout layoutDeleteHeader = findViewById(R.id.layout_delete_account_header);
        final LinearLayout layoutDeleteDetails = findViewById(R.id.layout_delete_account_details);
        final ImageView imgDeleteToggle = findViewById(R.id.img_delete_toggle);
        layoutDeleteHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (layoutDeleteDetails.getVisibility() == View.GONE) {
                    layoutDeleteDetails.setVisibility(View.VISIBLE);
                    imgDeleteToggle.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    layoutDeleteDetails.setVisibility(View.GONE);
                    imgDeleteToggle.setImageResource(R.drawable.ic_arrow_down);
                }
            }
        });
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        Button btnConfirmDeleteAccount = findViewById(R.id.btn_confirm_delete_account);
        btnConfirmDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = editConfirmPassword.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "Lütfen şifrenizi girin.", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(SettingsActivity.this, "Kullanıcı bulunamadı.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Reauthentication için mevcut email ve girilen şifre kullanılır.
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            // Önce Firestore'daki kullanıcı belgesini sil
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(user.getUid())
                                    .delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        // Ardından, FirebaseAuth'den kullanıcıyı sil
                                        user.delete()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Toast.makeText(SettingsActivity.this, "Hesabınız silindi.", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(SettingsActivity.this, "Hesap silme başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SettingsActivity.this, "Firestore belgesi silinemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SettingsActivity.this, "Doğrulama başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}
