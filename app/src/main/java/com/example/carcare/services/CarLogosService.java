package com.example.carcare.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Map;

public class CarLogosService {
    private static final String TAG = "CarLogosService";
    private static final String COLLECTION_CAR_BRAND_LOGOS = "car_brand_logos";

    private FirebaseFirestore db;

    public CarLogosService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface LogoLoadListener {
        void onLogoBitmapLoaded(Bitmap bitmap);
        void onLogoUrlLoaded(String logoUrl);
        void onLogoNotFound();
        void onError(Exception e);
    }

    /**
     * Verilen marka adına göre logoyu Firestore'dan yükler.
     * Önce exact match dener, bulamazsa fuzzy matching yapar.
     */
    public void loadLogoForBrand(String rawBrandName, @NonNull LogoLoadListener listener) {
        if (rawBrandName == null || rawBrandName.trim().isEmpty()) {
            Log.w(TAG, "loadLogoForBrand: Marka adı boş veya null.");
            listener.onLogoNotFound();
            return;
        }

        String sanitizedBrand = sanitizeBrandNameForFirestore(rawBrandName);
        Log.d(TAG, "loadLogoForBrand: '" + rawBrandName + "' -> '" + sanitizedBrand + "'");

        if (sanitizedBrand.isEmpty()) {
            Log.w(TAG, "loadLogoForBrand: Sanitize edilmiş marka adı boş kaldı.");
            listener.onLogoNotFound();
            return;
        }

        // 1. Önce exact match dene
        tryExactMatch(sanitizedBrand, rawBrandName, listener);
    }

