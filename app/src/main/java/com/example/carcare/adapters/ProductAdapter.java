package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;


import com.example.carcare.R;
import com.example.carcare.models.Product;
import com.example.carcare.utils.Cart;

import java.util.List;
import java.util.List;


public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public void filter(String text) {
        productList.clear();
        if (text.isEmpty()) {
            productList.addAll(allProducts);
        } else {
            text = text.toLowerCase();
            for (Product item : allProducts) {
                if (item.getName().toLowerCase().contains(text)) {
                    productList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    private List<Product> productList;
    private Context context;

    private List<Product> allProducts;


    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = new ArrayList<>(productList);
        this.allProducts = new ArrayList<>(productList); // <- Add this line
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, desc, price;
        public ImageView image;
        public Button addButton;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.textProductName);
            desc = view.findViewById(R.id.textProductDesc);
            price = view.findViewById(R.id.textProductPrice);
            image = view.findViewById(R.id.imageProduct);
            addButton = view.findViewById(R.id.buttonAddToCart);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product p = productList.get(position);
        holder.name.setText(p.getName());
        holder.desc.setText(p.getDescription());
        holder.price.setText("$" + p.getPrice());
        holder.image.setImageResource(p.getImageResId());

        holder.addButton.setOnClickListener(v -> {
            Cart.getInstance().addItem(p);
            Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
