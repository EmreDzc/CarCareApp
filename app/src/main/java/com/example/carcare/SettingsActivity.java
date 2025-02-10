package com.example.carcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    // Eleman tanımlamaları
    private CardView cardEditProfile, cardVehicleAppointment, cardProfilePicture, cardDarkMode,
            cardFAQ, cardRateApp, cardLogout, cardDeleteAccount;
    private Switch switchDarkMode;
    // Vehicle Appointment bölümü elemanları
    private EditText editNextMaintenanceDate, editTrafficInsuranceDate, editCarInsuranceDate;
    private Button btnSaveVehicleAppointment;
    // Delete Account bölümü elemanları
    private EditText editConfirmPassword;
    private Button btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Elemanları layout’dan bulma
        cardEditProfile = findViewById(R.id.card_edit_profile);
        cardVehicleAppointment = findViewById(R.id.card_vehicle_appointment);
        cardProfilePicture = findViewById(R.id.card_profile_picture);
        cardDarkMode = findViewById(R.id.card_dark_mode);
        cardFAQ = findViewById(R.id.card_faq);
        cardRateApp = findViewById(R.id.card_rate_app);
        cardLogout = findViewById(R.id.card_logout);
        cardDeleteAccount = findViewById(R.id.card_delete_account);

        switchDarkMode = findViewById(R.id.switch_dark_mode);

        editNextMaintenanceDate = findViewById(R.id.edit_next_maintenance_date);
        editTrafficInsuranceDate = findViewById(R.id.edit_traffic_insurance_date);
        editCarInsuranceDate = findViewById(R.id.edit_car_insurance_date);
        btnSaveVehicleAppointment = findViewById(R.id.btn_save_vehicle_appointment);

        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);

        // 1. Edit Profile'e Tıklama İşlemi
        cardEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Örneğin, EditProfileActivity adlı yeni bir Activity açabilirsiniz.


                // Alternatif olarak, bir Dialog ile de düzenleme yapabilirsiniz.
            }
        });

        // 2. Vehicle Appointment & Insurance Save İşlemi
        btnSaveVehicleAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Kullanıcıdan alınan tarih verilerini oku.
                String maintenanceDate = editNextMaintenanceDate.getText().toString();
                String trafficDate = editTrafficInsuranceDate.getText().toString();
                String carInsuranceDate = editCarInsuranceDate.getText().toString();

                // Burada verileri doğrulama, veri tabanına kaydetme veya sunucuya gönderme işlemlerini gerçekleştirin.
                Toast.makeText(SettingsActivity.this, "Vehicle appointment details saved.", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Profile Picture Yükleme İşlemi
        cardProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Örneğin, cihaz galerisine gitmek için:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);  // 100: request code
            }
        });

        // 4. Dark Mode Değiştirme İşlemi
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                // Dark Mode'u etkinleştir.
                // Örneğin, uygulamanızın tema ayarını değiştirin.
                Toast.makeText(SettingsActivity.this, "Dark Mode On", Toast.LENGTH_SHORT).show();
            } else {
                // Dark Mode'u devre dışı bırak.
                Toast.makeText(SettingsActivity.this, "Dark Mode Off", Toast.LENGTH_SHORT).show();
            }
        });

        // 5. Rate the App İşlemi
        cardRateApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Google Play Store'da uygulamanızın sayfasını açın.
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        // 6. Log Out İşlemi
        cardLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Çıkış işlemi için onay diyaloğu gösterin.
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Çıkış işlemini gerçekleştirin (örneğin, oturumu temizleyin ve LoginActivity'e yönlendirin).
                            Toast.makeText(SettingsActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // 7. Delete Account İşlemi
        btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String password = editConfirmPassword.getText().toString();
                // Burada şifre doğrulaması yapın.
                // Eğer doğrulama başarılı ise hesabı silme işlemini (sunucuya istek vb.) başlatın.
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Delete Account")
                        .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // Hesap silme işlemini başlatın.
                            Toast.makeText(SettingsActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    // Örneğin, profil resmi seçildikten sonra sonucu almak için onActivityResult() metodunu kullanın.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            ImageView imgProfilePicture = findViewById(R.id.img_profile_picture);
            imgProfilePicture.setImageURI(selectedImageUri);
            // Seçilen resmi yükleme veya sunucuya gönderme işlemleri yapılabilir.
        }
    }
}