    /**
     * Tam eşleşme dener
     */
    private void tryExactMatch(String sanitizedBrand, String originalBrand, LogoLoadListener listener) {
        db.collection(COLLECTION_CAR_BRAND_LOGOS).document(sanitizedBrand)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Exact match bulundu: " + sanitizedBrand);
                        loadLogoFromDocument(documentSnapshot, listener);
                    } else {
                        Log.d(TAG, "❌ Exact match bulunamadı: " + sanitizedBrand + ", fuzzy search başlatılıyor...");
                        // 2. Exact match bulunamadıysa fuzzy matching dene
                        tryFuzzyMatch(originalBrand, sanitizedBrand, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Exact match sorgusunda hata: " + sanitizedBrand, e);
                    // Hata durumunda da fuzzy matching dene
                    tryFuzzyMatch(originalBrand, sanitizedBrand, listener);
                });
    }

    /**
     * Veritabanındaki tüm markaları kontrol ederek benzer eşleşme arar
     */
    private void tryFuzzyMatch(String originalBrand, String sanitizedBrand, LogoLoadListener listener) {
        Log.d(TAG, "🔍 Fuzzy matching başlatılıyor: " + originalBrand);

        db.collection(COLLECTION_CAR_BRAND_LOGOS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String bestMatch = null;
                    int bestScore = 0;
                    DocumentSnapshot bestDocument = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();

                        // Çeşitli benzerlik kontrolleri
                        int score = calculateSimilarityScore(originalBrand, sanitizedBrand, documentId);

                        Log.v(TAG, "Similarity: '" + documentId + "' -> " + score);

                        if (score > bestScore && score >= 70) { // %70 benzerlik threshold
                            bestScore = score;
                            bestMatch = documentId;
                            bestDocument = document;
                        }
                    }

                    if (bestMatch != null && bestDocument != null) {
                        Log.i(TAG, "✅ Fuzzy match bulundu: '" + originalBrand + "' -> '" + bestMatch + "' (Score: " + bestScore + ")");
                        loadLogoFromDocument(bestDocument, listener);
                    } else {
                        Log.w(TAG, "❌ Hiçbir eşleşme bulunamadı: " + originalBrand);
                        listener.onLogoNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fuzzy matching sorgusunda hata", e);
                    listener.onError(e);
                });
    }

    /**
     * Benzerlik skorunu hesaplar (0-100 arası)
     */
    private int calculateSimilarityScore(String originalBrand, String sanitizedBrand, String documentId) {
        // Tüm stringleri normalize et
        String normalizedOriginal = normalizeForComparison(originalBrand);
        String normalizedSanitized = normalizeForComparison(sanitizedBrand);
        String normalizedDocId = normalizeForComparison(documentId);

        int maxScore = 0;

        // 1. Tam eşleşme kontrolü
        if (normalizedSanitized.equals(normalizedDocId)) {
            return 100;
        }
        if (normalizedOriginal.equals(normalizedDocId)) {
            return 100;
        }

        // 2. Contains kontrolü
        if (normalizedDocId.contains(normalizedSanitized) || normalizedSanitized.contains(normalizedDocId)) {
            maxScore = Math.max(maxScore, 90);
        }
        if (normalizedDocId.contains(normalizedOriginal) || normalizedOriginal.contains(normalizedDocId)) {
            maxScore = Math.max(maxScore, 85);
        }

        // 3. Başlangıç kontrolü
        if (normalizedDocId.startsWith(normalizedSanitized) || normalizedSanitized.startsWith(normalizedDocId)) {
            maxScore = Math.max(maxScore, 80);
        }

        // 4. Kelime bazlı kontrol
        String[] originalWords = normalizedOriginal.split("[\\s_-]+");
        String[] docWords = normalizedDocId.split("[\\s_-]+");

        int wordMatches = 0;
        for (String originalWord : originalWords) {
            for (String docWord : docWords) {
                if (originalWord.equals(docWord) && originalWord.length() > 2) {
                    wordMatches++;
                    break;
                }
            }
        }

        if (wordMatches > 0) {
            int wordScore = (wordMatches * 60) / Math.max(originalWords.length, docWords.length);
            maxScore = Math.max(maxScore, wordScore);
        }

        // 5. Levenshtein distance
        int distance = levenshteinDistance(normalizedSanitized, normalizedDocId);
        int maxLength = Math.max(normalizedSanitized.length(), normalizedDocId.length());
        if (maxLength > 0) {
            int similarity = (int) (((double) (maxLength - distance) / maxLength) * 50);
            maxScore = Math.max(maxScore, similarity);
        }

        return maxScore;
    }

    /**
     * Karşılaştırma için string normalizasyonu
     */
    private String normalizeForComparison(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]", "") // Sadece harf ve rakam bırak
                .trim();
    }

    /**
     * Levenshtein distance hesaplama
     */
    private int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Dokümandan logo yükler
     */
    private void loadLogoFromDocument(DocumentSnapshot document, LogoLoadListener listener) {
        // Öncelik: logoBase64
        if (document.contains("logoBase64")) {
            String base64String = document.getString("logoBase64");
            if (base64String != null && !base64String.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        Log.d(TAG, "Base64 logo başarıyla yüklendi: " + document.getId());
                        listener.onLogoBitmapLoaded(bitmap);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Base64 decode hatası: " + document.getId(), e);
                }
            }
        }

        // İkinci seçenek: logoUrl
        if (document.contains("logoUrl")) {
            String logoUrl = document.getString("logoUrl");
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Log.d(TAG, "Logo URL bulundu: " + logoUrl);
                listener.onLogoUrlLoaded(logoUrl);
                return;
            }
        }

        Log.w(TAG, "Dokümanda geçerli logo verisi bulunamadı: " + document.getId());
        listener.onLogoNotFound();
    }

    /**
     * Basit sanitizasyon - sadece temel temizlik
     */
    private String sanitizeBrandNameForFirestore(String brandName) {
        if (brandName == null) return "";

        String sanitized = brandName.toLowerCase().trim();

        // Yaygın şirket son eklerini kaldır
        String[] suffixesToRemove = {
                " ag", " gmbh", " & co. kg", " co. kg", " & co", " co",
                " ltd", " inc", " motors", " motor company", " corporation",
                " corp", " group", " s.a.", " s.a.s.", " s.p.a.", " llc", " pty",
                " automotive", " auto"
        };

        for (String suffix : suffixesToRemove) {
            if (sanitized.endsWith(suffix)) {
                sanitized = sanitized.substring(0, sanitized.length() - suffix.length()).trim();
                break;
            }
        }

        // Temel temizlik
        sanitized = sanitized
                .replaceAll("\\.", "")           // Noktaları kaldır
                .replaceAll("[^a-z0-9\\s-]", "") // Sadece alfanumerik, boşluk, tire
                .replaceAll("\\s+", "_")         // Boşlukları alt çizgiye
                .replace("-", "_")               // Tireleri alt çizgiye
                .replaceAll("__+", "_")          // Çoklu alt çizgileri tek yap
                .replaceAll("^_|_$", "");        // Baş/son alt çizgileri kaldır

        return sanitized;
    }

    /**
     * VIN detaylarından marka adını çıkarır
     */
    public static String extractBrandFromVinDetails(Map<String, Object> vinDetails) {
        if (vinDetails == null) {
            Log.w(TAG, "extractBrandFromVinDetails: vinDetails map is null.");
            return null;
        }

        // Öncelik: Make
        String make = (String) vinDetails.get(com.example.carcare.UserVehicleService.FIELD_DETAIL_MAKE);
        if (make != null && !make.trim().isEmpty()) {
            Log.d(TAG, "extractBrandFromVinDetails: 'make' alanından marka çıkarıldı: " + make.trim());
            return make.trim();
        }

        // İkinci Öncelik: Manufacturer
        String manufacturer = (String) vinDetails.get(com.example.carcare.UserVehicleService.FIELD_DETAIL_MANUFACTURER);
        if (manufacturer != null && !manufacturer.trim().isEmpty()) {
            Log.d(TAG, "extractBrandFromVinDetails: 'manufacturer' alanından marka çıkarıldı: " + manufacturer.trim());
            return manufacturer.trim();
        }

        Log.w(TAG, "extractBrandFromVinDetails: 'make' veya 'manufacturer' alanından marka çıkarılamadı.");
        return null;
    }
}