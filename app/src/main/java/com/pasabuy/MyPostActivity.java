package com.pasabuy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.pasabuy.models.Post;
import com.pasabuy.viewmodels.MyPostViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyPostActivity extends AppCompatActivity {

    private MyPostViewModel myPostViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_post);

        ImageButton backButtonMyPost = findViewById(R.id.backButtonMyPost);
        backButtonMyPost.setOnClickListener(v -> finish());

        // Find the postManageContainer
        LinearLayout postManageContainer = findViewById(R.id.postManageContainer);

        // Initialize ViewModel
        myPostViewModel = new ViewModelProvider(this).get(MyPostViewModel.class);

        // Observe LiveData from ViewModel
        myPostViewModel.getUserPostsLiveData().observe(this, userPosts -> {
            postManageContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            if (userPosts.isEmpty()) {
                View emptyView = inflater.inflate(R.layout.empty_text, postManageContainer, false);
                TextView emptyText = emptyView.findViewById(R.id.textName);
                emptyText.setText("You haven't posted yet.");
                postManageContainer.addView(emptyView);
                return;
            }

            for (Post post : userPosts) {
                View postView = inflater.inflate(R.layout.item_pasabuy_manage_post, postManageContainer, false);
                populatePostView(postView, post);
                postManageContainer.addView(postView);
            }
        });

        ImageView informationMyPost = findViewById(R.id.informationMyPost);
        informationMyPost.setOnClickListener(v -> showTooltip(v));
    }

    private void showTooltip(View anchorView) {
        // Create the tooltip layout
        View tooltipView = LayoutInflater.from(this).inflate(R.layout.tooltip_layout, null);
        TextView tooltipText = tooltipView.findViewById(R.id.tooltipText);
        tooltipText.setText("This section lets you view, manage, and edit all your posts. Hold post to delete it.");

        // Create the PopupWindow
        final PopupWindow tooltipPopup = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Measure the tooltipView to get its dimensions
        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();

        // Calculate the position to show the tooltip beside the icon
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorWidth = anchorView.getWidth();
        int xOffset = anchorWidth + 350;  // Adjust the offset as needed
        int yOffset = -(tooltipHeight / 12);  // Center the tooltip vertically

        // Show the PopupWindow at the calculated position
        tooltipPopup.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] - xOffset, location[1] + yOffset);

        // Dismiss the popup when clicked outside
        tooltipView.setOnTouchListener((v, event) -> {
            tooltipPopup.dismiss();
            return true;
        });
    }

    private void populatePostView(View postView, Post post) {
        TextView postTitle = postView.findViewById(R.id.postTitle);
        TextView postPrice = postView.findViewById(R.id.postPrice);
        TextView postDescription = postView.findViewById(R.id.postDescription);
        ImageView postThumbnail = postView.findViewById(R.id.postThumbnail);
        TextView amountOfTimePosted = postView.findViewById(R.id.amountOfTimePosted);
        TextView noOfPeopleJoined = postView.findViewById(R.id.noOfPeopleJoined);
        TextView deadlineCounter = postView.findViewById(R.id.deadlineCounter);

        postTitle.setText(post.getTitle());
        postPrice.setText("₱" + post.getPrice());
        postDescription.setText(post.getDescription());
        Glide.with(this).load(post.getThumbnail()).into(postThumbnail);
        noOfPeopleJoined.setText(String.valueOf(post.getJoinedUserIDs().size()));

        // Calculate remaining time
        long currentTime = System.currentTimeMillis();
        long deadlineTime = post.getDeadline().toDate().getTime();
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

        // Calculate time posted
        long timePosted = post.getTimePosted().toDate().getTime();
        long timePostedDiff = currentTime - timePosted;

        if (timePostedDiff < TimeUnit.MINUTES.toMillis(1)) {
            amountOfTimePosted.setText("Just Now");
        } else if (timePostedDiff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timePostedDiff);
            amountOfTimePosted.setText(minutes + (minutes == 1 ? " minute ago" : " minutes ago"));
        } else if (timePostedDiff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(timePostedDiff);
            amountOfTimePosted.setText(hours + (hours == 1 ? " hour ago" : " hours ago"));
        } else if (timePostedDiff < TimeUnit.DAYS.toMillis(3)) {
            long days = TimeUnit.MILLISECONDS.toDays(timePostedDiff);
            amountOfTimePosted.setText(days + (days == 1 ? " day ago" : " days ago"));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            amountOfTimePosted.setText(sdf.format(new Date(timePosted)));
        }

        // Set click listener to navigate to ManagePostActivity
        postView.setOnClickListener(v -> {
            Intent intent = new Intent(MyPostActivity.this, ManagePostActivity.class);
            intent.putExtra("postID", post.getPostID());
            startActivity(intent);
        });

        // Set long click listener to show delete confirmation dialog
        postView.setOnLongClickListener(v -> {
            showDeleteConfirmationDialog(post);
            return true;
        });
    }

    private void showDeleteConfirmationDialog(Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.confirm_delete_popup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button yesButton = dialogView.findViewById(R.id.yesButton);
        Button noButton = dialogView.findViewById(R.id.noButton);

        yesButton.setOnClickListener(v -> {
            deletePostAndConnections(post);
            dialog.dismiss();
        });

        noButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void deletePostAndConnections(Post post) {
        myPostViewModel.deletePostAndConnections(post, new MyPostViewModel.OnDeletePostListener() {
            @Override
            public void onDeleteSuccess() {
                Toast.makeText(MyPostActivity.this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                // Reload the activity after the post is deleted
                reloadActivity();
            }

            @Override
            public void onDeleteFailure(Exception e) {
                Toast.makeText(MyPostActivity.this, "Failed to delete post", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reloadActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}
