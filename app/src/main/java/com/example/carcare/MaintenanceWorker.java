package com.example.carcare;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MaintenanceWorker extends Worker {

    public MaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "TÜVTÜRK Araç Bakım Hatırlatması";
        String message = "Aracınızın bakımını yapmayı unutmayın! Lütfen TÜVTÜRK kontrolünüzü gerçekleştirin.";

        // Bildirimi göster
        showNotification(title, message);

        // Firestore'a kaydı ekle (isteğe bağlı, NotificationActivity’de gösterim için)
        saveNotificationToFirestore(title, message);

        // Bir sonraki 2 yılın Ocak 1’ine göre yeniden planlama yap
        scheduleNextWork();

        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Uygulama ikonunuzu kullanın
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Bildirim ID'si: 1
            notificationManager.notify(1, builder.build());
        }
    }

    private void saveNotificationToFirestore(String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("timestamp", new Date());

        db.collection("notifications")
                .add(notificationData)
                .addOnSuccessListener(documentReference -> Log.d("MaintenanceWorker", "Firestore kaydı eklendi"))
                .addOnFailureListener(e -> Log.e("MaintenanceWorker", "Firestore kaydı eklenemedi", e));
    }

    private void scheduleNextWork() {
        // Şu anki zamanı al
        Calendar now = Calendar.getInstance();

        // Hedef tarih: 2 yıl sonra Ocak 1, saat 00:00
        Calendar next = Calendar.getInstance();
        next.set(Calendar.MONTH, Calendar.JANUARY);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih bugünden önceyse (veya bugün ise) 2 yıl ekle
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 2);
        } else {
            // Eğer şu an, 2 yıl sonraki Ocak 1 tarihinden önceyse; yani hedef tarih güncelse, yine 2 yıl ekleyerek tekrarlama süresi belirlenir.
            next.add(Calendar.YEAR, 2);
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d("MaintenanceWorker", "Next work scheduled in " + delay + " ms");

        OneTimeWorkRequest nextWorkRequest = new OneTimeWorkRequest.Builder(MaintenanceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(nextWorkRequest);
    }
}
