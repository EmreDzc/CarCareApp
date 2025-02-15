package com.example.carcare;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
        // Programatik olarak layout oluşturuyoruz

        // Ana dikey LinearLayout
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Üst başlık (TopAppBar benzeri)
        TextView titleTextView = new TextView(this);
        titleTextView.setText("Bildirimler");
        titleTextView.setTextSize(20);
        titleTextView.setPadding(16, 16, 16, 16);
        rootLayout.addView(titleTextView);

        // Hata mesajı için TextView
        errorTextView = new TextView(this);
        errorTextView.setTextColor(Color.RED);
        errorTextView.setVisibility(View.GONE);
        rootLayout.addView(errorTextView);

        // Bildirimlerin listeleneceği ListView
        listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        listView.setLayoutParams(listParams);
        rootLayout.addView(listView);

        // Ana rootLayout'u FrameLayout içerisine alarak FAB için konumlandırma yapıyoruz
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        frameLayout.addView(rootLayout);

        // FloatingActionButton oluşturuluyor
        final FloatingActionButton fab = new FloatingActionButton(this);
        fab.setImageResource(android.R.drawable.ic_input_add);
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        fabParams.setMargins(margin, margin, margin, margin);
        fab.setLayoutParams(fabParams);
        frameLayout.addView(fab);

        setContentView(frameLayout);

        // Bildirim listesi ve adapter
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter();
        listView.setAdapter(adapter);

        // Firebase yöneticisi başlatılıyor
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

    private void updateNotification(final NotificationData notification) {
        // AlertDialog ile bildirim başlığını düzenleme
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

    // ListView için adapter
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
            LinearLayout layout;
            final ViewHolder holder;
            if (convertView == null) {
                layout = new LinearLayout(NotificationActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                int padding = (int) (16 * getResources().getDisplayMetrics().density);
                layout.setPadding(padding, padding, padding, padding);

                // Başlık TextView
                TextView titleView = new TextView(NotificationActivity.this);
                titleView.setTextSize(18);
                titleView.setId(View.generateViewId());
                layout.addView(titleView);

                // Mesaj TextView
                TextView messageView = new TextView(NotificationActivity.this);
                messageView.setId(View.generateViewId());
                layout.addView(messageView);

                // Butonların yer alacağı yatay LinearLayout
                LinearLayout buttonLayout = new LinearLayout(NotificationActivity.this);
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                buttonLayout.setGravity(Gravity.END);

                Button updateButton = new Button(NotificationActivity.this);
                updateButton.setText("Güncelle");
                updateButton.setId(View.generateViewId());
                buttonLayout.addView(updateButton);

                Button deleteButton = new Button(NotificationActivity.this);
                deleteButton.setText("Sil");
                deleteButton.setId(View.generateViewId());
                buttonLayout.addView(deleteButton);

                layout.addView(buttonLayout);

                holder = new ViewHolder();
                holder.titleView = titleView;
                holder.messageView = messageView;
                holder.updateButton = updateButton;
                holder.deleteButton = deleteButton;
                layout.setTag(holder);
            } else {
                layout = (LinearLayout) convertView;
                holder = (ViewHolder) layout.getTag();
            }

            final NotificationData notification = notifications.get(position);
            holder.titleView.setText((notification.getTitle() != null) ? notification.getTitle() : "Başlık Yok");
            holder.messageView.setText((notification.getMessage() != null) ? notification.getMessage() : "Mesaj Yok");

            holder.updateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateNotification(notification);
                }
            });
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNotification(notification);
                }
            });
            return layout;
        }

        private class ViewHolder {
            TextView titleView;
            TextView messageView;
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

        // Varsayılan yapıcı metod (Firestore'un deserialize edebilmesi için gerekli)
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

        // Tüm bildirimleri listeleme (timestamp sırasına göre)
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
