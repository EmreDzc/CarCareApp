package com.example.carcare;

import android.content.Context;
import android.util.Log;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MaintenanceScheduler {
    private static final String TAG = "MaintenanceScheduler";
    private final Context context;

    public MaintenanceScheduler(Context context) {
        this.context = context;
    }

    /**
     * Tüm bakım hatırlatmalarını planlar
     */
    public void scheduleAllMaintenance() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Kullanıcı oturum açmamış, bakım planlanamaz");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Kullanıcı " + userId + " için tüm bakım planlamaları yapılıyor");

        for (MaintenanceType type : MaintenanceType.values()) {
            scheduleMaintenance(type, userId);
        }
    }

    /**
     * Belirli bir bakım tipini planlar
     * @param type MaintenanceType
     * @param userId Kullanıcı ID'si
     */
    public void scheduleMaintenance(MaintenanceType type, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "UserId olmadan bakım planlanamaz");
            return;
        }

        // Kullanıcıya özel benzersiz worker ID oluştur
        String uniqueWorkerId = userId + "_" + type.getWorkerId();

        // Önce varsa önceki work request'i iptal et
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkerId);

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        // Hedef tarihi ayarla
        target.set(Calendar.MONTH, type.getMonth());
        target.set(Calendar.DAY_OF_MONTH, type.getDay());
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih geçmişse, sonraki periyoda atla
        if (!target.after(now)) {
            target.add(Calendar.YEAR, type.getYearInterval());
        }

        long delay = target.getTimeInMillis() - now.getTimeInMillis();
        Log.d(TAG, "Kullanıcı " + userId + " için " + type.name() + " planlanıyor: " + target.getTime());

        // Worker için input data oluştur - kullanıcı ID'sini de dahil et
        Data inputData = new Data.Builder()
                .putString(MaintenanceWorker.KEY_MAINTENANCE_TYPE, type.name())
                .putString(MaintenanceWorker.KEY_USER_ID, userId)
                .build();

        // Work request oluştur
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MaintenanceWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        // İşi planla
        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkerId,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Kullanıcı " + userId + " için " + type.name() + " planlandı, ID: " + uniqueWorkerId);
    }

    /**
     * Belirli bir bakım tipini şu anki oturum açan kullanıcı için planlar
     * @param type MaintenanceType
     */
    public void scheduleMaintenance(MaintenanceType type) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Kullanıcı oturum açmamış, bakım planlanamaz");
            return;
        }

        String userId = currentUser.getUid();
        scheduleMaintenance(type, userId);
    }
}