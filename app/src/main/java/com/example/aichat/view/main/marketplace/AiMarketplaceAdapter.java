package com.example.aichat.view.main.marketplace;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;

import java.util.ArrayList;
import java.util.List;

public class AiMarketplaceAdapter extends RecyclerView.Adapter<AiMarketplaceAdapter.ViewHolder> {

    private List<AiModel> items = new ArrayList<>();

    public void setItems(List<AiModel> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_model, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiModel item = items.get(position);

        holder.title.setText(item.title);
        holder.category.setText(item.category);
        holder.price.setText(item.price);
        holder.rating.setText(item.rating);
        holder.reviews.setText("(" + item.reviews + ")");
        holder.downloads.setText(item.downloads);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, category, price, rating, reviews, downloads;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            category = itemView.findViewById(R.id.category);
            price = itemView.findViewById(R.id.price);
            rating = itemView.findViewById(R.id.rating);
            reviews = itemView.findViewById(R.id.reviews);
            downloads = itemView.findViewById(R.id.downloads);
        }
    }
}