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

public class TrafficInsuranceWorker extends Worker {

    public TrafficInsuranceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "Zorunlu Trafik Sigortası Yenileme";
        String message = "Lütfen zorunlu trafik sigortanızı yenilemeyi unutmayın!";

        // Bildirimi oluştur ve göster
        showNotification(title, message);

        // Firestore'a kayıt ekle (opsiyonel, NotificationActivity için)
        saveNotificationToFirestore(title, message);

        // Bir sonraki çalıştırmayı planla: gelecek yılın Ocak 1’i
        scheduleNextWork();

        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)  // Uygulamanızın ikonunu kullanın
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Bildirim ID'si: Örneğin 4
            notificationManager.notify(4, builder.build());
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
                .addOnSuccessListener(documentReference -> Log.d("TrafficInsuranceWorker", "Firestore kaydı eklendi"))
                .addOnFailureListener(e -> Log.e("TrafficInsuranceWorker", "Firestore kaydı eklenemedi", e));
    }

    private void scheduleNextWork() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        // Hedef tarih: Gelecek Ocak 1, saat 00:00
        next.set(Calendar.MONTH, Calendar.JANUARY);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // Eğer hedef tarih bugün veya geçmişte ise, sonraki yıl için planla
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1);
        } else {
            // Eğer hedef tarih henüz gelmemişse yine gelecek yıl Ocak 1 için ayarlayalım
            next.add(Calendar.YEAR, 1);
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d("TrafficInsuranceWorker", "Next work scheduled in " + delay + " ms");

        OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(TrafficInsuranceWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(nextRequest);
    }
}
