package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.adapters.OrderHistoryAdapter;
import com.example.carcare.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity implements OrderHistoryAdapter.OnOrderClickListener {

    private static final String TAG = "OrderHistoryActivity";

    private RecyclerView recyclerViewOrders;
    private OrderHistoryAdapter orderAdapter;
    private List<Order> orderList;
    private ProgressBar progressBar;
    private TextView emptyOrdersText;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        initViews();
        setupRecyclerView();
        loadUserOrders();
    }

    private void initViews() {
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        progressBar = findViewById(R.id.progress_bar_orders);
        emptyOrdersText = findViewById(R.id.text_empty_orders);
        backButton = findViewById(R.id.btn_back_orders);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        orderList = new ArrayList<>();

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderHistoryAdapter(this, orderList, this);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(orderAdapter);
    }

    private void loadUserOrders() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        // DEĞİŞİKLİK: Kullanıcının alt koleksiyonundan siparişleri çek
        db.collection("users")
                .document(user.getUid())
                .collection("orders")  // Subcollection
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Order order = document.toObject(Order.class);
                            order.setId(document.getId());
                            // userId'yi manuel olarak set et (subcollection'da saklanmıyor)
                            order.setUserId(user.getUid());
                            orderList.add(order);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing order: " + document.getId(), e);
                        }
                    }

                    updateUI();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders", e);
                    Toast.makeText(this, "Error while loading orders: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void updateUI() {
        if (orderList.isEmpty()) {
            recyclerViewOrders.setVisibility(View.GONE);
            emptyOrdersText.setVisibility(View.VISIBLE);
            emptyOrdersText.setText("You don't have any orders yet.");
        } else {
            recyclerViewOrders.setVisibility(View.VISIBLE);
            emptyOrdersText.setVisibility(View.GONE);
            orderAdapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        recyclerViewOrders.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onOrderClick(Order order) {
        // Sipariş detay sayfasına git
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }
}