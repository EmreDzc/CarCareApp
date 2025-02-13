package com.example.carcare;

// NotificationActivity.java

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.carcare.Model.NotificationModel;
import com.example.carcare.Notification.NotificationAdapter;

import java.util.ArrayList;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private NotificationAdapter adapter;
    private ArrayList<NotificationModel> notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // RecyclerView'u layout'dan alıyoruz ve layout yöneticisi ekliyoruz
        recyclerNotifications = findViewById(R.id.recycler_notifications);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        // Örnek bildirim verilerini hazırlıyoruz
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationModel("Hoşgeldiniz", "Uygulamamıza hoşgeldiniz."));
        notificationList.add(new NotificationModel("Güncelleme", "Yeni özellikler eklendi."));
        notificationList.add(new NotificationModel("Hatırlatma", "Aracınızın bakım tarihini unutmayın."));
        // Buraya dinamik olarak gelen bildirimleri ekleyebilirsiniz.

        // Adapter nesnesini oluşturup RecyclerView'a atıyoruz
        adapter = new NotificationAdapter(notificationList);
        recyclerNotifications.setAdapter(adapter);
    }
}
