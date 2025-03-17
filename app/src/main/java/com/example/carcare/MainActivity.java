package com.example.carcare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.example.carcare.NotificationActivity.FirebaseNotificationManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import androidx.work.OneTimeWorkRequest;


public class MainActivity extends AppCompatActivity {

    public static final String CHANNEL_ID = "maintenance_channel";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Eğer kullanıcı giriş yapmamışsa, LoginActivity'ye yönlendir.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Kullanıcı giriş yapmışsa MainActivity layout'unu yükle.
        setContentView(R.layout.activity_main);

        // Bildirim kanalını oluştur (Android Oreo ve sonrası için)
        createNotificationChannel();
        // İlk giriş kontrolü yap ve gerekirse hoş geldiniz bildirimi ekle
        checkFirstTimeLogin();

        // WorkManager ile görevleri planla:
        scheduleMaintenanceWorker();
        scheduleTrafficFineWorker(); // 6 ayda bir trafik cezası sorgulama hatırlatması
        scheduleTrafficInsuranceWorker();
        scheduleWinterMaintenanceWorker();
        scheduleSummerMaintenanceWorker();
        scheduleMotorOilWorker();
        scheduleAirFilterWorker();
        scheduleFuelFilterWorker();
        scheduleBrakeSystemWorker();
        scheduleSparkPlugWorker();


