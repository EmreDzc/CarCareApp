package com.example.carcare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.models.AddressModel;

import java.util.List;

public class AddressSelectionAdapter extends RecyclerView.Adapter<AddressSelectionAdapter.AddressViewHolder> {

    private List<AddressModel> addressList;
    private String selectedAddressId;
    private OnAddressSelectedListener listener;

    public interface OnAddressSelectedListener {
        void onAddressSelected(AddressModel address);
    }

    public AddressSelectionAdapter(List<AddressModel> addressList, String selectedAddressId, OnAddressSelectedListener listener) {
        this.addressList = addressList;
        this.selectedAddressId = selectedAddressId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address_selection, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        AddressModel address = addressList.get(position);
        holder.bind(address);
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public class AddressViewHolder extends RecyclerView.ViewHolder {
        private RadioButton radioButton;
        private TextView textAddressTitle;
        private TextView textAddressLine1;
        private TextView textAddressLine2;
        private TextView textRecipientInfo;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radio_address);
            textAddressTitle = itemView.findViewById(R.id.text_address_title);
            textAddressLine1 = itemView.findViewById(R.id.text_address_line1);
            textAddressLine2 = itemView.findViewById(R.id.text_address_line2);
            textRecipientInfo = itemView.findViewById(R.id.text_recipient_info);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    AddressModel address = addressList.get(getAdapterPosition());
                    listener.onAddressSelected(address);
                }
            });

            radioButton.setOnClickListener(v -> {
                if (listener != null) {
                    AddressModel address = addressList.get(getAdapterPosition());
                    listener.onAddressSelected(address);
                }
            });
        }

        public void bind(AddressModel address) {
            textAddressTitle.setText(address.getTitle());
            textAddressLine1.setText(address.getAddressLine1());
            textAddressLine2.setText(address.getAddressLine2());

            String recipientInfo = address.getRecipientName() + " " + address.getRecipientSurname();
            if (address.getRecipientPhone() != null && !address.getRecipientPhone().isEmpty()) {
                recipientInfo += " • " + address.getRecipientPhone();
            }
            textRecipientInfo.setText(recipientInfo);

            // Radio button durumunu ayarla
            boolean isSelected = address.getDocumentId() != null && address.getDocumentId().equals(selectedAddressId);
            radioButton.setChecked(isSelected);
        }
    }
}