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

public class SummerMaintenanceWorker extends Worker {

    public SummerMaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "Yaz Bakımı Hatırlatması";
        String message = "Lütfen klima bakımı, lastik kontrolü ve diğer yazlık bakımlarınızı yapmayı unutmayın!";

        // Bildirimi oluştur ve göster
        showNotification(title, message);

        // Firestore'a kaydı ekle (opsiyonel, NotificationActivity'de görüntülemek için)
        saveNotificationToFirestore(title, message);

        // Gelecek yılın 1 Haziran'ına göre kendini yeniden planla
        scheduleNextWork();

        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)  // Uygulama ikonunuzu kullanın
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Örneğin bildirim ID'si olarak 6 kullanılıyor.
            notificationManager.notify(6, builder.build());
        }
    }

    private void saveNotificationToFirestore(String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("message", message);
        data.put("timestamp", new Date());

        db.collection("notifications")
                .add(data)
                .addOnSuccessListener(documentReference -> Log.d("SummerMaintenance", "Firestore kaydı eklendi"))
                .addOnFailureListener(e -> Log.e("SummerMaintenance", "Firestore kaydı eklenemedi", e));
    }

    private void scheduleNextWork() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        // Hedef tarih: Bu yılın 1 Haziran, saat 00:00 olarak ayarlanır
        next.set(Calendar.MONTH, Calendar.JUNE);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer bu yılın 1 Haziran'ı geçmişse, gelecek yılın 1 Haziran'ını hedefle
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1);
        } else {
            // Üretim için hedef, yine gelecek yılın 1 Haziran'ı olsun.
            next.add(Calendar.YEAR, 1);
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d("SummerMaintenance", "Next work scheduled in " + delay + " ms");

        OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(SummerMaintenanceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(nextRequest);
    }
}
