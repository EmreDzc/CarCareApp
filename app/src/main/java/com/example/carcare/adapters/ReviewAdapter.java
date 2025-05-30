package com.example.carcare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.models.Review;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> reviewList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());


    public ReviewAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.reviewerName.setText(review.getUserName() != null ? review.getUserName() : "Anonim");
        holder.ratingBar.setRating(review.getRating());
        holder.reviewComment.setText(review.getComment());

        if (review.getTimestamp() != null) {
            holder.reviewDate.setText(dateFormat.format(review.getTimestamp()));
            holder.reviewDate.setVisibility(View.VISIBLE);
        } else {
            holder.reviewDate.setVisibility(View.GONE);
        }

        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            holder.reviewComment.setVisibility(View.GONE);
        } else {
            holder.reviewComment.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public void updateReviews(List<Review> newReviews) {
        if (newReviews == null) return;
        this.reviewList.clear();
        this.reviewList.addAll(newReviews);
        notifyDataSetChanged();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerName, reviewComment, reviewDate;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerName = itemView.findViewById(R.id.text_reviewer_name);
            reviewComment = itemView.findViewById(R.id.text_review_comment);
            reviewDate = itemView.findViewById(R.id.text_review_date);
            ratingBar = itemView.findViewById(R.id.rating_bar_review_item);
        }
    }
}