package com.example.carcare;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FilterActivity extends AppCompatActivity {

    private EditText searchFilter, minPrice, maxPrice;
    private Spinner sortBySpinner;
    private CheckBox categoryOil, categoryFilters, categoryTires, categoryBatteries,
            categoryCleaning, categoryRepair, categoryAccessories;
    private Button applyButton, clearAllButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // View elemanlarını tanımla
        searchFilter = findViewById(R.id.search_filter);
        minPrice = findViewById(R.id.min_price);
        maxPrice = findViewById(R.id.max_price);
        sortBySpinner = findViewById(R.id.sort_by_spinner);

        // Araba kategorilerini tanımla
        categoryOil = findViewById(R.id.category_oil);
        categoryFilters = findViewById(R.id.category_filters);
        categoryTires = findViewById(R.id.category_tires);
        categoryBatteries = findViewById(R.id.category_batteries);
        categoryCleaning = findViewById(R.id.category_cleaning);
        categoryRepair = findViewById(R.id.category_repair);
        categoryAccessories = findViewById(R.id.category_accessories);

        // Butonları tanımla
        applyButton = findViewById(R.id.btn_apply_filters);
        clearAllButton = findViewById(R.id.btn_clear_all);

        // Uygula butonuna tıklama işlemi
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilters();
            }
        });

        // Temizle butonuna tıklama işlemi
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllFilters();
            }
        });

        // Mevcut filtreleri yükle
        loadExistingFilters();
    }

    private void loadExistingFilters() {
        // SharedPreferences'den mevcut filtreleri yükle
        android.content.SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);

        // Arama metnini yükle
        String searchText = prefs.getString("searchText", "");
        searchFilter.setText(searchText);

        // Fiyat aralığını yükle
        float minPriceValue = prefs.getFloat("minPrice", 0f);
        float maxPriceValue = prefs.getFloat("maxPrice", Float.MAX_VALUE);

        if (minPriceValue > 0) {
            minPrice.setText(String.valueOf((int) minPriceValue));
        }
        if (maxPriceValue < Float.MAX_VALUE) {
            maxPrice.setText(String.valueOf((int) maxPriceValue));
        }

        // Kategorileri yükle
        String categories = prefs.getString("categories", "");
        if (!categories.isEmpty()) {
            String[] categoryArray = categories.split(",");
            for (String category : categoryArray) {
                switch (category.trim()) {
                    case "oil":
                        categoryOil.setChecked(true);
                        break;
                    case "filters":
                        categoryFilters.setChecked(true);
                        break;
                    case "tires":
                        categoryTires.setChecked(true);
                        break;
                    case "batteries":
                        categoryBatteries.setChecked(true);
                        break;
                    case "cleaning":
                        categoryCleaning.setChecked(true);
                        break;
                    case "repair":
                        categoryRepair.setChecked(true);
                        break;
                    case "accessories":
                        categoryAccessories.setChecked(true);
                        break;
                }
            }
        }

        // Sıralama seçimini yükle
        String sortBy = prefs.getString("sortBy", "Relevance");
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortOptions[i].equals(sortBy)) {
                sortBySpinner.setSelection(i);
                break;
            }
        }
    }

    public void onHideFiltersClick(View view) {
        finish(); // Activity'yi kapat
    }

    private void applyFilters() {
        // Form doğrulaması
        if (!validateForm()) {
            return;
        }

        // Kategori seçimlerini topla
        java.util.List<String> selectedCategories = collectSelectedCategories();

        // Min-Max fiyat değerlerini al
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

        // Filtre seçimlerini SharedPreferences'e kaydet
        String searchText = searchFilter.getText().toString().trim();
        String sortBy = sortBySpinner.getSelectedItem().toString();

        // Kategori listesini string'e çevir
        StringBuilder categoriesStr = new StringBuilder();
        for (String cat : selectedCategories) {
            categoriesStr.append(cat).append(",");
        }

        getSharedPreferences("FilterPrefs", MODE_PRIVATE)
                .edit()
                .putString("searchText", searchText)
                .putFloat("minPrice", (float) minPriceValue)
                .putFloat("maxPrice", (float) maxPriceValue)
                .putString("sortBy", sortBy)
                .putString("categories", categoriesStr.toString())
                .putBoolean("hasFilters", true)
                .apply();

        Toast.makeText(this, "Filtreler uygulandı", Toast.LENGTH_SHORT).show();

        // Ana aktiviteye dön
        finish();
    }

    private java.util.List<String> collectSelectedCategories() {
        java.util.List<String> selectedCategories = new java.util.ArrayList<>();

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
        // Min-Max fiyat kontrolü
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
        // Tüm filtre alanlarını temizle
        searchFilter.setText("");
        minPrice.setText("");
        maxPrice.setText("");
        sortBySpinner.setSelection(0);

        // Tüm kategorilerin işaretini kaldır
        categoryOil.setChecked(false);
        categoryFilters.setChecked(false);
        categoryTires.setChecked(false);
        categoryBatteries.setChecked(false);
        categoryCleaning.setChecked(false);
        categoryRepair.setChecked(false);
        categoryAccessories.setChecked(false);

        // SharedPreferences'den filtre bilgilerini temizle
        getSharedPreferences("FilterPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Toast.makeText(this, "Tüm filtreler temizlendi", Toast.LENGTH_SHORT).show();
    }
}