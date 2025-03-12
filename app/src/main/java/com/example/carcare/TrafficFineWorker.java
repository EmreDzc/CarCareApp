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

public class TrafficFineWorker extends Worker {

    public TrafficFineWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "Trafik Cezası Hatırlatması";
        String message = "Trafik cezası sorgulamanızı yapmayı unutmayın!";

        // Bildirimi oluştur ve göster
        showNotification(title, message);

        // Firestore'a kayıt ekle (isteğe bağlı)
        saveNotificationToFirestore(title, message);

        // Bir sonraki bildirim tarihini hesaplayıp kendini yeniden planla
        scheduleNextWork();

        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Uygulamanızın ikonunu kullanın
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Farklı bildirim ID'si kullanıyoruz (örneğin 3)
            notificationManager.notify(3, builder.build());
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
                .addOnSuccessListener(documentReference -> Log.d("TrafficFineWorker", "Firestore kaydı eklendi"))
                .addOnFailureListener(e -> Log.e("TrafficFineWorker", "Firestore kaydı eklenemedi", e));
    }

    private void scheduleNextWork() {
        Calendar now = Calendar.getInstance();

        // İki hedef tarih: Ocak 1 ve Temmuz 1 (aynı yıl için)
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

        Calendar next = Calendar.getInstance();

        // Şu anki zamana göre hangi tarih daha yakın?
        if (now.before(jan1)) {
            // Eğer şu an Ocak 1 öncesindeyse
            next = jan1;
        } else if (now.before(jul1)) {
            // Ocak 1 geçti, ancak Temmuz 1'e kadar ise
            next = jul1;
        } else {
            // Eğer şimdi Temmuz 1'den sonra ise, sonraki bildirim gelecek yılın Ocak 1'inde olacak
            jan1.set(Calendar.YEAR, now.get(Calendar.YEAR) + 1);
            next = jan1;
        }

        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d("TrafficFineWorker", "Next traffic fine notification scheduled in " + delay + " ms");

        OneTimeWorkRequest nextWorkRequest = new OneTimeWorkRequest.Builder(TrafficFineWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(nextWorkRequest);
    }
}
