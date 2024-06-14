package com.pasabuy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.pasabuy.adapters.ImageAdapter;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.viewmodels.ManagePostViewModel;
import com.bumptech.glide.Glide;

import java.util.List;

public class ManagePostActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EDIT_POST = 2;

    private ManagePostViewModel managePostViewModel;
    private ViewPager viewPager;
    private LinearLayout pasabuyerContainer;
    private Button requestButton;
    private Button customerButton;
    private boolean isRequestSelected = true; // Flag to track which button is selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_post);

        ImageButton backButtonManagePost = findViewById(R.id.backButtonManagePost);
        backButtonManagePost.setOnClickListener(v -> finish());

        managePostViewModel = new ViewModelProvider(this).get(ManagePostViewModel.class);

        // Get postID from Intent
        Intent intent = getIntent();
        String postID = intent.getStringExtra("postID");

        // Set up ViewPager and TabLayout
        viewPager = findViewById(R.id.viewPagerManagePost);
        TabLayout tabLayout = findViewById(R.id.tabLayoutManagePost);
        tabLayout.setupWithViewPager(viewPager, true);

        // Find buttons and container
        requestButton = findViewById(R.id.requestButton);
        customerButton = findViewById(R.id.customerButton);
        pasabuyerContainer = findViewById(R.id.pasabuyerContainer);

        // Set default button states
        requestButton.setBackgroundResource(R.drawable.outline_top_bottom_background);
        requestButton.setTextColor(getResources().getColor(R.color.cyan_1));
        customerButton.setBackgroundResource(R.drawable.outline_top_bottom_gray_background);
        customerButton.setTextColor(getResources().getColor(R.color.cyan_3));

        // Observe LiveData from ViewModel
        managePostViewModel.getPostLiveData().observe(this, this::populatePostDetails);
        managePostViewModel.getImageUrlsLiveData().observe(this, this::setupViewPager);
        managePostViewModel.getOrdersLiveData().observe(this, this::populateOrders);

        // Fetch post details
        managePostViewModel.fetchPostDetails(postID);
        managePostViewModel.fetchOrders(postID);

        // Set button click listeners
        requestButton.setOnClickListener(v -> showRequests());
        customerButton.setOnClickListener(v -> showCustomers());

        // By default, show requests
        showRequests();

        // Add listener to editButton to navigate to EditPostActivity
        ImageButton editButton = findViewById(R.id.editButton);
        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(ManagePostActivity.this, EditPostActivity.class);
            editIntent.putExtra("postID", postID);
            startActivityForResult(editIntent, REQUEST_CODE_EDIT_POST);
        });
    }

    private void populatePostDetails(Post post) {
        try {
            TextView productTitle = findViewById(R.id.productTitleManagePost);
            TextView productPrice = findViewById(R.id.productPriceManagePost);
            TextView productDescription = findViewById(R.id.productDescriptionManagePost);
            TextView deadlineCounterManagePost = findViewById(R.id.deadlineCounterManagePost);
            TextView noOfPeopleJoinedManagePost = findViewById(R.id.noOfPeopleJoinedManagePost);

            productTitle.setText(post.getTitle());
            productPrice.setText("₱" + post.getPrice());
            productDescription.setText(post.getDescription());
            noOfPeopleJoinedManagePost.setText(String.valueOf(post.getJoinedUserIDs().size()));

            // Update deadline counter
            long currentTime = System.currentTimeMillis();
            long deadlineTime = post.getDeadline().toDate().getTime();
            long timeDiff = deadlineTime - currentTime;

            if (timeDiff <= 0) {
                deadlineCounterManagePost.setText("Ended");
            } else {
                long diffInMinutes = timeDiff / (1000 * 60);
                long diffInHours = timeDiff / (1000 * 60 * 60);
                long diffInDays = timeDiff / (1000 * 60 * 60 * 24);

                if (diffInDays > 0) {
                    deadlineCounterManagePost.setText("Ends in " + diffInDays + (diffInDays == 1 ? " day" : " days"));
                } else if (diffInHours > 0) {
                    deadlineCounterManagePost.setText("Ends in " + diffInHours + (diffInHours == 1 ? " hour" : " hours"));
                } else {
                    deadlineCounterManagePost.setText("Ends in " + diffInMinutes + (diffInMinutes == 1 ? " minute" : " minutes"));
                }
            }
            Log.d("ManagePostActivity", "Post details displayed: " + post);
        } catch (Exception e) {
            Log.e("ManagePostActivity", "Error in populatePostDetails: ", e);
        }
    }

    private void setupViewPager(List<String> imageUrls) {
        ImageAdapter adapter = new ImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        Log.d("ManagePostActivity", "Image URLs displayed: " + imageUrls);
    }

    private void populateOrders(List<Order> orders) {
        if (orders == null) {
            Log.d("ManagePostActivity", "No orders to display");
            return; // Prevents crashing if ordersLiveData is not initialized yet
        }

        // Clear current container views
        pasabuyerContainer.removeAllViews();
        Log.d("ManagePostActivity", "Populating orders: " + orders.size() + " orders");

        for (Order order : orders) {
            if (isRequestButtonSelected()) {
                if (order.getStatus().equals("Pending")) {
                    addRequestOrderToContainer(order);
                }
            } else {
                if (order.getStatus().equals("Buying") || order.getStatus().equals("Delivering")) {
                    addCustomerOrderToContainer(order);
                }
            }
        }
    }

    private void addRequestOrderToContainer(Order order) {
        // Inflate the request item layout
        View requestView = getLayoutInflater().inflate(R.layout.item_pasabuy_request, pasabuyerContainer, false);

        // Find and set user details using buyerID
        managePostViewModel.fetchUserDetails(order.getBuyerID()).observe(this, user -> {
            TextView userName = requestView.findViewById(R.id.user_name);
            TextView orderCount = requestView.findViewById(R.id.order_count);
            ImageView profileIcon = requestView.findViewById(R.id.profile_icon);

            userName.setText(user.getFirstName() + " " + user.getLastName());
            orderCount.setText("Orders: " + order.getQuantity());
            if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfilePictureURL())
                        .placeholder(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profileIcon);
            } else {
                Glide.with(this)
                        .load(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profileIcon);
            }
            Log.d("ManagePostActivity", "Request order user details displayed: " + user);
        });

        // Set click listeners for check and cross buttons
        ImageButton checkButton = requestView.findViewById(R.id.check_button);
        ImageButton crossButton = requestView.findViewById(R.id.cross_button);

        checkButton.setOnClickListener(v -> updateOrderStatus(order.getOrderID(), "Buying"));
        crossButton.setOnClickListener(v -> updateOrderStatus(order.getOrderID(), "Declined"));

        // Add the requestView to the container
        pasabuyerContainer.addView(requestView);
    }

    private void addCustomerOrderToContainer(Order order) {
        // Inflate the customer item layout
        View customerView = getLayoutInflater().inflate(R.layout.item_pasabuy_customer, pasabuyerContainer, false);

        // Find and set user details using buyerID
        managePostViewModel.fetchUserDetails(order.getBuyerID()).observe(this, user -> {
            TextView userName = customerView.findViewById(R.id.user_name_pasabuy_customer);
            TextView orderStatus = customerView.findViewById(R.id.order_status_pasabuy_customer);
            ImageView profileIcon = customerView.findViewById(R.id.profile_icon_pasabuy_customer);

            userName.setText(user.getFirstName() + " " + user.getLastName());
            orderStatus.setText("Status: " + order.getStatus());
            if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfilePictureURL())
                        .placeholder(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profileIcon);
            } else {
                Glide.with(this)
                        .load(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profileIcon);
            }
            Log.d("ManagePostActivity", "Customer order user details displayed: " + user);
        });

        // Set click listener for arrow button to navigate to ManageCustomerActivity
        ImageButton arrowButton = customerView.findViewById(R.id.arrow_button);
        arrowButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagePostActivity.this, ManageCustomerActivity.class);
            intent.putExtra("orderID", order.getOrderID());
            startActivity(intent);
        });

        // Add the customerView to the container
        pasabuyerContainer.addView(customerView);
    }

    private void updateOrderStatus(String orderID, String status) {
        managePostViewModel.updateOrderStatus(orderID, status).observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                // Refresh orders
                managePostViewModel.fetchOrders(getIntent().getStringExtra("postID"));
            } else {
                Log.e("ManagePostActivity", "Failed to update order status");
            }
        });
    }

    private boolean isRequestButtonSelected() {
        return isRequestSelected;
    }

    private void showRequests() {
        isRequestSelected = true;
        requestButton.setBackgroundResource(R.drawable.outline_top_bottom_background);
        requestButton.setTextColor(getResources().getColor(R.color.cyan_1));
        customerButton.setBackgroundResource(R.drawable.outline_top_bottom_gray_background);
        customerButton.setTextColor(getResources().getColor(R.color.cyan_3));
        populateOrders(managePostViewModel.getOrdersLiveData().getValue());
        Log.d("ManagePostActivity", "Showing requests");
    }

    private void showCustomers() {
        isRequestSelected = false;
        requestButton.setBackgroundResource(R.drawable.outline_top_bottom_gray_background);
        requestButton.setTextColor(getResources().getColor(R.color.cyan_3));
        customerButton.setBackgroundResource(R.drawable.outline_top_bottom_background);
        customerButton.setTextColor(getResources().getColor(R.color.cyan_1));
        populateOrders(managePostViewModel.getOrdersLiveData().getValue());
        Log.d("ManagePostActivity", "Showing customers");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_POST && resultCode == RESULT_OK) {
            // Refresh post details and orders after editing
            String postID = getIntent().getStringExtra("postID");
            managePostViewModel.fetchPostDetails(postID);
            managePostViewModel.fetchOrders(postID);
        }
    }
}
