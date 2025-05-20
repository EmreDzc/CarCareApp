package com.example.carcare.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.models.NearbyPlace;

import java.util.ArrayList;
import java.util.List;

public class NearbyPlacesAdapter extends RecyclerView.Adapter<NearbyPlacesAdapter.PlaceViewHolder> {

    private Context context;
    private List<NearbyPlace> placesList;
    private PlaceClickListener placeClickListener;

    public NearbyPlacesAdapter(Context context) {
        this.context = context;
        this.placesList = new ArrayList<>();
    }

    public void setPlaceClickListener(PlaceClickListener listener) {
        this.placeClickListener = listener;
    }

    public void updatePlaces(List<NearbyPlace> newPlaces) {
        this.placesList.clear();
        if (newPlaces != null) {
            this.placesList.addAll(newPlaces);
        }
        notifyDataSetChanged();
    }

    // Bu metot, adapter dışında erişim için eklenir
    public NearbyPlace getPlaceAtPosition(int position) {
        if (position >= 0 && position < placesList.size()) {
            return placesList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        NearbyPlace place = placesList.get(position);

        holder.placeName.setText(place.getName());
        holder.placeAddress.setText(place.getAddress());
        holder.placeDistance.setText(place.getFormattedDistance());

        // Only show rating if available
        if (place.getRating() > 0) {
            holder.placeRating.setVisibility(View.VISIBLE);
            holder.placeRating.setText(String.valueOf(place.getRating()));
        } else {
            holder.placeRating.setVisibility(View.GONE);
        }

        // Set appropriate icon based on place type
        switch (place.getType()) {
            case NearbyPlace.Type.GAS:
                holder.placeIcon.setImageResource(android.R.drawable.ic_dialog_dialer); // Replace with proper gas station icon
                break;
            case NearbyPlace.Type.SERVICE:
                holder.placeIcon.setImageResource(android.R.drawable.ic_menu_manage); // Replace with proper service icon
                break;
            case NearbyPlace.Type.WASH:
                holder.placeIcon.setImageResource(android.R.drawable.ic_menu_upload); // Replace with proper car wash icon
                break;
            case NearbyPlace.Type.PARTS:
                holder.placeIcon.setImageResource(android.R.drawable.ic_menu_gallery); // Replace with proper parts icon
                break;
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (placeClickListener != null) {
                placeClickListener.onPlaceClick(place);
            }
        });

        holder.navigateButton.setOnClickListener(v -> {
            navigateToPlace(place);
        });
    }

    private void navigateToPlace(NearbyPlace place) {
        // Create Google Maps navigation intent
        Uri navigationUri = Uri.parse("google.navigation:q=" +
                place.getLocation().latitude + "," +
                place.getLocation().longitude +
                "&mode=d");

        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, navigationUri);
        navigationIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (navigationIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(navigationIntent);
        } else {
            // Fallback to browser if Google Maps isn't installed
            Uri fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                    place.getLocation().latitude + "," +
                    place.getLocation().longitude);

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
            context.startActivity(browserIntent);
        }
    }

    @Override
    public int getItemCount() {
        return placesList.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        ImageView placeIcon;
        TextView placeName;
        TextView placeAddress;
        TextView placeDistance;
        TextView placeRating;
        ImageButton navigateButton;

        PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeIcon = itemView.findViewById(R.id.place_icon);
            placeName = itemView.findViewById(R.id.place_name);
            placeAddress = itemView.findViewById(R.id.place_address);
            placeDistance = itemView.findViewById(R.id.place_distance);
            placeRating = itemView.findViewById(R.id.place_rating);
            navigateButton = itemView.findViewById(R.id.navigate_button);
        }
    }

    // Interface for click events
    public interface PlaceClickListener {
        void onPlaceClick(NearbyPlace place);
    }
}