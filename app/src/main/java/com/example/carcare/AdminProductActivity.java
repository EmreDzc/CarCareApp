package com.example.carcare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carcare.utils.AdminUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminProductActivity extends AppCompatActivity {

    private static final String TAG = "AdminProductActivity";

    private EditText editName, editDescription, editPrice, editStock;
    private Spinner spinnerCategory;
    private ImageView imageViewProduct;
    private Button btnSelectImage, btnSaveProduct;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri selectedImageUri;
    private String uploadedImageUrl;

    // ActivityResultLauncher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product);

        // Firebase initialize
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        Log.d(TAG, "Firebase initialized successfully");

        initViews();
        setupImagePicker();
        setupClickListeners();
    }

    private void initViews() {
        editName = findViewById(R.id.edit_product_name);
        editDescription = findViewById(R.id.edit_product_description);
        editPrice = findViewById(R.id.edit_product_price);
        editStock = findViewById(R.id.edit_product_stock);
        spinnerCategory = findViewById(R.id.spinner_category);
        imageViewProduct = findViewById(R.id.image_product_preview);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSaveProduct = findViewById(R.id.btn_save_product);
        progressBar = findViewById(R.id.progress_bar);

        Log.d(TAG, "Views initialized");
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                // Seçilen resmi ImageView'da göster
                                Glide.with(AdminProductActivity.this)
                                        .load(selectedImageUri)
                                        .into(imageViewProduct);

                                btnSelectImage.setText("Resim Seçildi ✓");
                                Log.d(TAG, "Image selected: " + selectedImageUri.toString());
                            }
                        } else {
                            Log.w(TAG, "Image selection cancelled or failed");
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSaveProduct.setOnClickListener(v -> saveProduct());

        // Geri butonu
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void selectImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");

            // Mime type filtrelemesi
            String[] mimeTypes = {"image/jpeg", "image/png", "image/gif"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            imagePickerLauncher.launch(intent);
            Log.d(TAG, "Image picker launched");
        } catch (Exception e) {
            Log.e(TAG, "Error launching image picker", e);
            Toast.makeText(this, "Resim seçici açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProduct() {
        Log.d(TAG, "Save product button clicked");

        // Form validasyonu
        if (!validateInputs()) {
            Log.w(TAG, "Form validation failed");
            return;
        }

        showLoading(true);

        // Resim seçilip seçilmediğini kontrol et
        if (selectedImageUri != null) {
            Log.d(TAG, "Image selected, uploading image first");
            uploadImageAndSaveProduct();
        } else {
            Log.d(TAG, "No image selected, saving product without image");
            saveProductToFirestore("");
        }
    }

    private boolean validateInputs() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String stockText = editStock.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Ürün adı gerekli");
            editName.requestFocus();
            return false;
        }

        if (description.isEmpty()) {
            editDescription.setError("Açıklama gerekli");
            editDescription.requestFocus();
            return false;
        }

        if (priceText.isEmpty()) {
            editPrice.setError("Fiyat gerekli");
            editPrice.requestFocus();
            return false;
        }

        if (stockText.isEmpty()) {
            editStock.setError("Stok miktarı gerekli");
            editStock.requestFocus();
            return false;
        }

        try {
            double price = Double.parseDouble(priceText);
            int stock = Integer.parseInt(stockText);

            if (price < 0) {
                editPrice.setError("Fiyat negatif olamaz");
                editPrice.requestFocus();
                return false;
            }

            if (stock < 0) {
                editStock.setError("Stok negatif olamaz");
                editStock.requestFocus();
                return false;
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı", Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "Form validation passed");
        return true;
    }

    private void uploadImageAndSaveProduct() {
        if (selectedImageUri == null) {
            // Resim seçilmediyse boş URL ile kaydet
            saveProductToFirestore("");
            return;
        }

        try {
            // Unique dosya ismi oluştur
            String fileName = "product_images/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference imageRef = storageRef.child(fileName);

            // Resmi Firebase Storage'a yükle
            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Download URL'ini al
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String uploadedImageUrl = uri.toString();
                                    Log.d(TAG, "Resim URL'si: " + uploadedImageUrl);
                                    // Ürünü resim URL'si ile kaydet
                                    saveProductToFirestore(uploadedImageUrl);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Resim URL'i alınamadı", e);
                                    Toast.makeText(this, "Resim URL'i alınamadı", Toast.LENGTH_SHORT).show();
                                    // Boş URL ile kaydet
                                    saveProductToFirestore("");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Resim yüklenemedi", e);
                        Toast.makeText(this, "Resim yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Boş URL ile kaydet
                        saveProductToFirestore("");
                    });
        } catch (Exception e) {
            Log.e(TAG, "Resim yükleme hatası", e);
            Toast.makeText(this, "Resim yükleme hatası", Toast.LENGTH_SHORT).show();
            saveProductToFirestore("");
        }
    }

    private long getFileSize(Uri uri) {
        try {
            AssetFileDescriptor fileDescriptor = getContentResolver().openAssetFileDescriptor(uri, "r");
            long fileSize = fileDescriptor.getLength();
            fileDescriptor.close();
            return fileSize;
        } catch (IOException e) {
            Log.e(TAG, "Dosya boyutu alınamadı", e);
            return -1;
        }
    }

    private void uploadImage() {
        // Unique dosya ismi oluştur
        String fileName = "product_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        // Resmi Firebase Storage'a yükle
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Download URL'ini al
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String uploadedImageUrl = uri.toString();
                                // Ürünü resim URL'si ile kaydet
                                saveProductToFirestore(uploadedImageUrl);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Resim URL'i alınamadı", e);
                                Toast.makeText(this, "Resim URL'i alınamadı", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Resim yüklenemedi", e);
                    Toast.makeText(this, "Resim yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAdminStatus() {
        AdminUtils.checkAdminStatus(isAdmin -> {
            if (isAdmin) {
                // Admin için görünürlük ayarla
                com.google.android.material.floatingactionbutton.FloatingActionButton fabAdmin =
                        findViewById(R.id.fab_admin);

                if (fabAdmin != null) {
                    fabAdmin.setVisibility(View.VISIBLE);
                    fabAdmin.setOnClickListener(v -> {
                        Intent intent = new Intent(this, AdminProductActivity.class);
                        startActivity(intent);
                    });
                }
            } else {
                // Admin değilse uyarı ver veya erişimi engelle
                Toast.makeText(this, "Bu sayfaya erişim izniniz yok", Toast.LENGTH_SHORT).show();
                finish(); // Sayfadan çık
            }
        });
    }

    private void saveProductToFirestore(String imageUrl) {
        // Form verilerini al
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        double price = Double.parseDouble(editPrice.getText().toString().trim());
        int stock = Integer.parseInt(editStock.getText().toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();

        // Ürün verilerini hazırla
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("stock", stock);
        product.put("category", category);
        product.put("imageUrl", imageUrl); // Resim URL'si
        product.put("createdAt", FieldValue.serverTimestamp());

        Log.d(TAG, "Ürün kaydediliyor: " + product.toString());

        // Firestore'a kaydet
        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Ürün başarıyla eklendi: " + documentReference.getId());
                    Toast.makeText(this, "Ürün başarıyla eklendi!", Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ürün eklenemedi", e);
                    Toast.makeText(this, "Ürün eklenemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearForm() {
        // Tüm input alanlarını temizle
        editName.setText("");
        editDescription.setText("");
        editPrice.setText("");
        editStock.setText("");
        spinnerCategory.setSelection(0);

        // Resim önizlemesini sıfırla
        imageViewProduct.setImageResource(R.drawable.placeholder_image);

        // Resim seçme butonunu sıfırla
        btnSelectImage.setText("Resim Seç");
        selectedImageUri = null;

        // Fokusları sıfırla
        editName.clearFocus();
        editDescription.clearFocus();
        editPrice.clearFocus();
        editStock.clearFocus();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);

        if (isLoading) {
            btnSaveProduct.setText("Kaydediliyor...");
        } else {
            btnSaveProduct.setText("Ürünü Kaydet");
        }

        Log.d(TAG, "Loading state: " + isLoading);
    }
}