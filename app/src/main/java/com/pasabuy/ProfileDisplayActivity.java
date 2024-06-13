package com.pasabuy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Post;
import com.pasabuy.viewmodels.ProfileDisplayViewModel;

import java.util.List;

public class ProfileDisplayActivity extends AppCompatActivity {

    private ProfileDisplayViewModel profileDisplayViewModel;
    private FirebaseFirestore db;
    private LinearLayout pasabuyCardsProfileDisplayContainer;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_display);

        ImageButton backButtonProfileDisplay = findViewById(R.id.backButtonProfileDisplay);
        backButtonProfileDisplay.setOnClickListener(v -> finish());

        profileDisplayViewModel = new ViewModelProvider(this).get(ProfileDisplayViewModel.class);
        db = FirebaseFirestore.getInstance();

        String userId = getIntent().getStringExtra("userID");

        pasabuyCardsProfileDisplayContainer = findViewById(R.id.pasabuyCardsProfileDisplayContainer);

        loadUserProfile(userId);
        loadUserPosts(userId, selectedCategory);
        setupFilterButtons(userId);
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            String profilePictureURL = documentSnapshot.getString("profilePictureURL");
            String firstName = documentSnapshot.getString("firstName");
            String fullName = firstName + " " + documentSnapshot.getString("lastName");
            String username = documentSnapshot.getString("username");
            double rating = documentSnapshot.getDouble("rating");

            ImageView userProfilePicture = findViewById(R.id.userProfilePictureProfileDisplay);
            TextView userFullName = findViewById(R.id.userFullNameProfileDisplay);
            TextView userRatings = findViewById(R.id.userRatingsProfileDisplay);
            TextView usernameView = findViewById(R.id.usernameProfileDisplay);
            TextView profileDisplayText = findViewById(R.id.profileDisplayText);

            userFullName.setText(fullName);
            userRatings.setText(String.valueOf(rating));
            usernameView.setText("@" + username);
            profileDisplayText.setText(firstName + "'s Profile");

            Glide.with(this)
                    .load(profilePictureURL)
                    .circleCrop()
                    .into(userProfilePicture);
        });
    }

    private void loadUserPosts(String userId, String category) {
        db.collection("posts")
                .whereEqualTo("userID", userId)
                .whereEqualTo("status", "Open")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Post> postList = task.getResult().toObjects(Post.class);
                        filterAndDisplayPosts(postList, category);
                    }
                });
    }

    private void filterAndDisplayPosts(List<Post> posts, String category) {
        pasabuyCardsProfileDisplayContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        boolean hasPosts = false;
        for (Post post : posts) {
            if (category.equals("All") || post.getCategory().equals(category)) {
                hasPosts = true;
                View cardView = inflater.inflate(R.layout.item_pasabuy_post, pasabuyCardsProfileDisplayContainer, false);
                populateCardView(cardView, post);
                pasabuyCardsProfileDisplayContainer.addView(cardView);
            }
        }

        if (!hasPosts) {
            displayEmptyState(category);
        }
    }

    private void populateCardView(View cardView, Post post) {
        TextView postTitle = cardView.findViewById(R.id.postTitle);
        TextView postPrice = cardView.findViewById(R.id.postPrice);
        TextView postDescription = cardView.findViewById(R.id.postDescription);
        ImageView postThumbnail = cardView.findViewById(R.id.postThumbnail);
        TextView userFullNameInPost = cardView.findViewById(R.id.userFullNameInPost);
        TextView userRatingsInPost = cardView.findViewById(R.id.userRatingsInPost);
        TextView amountOfTimePosted = cardView.findViewById(R.id.amountOfTimePosted);
        ImageView profilePictureImage = cardView.findViewById(R.id.profilePictureImage);
        TextView noOfPeopleJoined = cardView.findViewById(R.id.noOfPeopleJoined);
        TextView deadlineCounter = cardView.findViewById(R.id.deadlineCounter);

        postTitle.setText(post.getTitle());
        postPrice.setText("₱" + post.getPrice());
        postDescription.setText(post.getDescription());
        Glide.with(this).load(post.getThumbnail()).into(postThumbnail);

        db.collection("users").document(post.getUserID()).get().addOnSuccessListener(documentSnapshot -> {
            String fullName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");
            double rating = documentSnapshot.getDouble("rating");
            String profilePictureURL = documentSnapshot.getString("profilePictureURL");

            userFullNameInPost.setText(fullName);
            userRatingsInPost.setText(String.valueOf(rating));
            Glide.with(this).load(profilePictureURL).circleCrop().into(profilePictureImage);
        });

        noOfPeopleJoined.setText(String.valueOf(post.getJoinedUserIDs().size()));
        updateDeadlineCounter(deadlineCounter, post.getDeadline().toDate().getTime());
        updateTimePosted(amountOfTimePosted, post.getTimePosted().toDate().getTime());

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewPostActivity.class);
            intent.putExtra("postID", post.getPostID());
            startActivity(intent);
        });
    }

    private void setupFilterButtons(String userId) {
        LinearLayout filterAll = findViewById(R.id.filterAllProfileDisplay);
        LinearLayout filterFoods = findViewById(R.id.filterFoodsProfileDisplay);
        LinearLayout filterItems = findViewById(R.id.filterItemsProfileDisplay);

        filterAll.setSelected(true);
        updateFilterButtonState(filterAll, R.drawable.all_icon_selected, R.color.white);
        updateFilterButtonState(filterFoods, R.drawable.foods_icon, R.color.cyan_2);
        updateFilterButtonState(filterItems, R.drawable.items_icon, R.color.cyan_2);

        View.OnClickListener filterClickListener = v -> {
            filterAll.setSelected(false);
            filterFoods.setSelected(false);
            filterItems.setSelected(false);
            updateFilterButtonState(filterAll, R.drawable.all_icon, R.color.cyan_2);
            updateFilterButtonState(filterFoods, R.drawable.foods_icon, R.color.cyan_2);
            updateFilterButtonState(filterItems, R.drawable.items_icon, R.color.cyan_2);

            v.setSelected(true);
            if (v.getId() == R.id.filterAllProfileDisplay) {
                updateFilterButtonState(filterAll, R.drawable.all_icon_selected, R.color.white);
                selectedCategory = "All";
            } else if (v.getId() == R.id.filterFoodsProfileDisplay) {
                updateFilterButtonState(filterFoods, R.drawable.foods_icon_selected, R.color.white);
                selectedCategory = "Food";
            } else if (v.getId() == R.id.filterItemsProfileDisplay) {
                updateFilterButtonState(filterItems, R.drawable.items_icon_selected, R.color.white);
                selectedCategory = "Item";
            }

            loadUserPosts(userId, selectedCategory);
        };

        filterAll.setOnClickListener(filterClickListener);
        filterFoods.setOnClickListener(filterClickListener);
        filterItems.setOnClickListener(filterClickListener);
    }

    private void updateFilterButtonState(LinearLayout filterButton, int iconResId, int textColorResId) {
        ImageView icon = filterButton.findViewById(R.id.filterAllIconProfileDisplay);
        TextView text = filterButton.findViewById(R.id.filterAllTextProfileDisplay);

        if (filterButton.getId() == R.id.filterFoodsProfileDisplay) {
            icon = filterButton.findViewById(R.id.filterFoodsIconProfileDisplay);
            text = filterButton.findViewById(R.id.filterFoodsTextProfileDisplay);
        } else if (filterButton.getId() == R.id.filterItemsProfileDisplay) {
            icon = filterButton.findViewById(R.id.filterItemsIconProfileDisplay);
            text = filterButton.findViewById(R.id.filterItemsTextProfileDisplay);
        }

        if (icon != null && text != null) {
            icon.setImageResource(iconResId);
            text.setTextColor(getResources().getColor(textColorResId, null));
        }
    }

    private void updateDeadlineCounter(TextView deadlineCounter, long deadlineTime) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = deadlineTime - currentTime;

        if (timeDiff <= 0) {
            deadlineCounter.setText("Ended");
        } else {
            long diffInMinutes = timeDiff / (1000 * 60);
            long diffInHours = timeDiff / (1000 * 60 * 60);
            long diffInDays = timeDiff / (1000 * 60 * 60 * 24);

            if (diffInDays > 0) {
                deadlineCounter.setText("Ends in " + diffInDays + (diffInDays == 1 ? " day" : " days"));
            } else if (diffInHours > 0) {
                deadlineCounter.setText("Ends in " + diffInHours + (diffInHours == 1 ? " hour" : " hours"));
            } else {
                deadlineCounter.setText("Ends in " + diffInMinutes + (diffInMinutes == 1 ? " minute" : " minutes"));
            }
        }
    }

    private void updateTimePosted(TextView amountOfTimePosted, long timePosted) {
        long currentTime = System.currentTimeMillis();
        long timePostedDiff = currentTime - timePosted;

        if (timePostedDiff < 60000) {
            amountOfTimePosted.setText("Just Now");
        } else if (timePostedDiff < 3600000) {
            long minutes = timePostedDiff / 60000;
            amountOfTimePosted.setText(minutes + (minutes == 1 ? " minute ago" : " minutes ago"));
        } else if (timePostedDiff < 86400000) {
            long hours = timePostedDiff / 3600000;
            amountOfTimePosted.setText(hours + (hours == 1 ? " hour ago" : " hours ago"));
        } else if (timePostedDiff < 259200000) {
            long days = timePostedDiff / 86400000;
            amountOfTimePosted.setText(days + (days == 1 ? " day ago" : " days ago"));
        } else {
            amountOfTimePosted.setText(android.text.format.DateFormat.format("MMM dd, yyyy", new java.util.Date(timePosted)));
        }
    }

    private void displayEmptyState(String category) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View emptyView = inflater.inflate(R.layout.empty_text, pasabuyCardsProfileDisplayContainer, false);
        TextView emptyText = emptyView.findViewById(R.id.textName);
        String message = "No posts available";

        switch (category) {
            case "All":
                message = "No posts available";
                break;
            case "Food":
                message = "No food posts available";
                break;
            case "Item":
                message = "No item posts available";
                break;
        }

        emptyText.setText(message);
        pasabuyCardsProfileDisplayContainer.addView(emptyView);
    }
}
