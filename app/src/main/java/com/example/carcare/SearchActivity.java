package com.example.carcare; // Paket adınızı kontrol edin

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.StoreActivity; // StoreActivity importu
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_SEARCH_HISTORY = "SearchHistory";
    private static final int MAX_HISTORY_SIZE = 10;

    private EditText editTextSearchActual;
    private ImageButton btnSearchBack, btnSearchWithCamera;
    private TextView textSearchAction;
    private ChipGroup chipGroupSearchHistory;
    private RelativeLayout layoutSearchHistoryHeader;
    private TextView btnClearSearchHistory;

    // İsteğe bağlı: Son gezilen ürünler ve popüler aramalar için
    private RecyclerView recyclerLastViewedProducts;
    private ChipGroup chipGroupPopularSearches;
    // private LastViewedAdapter lastViewedAdapter;
    // private List<Product> lastViewedProductList;

    private SharedPreferences searchPrefs;
    private List<String> searchHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        loadSearchHistory();
        setupListeners();

        // Aktivite açıldığında EditText'e odaklan ve klavyeyi göster
        editTextSearchActual.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editTextSearchActual, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void initViews() {
        btnSearchBack = findViewById(R.id.btn_search_back);
        editTextSearchActual = findViewById(R.id.edit_text_search_actual);
        btnSearchWithCamera = findViewById(R.id.btn_search_with_camera); // XML'de ID olduğundan emin olun
        textSearchAction = findViewById(R.id.text_search_action);
        chipGroupSearchHistory = findViewById(R.id.chip_group_search_history);
        layoutSearchHistoryHeader = findViewById(R.id.layout_search_history_header);
        btnClearSearchHistory = findViewById(R.id.btn_clear_search_history);

        // İsteğe bağlı kısımlar
        // recyclerLastViewedProducts = findViewById(R.id.recycler_last_viewed_products);
        // chipGroupPopularSearches = findViewById(R.id.chip_group_popular_searches);
    }

    private void setupListeners() {
        btnSearchBack.setOnClickListener(v -> finish());

        textSearchAction.setOnClickListener(v -> performSearch());

        editTextSearchActual.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        editTextSearchActual.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textSearchAction.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearchHistory.setOnClickListener(v -> {
            clearSearchHistory();
            loadSearchHistory(); // UI'ı güncelle
        });

        // btnSearchWithCamera.setOnClickListener(v -> { /* TODO: Kamera ile arama */ });
    }

    private void performSearch() {
        String query = editTextSearchActual.getText().toString().trim();
        if (!TextUtils.isEmpty(query)) {
            hideKeyboard();
            saveSearchQuery(query);
            // Arama sonucunu StoreActivity'ye geri gönder
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SEARCH_QUERY", query);
            setResult(Activity.RESULT_OK, resultIntent);
            finish(); // SearchActivity'yi kapat
        } else {
            Toast.makeText(this, "Lütfen bir arama terimi girin", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSearchQuery(String query) {
        Set<String> historySet = new HashSet<>(searchHistoryList); // Mevcut geçmişi al (kopya)

        // Eğer sorgu zaten varsa, önce onu kaldırıp başa ekleyeceğiz (en son aranan olması için)
        historySet.remove(query);

        List<String> tempList = new ArrayList<>(historySet);
        tempList.add(0, query); // Yeni sorguyu başa ekle

        // Maksimum geçmiş boyutunu kontrol et
        while (tempList.size() > MAX_HISTORY_SIZE) {
            tempList.remove(tempList.size() - 1); // En eski olanı kaldır
        }
        searchHistoryList = new ArrayList<>(tempList); // Ana listeyi güncelle

        // SharedPreferences'e kaydet
        SharedPreferences.Editor editor = searchPrefs.edit();
        editor.putStringSet(KEY_SEARCH_HISTORY, new HashSet<>(searchHistoryList));
        editor.apply();
    }

    private void loadSearchHistory() {
        Set<String> historySet = searchPrefs.getStringSet(KEY_SEARCH_HISTORY, new HashSet<>());
        searchHistoryList = new ArrayList<>(historySet);
        // Genellikle en son arananlar üstte olsun istenir, ancak Set sıralamayı garanti etmez.
        // Kaydederken listeyi sıralı tuttuğumuz için burada direkt kullanabiliriz.
        // Veya burada Collections.reverse(searchHistoryList); yapılabilir.
        // Şimdilik saveSearchQuery'deki sıralamaya güveniyoruz.

        chipGroupSearchHistory.removeAllViews();
        if (searchHistoryList.isEmpty()) {
            layoutSearchHistoryHeader.setVisibility(View.GONE);
            chipGroupSearchHistory.setVisibility(View.GONE);
        } else {
            layoutSearchHistoryHeader.setVisibility(View.VISIBLE);
            chipGroupSearchHistory.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (String query : searchHistoryList) {
                View chipView = inflater.inflate(R.layout.item_search_history, chipGroupSearchHistory, false);
                TextView textQuery = chipView.findViewById(R.id.text_search_query);
                ImageButton btnRemove = chipView.findViewById(R.id.btn_remove_search_history);

                textQuery.setText(query);
                chipView.setOnClickListener(v -> {
                    editTextSearchActual.setText(query);
                    editTextSearchActual.setSelection(query.length()); // İmleci sona taşı
                    performSearch();
                });
                btnRemove.setOnClickListener(v -> {
                    removeSearchQuery(query);
                    loadSearchHistory(); // UI'ı güncelle
                });
                chipGroupSearchHistory.addView(chipView);
            }
        }
    }

    private void removeSearchQuery(String query) {
        searchHistoryList.remove(query);
        SharedPreferences.Editor editor = searchPrefs.edit();
        editor.putStringSet(KEY_SEARCH_HISTORY, new HashSet<>(searchHistoryList));
        editor.apply();
    }

    private void clearSearchHistory() {
        searchHistoryList.clear();
        SharedPreferences.Editor editor = searchPrefs.edit();
        editor.remove(KEY_SEARCH_HISTORY); // Veya editor.putStringSet(KEY_SEARCH_HISTORY, new HashSet<>());
        editor.apply();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    // Son gezilen ürünler ve popüler aramalar için metodlar eklenebilir.
    // Örneğin:
    // private void loadLastViewedProducts() { /* ... */ }
    // private void loadPopularSearches() { /* ... */ }
}