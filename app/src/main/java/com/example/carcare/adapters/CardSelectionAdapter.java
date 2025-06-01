package com.example.carcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.models.CardModel;

import java.util.List;

public class CardSelectionAdapter extends RecyclerView.Adapter<CardSelectionAdapter.CardViewHolder> {

    private List<CardModel> cardList;
    private String selectedCardId;
    private OnCardSelectedListener listener;

    public interface OnCardSelectedListener {
        void onCardSelected(CardModel card);
    }

    public CardSelectionAdapter(List<CardModel> cardList, String selectedCardId, OnCardSelectedListener listener) {
        this.cardList = cardList;
        this.selectedCardId = selectedCardId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_selection, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardModel card = cardList.get(position);
        holder.bind(card);
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public class CardViewHolder extends RecyclerView.ViewHolder {
        private RadioButton radioButton;
        private TextView textCardName;
        private TextView textCardNumber;
        private TextView textCardHolder;
        private TextView textExpiryDate;
        private ImageView imageCardLogo;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_card);
            textCardName = itemView.findViewById(R.id.text_card_name);
            textCardNumber = itemView.findViewById(R.id.text_card_number);
            textCardHolder = itemView.findViewById(R.id.text_card_holder);
            textExpiryDate = itemView.findViewById(R.id.text_expiry_date);
            imageCardLogo = itemView.findViewById(R.id.image_card_logo);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    CardModel card = cardList.get(getAdapterPosition());
                    listener.onCardSelected(card);
                }
            });

            radioButton.setOnClickListener(v -> {
                if (listener != null) {
                    CardModel card = cardList.get(getAdapterPosition());
                    listener.onCardSelected(card);
                }
            });
        }

        public void bind(CardModel card) {
            textCardName.setText(card.getCardName());
            textCardNumber.setText(card.getMaskedCardNumber());
            textCardHolder.setText(card.getCardHolderName());
            textExpiryDate.setText(card.getExpiryMonth() + "/" + card.getExpiryYear().substring(2));

            // Kart tipine göre logo
            if ("VISA".equalsIgnoreCase(card.getCardType())) {
                imageCardLogo.setImageResource(R.drawable.ic_visa_logo);
                imageCardLogo.setVisibility(View.VISIBLE);
            } else if ("MASTERCARD".equalsIgnoreCase(card.getCardType())) {
                imageCardLogo.setImageResource(R.drawable.ic_masterpass_logo);
                imageCardLogo.setVisibility(View.VISIBLE);
            } else {
                imageCardLogo.setVisibility(View.INVISIBLE);
            }

            // Radio button durumunu ayarla
            boolean isSelected = card.getDocumentId() != null && card.getDocumentId().equals(selectedCardId);
            radioButton.setChecked(isSelected);
        }
    }
}