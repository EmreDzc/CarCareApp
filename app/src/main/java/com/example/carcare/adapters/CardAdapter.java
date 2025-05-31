package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.models.CardModel;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private Context context;
    private List<CardModel> cardList;
    private CardInteractionListener listener;

    public interface CardInteractionListener {
        void onDeleteCard(CardModel card);
        void onEditCard(CardModel card);
        // void onSetAsDefaultCard(CardModel card); // Opsiyonel
    }

    public CardAdapter(Context context, List<CardModel> cardList, CardInteractionListener listener) {
        this.context = context;
        this.cardList = cardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardModel card = cardList.get(position);

        holder.tvBankName.setText(card.getBankName() != null ? card.getBankName() : "Banka Bilgisi Yok"); // Banka adı
        holder.tvCardNumber.setText(card.getMaskedCardNumber());
        holder.tvCardHolder.setText(card.getCardHolderName());
        holder.tvExpiryDate.setText(card.getExpiryMonth() + "/" + card.getExpiryYear().substring(2)); // Yılın son iki hanesi

        // Kart tipine göre logoyu ayarla
        if ("VISA".equalsIgnoreCase(card.getCardType())) {
            holder.ivCardLogo.setImageResource(R.drawable.ic_visa_logo);
            holder.ivCardLogo.setVisibility(View.VISIBLE);
        } else if ("MASTERCARD".equalsIgnoreCase(card.getCardType())) {
            holder.ivCardLogo.setImageResource(R.drawable.ic_masterpass_logo);
            holder.ivCardLogo.setVisibility(View.VISIBLE);
        } else {
            // Bilinmeyen veya logosu olmayan kartlar için placeholder veya gizle
            holder.ivCardLogo.setVisibility(View.INVISIBLE); // veya GONE
        }

        // Varsayılan kart rozetini yönet
        if (card.isDefault()) {
            holder.tvDefaultBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvDefaultBadge.setVisibility(View.GONE);
        }

        holder.btnCardMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnCardMenu);
            popup.inflate(R.menu.card_item_menu); // Aynı menüyü kullanabiliriz veya card_item_menu oluşturabiliriz
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit_address) { // ID'yi R.id.action_edit_card yapın
                    if (listener != null) {
                        listener.onEditCard(card);
                    }
                    return true;
                } else if (itemId == R.id.action_delete_address) { // ID'yi R.id.action_delete_card yapın
                    if (listener != null) {
                        listener.onDeleteCard(card);
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // Kartın kendisine tıklandığında (MaterialCardView) düzenlemeye git
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditCard(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvBankName, tvDefaultBadge, tvCardNumber, tvCardHolder, tvExpiryDate;
        ImageView ivCardLogo;
        ImageButton btnCardMenu;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBankName = itemView.findViewById(R.id.tv_bank_name);
            tvDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
            tvCardNumber = itemView.findViewById(R.id.tv_card_number);
            tvCardHolder = itemView.findViewById(R.id.tv_card_holder);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            ivCardLogo = itemView.findViewById(R.id.iv_card_logo);
            btnCardMenu = itemView.findViewById(R.id.btn_card_menu);
        }
    }
}