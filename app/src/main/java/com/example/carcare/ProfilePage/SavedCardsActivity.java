package com.example.carcare.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.adapters.CardAdapter; // Oluşturulacak Adapter
import com.example.carcare.models.CardModel;    // Oluşturulacak Model
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SavedCardsActivity extends AppCompatActivity implements CardAdapter.CardInteractionListener {

    private static final String TAG = "SavedCardsActivity";

    private RecyclerView recyclerViewSavedCards;
    private CardAdapter cardAdapter;
    private List<CardModel> cardList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private LinearLayout layoutNoCards;
    private Button btnAddNewCardEmpty;
    private LinearLayout layoutMasterpassAndAddButton;
    private Button btnAddNewCardList;
    private TextView tvMasterpassWarning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_cards);

        Toolbar toolbar = findViewById(R.id.toolbar_saved_cards);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Başlık XML'den geliyor
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewSavedCards = findViewById(R.id.recycler_view_saved_cards);
        layoutNoCards = findViewById(R.id.layout_no_cards);
        btnAddNewCardEmpty = findViewById(R.id.btn_add_new_card_empty);
        layoutMasterpassAndAddButton = findViewById(R.id.layout_masterpass_and_add_button);
        btnAddNewCardList = findViewById(R.id.btn_add_new_card_list);
        tvMasterpassWarning = findViewById(R.id.tv_masterpass_warning);


        cardList = new ArrayList<>();
        cardAdapter = new CardAdapter(this, cardList, this); // 'this' listener için

        recyclerViewSavedCards.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSavedCards.setAdapter(cardAdapter);

        View.OnClickListener addNewCardListener = v -> {
            Intent intent = new Intent(SavedCardsActivity.this, AddEditCardActivity.class);
            startActivity(intent);
        };

        btnAddNewCardEmpty.setOnClickListener(addNewCardListener);
        btnAddNewCardList.setOnClickListener(addNewCardListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCards();
    }

    private void loadSavedCards() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            updateUIForNoCards();
            Toast.makeText(this, "Kayıtlı kartları görmek için giriş yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        // Firestore'da kartların saklanacağı yolu belirleyin (örneğin: users/{userId}/savedCards)
        // DİKKAT: Gerçek kart numaralarını ASLA saklamayın. Sadece token, son 4 hane, tip gibi referans bilgileri.
        db.collection("users").document(userId).collection("savedCards")
                .orderBy("isDefault", com.google.firebase.firestore.Query.Direction.DESCENDING) // Varsayılanı üste al
                .orderBy("cardName") // Sonra isme göre sırala
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cardList.clear();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                CardModel card = document.toObject(CardModel.class);
                                card.setDocumentId(document.getId()); // Firestore doküman ID'sini modele ekle
                                cardList.add(card);
                            }
                        }
                        cardAdapter.notifyDataSetChanged();
                        updateUIVisibility();
                    } else {
                        Log.w(TAG, "Error getting saved cards.", task.getException());
                        Toast.makeText(SavedCardsActivity.this, "Kayıtlı kartlar yüklenirken hata oluştu.", Toast.LENGTH_SHORT).show();
                        updateUIForNoCards();
                    }
                });
    }

    private void updateUIVisibility() {
        if (cardList.isEmpty()) {
            updateUIForNoCards();
        } else {
            layoutNoCards.setVisibility(View.GONE);
            recyclerViewSavedCards.setVisibility(View.VISIBLE);
            layoutMasterpassAndAddButton.setVisibility(View.VISIBLE);
            btnAddNewCardList.setVisibility(View.VISIBLE);
            // tvMasterpassWarning.setVisibility(View.VISIBLE); // Gerekirse bu uyarıyı her zaman göster
        }
    }
    private void updateUIForNoCards() {
        layoutNoCards.setVisibility(View.VISIBLE);
        recyclerViewSavedCards.setVisibility(View.GONE);
        layoutMasterpassAndAddButton.setVisibility(View.VISIBLE); // Masterpass bilgisi her zaman görünsün
        btnAddNewCardList.setVisibility(View.GONE); // Liste boşken bu buton gizli, diğeri aktif
        tvMasterpassWarning.setVisibility(View.GONE); // Liste boşken uyarıya gerek yok
    }


    @Override
    public void onDeleteCard(CardModel card) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || card.getDocumentId() == null) {
            Toast.makeText(this, "Kart silinemedi.", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kartı Sil")
                .setMessage(card.getCardName() + " isimli (" + card.getMaskedCardNumber() + ") kartı silmek istediğinizden emin misiniz?")
                .setPositiveButton("Sil", (dialog, which) -> {
                    db.collection("users").document(currentUser.getUid())
                            .collection("savedCards").document(card.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(SavedCardsActivity.this, "Kart başarıyla silindi.", Toast.LENGTH_SHORT).show();
                                loadSavedCards(); // Listeyi yenile
                            })
                            .addOnFailureListener(e -> Toast.makeText(SavedCardsActivity.this, "Kart silinirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    // Düzenleme için:
    @Override
    public void onEditCard(CardModel card) {
        Intent intent = new Intent(this, AddEditCardActivity.class);
        intent.putExtra("card_id_to_edit", card.getDocumentId());
        // Gerekirse diğer kart bilgilerini de gönderebilirsiniz
        startActivity(intent);
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