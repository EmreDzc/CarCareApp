package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu; // PopupMenu için eklendi
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.models.AddressModel;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private Context context;
    private List<AddressModel> addressList;
    private AddressInteractionListener listener;

    // Listener interface'i aynı kalabilir veya "Varsayılan Yap" gibi yeni metodlar eklenebilir
    public interface AddressInteractionListener {
        void onEditAddress(AddressModel address);
        void onDeleteAddress(AddressModel address);
        // void onSetAsDefaultAddress(AddressModel address); // Opsiyonel
    }

    public AddressAdapter(Context context, List<AddressModel> addressList, AddressInteractionListener listener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        AddressModel address = addressList.get(position);

        holder.textAddressTitle.setText(address.getTitle());
        holder.textAddressLine.setText(address.getAddressLine1()); // Modeldeki yeni yardımcı metodu kullan
        holder.textAddressDistrict.setText(address.getAddressLine2()); // Modeldeki yeni yardımcı metodu kullan

        // Varsayılan adres rozetini yönet
        if (address.isDefaultAddress()) { // Modelde isDefaultAddress alanı olmalı
            holder.textDefaultBadge.setVisibility(View.VISIBLE);
        } else {
            holder.textDefaultBadge.setVisibility(View.GONE);
        }

        // Alıcı bilgilerini yönet
        if ("delivery".equalsIgnoreCase(address.getAddressType()) &&
                address.getRecipientName() != null && !address.getRecipientName().isEmpty()) {
            holder.textRecipientName.setText(address.getRecipientName() + " " + address.getRecipientSurname());
            holder.textRecipientPhone.setText(address.getRecipientPhone());
            holder.layoutRecipientInfo.setVisibility(View.VISIBLE); // LinearLayout'u görünür yap
        } else {
            holder.layoutRecipientInfo.setVisibility(View.GONE); // LinearLayout'u gizle
        }

        // Menü butonuna tıklama olayı
        holder.btnAddressMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnAddressMenu);
            popup.inflate(R.menu.address_item_menu); // address_item_menu.xml oluşturulacak
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit_address) {
                    if (listener != null) {
                        listener.onEditAddress(address);
                    }
                    return true;
                } else if (itemId == R.id.action_delete_address) {
                    if (listener != null) {
                        listener.onDeleteAddress(address);
                    }
                    return true;
                }
                // Opsiyonel: Varsayılan yap
                /* else if (itemId == R.id.action_set_default_address) {
                    if (listener != null) {
                        listener.onSetAsDefaultAddress(address);
                    }
                    return true;
                }*/
                return false;
            });
            popup.show();
        });

        // Kartın kendisine tıklanırsa ne olacağı (belki düzenlemeye gitmek?)
        holder.itemView.setOnClickListener(v -> {
            // İsteğe bağlı: Karta tıklandığında da düzenleme sayfasına gidilebilir
            // if (listener != null) {
            // listener.onEditAddress(address);
            // }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView textAddressTitle, textDefaultBadge, textAddressLine, textAddressDistrict, textRecipientName, textRecipientPhone;
        ImageButton btnAddressMenu;
        View layoutRecipientInfo; // Alıcı bilgilerini içeren LinearLayout

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            textAddressTitle = itemView.findViewById(R.id.tv_address_title);
            textDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
            textAddressLine = itemView.findViewById(R.id.tv_address_line);
            textAddressDistrict = itemView.findViewById(R.id.tv_address_district);
            textRecipientName = itemView.findViewById(R.id.tv_recipient_name);
            textRecipientPhone = itemView.findViewById(R.id.tv_recipient_phone);
            btnAddressMenu = itemView.findViewById(R.id.btn_address_menu);
            layoutRecipientInfo = itemView.findViewById(R.id.layout_recipient_info); // XML'de bu LinearLayout'a bir ID verin
        }
    }
}