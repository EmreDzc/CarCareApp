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

public class MotorOilWorker extends Worker {

    public MotorOilWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "Motor Yağı ve Filtre Değişimi Hatırlatması";
        String message = "Lütfen motor yağınızı ve filtrenizi değiştirmeyi unutmayın!";

        showNotification(title, message);
        saveNotificationToFirestore(title, message);
        scheduleNextWork();

        return Result.success();
    }

    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Uygulama ikonunuz
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm != null) {
            nm.notify(10, builder.build());
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
                .addOnSuccessListener(doc -> Log.d("MotorOilWorker", "Firestore kaydı eklendi"))
                .addOnFailureListener(e -> Log.e("MotorOilWorker", "Firestore kaydı eklenemedi", e));
    }

    private void scheduleNextWork() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        // Hedef tarih: Her yıl 1 Şubat, 00:00
        next.set(Calendar.MONTH, Calendar.FEBRUARY);
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        // Eğer hedef tarih bugünden önceyse, gelecek yılın 1 Şubat'ını hedefle
        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1);
        } else {
            next.add(Calendar.YEAR, 1);
        }
        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        Log.d("MotorOilWorker", "Next work scheduled in " + delay + " ms");
        OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(MotorOilWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(nextRequest);
    }
}