        // "Open Settings" butonuna tıklayınca SettingsActivity'ye yönlendir.
        Button btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnOpenSettings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // "Notifications" butonuna tıklayınca NotificationActivity'ye yönlendir.
        Button btnGoToNotification = findViewById(R.id.btn_go_to_notification);
        btnGoToNotification.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // "Harita Sayfasına Git" butonuna tıklayınca MapActivity'ye yönlendir.
        Button btnGoToMap = findViewById(R.id.btn_go_to_map);
        btnGoToMap.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }

    // İlk giriş kontrolü ve hoş geldiniz bildirimi ekleme
    private void checkFirstTimeLogin() {
        // SharedPreferences üzerinden ilk giriş kontrolü
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        // Eğer kullanıcı ilk kez giriyorsa
        if (isFirstTime) {
            // Hoş geldiniz bildirimini ekle
            FirebaseNotificationManager notificationManager = new FirebaseNotificationManager();
            notificationManager.addWelcomeNotification(new FirebaseNotificationManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d("MainActivity", "Hoş geldiniz bildirimi başarıyla eklendi");
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("MainActivity", "Hoş geldiniz bildirimi eklenirken hata: " + e.getMessage());
                }
            });

            // Artık ilk kez değil olarak işaretle
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isFirstTime", false);
            editor.apply();
        }
    }

    // Android Oreo ve sonrası için bildirim kanalı oluşturma
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Maintenance Channel";
            String description = "Channel for maintenance reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // İlk MaintenanceWorker görevini planlama: her 2 yılda bir Ocak 1'inde çalışacak
    private void scheduleMaintenanceWorker() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();

        // Hedef tarih: Ocak 1, saat 00:00
        next.set(Calendar.MONTH, Calendar.JANUARY);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih bugünden önce veya bugün ise 2 yıl ekle
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 2);
        } else {
            // Eğer gelecek Ocak 1 tarihi henüz gelmediyse yine 2 yıl sonraya ayarlıyoruz (örneğin, üretim için)
            next.add(Calendar.YEAR, 2);
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest initialWorkRequest = new OneTimeWorkRequest.Builder(MaintenanceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "maintenance_worker",
                ExistingWorkPolicy.KEEP,
                initialWorkRequest
        );
    }


    // 6 ayda bir trafik cezası sorgulama hatırlatması için WorkManager görevi planlama
    // MainActivity içinde (örneğin onCreate() metodu içinde)
    private void scheduleTrafficFineWorker() {
        Calendar now = Calendar.getInstance();
        Calendar jan1 = Calendar.getInstance();
        jan1.set(Calendar.YEAR, now.get(Calendar.YEAR));
        jan1.set(Calendar.MONTH, Calendar.JANUARY);
        jan1.set(Calendar.DAY_OF_MONTH, 1);
        jan1.set(Calendar.HOUR_OF_DAY, 0);
        jan1.set(Calendar.MINUTE, 0);
        jan1.set(Calendar.SECOND, 0);
        jan1.set(Calendar.MILLISECOND, 0);

        Calendar jul1 = Calendar.getInstance();
        jul1.set(Calendar.YEAR, now.get(Calendar.YEAR));
        jul1.set(Calendar.MONTH, Calendar.JULY);
        jul1.set(Calendar.DAY_OF_MONTH, 1);
        jul1.set(Calendar.HOUR_OF_DAY, 0);
        jul1.set(Calendar.MINUTE, 0);
        jul1.set(Calendar.SECOND, 0);
        jul1.set(Calendar.MILLISECOND, 0);

        Calendar next;
        // Şu anki tarihe göre en yakın hedef tarihi belirle:
        if (now.before(jan1)) {
            next = jan1;
        } else if (now.before(jul1)) {
            next = jul1;
        } else {
            // Eğer şimdi Temmuz 1'den sonra ise, gelecek yılın Ocak 1'i hedeflenir.
            jan1.set(Calendar.YEAR, now.get(Calendar.YEAR) + 1);
            next = jan1;
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest trafficFineRequest = new OneTimeWorkRequest.Builder(TrafficFineWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "traffic_fine_worker",
                ExistingWorkPolicy.KEEP,
                trafficFineRequest
        );
    }

    private void scheduleTrafficInsuranceWorker() {
        // Şu anki zamanı al
        Calendar now = Calendar.getInstance();

        // Hedef tarih: Bu yılın Ocak 1'i, saat 00:00 olarak ayarlanır
        Calendar next = Calendar.getInstance();
        next.set(Calendar.MONTH, Calendar.JANUARY);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih (bu yılın Ocak 1'i) zaten geçmişse, gelecek yılın Ocak 1'ini hedefle
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1);
        }

        // Aradaki farkı milisaniye cinsinden hesapla
        long delay = next.getTimeInMillis() - now.getTimeInMillis();

        // OneTimeWorkRequest oluştur, hedef tarihe kadar delay eklenmiş olarak
        OneTimeWorkRequest insuranceRequest = new OneTimeWorkRequest.Builder(TrafficInsuranceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        // Unique iş adı ile planla
        WorkManager.getInstance(this).enqueueUniqueWork(
                "traffic_insurance_worker",
                ExistingWorkPolicy.KEEP,
                insuranceRequest
        );
    }
    // Kış bakımı bildirimi: Her yılın Aralık 1'inde
    private void scheduleWinterMaintenanceWorker() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.MONTH, Calendar.DECEMBER);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1);
        }
        long delay = next.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest winterRequest = new OneTimeWorkRequest.Builder(WinterMaintenanceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "winter_maintenance_worker",
                ExistingWorkPolicy.KEEP,
                winterRequest
        );
    }
    // Yaz bakımı bildirimi: Her yılın Haziran 1'inde
    private void scheduleSummerMaintenanceWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.MONTH, Calendar.JUNE);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 1);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SummerMaintenanceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "summer_maintenance_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }
    private void scheduleMotorOilWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        // Hedef tarih: 1 Şubat, 00:00
        target.set(Calendar.MONTH, Calendar.FEBRUARY);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        // Eğer 1 Şubat zaten geçmişse, gelecek yılın 1 Şubat'ını hedefle
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 1);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MotorOilWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "motor_oil_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }
    private void scheduleAirFilterWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        // Hedef tarih: 1 Mart, 00:00
        target.set(Calendar.MONTH, Calendar.MARCH);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        // 2 yıl aralığı: Eğer 1 Mart zaten geçmişse, 2 yıl ekle
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 2);
        } else {
            target.add(Calendar.YEAR, 2);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AirFilterWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "air_filter_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }
    private void scheduleFuelFilterWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        // Hedef tarih: 1 Nisan, 00:00
        target.set(Calendar.MONTH, Calendar.APRIL);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 4);
        } else {
            target.add(Calendar.YEAR, 4);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(FuelFilterWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "fuel_filter_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }
    private void scheduleBrakeSystemWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        // Hedef tarih: 1 Mayıs, 00:00
        target.set(Calendar.MONTH, Calendar.MAY);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 2);
        } else {
            target.add(Calendar.YEAR, 2);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BrakeSystemWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "brake_system_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }
    private void scheduleSparkPlugWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        // Hedef tarih: 1 Haziran, 00:00
        target.set(Calendar.MONTH, Calendar.JUNE);
        target.set(Calendar.DAY_OF_MONTH, 1);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.YEAR, 3);
        } else {
            target.add(Calendar.YEAR, 3);
        }
        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SparkPlugWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(
                "spark_plug_worker",
                ExistingWorkPolicy.KEEP,
                request
        );
    }

}
