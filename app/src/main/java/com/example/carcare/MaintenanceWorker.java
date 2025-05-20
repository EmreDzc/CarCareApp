package com.example.carcare;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.Data;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MaintenanceWorker extends Worker {
    private static final String TAG = "MaintenanceWorker";
    public static final String KEY_MAINTENANCE_TYPE = "maintenance_type";
    public static final String KEY_USER_ID = "user_id"; // Kullanıcı ID'si için yeni anahtar

    private MaintenanceType maintenanceType;
    private String userId;

    public MaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        // Data'dan maintenance type'ı al
        String typeStr = getInputData().getString(KEY_MAINTENANCE_TYPE);
        try {
            if (typeStr != null) {
                maintenanceType = MaintenanceType.valueOf(typeStr);
            } else {
                maintenanceType = MaintenanceType.MOTOR_OIL;
                Log.w(TAG, "No maintenance type specified, using default: MOTOR_OIL");
            }
        } catch (IllegalArgumentException e) {
            maintenanceType = MaintenanceType.MOTOR_OIL;
            Log.e(TAG, "Invalid maintenance type: " + typeStr + ", using default: MOTOR_OIL", e);
        }

        // Kullanıcı ID'sini al
        userId = getInputData().getString(KEY_USER_ID);
        // Eğer worker oluşturulduğunda kullanıcı ID'si verilmediyse, şu anki oturum açan kullanıcıyı al
        if (userId == null || userId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
            } else {
                Log.e(TAG, "Kullanıcı oturum açmamış ve worker'da userId yok!");
            }
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        // Kullanıcı ID'si yoksa çalışma
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "UserId olmadan worker çalıştırılamaz");
            return Result.failure();
        }

        Log.d(TAG, "Executing maintenance worker for: " + maintenanceType.name() + " (User: " + userId + ")");

        String title = maintenanceType.getTitle();
        String message = maintenanceType.getMessage();

        // Bildirimi göster
        NotificationHelper.showNotification(getApplicationContext(), title, message, maintenanceType.getNotificationId());

        // Firestore'a kaydet
        NotificationHelper.saveNotificationToFirestore(title, message);

        // Sonraki bildirimi planla
        scheduleNextWork();

        return Result.success();
    }

    private void scheduleNextWork() {
        // Kullanıcı ID'si yoksa çalışma
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "UserId olmadan sonraki bildirim planlanamaz");
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();

        // Hedef tarihi ayarla
        next.set(Calendar.MONTH, maintenanceType.getMonth());
        next.set(Calendar.DAY_OF_MONTH, maintenanceType.getDay());
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih geçmişse, sonraki periyoda atla
        if (!next.after(now)) {
            next.add(Calendar.YEAR, maintenanceType.getYearInterval());
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d(TAG, "User " + userId + ", " + maintenanceType.name() + " next work scheduled in " + delay + " ms");

        // Work request oluştur ve planla - Kullanıcı ID'sini de dahil et
        OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(MaintenanceWorker.class)
                .setInputData(new Data.Builder()
                        .putString(KEY_MAINTENANCE_TYPE, maintenanceType.name())
                        .putString(KEY_USER_ID, userId)
                        .build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        // Kullanıcıya özel benzersiz worker ID oluştur
        String uniqueWorkerId = userId + "_" + maintenanceType.getWorkerId();

        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                uniqueWorkerId,
                androidx.work.ExistingWorkPolicy.REPLACE,
                nextRequest
        );
    }
}