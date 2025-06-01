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
import android.content.SharedPreferences; // EKLENDİ
import java.util.concurrent.TimeUnit;    // EKLENDİ

public class MaintenanceScheduler {
    private static final String TAG = "MaintenanceScheduler";
    private final Context context;
    private static final String PREFS_NAME = "MaintenancePrefs";
    private static final String LAST_SCHEDULE_ALL_KEY_PREFIX = "last_schedule_all_time_";

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

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastScheduleKey = LAST_SCHEDULE_ALL_KEY_PREFIX + userId;
        long lastScheduledTime = prefs.getLong(lastScheduleKey, 0);
        long currentTime = System.currentTimeMillis();

        // Son planlamadan bu yana 23 saatten fazla geçtiyse veya hiç planlanmadıysa
        // 23 saat, gün dönümlerinde veya saat farklarında küçük oynamalara karşı bir tampon sağlar.
        if (currentTime - lastScheduledTime > TimeUnit.HOURS.toMillis(23)) {
            Log.d(TAG, "Kullanıcı " + userId + " için tüm bakım planlamaları yapılıyor (günde bir kez kontrolü geçti).");

            for (MaintenanceType type : MaintenanceType.values()) {
                scheduleMaintenance(type, userId); // Bu metod zaten REPLACE kullanıyor
            }
            prefs.edit().putLong(lastScheduleKey, currentTime).apply();
            Log.i(TAG, "Kullanıcı " + userId + " için tüm bakımlar planlandı ve son planlama zamanı kaydedildi.");
        } else {
            Log.d(TAG, "Kullanıcı " + userId + " için tüm bakım planlamaları bugün zaten yapılmış, atlanıyor.");
        }
    }

    public void scheduleMaintenance(MaintenanceType type, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "UserId olmadan bakım planlanamaz");
            return;
        }

        String uniqueWorkerId = userId + "_" + type.getWorkerId();

        // Önce varsa önceki work request'i iptal et (REPLACE zaten bunu yapar ama garanti için loglanabilir)
        // WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkerId);
        // Log.d(TAG, "Attempting to cancel existing work for: " + uniqueWorkerId);


        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        target.set(Calendar.MONTH, type.getMonth());
        target.set(Calendar.DAY_OF_MONTH, type.getDay());
        // Saati günün başına alalım ki, gün içinde herhangi bir saatte planlama yapılsa bile
        // aynı gün için tetikleme olasılığı artsın, ama REPLACE sayesinde tek iş kalmalı.
        target.set(Calendar.HOUR_OF_DAY, 8); // Örneğin sabah 8
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih geçmişse veya şu anki anın gerisindeyse, sonraki periyoda atla
        // Bu kısım önemli: Sadece gün bazında değil, saat bazında da kontrol edelim.
        while (target.before(now) || target.equals(now)) {
            // Eğer target.before(now) ise, zaten geçmiş demektir.
            // Eğer target.equals(now) ise ve planlama tam o anda yapılıyorsa,
            // bir sonraki yıla atlaması mantıklı olabilir, aksi takdirde anında tetiklenebilir
            // ve `REPLACE` ile yarışabilir. Ancak burada amaç "o gün" atmak olduğu için,
            // eğer saat geçmişse sonraki yıla atlaması doğru.
            // Eğer saat geçmemişse ve bugünse, `delay` pozitif olacaktır.
            target.add(Calendar.YEAR, type.getYearInterval());
            Log.d(TAG, type.name() + " için hedef tarih ("+ target.getTime() +") geçmiş veya şu an, sonraki periyoda atlandı: " + type.getYearInterval() + " yıl eklendi.");
        }
        // Yukardaki while döngüsünü şöyle basitleştirebiliriz:
        // Hedef bu yılın belirtilen günü. Eğer o gün ve saat geçtiyse, bir sonraki tekrara atla.
        // Eğer bu yılın o günü ve saati geçmediyse, o zamana planla.

        Calendar checkTarget = Calendar.getInstance();
        checkTarget.set(Calendar.MONTH, type.getMonth());
        checkTarget.set(Calendar.DAY_OF_MONTH, type.getDay());
        checkTarget.set(Calendar.HOUR_OF_DAY, 8); // Sabah 8
        checkTarget.set(Calendar.MINUTE, 0);
        checkTarget.set(Calendar.SECOND, 0);
        checkTarget.set(Calendar.MILLISECOND, 0);

        if (now.after(checkTarget)) { // Eğer bu yılki belirtilen gün ve saat geçtiyse
            checkTarget.add(Calendar.YEAR, type.getYearInterval()); // Bir sonraki periyoda git
        }
        // Artık checkTarget kesinlikle gelecekte bir tarih.
        target = checkTarget;


        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        // Gecikme 0 veya negatifse (çok nadir bir durum, yukarıdaki mantık bunu engellemeli),
        // çok küçük bir pozitif gecikme ayarla veya logla.
        if (delay <= 0) {
            Log.w(TAG, type.name() + " için hesaplanan gecikme <= 0 (" + delay + "ms). Bu beklenmedik. Target: " + target.getTime() + ", Now: " + now.getTime() + ". Minik bir gecikmeyle planlanacak.");
            delay = 1000; // En az 1 saniye gecikme verelim ki WorkManager'ın anlık işleme şansı olsun.
        }


        Log.i(TAG, "SCHEDULE_MAINT: User: " + userId + ", Type: " + type.name() +
                ", WorkerID: " + uniqueWorkerId + ", TargetDate: " + target.getTime() +
                ", DelayMillis: " + delay);


        Data inputData = new Data.Builder()
                .putString(MaintenanceWorker.KEY_MAINTENANCE_TYPE, type.name())
                .putString(MaintenanceWorker.KEY_USER_ID, userId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MaintenanceWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                // Gerekirse kısıtlamalar eklenebilir (örn: ağ bağlantısı, şarj durumu)
                // .setConstraints(Constraints. ...)
                .addTag(uniqueWorkerId) // Etiket eklemek loglama ve yönetim için faydalı olabilir
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkerId,
                ExistingWorkPolicy.REPLACE, // BU POLİTİKA KRİTİK!
                workRequest
        );

        Log.d(TAG, "Kullanıcı " + userId + " için " + type.name() + " planlandı, ID: " + uniqueWorkerId + ", Policy: REPLACE");
    }
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