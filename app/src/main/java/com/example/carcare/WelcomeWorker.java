package com.example.carcare;

import android.app.NotificationManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WelcomeWorker extends Worker {

    public WelcomeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Hoş geldiniz bildirimi oluşturuluyor
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)  // Uygulamanızın ikonunu kullanın
                .setContentTitle("Hoş Geldiniz")
                .setContentText("CarCare+ uygulamamıza hoş geldiniz, keyifli kullanımlar dileriz.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(99, builder.build());
        }

        return Result.success();
    }
}
