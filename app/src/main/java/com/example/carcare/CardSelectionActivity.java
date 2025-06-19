package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.adapters.CardSelectionAdapter;
import com.example.carcare.models.CardModel;
import com.example.carcare.ProfilePage.card.AddEditCardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CardSelectionActivity extends AppCompatActivity implements CardSelectionAdapter.OnCardSelectedListener {

    private static final String TAG = "CardSelectionActivity";
    private static final int REQUEST_ADD_CARD = 1004;

    private RecyclerView recyclerViewCards;
    private LinearLayout layoutNoCards;
    private Button buttonAddNewCard;
    private CardSelectionAdapter cardAdapter;
    private List<CardModel> cardList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedCardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_selection);

        // Toolbar ayarla
        Toolbar toolbar = findViewById(R.id.toolbar_card_selection);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Card");
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        selectedCardId = getIntent().getStringExtra("selected_card_id");

        initializeViews();
        setupRecyclerView();
        loadCards();
    }

    private void initializeViews() {
        recyclerViewCards = findViewById(R.id.recycler_view_cards);
        layoutNoCards = findViewById(R.id.layout_no_cards);
        buttonAddNewCard = findViewById(R.id.button_add_new_card);

        buttonAddNewCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditCardActivity.class);
            startActivityForResult(intent, REQUEST_ADD_CARD);
        });
    }

    private void setupRecyclerView() {
        cardList = new ArrayList<>();
        cardAdapter = new CardSelectionAdapter(cardList, selectedCardId, this);
        recyclerViewCards.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCards.setAdapter(cardAdapter);
    }

    private void loadCards() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoCardsState();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("savedCards")
                .orderBy("cardName")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cardList.clear();
                    List<CardModel> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CardModel card = document.toObject(CardModel.class);
                        card.setDocumentId(document.getId());
                        tempList.add(card);
                    }

                    tempList.sort((a, b) -> {
                        if (a.isDefault() && !b.isDefault()) return -1;
                        if (!a.isDefault() && b.isDefault()) return 1;
                        return a.getCardName().compareTo(b.getCardName());
                    });

                    cardList.addAll(tempList);
                    cardAdapter.notifyDataSetChanged();
                    updateUIVisibility();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cards", e);
                    Toast.makeText(this, "An error occurred while loading cards", Toast.LENGTH_SHORT).show();
                    showNoCardsState();
                });
    }

    private void updateUIVisibility() {
        if (cardList.isEmpty()) {
            showNoCardsState();
        } else {
            layoutNoCards.setVisibility(View.GONE);
            recyclerViewCards.setVisibility(View.VISIBLE);
        }
    }

    private void showNoCardsState() {
        layoutNoCards.setVisibility(View.VISIBLE);
        recyclerViewCards.setVisibility(View.GONE);
    }

    @Override
    public void onCardSelected(CardModel card) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_card_id", card.getDocumentId());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_CARD && resultCode == RESULT_OK) {
            loadCards();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}