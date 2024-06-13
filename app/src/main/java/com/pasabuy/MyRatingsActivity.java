package com.pasabuy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Rating;
import com.pasabuy.viewmodels.MyRatingsViewModel;

import java.util.List;

public class MyRatingsActivity extends AppCompatActivity {

    private MyRatingsViewModel myRatingsViewModel;
    private FirebaseFirestore db;
    private static final String TAG = "MyRatingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_ratings);

        ImageButton backButtonMyRatings = findViewById(R.id.backButtonMyRatings);
        backButtonMyRatings.setOnClickListener(v -> finish());

        myRatingsViewModel = new ViewModelProvider(this).get(MyRatingsViewModel.class);
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userID", "");

        myRatingsViewModel.getRatings(userId).observe(this, this::populateRatings);

        ImageView informationMyRatings = findViewById(R.id.informationMyRatings);
        informationMyRatings.setOnClickListener(v -> showTooltip(v, "This section displays the ratings you've given to other users."));
    }

    private void showTooltip(View anchorView, String message) {
        View tooltipView = LayoutInflater.from(this).inflate(R.layout.tooltip_layout, null);
        TextView tooltipText = tooltipView.findViewById(R.id.tooltipText);
        tooltipText.setText(message);

        final PopupWindow tooltipPopup = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorWidth = anchorView.getWidth();
        int xOffset = anchorWidth + 350;
        int yOffset = -(tooltipHeight / 12);

        tooltipPopup.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] - xOffset, location[1] + yOffset);

        tooltipView.setOnTouchListener((v, event) -> {
            tooltipPopup.dismiss();
            return true;
        });
    }

    private void populateRatings(List<Rating> ratings) {
        LinearLayout myRatingsContainer = findViewById(R.id.myRatingsContainer);

        LayoutInflater inflater = LayoutInflater.from(this);

        Log.d(TAG, "Retrieved " + ratings.size() + " ratings");
        for (Rating rating : ratings) {
            Log.d(TAG, "Rating: " + rating.toString());
        }

        if (ratings.isEmpty()) {
            View emptyView = inflater.inflate(R.layout.empty_text, myRatingsContainer, false);
            TextView emptyText = emptyView.findViewById(R.id.textName);
            emptyText.setText("You haven't rated any user yet.");
            myRatingsContainer.addView(emptyView);
            return;
        }

        boolean isFirstRating = true;
        int ratingCount = 0;

        for (Rating rating : ratings) {
            ratingCount++;

            View ratingView = inflater.inflate(R.layout.item_user_rating, myRatingsContainer, false);

            if (!isFirstRating) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) ratingView.getLayoutParams();
                params.setMargins(0, -10, 0, 0);
                ratingView.setLayoutParams(params);
            }

            db.collection("users").document(rating.getPostUserID()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String profilePictureURL = documentSnapshot.getString("profilePictureURL");
                        String fullName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");
                        String username = documentSnapshot.getString("username");

                        ImageView profilePicture = ratingView.findViewById(R.id.order_post_user_profilePicture_userRating);
                        TextView fullNameText = ratingView.findViewById(R.id.order_post_user_fullName_userRating);
                        TextView usernameText = ratingView.findViewById(R.id.order_post_user_username_userRating);

                        fullNameText.setText(fullName);
                        usernameText.setText("@" + username);

                        Glide.with(this)
                                .load(profilePictureURL)
                                .circleCrop()
                                .into(profilePicture);
                    });

            db.collection("orders").document(rating.getOrderID()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        int quantity = documentSnapshot.getLong("quantity").intValue();
                        double totalPrice = documentSnapshot.getDouble("totalPrice");
                        String postID = documentSnapshot.getString("postID");

                        TextView quantityText = ratingView.findViewById(R.id.order_quantity_userRating);
                        TextView totalPriceText = ratingView.findViewById(R.id.order_totalPrice_userRating);

                        quantityText.setText("Quantity: " + quantity);
                        totalPriceText.setText("Total: ₱" + totalPrice);

                        db.collection("posts").document(postID).get()
                                .addOnSuccessListener(postSnapshot -> {
                                    String thumbnailURL = postSnapshot.getString("thumbnail");
                                    String title = postSnapshot.getString("title");

                                    ImageView thumbnail = ratingView.findViewById(R.id.order_post_thumbnail_userRating);
                                    TextView titleText = ratingView.findViewById(R.id.order_post_title_userRating);

                                    titleText.setText(title);

                                    Glide.with(this)
                                            .load(thumbnailURL)
                                            .into(thumbnail);
                                });
                    });

            int ratingValue = rating.getRating();
            LinearLayout starContainer = ratingView.findViewById(R.id.star_rating_container_userRating);
            for (int i = 0; i < starContainer.getChildCount(); i++) {
                ImageView star = (ImageView) starContainer.getChildAt(i);
                if (i < ratingValue) {
                    star.setImageResource(R.drawable.red_star_icon);
                } else {
                    star.setImageResource(R.drawable.grey_star_icon);
                }
            }

            myRatingsContainer.addView(ratingView);
            isFirstRating = false;
        }

        Log.d(TAG, "Iterated over " + ratingCount + " ratings.");
    }
}
