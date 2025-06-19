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
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {
    private static final String TAG = "FilterActivity";

    // UI Elements
    private EditText  minPrice, maxPrice;
    private Spinner sortBySpinner;
    private Button applyButton, clearAllButton, hideFiltersButton;

    // Category CheckBoxes - Araba kategorileri
    private CheckBox categoryMotorOil, categoryFilters, categoryBrakeParts,
            categoryTires, categoryBatteries, categoryCleaning,
            categoryTools, categoryAccessories, categoryLights, categoryElectronics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        initViews();
        setupSpinner();
        setupEventListeners();
        loadExistingFilters();
    }

    private void initViews() {
        // Text ve numeric inputs
        minPrice = findViewById(R.id.min_price);
        maxPrice = findViewById(R.id.max_price);
        sortBySpinner = findViewById(R.id.sort_by_spinner);

        // Araba kategorileri checkboxları
        categoryMotorOil = findViewById(R.id.category_motor_oil);
        categoryFilters = findViewById(R.id.category_filters);
        categoryBrakeParts = findViewById(R.id.category_brake_parts);
        categoryTires = findViewById(R.id.category_tires);
        categoryBatteries = findViewById(R.id.category_batteries);
        categoryCleaning = findViewById(R.id.category_cleaning);
        categoryTools = findViewById(R.id.category_tools);
        categoryAccessories = findViewById(R.id.category_accessories);
        categoryLights = findViewById(R.id.category_lights);
        categoryElectronics = findViewById(R.id.category_electronics);

        // Buttons
        applyButton = findViewById(R.id.btn_apply_filters);
        clearAllButton = findViewById(R.id.btn_clear_all);
        hideFiltersButton = findViewById(R.id.btn_hide_filters);

        Log.d(TAG, "Views initialized");
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);
    }

    private void setupEventListeners() {
        applyButton.setOnClickListener(v -> applyFilters());
        clearAllButton.setOnClickListener(v -> clearAllFilters());
        hideFiltersButton.setOnClickListener(v -> finish());
    }

    private void loadExistingFilters() {
        SharedPreferences prefs = getSharedPreferences("FilterPrefs", MODE_PRIVATE);


        // Fiyat aralığı
        float minPriceValue = prefs.getFloat("minPrice", 0f);
        if (minPriceValue > 0) {
            minPrice.setText(String.valueOf((int) minPriceValue));
        }

        float maxPriceValue = prefs.getFloat("maxPrice", Float.MAX_VALUE);
        if (maxPriceValue < Float.MAX_VALUE) {
            maxPrice.setText(String.valueOf((int) maxPriceValue));
        }

        // Kategoriler
        String categories = prefs.getString("categories", "");
        if (!categories.isEmpty()) {
            String[] categoryArray = categories.split(",");
            for (String category : categoryArray) {
                setCategory(category.trim(), true);
            }
        }

        // Sıralama
        String sortBy = prefs.getString("sortBy", "Relevance");
        String[] sortOptions = getResources().getStringArray(R.array.sort_options);
        for (int i = 0; i < sortOptions.length; i++) {
            if (sortOptions[i].equals(sortBy)) {
                sortBySpinner.setSelection(i);
                break;
            }
        }

        Log.d(TAG, "Existing filters loaded");
    }

    private void setCategory(String category, boolean checked) {
        switch (category.toLowerCase()) {
            case "motor_oil":
                categoryMotorOil.setChecked(checked);
                break;
            case "filters":
                categoryFilters.setChecked(checked);
                break;
            case "brake_parts":
                categoryBrakeParts.setChecked(checked);
                break;
            case "tires":
                categoryTires.setChecked(checked);
                break;
            case "batteries":
                categoryBatteries.setChecked(checked);
                break;
            case "cleaning":
                categoryCleaning.setChecked(checked);
                break;
            case "tools":
                categoryTools.setChecked(checked);
                break;
            case "accessories":
                categoryAccessories.setChecked(checked);
                break;
            case "lights":
                categoryLights.setChecked(checked);
                break;
            case "electronics":
                categoryElectronics.setChecked(checked);
                break;
        }
    }

    private void applyFilters() {
        if (!validateForm()) return;

        // Seçili kategorileri topla
        List<String> selectedCategories = collectSelectedCategories();

        // Fiyat değerlerini al
        double minPriceValue = 0;
        double maxPriceValue = Double.MAX_VALUE;

        try {
            if (!minPrice.getText().toString().trim().isEmpty()) {
                minPriceValue = Double.parseDouble(minPrice.getText().toString().trim());
            }
            if (!maxPrice.getText().toString().trim().isEmpty()) {
                maxPriceValue = Double.parseDouble(maxPrice.getText().toString().trim());
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Lütfen geçerli fiyat değerleri girin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Diğer değerleri al
        String sortBy = sortBySpinner.getSelectedItem().toString();
        String categoriesStr = String.join(",", selectedCategories);

        // SharedPreferences'a kaydet
        SharedPreferences.Editor editor = getSharedPreferences("FilterPrefs", MODE_PRIVATE).edit();
        editor.putFloat("minPrice", (float) minPriceValue);
        editor.putFloat("maxPrice", (float) maxPriceValue);
        editor.putString("sortBy", sortBy);
        editor.putString("categories", categoriesStr);

        // Filtre var mı kontrolü
        boolean hasFilters = minPriceValue > 0 ||
                maxPriceValue < Double.MAX_VALUE ||
                !selectedCategories.isEmpty() ||
                !"Relevance".equals(sortBy);

        editor.putBoolean("hasFilters", hasFilters);
        editor.apply();

        Log.d(TAG, "Filters applied: " + hasFilters);
        Toast.makeText(this, "Filtreler uygulandı", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK);
        finish();
    }

    private List<String> collectSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        if (categoryMotorOil.isChecked()) selectedCategories.add("motor_oil");
        if (categoryFilters.isChecked()) selectedCategories.add("filters");
        if (categoryBrakeParts.isChecked()) selectedCategories.add("brake_parts");
        if (categoryTires.isChecked()) selectedCategories.add("tires");
        if (categoryBatteries.isChecked()) selectedCategories.add("batteries");
        if (categoryCleaning.isChecked()) selectedCategories.add("cleaning");
        if (categoryTools.isChecked()) selectedCategories.add("tools");
        if (categoryAccessories.isChecked()) selectedCategories.add("accessories");
        if (categoryLights.isChecked()) selectedCategories.add("lights");
        if (categoryElectronics.isChecked()) selectedCategories.add("electronics");

        return selectedCategories;
    }

    private boolean validateForm() {
        String minText = minPrice.getText().toString().trim();
        String maxText = maxPrice.getText().toString().trim();

        if (!minText.isEmpty() && !maxText.isEmpty()) {
            try {
                double min = Double.parseDouble(minText);
                double max = Double.parseDouble(maxText);

                if (min < 0 || max < 0) {
                    Toast.makeText(this, "Fiyat değerleri negatif olamaz", Toast.LENGTH_SHORT).show();
                    return false;
                }

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
        // Tüm alanları temizle
        minPrice.setText("");
        maxPrice.setText("");
        sortBySpinner.setSelection(0); // Relevance

        // Tüm kategorileri temizle
        categoryMotorOil.setChecked(false);
        categoryFilters.setChecked(false);
        categoryBrakeParts.setChecked(false);
        categoryTires.setChecked(false);
        categoryBatteries.setChecked(false);
        categoryCleaning.setChecked(false);
        categoryTools.setChecked(false);
        categoryAccessories.setChecked(false);
        categoryLights.setChecked(false);
        categoryElectronics.setChecked(false);

        // SharedPreferences'ı temizle
        SharedPreferences.Editor editor = getSharedPreferences("FilterPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.putBoolean("hasFilters", false);
        editor.apply();

        Log.d(TAG, "All filters cleared");
        Toast.makeText(this, "Tüm filtreler temizlendi", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK);
    }
}