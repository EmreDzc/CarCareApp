package com.example.carcare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat; // Eksik import olabilir
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminProductActivity extends AppCompatActivity {

    private static final String TAG = "AdminProductActivity";

    private TextInputEditText editName, editDescription, editPrice, editStock, editBrand, editModelCode,
            editSellerName, editWarrantyInfo, editShippingInfo, editReturnPolicy, editSpecifications;

    private Spinner spinnerCategory;
    private SwitchCompat switchIsFeatured;
    private ImageView imageViewProduct;
    private Button btnSelectImage, btnSaveProduct;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private static final int IMAGE_MAX_WIDTH = 800;
    private static final int IMAGE_MAX_HEIGHT = 800;
    private static final int IMAGE_COMPRESSION_QUALITY = 75;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupImagePicker(); // Bu metod imagePickerLauncher'ı initialize ediyor
        setupClickListeners(); // Bu metod selectImage'i kullanıyor
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

        editBrand = findViewById(R.id.edit_product_brand);
        editModelCode = findViewById(R.id.edit_product_model_code);
        editSellerName = findViewById(R.id.edit_product_seller_name);
        switchIsFeatured = findViewById(R.id.switch_is_featured);
        editWarrantyInfo = findViewById(R.id.edit_product_warranty_info);
        editShippingInfo = findViewById(R.id.edit_product_shipping_info);
        editReturnPolicy = findViewById(R.id.edit_product_return_policy);
        editSpecifications = findViewById(R.id.edit_product_specifications);
        Log.d(TAG, "Views initialized.");
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).into(imageViewProduct);
                            btnSelectImage.setText(R.string.admin_image_selected);
                            Log.d(TAG, "Image selected: " + selectedImageUri.toString());
                        }
                    } else {
                        Log.w(TAG, "Image selection cancelled or failed.");
                    }
                }
        );
        Log.d(TAG, "Image picker setup.");
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage()); // selectImage() burada çağrılıyor
        btnSaveProduct.setOnClickListener(v -> saveProduct());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        Log.d(TAG, "Click listeners setup.");
    }

    // Bu metodun var olduğundan ve doğru yazıldığından emin olun
    private void selectImage() {
        if (imagePickerLauncher == null) {
            Log.e(TAG, "imagePickerLauncher is null in selectImage(). Did setupImagePicker() run?");
            Toast.makeText(this, "Resim seçici hazır değil.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); // Sadece resim dosyalarını filtrele
            imagePickerLauncher.launch(intent);
            Log.d(TAG, "Image picker launched.");
        } catch (Exception e) {
            Log.e(TAG, "Error launching image picker", e);
            Toast.makeText(this, "Resim seçici açılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String convertUriToResizedBase64(Uri uri) {
        if (uri == null) return "";
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: " + uri);
                return "";
            }
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                Log.e(TAG, "BitmapFactory.decodeStream returned null for URI: " + uri);
                return "";
            }

            Bitmap resizedBitmap = resizeBitmap(originalBitmap, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Log.d(TAG, "Base64 string length: " + base64Image.length() + " bytes. Original bitmap size: " + originalBitmap.getByteCount() + ", Resized byte array size: " + byteArray.length);
            if (base64Image.length() > 700000) {
                Toast.makeText(this, "Resim boyutu çok büyük. Lütfen daha küçük bir resim seçin.", Toast.LENGTH_LONG).show();
                return "";
            }
            return base64Image;
        } catch (IOException | OutOfMemoryError e) {
            Log.e(TAG, "Error converting URI to Base64 or resizing image", e);
            Toast.makeText(this, "Resim işlenirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight <= 0 || maxWidth <= 0 || image == null) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxWidth && height <= maxHeight) return image; // No need to resize
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
        Log.d(TAG, "Save product button clicked.");
        if (!validateInputs()) {
            Toast.makeText(this, "Lütfen zorunlu alanları (*) doldurun ve geçerli değerler girin.", Toast.LENGTH_LONG).show();
            return;
        }
        showLoading(true);

        String imageBase64Data = "";
        if (selectedImageUri != null) {
            Log.d(TAG, "Image selected, converting to Base64...");
            imageBase64Data = convertUriToResizedBase64(selectedImageUri);
            if (TextUtils.isEmpty(imageBase64Data) && selectedImageUri != null) {
                Toast.makeText(this, "Resim işlenemedi. Lütfen farklı bir resim deneyin veya resim seçmeyin.", Toast.LENGTH_LONG).show();
                showLoading(false);
                return;
            }
        } else {
            Log.d(TAG, "No image selected, will save without image.");
        }
        saveProductToFirestore(imageBase64Data);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(editName.getText())) { editName.setError("Gerekli"); editName.requestFocus(); return false; }
        if (TextUtils.isEmpty(editDescription.getText())) { editDescription.setError("Gerekli"); editDescription.requestFocus(); return false; }
        if (TextUtils.isEmpty(editPrice.getText())) { editPrice.setError("Gerekli"); editPrice.requestFocus(); return false; }
        if (TextUtils.isEmpty(editStock.getText())) { editStock.setError("Gerekli"); editStock.requestFocus(); return false; }
        try {
            Double.parseDouble(editPrice.getText().toString());
            Integer.parseInt(editStock.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Sayısal alanlarda geçersiz format.", Toast.LENGTH_SHORT).show();
            if (editPrice.getText().toString().isEmpty() || !editPrice.getText().toString().matches("\\d+(\\.\\d+)?")) {
                editPrice.requestFocus();
            } else if (editStock.getText().toString().isEmpty() || !editStock.getText().toString().matches("\\d+")) {
                editStock.requestFocus();
            }
            return false;
        }
        Log.d(TAG, "Input validation successful.");
        return true;
    }

    private Map<String, String> parseSpecifications(String specInput) {
        Map<String, String> specs = new HashMap<>();
        if (TextUtils.isEmpty(specInput)) return specs;
        String[] pairs = specInput.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2 && !TextUtils.isEmpty(keyValue[0].trim()) && !TextUtils.isEmpty(keyValue[1].trim())) {
                specs.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return specs;
    }

    private List<String> parseListFromString(String input) {
        if (TextUtils.isEmpty(input)) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(input.split("\\s*,\\s*")));
    }

    private void saveProductToFirestore(String imageBase64) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", editName.getText().toString().trim());
        product.put("description", editDescription.getText().toString().trim());
        product.put("price", Double.parseDouble(editPrice.getText().toString().trim()));
        product.put("stock", Integer.parseInt(editStock.getText().toString().trim()));
        product.put("category", spinnerCategory.getSelectedItem().toString());
        product.put("imageBase64", imageBase64 != null ? imageBase64 : ""); // Null check
        product.put("createdAt", FieldValue.serverTimestamp());
        product.put("updatedAt", FieldValue.serverTimestamp());

        product.put("brand", editBrand.getText().toString().trim());
        product.put("modelCode", editModelCode.getText().toString().trim());
        product.put("sellerName", editSellerName.getText().toString().trim());

        product.put("averageRating", 0.0f); // Başlangıç değeri
        product.put("totalReviews", 0);     // Başlangıç değeri

        product.put("isFeatured", switchIsFeatured.isChecked());
        product.put("warrantyInfo", editWarrantyInfo.getText().toString().trim());
        product.put("shippingInfo", editShippingInfo.getText().toString().trim());
        product.put("returnPolicy", editReturnPolicy.getText().toString().trim());
        product.put("specifications", parseSpecifications(editSpecifications.getText().toString().trim()));

        Log.d(TAG, "Saving product: " + product.get("name") + ", ImageBase64 length: " + (imageBase64 != null ? imageBase64.length() : "null"));

        db.collection("products")
                .add(product)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Product added successfully with ID: " + documentReference.getId());
                    Toast.makeText(this, "Ürün başarıyla eklendi!", Toast.LENGTH_LONG).show();
                    clearForm();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding product", e);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    private void clearForm() {
        selectedImageUri = null;
        imageViewProduct.setImageResource(R.drawable.placeholder_image);
        btnSelectImage.setText(R.string.admin_select_image);
        editName.setText(""); editDescription.setText(""); editPrice.setText(""); editStock.setText("");
        editBrand.setText(""); editModelCode.setText(""); editSellerName.setText("");
        editWarrantyInfo.setText(""); editShippingInfo.setText(""); editReturnPolicy.setText("");
        editSpecifications.setText("");
        spinnerCategory.setSelection(0);
        switchIsFeatured.setChecked(false);
        editName.clearFocus();
        Log.d(TAG, "Form cleared.");
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProduct.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
        // Diğer input alanlarını da disable/enable yapabilirsiniz
        for (View v : new View[]{editName, editDescription, editPrice, editStock, editBrand, editModelCode, editSellerName, editWarrantyInfo, editShippingInfo, editReturnPolicy, editSpecifications, spinnerCategory, switchIsFeatured}) {
            if (v!= null) v.setEnabled(!isLoading);
        }
        btnSaveProduct.setText(isLoading ? R.string.admin_saving : R.string.admin_save_product);
        Log.d(TAG, "Loading state: " + isLoading);
    }
}