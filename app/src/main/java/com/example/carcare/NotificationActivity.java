package com.example.carcare;

import com.example.carcare.ProfilePage.ProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseNotificationManager notificationManager;
    private ListView listView;
    private NotificationAdapter adapter;
    private List<NotificationData> notifications;
    private TextView errorTextView;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Başlangıçta Notifications sekmesini vurgula
        bottomNavigationView.setSelectedItemId(R.id.nav_notifications);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(NotificationActivity.this, CarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_store) {
                startActivity(new Intent(NotificationActivity.this, StoreActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(NotificationActivity.this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_notifications) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(NotificationActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // XML'deki view'ları alıyoruz
        errorTextView = findViewById(R.id.errorTextView);
        listView = findViewById(R.id.notificationListView);

        // Bildirim listesi ve adapter kurulumu
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter();
        listView.setAdapter(adapter);

        // Firebase yöneticisini başlatıyoruz
        notificationManager = new FirebaseNotificationManager();

        // Cihazı "maintenance" topic'ine abone ediyoruz
        FirebaseMessaging.getInstance().subscribeToTopic("maintenance")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "maintenance topic'ine abone olundu");
                    } else {
                        Log.d("FCM", "Topic aboneliği başarısız");
                    }
                });

        // Uygulama açıldığında bildirimleri yüklüyoruz
        loadNotifications();
    }

    private void loadNotifications() {
        notificationManager.getAllNotifications(new FirebaseNotificationManager.NotificationListCallback() {
            @Override
            public void onComplete(List<NotificationData> list) {
                notifications.clear();
                notifications.addAll(list);
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    errorTextView.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(Exception e) {
                final String err = (e.getMessage() != null) ? e.getMessage() : "Notifications not received.";
                Log.e("NotificationActivity", err);
                runOnUiThread(() -> {
                    errorTextView.setText("Hata: " + err);
                    errorTextView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // Kullanıcıya sadece bildirimleri silme imkanı sunuluyor.
    private void deleteNotification(final NotificationData notification) {
        if (notification.getId() != null) {
            notificationManager.deleteNotification(notification.getId(), new FirebaseNotificationManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    loadNotifications();
                }

                @Override
                public void onFailure(Exception e) {
                    final String err = (e.getMessage() != null) ? e.getMessage() : "Delete failed.";
                    Log.e("NotificationActivity", err);
                    runOnUiThread(() -> {
                        errorTextView.setText("Hata: " + err);
                        errorTextView.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }

    // ListView için adapter: Her satırda item_notification.xml dosyası inflate ediliyor.
    private class NotificationAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return notifications.size();
        }

        @Override
        public Object getItem(int position) {
            return notifications.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            NotificationViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(NotificationActivity.this)
                        .inflate(R.layout.item_notification, parent, false);
                holder = new NotificationViewHolder();
                holder.titleTextView = convertView.findViewById(R.id.titleTextView);
                holder.messageTextView = convertView.findViewById(R.id.messageTextView);
                holder.deleteButton = convertView.findViewById(R.id.deleteButton);
                convertView.setTag(holder);
            } else {
                holder = (NotificationViewHolder) convertView.getTag();
            }

            final NotificationData notification = notifications.get(position);
            holder.titleTextView.setText(notification.getTitle() != null ? notification.getTitle() : "No Title");
            holder.messageTextView.setText(notification.getMessage() != null ? notification.getMessage() : "No Message");

            holder.deleteButton.setOnClickListener(v -> deleteNotification(notification));

            return convertView;
        }

        private class NotificationViewHolder {
            TextView titleTextView;
            TextView messageTextView;
            Button deleteButton;
        }
    }

    // Firestore tarafından kullanılacak veri modeli
    public static class NotificationData {
        private String id;
        private String title;
        private String message;
        private Date timestamp;

        public NotificationData() { }

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public Date getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }

    // Firebase ile CRUD işlemlerini yöneten sınıf
    // NotificationActivity.java içinde - FirebaseNotificationManager iç sınıfını aşağıdaki şekilde güncelleyin

    public static class FirebaseNotificationManager {
        private FirebaseFirestore db;
        private FirebaseUser currentUser;
        private String userId;

        public FirebaseNotificationManager() {
            db = FirebaseFirestore.getInstance();
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
            } else {
                Log.e("FirebaseNotificationManager", "User is not logged in");
            }
        }

        // Callback arayüzleri
        public interface NotificationListCallback {
            void onComplete(List<NotificationData> list);
            void onFailure(Exception e);
        }

        public interface SimpleCallback {
            void onSuccess();
            void onFailure(Exception e);
        }

        // Hoş geldiniz bildirimi ekleme metodu
        public void addWelcomeNotification(final SimpleCallback callback) {
            if (currentUser == null) {
                callback.onFailure(new Exception("User not logged in"));
                return;
            }
            NotificationData notification = new NotificationData();
            notification.setTitle("Welcome to CarCare+"); // YENİ
            notification.setMessage("Welcome to our CarCare+ app. We wish you a pleasant experience."); // YENİ
            notification.setTimestamp(new Date());

            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
        }

        public void addCustomNotification(NotificationData notification, final SimpleCallback callback) {
            if (currentUser == null) {
                if (callback != null) callback.onFailure(new Exception("User is not logged in"));
                return;
            }
            if (notification.getTimestamp() == null) { // Timestamp yoksa şimdi ayarla
                notification.setTimestamp(new Date());
            }

            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e);
                    });
        }

        // Tüm bildirimleri listeleme (timestamp'e göre)
        public void getAllNotifications(final NotificationListCallback callback) {
            if (currentUser == null) {
                callback.onFailure(new Exception("User is not logged in"));
                return;
            }

            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .orderBy("timestamp")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<NotificationData> notifications = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            NotificationData notification = doc.toObject(NotificationData.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                notifications.add(notification);
                            } else {
                                notifications.add(new NotificationData());
                            }
                        }
                        callback.onComplete(notifications);
                    })
                    .addOnFailureListener(callback::onFailure);
        }

        public void updateNotification(String docId, java.util.Map<String, Object> updates, final SimpleCallback callback) {
            if (currentUser == null) {
                callback.onFailure(new Exception("User is not logged in"));
                return;
            }

            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(docId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
        }

        public void deleteNotification(String docId, final SimpleCallback callback) {
            if (currentUser == null) {
                callback.onFailure(new Exception("User is not logged in"));
                return;
            }

            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
        }
    }
}
