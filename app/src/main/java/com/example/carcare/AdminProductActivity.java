package com.example.carcare;

import android.app.Activity;
import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        setContentView(R.layout.activity_admin_product); // Layout'unuzun adını değiştirin

        // Firebase initialize
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

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
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSaveProduct.setOnClickListener(v -> saveProduct());

        // Geri butonu varsa
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProduct() {
        // Form validasyonu
        if (!validateInputs()) {
            return;
        }

        showLoading(true);

        // Eğer resim seçilmişse önce resmi yükle, sonra ürünü kaydet
        if (selectedImageUri != null) {
            uploadImageAndSaveProduct();
        } else {
            // Resim olmadan ürün kaydet
            saveProductToFirestore(null);
        }
    }

    private boolean validateInputs() {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String priceText = editPrice.getText().toString().trim();
        String stockText = editStock.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Ürün adı gerekli");
            return false;
        }

        if (description.isEmpty()) {
            editDescription.setError("Açıklama gerekli");
            return false;
        }

        if (priceText.isEmpty()) {
            editPrice.setError("Fiyat gerekli");
            return false;
        }

        if (stockText.isEmpty()) {
            editStock.setError("Stok miktarı gerekli");
            return false;
        }

        try {
            Double.parseDouble(priceText);
            Integer.parseInt(stockText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçersiz sayı formatı", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void uploadImageAndSaveProduct() {
        // Unique dosya ismi oluştur
        String fileName = "product_images/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        Log.d(TAG, "Uploading image to: " + fileName);

        // Resmi Firebase Storage'a yükle
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image upload successful");

                    // Upload başarılı, download URL'ini al
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                uploadedImageUrl = uri.toString();
                                Log.d(TAG, "Download URL: " + uploadedImageUrl);

                                // Şimdi ürünü kaydet
                                saveProductToFirestore(uploadedImageUrl);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL", e);
                                showLoading(false);
                                Toast.makeText(this, "Resim URL'i alınamadı: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    showLoading(false);
                    Toast.makeText(this, "Resim yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    // Upload progress gösterebilirsiniz
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
    }

    private void saveProductToFirestore(String imageUrl) {
        // Form verilerini al
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        double price = Double.parseDouble(editPrice.getText().toString().trim());
        int stock = Integer.parseInt(editStock.getText().toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();

        // Product map oluştur
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("stock", stock);
        product.put("category", category);
        product.put("imageUrl", imageUrl != null ? imageUrl : ""); // Null kontrolü
        product.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        Log.d(TAG, "Saving product to Firestore: " + product.toString());

        // Firestore'a kaydet
        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Product saved successfully with ID: " + documentReference.getId());
                    showLoading(false);
                    Toast.makeText(this, "Ürün başarıyla eklendi!", Toast.LENGTH_SHORT).show();

                    // Formu temizle
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving product", e);
                    showLoading(false);
                    Toast.makeText(this, "Ürün kaydedilemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        editName.setText("");
        editDescription.setText("");
        editPrice.setText("");
        editStock.setText("");
        spinnerCategory.setSelection(0);
        imageViewProduct.setImageResource(R.drawable.placeholder_image);
        btnSelectImage.setText("Resim Seç");
        selectedImageUri = null;
        uploadedImageUrl = null;
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
    }
}