package com.example.carcare.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast; // Buton tıklamaları için
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carcare.R;
import com.example.carcare.models.Review;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private static final String TAG = "ReviewAdapter";
    private Context context;
    private List<Review> internalReviewList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr", "TR"));

    public ReviewAdapter(Context context, List<Review> initialReviewList) {
        this.context = context;
        this.internalReviewList = new ArrayList<>();
        if (initialReviewList != null) {
            this.internalReviewList.addAll(initialReviewList);
        }
        Log.d(TAG, "ReviewAdapter constructor - initial internal size: " + this.internalReviewList.size());
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called");
        // Yeni item_review.xml layout'unu inflate et
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder called for position: " + position);

        if (internalReviewList == null || position < 0 || position >= internalReviewList.size()) {
            Log.e(TAG, "Invalid position or internalReviewList. Position: " + position +
                    ", List size: " + (internalReviewList != null ? internalReviewList.size() : "null"));
            // Gerekirse varsayılan değerler ata
            holder.reviewerName.setText("N/A");
            holder.ratingBar.setRating(0);
            holder.reviewComment.setText("");
            holder.reviewDate.setText("");
            holder.layoutSellerInfoReview.setVisibility(View.GONE);
            holder.helpfulCountText.setText("0");
            return;
        }

        Review review = internalReviewList.get(position);
        if (review == null) {
            Log.e(TAG, "Review at position " + position + " is null");
            // Gerekirse varsayılan değerler ata
            holder.reviewerName.setText("N/A");
            holder.ratingBar.setRating(0);
            holder.reviewComment.setText("");
            holder.reviewDate.setText("");
            holder.layoutSellerInfoReview.setVisibility(View.GONE);
            holder.helpfulCountText.setText("0");
            return;
        }

        // Kullanıcı Adını Maskeleme (Örnek)
        String originalUserName = review.getUserName();
        if (originalUserName != null && !originalUserName.isEmpty()) {
            if (originalUserName.length() > 2) {
                holder.reviewerName.setText(originalUserName.charAt(0) + "**" + originalUserName.substring(originalUserName.length() - Math.min(2, originalUserName.length()-1) ));
            } else if (originalUserName.length() == 1) {
                holder.reviewerName.setText(originalUserName.charAt(0) + "**");
            }
            else { // 2 karakter ise
                holder.reviewerName.setText(originalUserName.charAt(0) + "*" + originalUserName.charAt(1));
            }
        } else {
            holder.reviewerName.setText("Anonim");
        }


        holder.ratingBar.setRating(review.getRating());

        if (TextUtils.isEmpty(review.getComment())) {
            holder.reviewComment.setVisibility(View.GONE);
        } else {
            holder.reviewComment.setVisibility(View.VISIBLE);
            holder.reviewComment.setText(review.getComment());
        }

        if (review.getTimestamp() != null) {
            holder.reviewDate.setText(dateFormat.format(review.getTimestamp()));
            holder.reviewDate.setVisibility(View.VISIBLE);
        } else {
            holder.reviewDate.setVisibility(View.GONE);
        }

        // Satıcı Adı (Bu bilgi Review modelinizde varsa ve doluysa gösterin)
        // Örneğin, Review modelinde `private String sellerName;` alanı varsa:
        // String sellerName = review.getSellerName();
        // if (sellerName != null && !sellerName.isEmpty()) {
        //     holder.layoutSellerInfoReview.setVisibility(View.VISIBLE);
        //     holder.reviewSellerName.setText(sellerName);
        // } else {
        //     holder.layoutSellerInfoReview.setVisibility(View.GONE);
        // }
        // Şimdilik örnek olarak gizli bırakıyoruz:
        holder.layoutSellerInfoReview.setVisibility(View.GONE);


        // Faydalı Oy Sayısı (Bu bilgi Review modelinizde varsa)
        // Örneğin, Review modelinde `private int helpfulVotes;` alanı varsa:
        // holder.helpfulCountText.setText(String.valueOf(review.getHelpfulVotes()));
        // Şimdilik varsayılan olarak 0 gösteriyoruz:
        holder.helpfulCountText.setText("0");

        // Buton Tıklama Listener'ları
        holder.btnHelpfulYes.setOnClickListener(v -> {
            // TODO: Faydalı oy verme mantığını implemente edin
            // Örneğin, review.getId() ile Firestore'da ilgili yorumu güncelleyin
            Toast.makeText(context, "Yorum faydalı bulundu (ID: " + review.getId() + ") - TODO", Toast.LENGTH_SHORT).show();
        });

        holder.btnReportReview.setOnClickListener(v -> {
            // TODO: Yorumu bildirme mantığını implemente edin
            Toast.makeText(context, "Yorum bildirildi (ID: " + review.getId() + ") - TODO", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        int count = internalReviewList != null ? internalReviewList.size() : 0;
        Log.d(TAG, "getItemCount returning: " + count);
        return count;
    }

    public void updateReviews(List<Review> newListOfReviews) {
        Log.d(TAG, "updateReviews called with " +
                (newListOfReviews != null ? newListOfReviews.size() : "null") + " reviews");

        this.internalReviewList.clear();
        if (newListOfReviews != null) {
            this.internalReviewList.addAll(newListOfReviews);
        }

        Log.d(TAG, "Adapter's internal list updated. New size: " + this.internalReviewList.size());
        notifyDataSetChanged();
        Log.d(TAG, "notifyDataSetChanged completed. Final internal list size: " + this.internalReviewList.size());
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewerName, reviewComment, reviewDate, reviewSellerName, helpfulCountText;
        RatingBar ratingBar;
        ImageButton btnHelpfulYes, btnReportReview;
        LinearLayout layoutSellerInfoReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewerName = itemView.findViewById(R.id.text_reviewer_name);
            reviewComment = itemView.findViewById(R.id.text_review_comment);
            reviewDate = itemView.findViewById(R.id.text_review_date);
            ratingBar = itemView.findViewById(R.id.rating_bar_review_item);

            layoutSellerInfoReview = itemView.findViewById(R.id.layout_seller_info_review);
            reviewSellerName = itemView.findViewById(R.id.text_review_seller_name);
            btnHelpfulYes = itemView.findViewById(R.id.btn_helpful_yes);
            helpfulCountText = itemView.findViewById(R.id.text_helpful_count);
            btnReportReview = itemView.findViewById(R.id.btn_report_review);
        }
    }
}