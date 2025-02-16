package com.example.carcare;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseNotificationManager notificationManager;
    private ListView listView;
    private NotificationAdapter adapter;
    private List<NotificationData> notifications;
    private TextView errorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // XML layout'u kullanıyoruz
        setContentView(R.layout.activity_notification);

        // XML'deki view'ları alıyoruz
        errorTextView = findViewById(R.id.errorTextView);
        listView = findViewById(R.id.notificationListView);
        FloatingActionButton fab = findViewById(R.id.fab);

        // Bildirim listesi ve adapter kurulumu
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter();
        listView.setAdapter(adapter);

        // Firebase yöneticisini başlatıyoruz
        notificationManager = new FirebaseNotificationManager();

        // Uygulama açıldığında bildirimleri yüklüyoruz
        loadNotifications();

        // FAB tıklama olayı: Yeni bildirim ekle
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewNotification();
            }
        });
    }

    private void loadNotifications() {
        notificationManager.getAllNotifications(new FirebaseNotificationManager.NotificationListCallback() {
            @Override
            public void onComplete(List<NotificationData> list) {
                notifications.clear();
                notifications.addAll(list);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        errorTextView.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                final String err = (e.getMessage() != null) ? e.getMessage() : "Bildirimler alınamadı.";
                Log.e("NotificationActivity", err);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorTextView.setText("Hata: " + err);
                        errorTextView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void addNewNotification() {
        NotificationData newNotification = new NotificationData();
        newNotification.setTitle("Yeni Bildirim");
        newNotification.setMessage("Bu bir test bildirimi");
        newNotification.setTimestamp(new Date());

        notificationManager.addNotification(newNotification, new FirebaseNotificationManager.IdCallback() {
            @Override
            public void onSuccess(String id) {
                Log.d("NotificationActivity", "Bildirim eklendi, id: " + id);
                loadNotifications();
            }

            @Override
            public void onFailure(Exception e) {
                final String err = (e.getMessage() != null) ? e.getMessage() : "Bildirim eklenemedi.";
                Log.e("NotificationActivity", err);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorTextView.setText("Hata: " + err);
                        errorTextView.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    // Bildirim güncelleme işlemi: update butonuna tıklanırsa çağrılır.
    private void updateNotification(final NotificationData notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bildirim Düzenle");

        final EditText input = new EditText(this);
        input.setText(notification.getTitle());
        builder.setView(input);

        builder.setPositiveButton("Kaydet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String newTitle = input.getText().toString();
                if (notification.getId() != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", newTitle);
                    notificationManager.updateNotification(notification.getId(), updates, new FirebaseNotificationManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            loadNotifications();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            final String err = (e.getMessage() != null) ? e.getMessage() : "Güncelleme başarısız.";
                            Log.e("NotificationActivity", err);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    errorTextView.setText("Hata: " + err);
                                    errorTextView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        builder.show();
    }

    // Bildirim silme işlemi: delete butonuna tıklanırsa çağrılır.
    private void deleteNotification(final NotificationData notification) {
        if (notification.getId() != null) {
            notificationManager.deleteNotification(notification.getId(), new FirebaseNotificationManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    loadNotifications();
                }

                @Override
                public void onFailure(Exception e) {
                    final String err = (e.getMessage() != null) ? e.getMessage() : "Silme başarısız.";
                    Log.e("NotificationActivity", err);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorTextView.setText("Hata: " + err);
                            errorTextView.setVisibility(View.VISIBLE);
                        }
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
                holder.updateButton = convertView.findViewById(R.id.updateButton);
                holder.deleteButton = convertView.findViewById(R.id.deleteButton);
                convertView.setTag(holder);
            } else {
                holder = (NotificationViewHolder) convertView.getTag();
            }

            final NotificationData notification = notifications.get(position);
            holder.titleTextView.setText(notification.getTitle() != null ? notification.getTitle() : "Başlık Yok");
            holder.messageTextView.setText(notification.getMessage() != null ? notification.getMessage() : "Mesaj Yok");

            // Güncelle butonuna tıklanınca updateNotification() metodunu çağırıyoruz.
            holder.updateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateNotification(notification);
                }
            });

            // Sil butonuna tıklanınca deleteNotification() metodunu çağırıyoruz.
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNotification(notification);
                }
            });

            return convertView;
        }

        private class NotificationViewHolder {
            TextView titleTextView;
            TextView messageTextView;
            Button updateButton;
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
    public static class FirebaseNotificationManager {
        private FirebaseFirestore db;
        private CollectionReference notificationsRef;

        public FirebaseNotificationManager() {
            db = FirebaseFirestore.getInstance();
            notificationsRef = db.collection("notifications");
        }

        // Callback arayüzleri
        public interface IdCallback {
            void onSuccess(String id);
            void onFailure(Exception e);
        }

        public interface NotificationListCallback {
            void onComplete(List<NotificationData> list);
            void onFailure(Exception e);
        }

        public interface SimpleCallback {
            void onSuccess();
            void onFailure(Exception e);
        }

        // Bildirim ekleme
        public void addNotification(NotificationData notification, final IdCallback callback) {
            notificationsRef.add(notification)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            callback.onSuccess(documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
        }

        // Tüm bildirimleri listeleme (timestamp'e göre)
        public void getAllNotifications(final NotificationListCallback callback) {
            notificationsRef.orderBy("timestamp")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
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
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
        }

        // Bildirim güncelleme
        public void updateNotification(String docId, Map<String, Object> updates, final SimpleCallback callback) {
            notificationsRef.document(docId)
                    .update(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
        }

        // Bildirim silme
        public void deleteNotification(String docId, final SimpleCallback callback) {
            notificationsRef.document(docId)
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onFailure(e);
                        }
                    });
        }
    }
}
