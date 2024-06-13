package com.pasabuy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Rating;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.pasabuy.SHOW_POPUP".equals(intent.getAction())) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
            String userID = sharedPreferences.getString("userID", null);

            if (userID != null) {
                checkForDeliveredOrders(context, userID);
            }
        }
    }

    private void checkForDeliveredOrders(Context context, String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders")
                .whereEqualTo("buyerID", userID)
                .whereEqualTo("status", "Delivered")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        AtomicBoolean popupShown = new AtomicBoolean(false);
                        for (QueryDocumentSnapshot orderDocument : task.getResult()) {
                            if (popupShown.get()) {
                                break;
                            }
                            String orderID = orderDocument.getId();
                            db.collection("ratings")
                                    .whereEqualTo("orderID", orderID)
                                    .whereEqualTo("ratedByUserID", userID)
                                    .get()
                                    .addOnCompleteListener(ratingTask -> {
                                        if (ratingTask.isSuccessful() && ratingTask.getResult().isEmpty()) {
                                            // If no rating document exists for this order, show the popup
                                            showRateUserPopup(context, orderDocument);
                                            popupShown.set(true);
                                        }
                                    });
                        }
                    }
                });
    }

    private void showRateUserPopup(Context context, QueryDocumentSnapshot orderDocument) {
        String postID = orderDocument.getString("postID");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("posts").document(postID).get().addOnCompleteListener(postTask -> {
            if (postTask.isSuccessful() && postTask.getResult() != null) {
                DocumentSnapshot postDocument = postTask.getResult();

                String postUserID = postDocument.getString("userID");
                if (postUserID != null) {

                    db.collection("users").document(postUserID).get().addOnCompleteListener(userTask -> {
                        if (userTask.isSuccessful() && userTask.getResult() != null) {
                            DocumentSnapshot userDocument = userTask.getResult();

                            LayoutInflater inflater = LayoutInflater.from(context);
                            View popupView = inflater.inflate(R.layout.rate_user_popup, null);

                            // Set user data in popup
                            ImageView ownerOfPostProfilePicture = popupView.findViewById(R.id.ownerOfPostProfilePicture);
                            TextView ownerOfPostFullname = popupView.findViewById(R.id.ownerOfPostFullname);
                            TextView ownerOfPostUsername = popupView.findViewById(R.id.ownerOfPostUsername);

                            Glide.with(context)
                                    .load(userDocument.getString("profilePictureURL"))
                                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                    .into(ownerOfPostProfilePicture);

                            ownerOfPostFullname.setText(userDocument.getString("firstName") + " " + userDocument.getString("lastName"));
                            ownerOfPostUsername.setText("@" + userDocument.getString("username"));

                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setView(popupView);
                            builder.setCancelable(false);
                            AlertDialog dialog = builder.create();

                            // Set the dialog to full-screen
                            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                            // Ensure the background is dimmed
                            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
                            lp.dimAmount = 0.8f; // Adjust the dim level (0.0 to 1.0)
                            dialog.getWindow().setAttributes(lp);

                            // Star rating behavior
                            List<ImageView> stars = new ArrayList<>();
                            stars.add(popupView.findViewById(R.id.star_rating_1));
                            stars.add(popupView.findViewById(R.id.star_rating_2));
                            stars.add(popupView.findViewById(R.id.star_rating_3));
                            stars.add(popupView.findViewById(R.id.star_rating_4));
                            stars.add(popupView.findViewById(R.id.star_rating_5));

                            final int[] rating = {0};

                            for (int i = 0; i < stars.size(); i++) {
                                final int index = i;
                                stars.get(i).setOnClickListener(v -> {
                                    rating[0] = index + 1;
                                    for (int j = 0; j < stars.size(); j++) {
                                        stars.get(j).setImageResource(j <= index ? R.drawable.filled_red_star : R.drawable.non_filled_star);
                                    }
                                });
                            }

                            Button submitButton = popupView.findViewById(R.id.submit_user_rate);
                            submitButton.setOnClickListener(v -> {
                                if (rating[0] > 0) {
                                    createRatingDocument(context, orderDocument, userDocument, rating[0]);
                                    dialog.dismiss();
                                }
                            });

                            Button declineButton = popupView.findViewById(R.id.decline_user_rate);
                            declineButton.setOnClickListener(v -> {
                                createRatingDocument(context, orderDocument, userDocument, 0);
                                dialog.dismiss();
                            });

                            dialog.show();

                            // Set post data in popup
                            ImageView orderPostPicture = popupView.findViewById(R.id.orderPostPicture);
                            TextView orderPostTitle = popupView.findViewById(R.id.orderPostTitle);
                            TextView orderPostPrice = popupView.findViewById(R.id.orderPostPrice);
                            TextView orderPostDescription = popupView.findViewById(R.id.orderPostDescription);

                            Glide.with(context)
                                    .load(postDocument.getString("thumbnail"))
                                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(20))) // Adjust the corner radius as needed
                                    .into(orderPostPicture);

                            orderPostTitle.setText(postDocument.getString("title"));
                            orderPostPrice.setText("₱" + postDocument.getDouble("price"));
                            orderPostDescription.setText(postDocument.getString("description"));
                        }
                    });
                }
            }
        });
    }

    private void createRatingDocument(Context context, QueryDocumentSnapshot orderDocument, DocumentSnapshot userDocument, int rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String ratingID = db.collection("ratings").document().getId();
        String orderID = orderDocument.getId();
        String ratedByUserID = orderDocument.getString("buyerID");
        String postUserID = userDocument.getId();
        Timestamp timestamp = Timestamp.now();

        Rating ratingData = new Rating(ratingID, orderID, ratedByUserID, postUserID, rating, timestamp);

        db.collection("ratings").document(ratingID).set(ratingData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (rating > 0) {
                            updateUserRating(context, userDocument, rating);
                        }
                    }
                });
    }

    private void updateUserRating(Context context, DocumentSnapshot userDocument, int rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userID = userDocument.getId();
        double currentRating = userDocument.getDouble("rating");
        int currentRatingCount = userDocument.getLong("ratingCount").intValue();

        double newRating = ((currentRating * currentRatingCount) + rating) / (currentRatingCount + 1);
        int newRatingCount = currentRatingCount + 1;

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", newRating);
        updates.put("ratingCount", newRatingCount);

        db.collection("users").document(userID).update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Rating updated successfully
                    }
                });
    }
}
