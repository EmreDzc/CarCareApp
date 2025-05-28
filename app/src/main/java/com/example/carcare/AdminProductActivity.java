package com.example.carcare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AdminProductActivity extends AppCompatActivity {

    private static final String TAG = "AdminProductActivity";

    private EditText editName, editDescription, editPrice, editStock;
    private Spinner spinnerCategory;
    private ImageView imageViewProduct;
    private Button btnSelectImage, btnSaveProduct;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Resim sıkıştırma ayarları (Firestore limitlerini aşmamak için önemli)
    private static final int IMAGE_MAX_WIDTH = 600; // Piksel
    private static final int IMAGE_MAX_HEIGHT = 600; // Piksel
    private static final int IMAGE_COMPRESSION_QUALITY = 70; // JPEG için 0-100 arası

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firebase Firestore başlatıldı");

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
        Log.d(TAG, "View'lar başlatıldı");
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(AdminProductActivity.this)
                                    .load(selectedImageUri)
                                    .into(imageViewProduct);
                            btnSelectImage.setText("Resim Seçildi ✓");
                            Log.d(TAG, "Resim seçildi: " + selectedImageUri.toString());
                        }
                    } else {
                        Log.w(TAG, "Resim seçimi iptal edildi veya başarısız oldu");
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSaveProduct.setOnClickListener(v -> saveProduct());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void selectImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            String[] mimeTypes = {"image/jpeg", "image/png"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            imagePickerLauncher.launch(intent);
            Log.d(TAG, "Resim seçici başlatıldı");
        } catch (Exception e) {
            Log.e(TAG, "Resim seçici başlatılırken hata", e);
            Toast.makeText(this, "Resim seçici açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String convertUriToResizedBase64(Uri uri) {
        if (uri == null) return "";
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Log.e(TAG, "URI için InputStream null: " + uri);
                return "";
            }
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                Log.e(TAG, "BitmapFactory.decodeStream null döndürdü URI için: " + uri);
                return "";
            }

            Bitmap resizedBitmap = resizeBitmap(originalBitmap, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Log.d(TAG, "Base64 string uzunluğu: " + base64Image.length() + " byte. Orijinal: " + originalBitmap.getByteCount() + ", Sıkıştırılmış byte array: " + byteArray.length);

            // Firestore limiti 1MB (1,048,576 byte). Base64 string'i bu sınıra yakın olmamalı.
            // Pratikte ~700KB'lık bir Base64 stringi bile riskli olabilir.
            if (base64Image.length() > 750000) { // Yaklaşık 750KB string limiti (çok kaba bir tahmin)
                Toast.makeText(this, "Resim boyutu Firestore için çok büyük. Lütfen daha küçük bir resim seçin veya sıkıştırma ayarlarını optimize edin.", Toast.LENGTH_LONG).show();
                return ""; // Hata durumunda boş string döndür
            }
            return base64Image;

        } catch (IOException e) {
            Log.e(TAG, "URI Base64'e dönüştürülürken G/Ç hatası", e);
            Toast.makeText(this, "Resim dönüştürülürken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return "";
        } catch (OutOfMemoryError ome) {
            Log.e(TAG, "Resim işlenirken bellek yetersiz kaldı. Resim çok büyük olabilir.", ome);
            Toast.makeText(this, "Resim işlenirken bellek yetersiz. Daha küçük bir resim deneyin.", Toast.LENGTH_LONG).show();
            return "";
        }
    }

    private Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight <= 0 || maxWidth <= 0 || image == null) {
            return image;
        }
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return image; // Yeniden boyutlandırmaya gerek yok
        }

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }
        if (finalWidth <= 0) finalWidth = 1;
        if (finalHeight <= 0) finalHeight = 1;

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
    }

    private void saveProduct() {
        Log.d(TAG, "Ürünü kaydet butonuna tıklandı");
        if (!validateInputs()) {
            Log.w(TAG, "Form validasyonu başarısız");
            return;
        }
        showLoading(true);

        String imageBase64Data = "";
        if (selectedImageUri != null) {
            Log.d(TAG, "Resim seçili, Base64'e dönüştürülüyor");
            imageBase64Data = convertUriToResizedBase64(selectedImageUri);
            if (imageBase64Data.isEmpty() && selectedImageUri != null) {
                // Eğer resim seçilmiş ama dönüştürme başarısız olmuşsa
                Toast.makeText(this, "Resim işlenemedi. Lütfen farklı bir resim deneyin veya seçimi kaldırın.", Toast.LENGTH_LONG).show();
                showLoading(false);
                return; // Kaydetmeyi durdur
            }
        } else {
            Log.d(TAG, "Resim seçilmedi, resimsiz kaydedilecek.");
        }
        saveProductToFirestore(imageBase64Data);
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
        Log.d(TAG, "Form validasyonu başarılı");
        return true;
    }

    private void saveProductToFirestore(String imageBase64) {
        String name = editName.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        double price = Double.parseDouble(editPrice.getText().toString().trim());
        int stock = Integer.parseInt(editStock.getText().toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();

        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("stock", stock);
        product.put("category", category);
        product.put("imageBase64", imageBase64 != null ? imageBase64 : ""); // Base64 string'i sakla
        product.put("createdAt", FieldValue.serverTimestamp());

        Log.d(TAG, "Ürün kaydediliyor: " + name + ", Kategori: " + category + ", ImageBase64 uzunluğu: " + (imageBase64 != null ? imageBase64.length() : 0));

        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Ürün başarıyla eklendi: " + documentReference.getId());
                    Toast.makeText(this, "Ürün başarıyla eklendi!", Toast.LENGTH_LONG).show();
                    clearForm();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ürün eklenemedi", e);
                    Toast.makeText(this, "Ürün eklenemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    private void clearForm() {
        editName.setText("");
        editDescription.setText("");
        editPrice.setText("");
        editStock.setText("");
        spinnerCategory.setSelection(0);
        imageViewProduct.setImageResource(R.drawable.placeholder_image); // drawable içinde placeholder_image olmalı
        btnSelectImage.setText("Resim Seç");
        selectedImageUri = null;
        editName.clearFocus();
        // Diğer alanların focus'unu da temizleyebilirsiniz.
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
        btnSaveProduct.setText(isLoading ? "Kaydediliyor..." : "Ürünü Kaydet");
        Log.d(TAG, "Yükleme durumu: " + isLoading);
    }
}