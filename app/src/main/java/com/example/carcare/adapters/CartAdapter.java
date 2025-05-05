package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private List<Product> cartItems;
    private Context context;
    private Runnable onCartUpdated;

    public CartAdapter(List<Product> items, Context ctx, Runnable updateCallback) {
        this.cartItems = items;
        this.context = ctx;
        this.onCartUpdated = updateCallback;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, price;
        public ImageView image;
        public Button remove;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.textCartItemName);
            price = view.findViewById(R.id.textCartItemPrice);
            image = view.findViewById(R.id.imageCartItem);
            remove = view.findViewById(R.id.buttonRemove);
        }
    }

    @Override
    public CartAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product product = cartItems.get(position);
        holder.name.setText(product.getName());
        holder.price.setText("$" + product.getPrice());
        holder.image.setImageResource(product.getImageResId());

        holder.remove.setOnClickListener(v -> {
            Cart.getInstance().removeItem(product);
            notifyDataSetChanged();
            onCartUpdated.run();
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }
}
