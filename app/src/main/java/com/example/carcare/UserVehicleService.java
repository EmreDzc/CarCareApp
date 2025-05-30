// com/example/carcare/UserVehicleService.java

package com.example.carcare;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.carcare.api.NhtsaApiService;
import com.example.carcare.api.RetrofitClient;
import com.example.carcare.api.NhtsaVehicleInfo;
import com.example.carcare.api.NhtsaVinResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
// AlertDialog ve LayoutInflater importları bu sınıfta kullanılmıyor, kaldırılabilir.

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserVehicleService {
    private static final String TAG = "UserVehicleService";
    private static final String COLLECTION_USERS = "users";
    public static final String FIELD_VEHICLE_VIN = "vehicleVin";
    private static final String FIELD_LAST_VIN_CHECK_TIMESTAMP = "lastVinCheckTimestamp";
    public static final String MAP_VIN_DETAILS = "vinDetails";

    public static final String FIELD_DETAIL_MAKE = "make";
    public static final String FIELD_DETAIL_MODEL = "model";
    public static final String FIELD_DETAIL_YEAR = "modelYear";
    public static final String FIELD_DETAIL_MANUFACTURER = "manufacturer";
    public static final String FIELD_DETAIL_VEHICLE_TYPE = "vehicleType";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final NhtsaApiService nhtsaApiService;

    public UserVehicleService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        nhtsaApiService = RetrofitClient.getNhtsaClient().create(NhtsaApiService.class);
    }

    public interface VinUpdateCallback {
        void onSuccess(String vin, boolean newVinRegistered, Map<String, Object> vehicleDetails);
        void onFailure(Exception e);
        void onVinAlreadyCurrent(String vin, Map<String, Object> existingDetails);
    }

    public interface VehicleDetailsFetchCallback {
        void onDetailsFetched(Map<String, Object> newDetails);
        void onFetchFailed(Exception e);
        void onNoDetailsFoundOrNotNeeded();
    }

    public void updateProfileWithVin(String newVin, VinUpdateCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Kullanıcı oturumu açık değil, VIN güncellenemiyor.");
            callback.onFailure(new Exception("User not logged in."));
            return;
        }
        if (newVin == null || newVin.trim().isEmpty() || newVin.trim().length() != 17) { // trim() eklendi
            Log.w(TAG, "Geçersiz VIN sağlandı: " + newVin);
            callback.onFailure(new Exception("Invalid VIN provided. VIN must be 17 characters."));
            return;
        }

        final String finalNewVin = newVin.trim().toUpperCase(); // VIN'i trim ve uppercase yapalım
        String userId = currentUser.getUid();
        DocumentReference userDocRef = db.collection(COLLECTION_USERS).document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String currentStoredVin = null;
                Map<String, Object> existingDetails = null;

                if (document != null && document.exists()) {
                    currentStoredVin = document.getString(FIELD_VEHICLE_VIN);
                    if (document.contains(MAP_VIN_DETAILS)) {
                        existingDetails = (Map<String, Object>) document.get(MAP_VIN_DETAILS);
                    }
                }

                final String finalCurrentStoredVin = currentStoredVin;
                final Map<String, Object> finalExistingDetails = existingDetails;

                if (finalNewVin.equals(finalCurrentStoredVin)) {
                    Log.d(TAG, "Sağlanan VIN (" + finalNewVin + ") zaten Firestore'da güncel.");
                    updateLastVinCheckTimestamp(userDocRef);
                    if (finalExistingDetails == null || finalExistingDetails.isEmpty()) {
                        Log.d(TAG, "VIN güncel ama detaylar eksik/yok, NHTSA API'den çekiliyor...");
                        fetchAndSaveVinDetailsFromNhtsa(userId, finalNewVin, new VehicleDetailsFetchCallback() {
                            @Override
                            public void onDetailsFetched(Map<String, Object> fetchedNewDetails) {
                                Map<String, Object> update = new HashMap<>();
                                update.put(MAP_VIN_DETAILS, fetchedNewDetails);
                                userDocRef.set(update, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.i(TAG, "Mevcut VIN için eksik detaylar başarıyla Firestore'a kaydedildi.");
                                            callback.onVinAlreadyCurrent(finalNewVin, fetchedNewDetails);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Mevcut VIN için eksik detaylar kaydedilirken hata.", e);
                                            callback.onVinAlreadyCurrent(finalNewVin, null);
                                        });
                            }
                            @Override
                            public void onFetchFailed(Exception e) {
                                Log.e(TAG, "Mevcut VIN için detay çekme başarısız oldu (API hatası).", e);
                                callback.onVinAlreadyCurrent(finalNewVin, null);
                            }
                            @Override
                            public void onNoDetailsFoundOrNotNeeded() {
                                Log.w(TAG, "Mevcut VIN için NHTSA'dan detay bulunamadı/gerekli değil.");
                                callback.onVinAlreadyCurrent(finalNewVin, finalExistingDetails);
                            }
                        });
                    } else {
                        callback.onVinAlreadyCurrent(finalNewVin, finalExistingDetails);
                    }
                } else {
                    Log.d(TAG, "Yeni/farklı VIN (" + finalNewVin + "), NHTSA API'den detaylar çekiliyor...");
                    fetchAndSaveVinDetailsFromNhtsa(userId, finalNewVin, new VehicleDetailsFetchCallback() {
                        @Override
                        public void onDetailsFetched(Map<String, Object> fetchedNewDetails) {
                            saveVinAndDetailsToFirestore(userDocRef, finalNewVin, fetchedNewDetails, true, callback);
                        }
                        @Override
                        public void onFetchFailed(Exception e) {
                            Log.e(TAG, "Yeni VIN için detay çekme başarısız oldu, sadece VIN kaydedilecek.", e);
                            saveVinAndDetailsToFirestore(userDocRef, finalNewVin, null, true, callback);
                        }
                        @Override
                        public void onNoDetailsFoundOrNotNeeded() {
                            Log.w(TAG, "Yeni VIN için NHTSA'dan detay bulunamadı/gerekli değil, sadece VIN kaydedilecek.");
                            saveVinAndDetailsToFirestore(userDocRef, finalNewVin, null, true, callback);
                        }
                    });
                }
            } else {
                Log.e(TAG, "Firestore'dan kullanıcı dokümanı alınırken hata", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }

    private void updateLastVinCheckTimestamp(DocumentReference userDocRef) {
        Map<String, Object> timestampUpdate = new HashMap<>();
        timestampUpdate.put(FIELD_LAST_VIN_CHECK_TIMESTAMP, System.currentTimeMillis());
        userDocRef.set(timestampUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Son VIN kontrol zamanı güncellendi."))
                .addOnFailureListener(e -> Log.w(TAG, "Son VIN kontrol zamanı güncellenirken hata", e));
    }

    private void saveVinAndDetailsToFirestore(DocumentReference userDocRef, String vin, Map<String, Object> details, boolean newVinRegistered, VinUpdateCallback callback) {
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put(FIELD_VEHICLE_VIN, vin);
        vehicleData.put(FIELD_LAST_VIN_CHECK_TIMESTAMP, System.currentTimeMillis());

        if (details != null && !details.isEmpty()) {
            vehicleData.put(MAP_VIN_DETAILS, details);
        } else {
            vehicleData.put(MAP_VIN_DETAILS, FieldValue.delete());
        }

        userDocRef.set(vehicleData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "VIN ve detaylar başarıyla Firestore'a kaydedildi/güncellendi: " + vin);
                    callback.onSuccess(vin, newVinRegistered, details);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "VIN ve detaylar Firestore'a kaydedilirken hata", e);
                    callback.onFailure(e);
                });
    }

    public void fetchAndSaveVinDetailsFromNhtsa(String userId, String vin, VehicleDetailsFetchCallback detailsCallback) {
        if (vin == null || vin.length() != 17) {
            Log.e(TAG, "Geçersiz VIN: " + vin + ". NHTSA sorgusu yapılamıyor.");
            detailsCallback.onFetchFailed(new IllegalArgumentException("Invalid VIN for NHTSA query."));
            return;
        }

        Log.d(TAG, "NHTSA API çağrılıyor, VIN: " + vin);
        nhtsaApiService.getVehicleDetailsByVin(vin, "json").enqueue(new Callback<NhtsaVinResponse>() {
            @Override
            public void onResponse(@NonNull Call<NhtsaVinResponse> call, @NonNull Response<NhtsaVinResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null && !response.body().getResults().isEmpty()) {
                    NhtsaVehicleInfo vehicleInfo = response.body().getResults().get(0);

                    // Loglama: API'den gelen ham veriyi görelim (getter'ları kullanarak)
                    Log.d(TAG, "NHTSA Raw VehicleInfo Data: Make='" + vehicleInfo.getMake() +
                            "', Model='" + vehicleInfo.getModel() +
                            "', ModelYear='" + vehicleInfo.getModelYear() +
                            "', Manufacturer='" + vehicleInfo.getManufacturer() +
                            "', VehicleType='" + vehicleInfo.getVehicleType() + "'");

                    // Anlamlı detay kontrolü (Make, Model, Year en az biri olmalı)
                    boolean hasMeaningfulData = (vehicleInfo.getMake() != null && !vehicleInfo.getMake().isEmpty()) ||
                            (vehicleInfo.getModel() != null && !vehicleInfo.getModel().isEmpty()) ||
                            (vehicleInfo.getModelYear() != null && !vehicleInfo.getModelYear().isEmpty());

                    if (!hasMeaningfulData) {
                        Log.w(TAG, "NHTSA API'den VIN (" + vin + ") için anlamlı detay (make/model/year) bulunamadı.");
                        detailsCallback.onNoDetailsFoundOrNotNeeded();
                        return;
                    }

                    Map<String, Object> detailsMap = new HashMap<>();
                    String make = vehicleInfo.getMake();
                    String model = vehicleInfo.getModel();
                    String modelYear = vehicleInfo.getModelYear();
                    String manufacturer = vehicleInfo.getManufacturer();
                    String vehicleType = vehicleInfo.getVehicleType();

                    if (make != null && !make.isEmpty()) {
                        detailsMap.put(FIELD_DETAIL_MAKE, make);
                    } else {
                        Log.w(TAG, "NHTSA: Make is null or empty for VIN: " + vin);
                    }

                    if (model != null && !model.isEmpty()) {
                        detailsMap.put(FIELD_DETAIL_MODEL, model);
                    } else {
                        Log.w(TAG, "NHTSA: Model is null or empty for VIN: " + vin);
                    }

                    if (modelYear != null && !modelYear.isEmpty()) {
                        detailsMap.put(FIELD_DETAIL_YEAR, modelYear);
                    } else {
                        Log.w(TAG, "NHTSA: ModelYear is null or empty for VIN: " + vin);
                    }

                    if (manufacturer != null && !manufacturer.isEmpty()) {
                        detailsMap.put(FIELD_DETAIL_MANUFACTURER, manufacturer);
                    } else {
                        Log.w(TAG, "NHTSA: Manufacturer is null or empty for VIN: " + vin);
                    }

                    if (vehicleType != null && !vehicleType.isEmpty()) {
                        detailsMap.put(FIELD_DETAIL_VEHICLE_TYPE, vehicleType);
                    } else {
                        Log.w(TAG, "NHTSA: VehicleType is null or empty for VIN: " + vin);
                    }

                    if (!detailsMap.isEmpty()) {
                        Log.i(TAG, "NHTSA API'den VIN (" + vin + ") detayları başarıyla çekildi (detailsMap): " + detailsMap);
                        detailsCallback.onDetailsFetched(detailsMap);
                    } else {
                        // Bu durum, yukarıdaki hasMeaningfulData kontrolü nedeniyle pek olası değil
                        // ama yine de bir güvenlik önlemi olarak kalabilir.
                        Log.w(TAG, "NHTSA API'den VIN (" + vin + ") için detaylar çekildi ancak map boş kaldı (beklenmedik durum).");
                        detailsCallback.onNoDetailsFoundOrNotNeeded();
                    }
                } else {
                    String errorMsg = "NHTSA API yanıtı başarısız veya boş. VIN: " + vin;
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += ". Hata: " + response.errorBody().string();
                        } catch (IOException e) { /* ignore */ }
                    } else {
                        errorMsg += ". Kod: " + response.code();
                        if (response.body() != null && response.body().getResults() != null && response.body().getResults().isEmpty()) {
                            errorMsg += ". (Results listesi boş geldi)";
                        } else if (response.body() != null && response.body().getResults() == null) {
                            errorMsg += ". (Results listesi null geldi)";
                        } else if (response.body() == null) {
                            errorMsg += ". (Response body null geldi)";
                        }
                    }
                    Log.e(TAG, errorMsg);
                    detailsCallback.onFetchFailed(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<NhtsaVinResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "NHTSA API çağrısı tamamen başarısız. VIN: " + vin, t);
                detailsCallback.onFetchFailed(new Exception(t));
            }
        });
    }
}