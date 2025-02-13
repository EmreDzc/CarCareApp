package com.example.carcare;

import android.content.Intent;
import android.net.Uri;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    // Vehicle Appointment & Insurance bölümü elemanları
    private EditText editNextMaintenanceDate, editTrafficInsuranceDate, editCarInsuranceDate;
    // Delete Account bölümü elemanları
    private EditText editConfirmPassword;
    private Button btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // --- 1. Collapsible Edit Profile Bölümü ---
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

        // --- 2. Collapsible Vehicle Appointment and Insurance Bölümü ---
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

        // Vehicle Appointment & Insurance işlemleri
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
                // Gerekli doğrulama ve veri kaydetme işlemleri burada yapılabilir.
                Toast.makeText(SettingsActivity.this, "Vehicle appointment details saved.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 3. Collapsible Upload Picture Bölümü ---
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
                // Cihazın galerisinden resim seçme işlemi
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            }
        });

        // --- 4. Collapsible Dark Mode Bölümü ---
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
                // Eğer switch açık ise (dark mode aktif):
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Toast.makeText(SettingsActivity.this, "Dark Mode On", Toast.LENGTH_SHORT).show();
            } else {
                // Eğer switch kapalı ise (dark mode kapalı):
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(SettingsActivity.this, "Dark Mode Off", Toast.LENGTH_SHORT).show();
            }
            // Tema değişikliğinin hemen yansıması için activity'yi yeniden oluşturun:
            recreate();
        });

        // --- Collapsible FAQ Bölümü ---
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


        // --- Collapsible Log Out Bölümü ---
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
                // Burada gerçek log out işlemini gerçekleştirin
                Toast.makeText(SettingsActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Collapsible Delete Account Bölümü ---
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
                // Şifre doğrulaması ve hesap silme işlemleri burada yapılır.
                Toast.makeText(SettingsActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
