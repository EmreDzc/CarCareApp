package com.example.carcare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {
    private static final String TAG = "FilterActivity";

    private EditText searchFilter, minPrice, maxPrice;
    private Spinner sortBySpinner;
    private CheckBox categoryOil, categoryFilters, categoryTires, categoryBatteries,
            categoryCleaning, categoryRepair, categoryAccessories;
    private Button applyButton, clearAllButton, hideFiltersButton; // hideFiltersButton eklendi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        searchFilter = findViewById(R.id.search_filter);
        minPrice = findViewById(R.id.min_price);
        maxPrice = findViewById(R.id.max_price);
        sortBySpinner = findViewById(R.id.sort_by_spinner);

        categoryOil = findViewById(R.id.category_oil);
        categoryFilters = findViewById(R.id.category_filters);
        categoryTires = findViewById(R.id.category_tires);
        categoryBatteries = findViewById(R.id.category_batteries);
        categoryCleaning = findViewById(R.id.category_cleaning);
        categoryRepair = findViewById(R.id.category_repair);
        categoryAccessories = findViewById(R.id.category_accessories);

        applyButton = findViewById(R.id.btn_apply_filters);
        clearAllButton = findViewById(R.id.btn_clear_all);
        // XML'deki ID ile eşleştirildi
        hideFiltersButton = findViewById(R.id.btn_hide_filters);


        applyButton.setOnClickListener(v -> applyFilters());
        clearAllButton.setOnClickListener(v -> clearAllFilters());

        // hideFiltersButton için listener. XML'deki android:onClick="onHideFiltersClick" zaten bu işi yapıyor.
        // Ancak programatik olarak da eklenebilir, XML'deki onClick kaldırılırsa bu kullanılır.
        if (hideFiltersButton != null) {
            hideFiltersButton.setOnClickListener(v -> onHideFiltersClick(v));
        } else {
            Log.w(TAG, "Filtreleri gizle butonu (btn_hide_filters) layout'ta bulunamadı.");
        }

        // Genel bir geri butonu için arama kaldırıldı çünkü XML'de öyle bir ID yok.
        // "Hide Filters" butonu bu işlevi görüyor.

        loadExistingFilters();
    }

    private void loadExistingFilters() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);
        searchFilter.setText(prefs.getString("searchText", ""));

        float minPriceValue = prefs.getFloat("minPrice", 0f);
        if (minPriceValue > 0) minPrice.setText(String.valueOf((int) minPriceValue));

        float maxPriceValue = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        if (maxPriceValue < Float.MAX_VALUE) maxPrice.setText(String.valueOf((int) maxPriceValue));

        String categories = prefs.getString("categories", "");
        if (!categories.isEmpty()) {
            String[] categoryArray = categories.split(",");
            for (String category : categoryArray) {
                switch (category.trim().toLowerCase()) {
                    case "oil": categoryOil.setChecked(true); break;
                    case "filters": categoryFilters.setChecked(true); break;
                    case "tires": categoryTires.setChecked(true); break;
                    case "batteries": categoryBatteries.setChecked(true); break;
                    case "cleaning": categoryCleaning.setChecked(true); break;
                    case "repair": categoryRepair.setChecked(true); break;
                    case "accessories": categoryAccessories.setChecked(true); break;
                }
            }
        }

        String sortBy = prefs.getString("sortBy", "Relevance");
        // Spinner'ın adaptöründeki değerlerle eşleştirmek için array'i alıyoruz
        String[] sortOptions = getResources().getStringArray(R.array.sort_options); // R.array.sort_options strings.xml'de tanımlı olmalı
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortOptions[i].equals(sortBy)) {
                sortBySpinner.setSelection(i);
                break;
            }
        }
    }

    // Bu metod XML'deki android:onClick="onHideFiltersClick" ile çağrılıyor.
    // Eğer XML'den onClick kaldırılırsa, onCreate içinde programatik olarak setOnClickListener kullanılabilir.
    public void onHideFiltersClick(View view) {
        finish();
    }

    private void applyFilters() {
        if (!validateForm()) return;

        List<String> selectedCategories = collectSelectedCategories();
        double minPriceValue = 0;
        double maxPriceValue = Double.MAX_VALUE;

        try {
            if (!minPrice.getText().toString().isEmpty()) {
                minPriceValue = Double.parseDouble(minPrice.getText().toString());
            }
            if (!maxPrice.getText().toString().isEmpty()) {
                maxPriceValue = Double.parseDouble(maxPrice.getText().toString());
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Lütfen geçerli fiyat değerleri girin", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchText = searchFilter.getText().toString().trim();
        String sortBy = sortBySpinner.getSelectedItem().toString();
        String categoriesStr = String.join(",", selectedCategories);

        SharedPreferences.Editor editor = getSharedPreferences("FilterPrefs", MODE_PRIVATE).edit();
        editor.putString("searchText", searchText);
        editor.putFloat("minPrice", (float) minPriceValue);
        editor.putFloat("maxPrice", (float) maxPriceValue);
        editor.putString("sortBy", sortBy);
        editor.putString("categories", categoriesStr);
        editor.putBoolean("hasFilters", true);
        editor.apply();

        Toast.makeText(this, "Filtreler uygulandı", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private List<String> collectSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();
        if (categoryOil.isChecked()) selectedCategories.add("oil");
        if (categoryFilters.isChecked()) selectedCategories.add("filters");
        if (categoryTires.isChecked()) selectedCategories.add("tires");
        if (categoryBatteries.isChecked()) selectedCategories.add("batteries");
        if (categoryCleaning.isChecked()) selectedCategories.add("cleaning");
        if (categoryRepair.isChecked()) selectedCategories.add("repair");
        if (categoryAccessories.isChecked()) selectedCategories.add("accessories");
        return selectedCategories;
    }

    private boolean validateForm() {
        if (!minPrice.getText().toString().isEmpty() && !maxPrice.getText().toString().isEmpty()) {
            try {
                double min = Double.parseDouble(minPrice.getText().toString());
                double max = Double.parseDouble(maxPrice.getText().toString());
                if (min > max) {
                    Toast.makeText(this, "Minimum fiyat, maksimum fiyattan büyük olamaz", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Lütfen geçerli fiyat değerleri girin", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void clearAllFilters() {
        searchFilter.setText("");
        minPrice.setText("");
        maxPrice.setText("");
        sortBySpinner.setSelection(0);
        categoryOil.setChecked(false);
        categoryFilters.setChecked(false);
        categoryTires.setChecked(false);
        categoryBatteries.setChecked(false);
        categoryCleaning.setChecked(false);
        categoryRepair.setChecked(false);
        categoryAccessories.setChecked(false);

        SharedPreferences.Editor editor = getSharedPreferences("FilterPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.putBoolean("hasFilters", false);
        editor.apply();

        Toast.makeText(this, "Tüm filtreler temizlendi", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        // finish(); // İsteğe bağlı, temizledikten sonra otomatik kapatılsın mı?
    }
}