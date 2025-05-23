package com.example.carcare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class AdminProductActivity extends AppCompatActivity {

    private EditText editProductName, editProductDescription, editProductPrice, editProductStock;
    private Spinner spinnerCategory;
    private ImageView productImage;
    private Button btnSelectImage, btnSaveProduct, btnCancel;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private String productId;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(productImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Views
        editProductName = findViewById(R.id.edit_product_name);
        editProductDescription = findViewById(R.id.edit_product_description);
        editProductPrice = findViewById(R.id.edit_product_price);
        editProductStock = findViewById(R.id.edit_product_stock);
        spinnerCategory = findViewById(R.id.spinner_category);
        productImage = findViewById(R.id.product_image);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnSaveProduct = findViewById(R.id.btn_save_product);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);

        // Kategori spinner'ını ayarla
        setupCategorySpinner();

        // Düzenleme modu kontrolü
        productId = getIntent().getStringExtra("PRODUCT_ID");
        isEditMode = productId != null;

        if (isEditMode) {
            loadProductDetails();
            btnSaveProduct.setText("Ürünü Güncelle");
        } else {
            productId = UUID.randomUUID().toString();
            btnSaveProduct.setText("Ürün Ekle");
        }

        // Resim seçme butonu
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Kaydet butonu
        btnSaveProduct.setOnClickListener(v -> saveProduct());

        // İptal butonu
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "Motor Yağları (oil)",
                "Filtreler (filters)",
                "Lastikler (tires)",
                "Aküler (batteries)",
                "Temizlik Ürünleri (cleaning)",
                "Tamir Kitleri (repair)",
                "Aksesuar (accessories)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadProductDetails() {
        showLoading(true);

        db.collection("products").document(productId).get()
                .addOnSuccessListener(document -> {
                    showLoading(false);
                    if (document.exists()) {
                        Product product = document.toObject(Product.class);
                        if (product != null) {
                            editProductName.setText(product.getName());
                            editProductDescription.setText(product.getDescription());
                            editProductPrice.setText(String.valueOf(product.getPrice()));
                            editProductStock.setText(String.valueOf(product.getStock()));

                            // Kategoriyi seçici listede seç
                            setSpinnerSelection(product.getCategory());

                            // Ürün resmini göster
                            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(product.getImageUrl())
                                        .placeholder(R.drawable.placeholder_image)
                                        .error(R.drawable.error_image)
                                        .into(productImage);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Ürün bulunamadı", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Ürün yüklenirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setSpinnerSelection(String category) {
        String[] categoryValues = {"oil", "filters", "tires", "batteries", "cleaning", "repair", "accessories"};
        for (int i = 0; i < categoryValues.length; i++) {
            if (categoryValues[i].equals(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void saveProduct() {
        if (!validateForm()) {
            return;
        }

        showLoading(true);

        String name = editProductName.getText().toString().trim();
        String description = editProductDescription.getText().toString().trim();
        double price = Double.parseDouble(editProductPrice.getText().toString().trim());
        int stock = Integer.parseInt(editProductStock.getText().toString().trim());

        // Kategori değerini al
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        String category = selectedCategory.substring(selectedCategory.lastIndexOf("(") + 1, selectedCategory.lastIndexOf(")"));

        // Yeni resim seçildiyse önce resmi yükle
        if (selectedImageUri != null) {
            uploadImageAndSaveProduct(name, description, price, stock, category);
        } else {
            // Resim yoksa doğrudan ürünü kaydet
            String imageUrl = isEditMode ? getIntent().getStringExtra("IMAGE_URL") : "";
            saveProductToFirestore(name, description, price, stock, category, imageUrl);
        }
    }

    private void uploadImageAndSaveProduct(String name, String description, double price, int stock, String category) {
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("images/products/" + productId + ".jpg");

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveProductToFirestore(name, description, price, stock, category, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Resim yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProductToFirestore(String name, String description, double price, int stock, String category, String imageUrl) {
        Product product = new Product(productId, name, description, price, imageUrl, category, stock);

        db.collection("products").document(productId)
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Ürün başarıyla kaydedildi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Ürün kaydedilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateForm() {
        boolean valid = true;

        if (editProductName.getText().toString().trim().isEmpty()) {
            editProductName.setError("Ürün adı gerekli");
            valid = false;
        }

        if (editProductDescription.getText().toString().trim().isEmpty()) {
            editProductDescription.setError("Ürün açıklaması gerekli");
            valid = false;
        }

        try {
            double price = Double.parseDouble(editProductPrice.getText().toString().trim());
            if (price <= 0) {
                editProductPrice.setError("Geçerli bir fiyat girin");
                valid = false;
            }
        } catch (NumberFormatException e) {
            editProductPrice.setError("Geçerli bir fiyat girin");
            valid = false;
        }

        try {
            int stock = Integer.parseInt(editProductStock.getText().toString().trim());
            if (stock < 0) {
                editProductStock.setError("Stok 0 veya daha fazla olmalı");
                valid = false;
            }
        } catch (NumberFormatException e) {
            editProductStock.setError("Geçerli bir stok miktarı girin");
            valid = false;
        }

        return valid;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!isLoading);
        btnCancel.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
    }
}