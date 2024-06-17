package com.pasabuy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.adapters.ImageAdapter;
import com.pasabuy.models.Address;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.ViewPostViewModel;

import java.util.HashMap;
import java.util.Map;

public class ViewPostActivity extends AppCompatActivity {

    private View joinPasabuyPopup;
    private ConstraintLayout parentLayout;
    private View darkBackground;
    private ViewPostViewModel viewPostViewModel;
    private ViewPager viewPager;
    private EditText editAddressEditText;
    private TextView totalPriceTextView;
    private EditText productQuantityEditText;
    private String currentUserID;
    private String currentUserProvince;
    private String deliveryAddress;
    private double postPrice;
    private Post currentPost;
    private boolean isToastShown = false; // Field to track if Toast has been shown

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme); // Explicitly set the theme
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_post);

        try {
            // Initialize ViewModel
            viewPostViewModel = new ViewModelProvider(this).get(ViewPostViewModel.class);

            // Get postID from Intent
            Intent intent = getIntent();
            String postID = intent.getStringExtra("postID");

            // Retrieve shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
            currentUserID = sharedPreferences.getString("userID", "");
            currentUserProvince = sharedPreferences.getString("userProvince", "");
            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

            // Setup UI
            setupUI();

            // Fetch post details
            viewPostViewModel.fetchPostDetails(postID);
            viewPostViewModel.getPostLiveData().observe(this, post -> {
                if (post != null) {
                    currentPost = post;
                    // Populate post details
                    populatePostDetails(post);
                    // Fetch user details (post creator)
                    viewPostViewModel.fetchUserDetails(post.getUserID());
                }
            });

            viewPostViewModel.getUserLiveData().observe(this, user -> {
                if (user != null) {
                    // Populate user details
                    populateUserDetails(user);
                }
            });

            viewPostViewModel.getImageUrlsLiveData().observe(this, imageUrls -> {
                if (imageUrls != null) {
                    // Set the adapter for ViewPager
                    ImageAdapter adapter = new ImageAdapter(this, imageUrls);
                    viewPager.setAdapter(adapter);
                }
            });

            // Fetch current user's address
            fetchCurrentUserAddress();
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in onCreate: ", e);
        }
    }

    private void setupUI() {
        try {
            ImageButton backButtonAddPost = findViewById(R.id.backButtonViewPost);
            backButtonAddPost.setOnClickListener(v -> finish());

            ImageButton clickToMessageViewPostButton = findViewById(R.id.clickToMessageViewPostButton);
            clickToMessageViewPostButton.setOnClickListener(v -> {
                if (currentPost != null) {
                    Intent intent = new Intent(ViewPostActivity.this, ViewMessageActivity.class);
                    intent.putExtra("userID", currentPost.getUserID());
                    startActivity(intent);
                }
            });

            // Initialize ViewPager and TabLayout
            viewPager = findViewById(R.id.viewPager);
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(viewPager, true);

            joinPasabuyPopup = findViewById(R.id.join_pasabuy_popup);
            Button joinButton = findViewById(R.id.joinPasabuybutton);
            parentLayout = findViewById(R.id.parentLayout);

            // Initialize dark background
            darkBackground = new View(this);
            darkBackground.setLayoutParams(new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
            ));
            darkBackground.setBackgroundColor(0xB3000000); // Semi-transparent dark color
            darkBackground.setVisibility(View.GONE);

            // Add dark background to parentLayout before the popup
            parentLayout.addView(darkBackground, parentLayout.indexOfChild(joinPasabuyPopup));

            joinButton.setOnClickListener(v -> {
                if (currentPost != null && currentUserID.equals(currentPost.getUserID())) {
                    if (!isToastShown) {
                        Toast.makeText(this, "You cannot join your own post.", Toast.LENGTH_SHORT).show();
                        isToastShown = true;
                    }
                } else {
                    showJoinPopup();
                }
            });

            // Set touch listener on the dark background to detect taps outside the popup
            darkBackground.setOnTouchListener((v, event) -> {
                float x = event.getX();
                float y = event.getY();
                if (event.getAction() == MotionEvent.ACTION_DOWN &&
                        (x < joinPasabuyPopup.getLeft() || x > joinPasabuyPopup.getRight() ||
                                y < joinPasabuyPopup.getTop() || y > joinPasabuyPopup.getBottom())) {
                    hideJoinPopup();
                    return true;
                }
                return false;
            });

            editAddressEditText = findViewById(R.id.editAddressEditText);
            TextView editAddressButton = findViewById(R.id.editAddressButton);
            editAddressButton.setOnClickListener(v -> editAddressEditText.setEnabled(true));

            Button minusQuantityButton = findViewById(R.id.minusQuantityButton);
            Button addQuantityButton = findViewById(R.id.addQuantityButton);
            productQuantityEditText = findViewById(R.id.productQuantityEditText);
            totalPriceTextView = findViewById(R.id.totalPriceTextView);

            minusQuantityButton.setOnClickListener(v -> {
                int quantity = Integer.parseInt(productQuantityEditText.getText().toString());
                if (quantity > 1) {
                    quantity--;
                    productQuantityEditText.setText(String.valueOf(quantity));
                    updateTotalPrice(quantity);
                }
            });

            addQuantityButton.setOnClickListener(v -> {
                int quantity = Integer.parseInt(productQuantityEditText.getText().toString());
                quantity++;
                productQuantityEditText.setText(String.valueOf(quantity));
                updateTotalPrice(quantity);
            });

            Button proceedButton = findViewById(R.id.proceedButton);
            proceedButton.setOnClickListener(v -> {
                // Prepare functionality for proceed button
                createOrder();
            });

        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in setupUI: ", e);
        }
    }

    private void fetchCurrentUserAddress() {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUserID).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String addressID = documentSnapshot.getString("addressID");
                    if (addressID != null) {
                        fetchUserAddress(addressID); // Fetch address using addressID
                    }
                }
            }).addOnFailureListener(e -> Log.e("ViewPostActivity", "Error fetching current user address: ", e));
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in fetchCurrentUserAddress: ", e);
        }
    }

    private void fetchUserAddress(String addressID) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("addresses").document(addressID).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Address address = documentSnapshot.toObject(Address.class);
                    if (address != null) {
                        deliveryAddress = address.getHouseStreet() + ", " +
                                address.getBarangay() + ", " +
                                address.getMunicipality() + ", " +
                                address.getProvince();
                    }
                }
            }).addOnFailureListener(e -> Log.e("ViewPostActivity", "Error fetching user address: ", e));
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in fetchUserAddress: ", e);
        }
    }

    private void showJoinPopup() {
        try {
            if (joinPasabuyPopup.getVisibility() == View.GONE) {
                Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                joinPasabuyPopup.startAnimation(slideUp);
                joinPasabuyPopup.setVisibility(View.VISIBLE);
                darkBackground.setVisibility(View.VISIBLE); // Show dark background

                // Set default values
                editAddressEditText.setText(deliveryAddress); // Set the current user's delivery address
                productQuantityEditText.setText("1");
                updateTotalPrice(1);
            }
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in showJoinPopup: ", e);
        }
    }

    private void hideJoinPopup() {
        try {
            if (joinPasabuyPopup.getVisibility() == View.VISIBLE) {
                Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
                joinPasabuyPopup.startAnimation(slideDown);
                joinPasabuyPopup.setVisibility(View.GONE);
                darkBackground.setVisibility(View.GONE); // Hide dark background
            }
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in hideJoinPopup: ", e);
        }
    }

    private void populatePostDetails(Post post) {
        try {
            TextView productTitle = findViewById(R.id.productTitle);
            TextView productPrice = findViewById(R.id.productPrice);
            TextView productDescription = findViewById(R.id.productDescription);
            TextView deadlineCounterViewPost = findViewById(R.id.deadlineCounterViewPost);
            TextView noOfPeopleJoined = findViewById(R.id.noOfPeopleJoined);

            productTitle.setText(post.getTitle());
            productPrice.setText("₱" + post.getPrice());
            productDescription.setText(post.getDescription());

            postPrice = post.getPrice();

            // Update deadline counter
            long currentTime = System.currentTimeMillis();
            long deadlineTime = post.getDeadline().toDate().getTime();
            long timeDiff = deadlineTime - currentTime;

            if (timeDiff <= 0) {
                deadlineCounterViewPost.setText("Ended");
            } else {
                long diffInMinutes = timeDiff / (1000 * 60);
                long diffInHours = timeDiff / (1000 * 60 * 60);
                long diffInDays = timeDiff / (1000 * 60 * 60 * 24);

                if (diffInDays > 0) {
                    deadlineCounterViewPost.setText("Ends in " + diffInDays + (diffInDays == 1 ? " day" : " days"));
                } else if (diffInHours > 0) {
                    deadlineCounterViewPost.setText("Ends in " + diffInHours + (diffInHours == 1 ? " hour" : " hours"));
                } else {
                    deadlineCounterViewPost.setText("Ends in " + diffInMinutes + (diffInMinutes == 1 ? " minute" : " minutes"));
                }
            }

            // Update joined users count
            noOfPeopleJoined.setText(String.valueOf(post.getJoinedUserIDs().size()));

            // Fetch image URLs
            viewPostViewModel.fetchImageUrls(post.getImageIDs());
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in populatePostDetails: ", e);
        }
    }

    private void populateUserDetails(User user) {
        try {
            TextView userFullNameViewPost = findViewById(R.id.userFullNameViewPost);
            TextView userRatingsViewPost = findViewById(R.id.userRatingsViewPost);
            ImageView profilePictureImageViewPost = findViewById(R.id.profilePictureImageViewPost);

            userFullNameViewPost.setText(user.getFirstName() + " " + user.getLastName());
            userRatingsViewPost.setText(String.valueOf(user.getRating()));

            String profilePictureURL = user.getProfilePictureURL();
            if (profilePictureURL == null || profilePictureURL.isEmpty()) {
                Glide.with(this)
                        .load(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profilePictureImageViewPost);
            } else {
                Glide.with(this)
                        .load(profilePictureURL)
                        .placeholder(R.drawable.placeholder_profile)
                        .circleCrop()
                        .into(profilePictureImageViewPost);
            }

            // Set OnClickListener to navigate to ProfileDisplayActivity
            profilePictureImageViewPost.setOnClickListener(v -> {
                Intent intent = new Intent(ViewPostActivity.this, ProfileDisplayActivity.class);
                intent.putExtra("userID", user.getUserID());
                startActivity(intent);
            });
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in populateUserDetails: ", e);
        }
    }

    private void updateTotalPrice(int quantity) {
        try {
            double totalPrice = quantity * postPrice;
            totalPriceTextView.setText("₱" + totalPrice);
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in updateTotalPrice: ", e);
        }
    }

    private void createOrder() {
        try {
            String postID = getIntent().getStringExtra("postID");
            String status = "Pending";
            String paymentMethod = "Cash on Delivery";
            String deliveryAddress = editAddressEditText.getText().toString();
            int quantity = Integer.parseInt(productQuantityEditText.getText().toString());
            double totalPrice = quantity * postPrice;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("postID", postID);
            orderData.put("buyerID", currentUserID);
            orderData.put("status", status);
            orderData.put("paymentMethod", paymentMethod);
            orderData.put("deliveryAddress", deliveryAddress);
            orderData.put("quantity", quantity);
            orderData.put("totalPrice", totalPrice);

            db.collection("orders").add(orderData).addOnSuccessListener(documentReference -> {
                String orderID = documentReference.getId();
                documentReference.update("orderID", orderID);

                // Update the post's joinedUserIDs and show success popup
                updatePostJoinedUserIDs(postID, () -> {
                    showSuccessPopup();
                    hideJoinPopup(); // Hide the join popup
                });
            }).addOnFailureListener(e -> Log.e("ViewPostActivity", "Error creating order: ", e));
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in createOrder: ", e);
        }
    }

    private void updatePostJoinedUserIDs(String postID, Runnable onSuccess) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("posts").document(postID).update("joinedUserIDs", FieldValue.arrayUnion(currentUserID))
                    .addOnSuccessListener(aVoid -> {
                        // Successfully updated joinedUserIDs
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ViewPostActivity", "Error updating joinedUserIDs: ", e));
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in updatePostJoinedUserIDs: ", e);
        }
    }

    private void showSuccessPopup() {
        try {
            // Show the success popup
            View successPopup = findViewById(R.id.join_pasabuy_success_popup);
            successPopup.setVisibility(View.VISIBLE);

            // Disable scrolling
            ScrollView scrollView = findViewById(R.id.scrollView);
            scrollView.setOnTouchListener((v, event) -> true);

            // Disable back button
            successPopup.setFocusableInTouchMode(true);
            successPopup.requestFocus();
            successPopup.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            });

            // Set up the go home button click listener
            Button goHomeButton = successPopup.findViewById(R.id.go_home_button);
            goHomeButton.setOnClickListener(v -> {
                Intent intent = new Intent(ViewPostActivity.this, PasabuyAppActivity.class);
                startActivity(intent);
                finish();
            });

            // Disable the join button
            Button joinButton = findViewById(R.id.joinPasabuybutton);
            joinButton.setEnabled(false);
        } catch (Exception e) {
            Log.e("ViewPostActivity", "Error in showSuccessPopup: ", e);
        }
    }

}
