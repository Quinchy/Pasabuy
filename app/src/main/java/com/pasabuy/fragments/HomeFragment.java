package com.pasabuy.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.R;
import com.pasabuy.ViewPostActivity;
import com.pasabuy.models.Post;
import com.pasabuy.viewmodels.HomeViewModel;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private LinearLayout filterAll;
    private LinearLayout filterFoods;
    private LinearLayout filterItems;
    private LinearLayout pasabuyCardsContainer;
    private TextView currentLocationTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private HomeViewModel homeViewModel;
    private String selectedCategory = "All";
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        });

        speechRecognizerLauncher = registerForActivityResult(
                new StartActivityForResult(),
                this::handleSpeechResult
        );

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupFilterButtons();
        getCurrentLocation();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        String userProvince = sharedPreferences.getString("userProvince", "");

        homeViewModel.getPosts(userProvince, selectedCategory).observe(getViewLifecycleOwner(), this::updateUI);
        setupSearchFunctionality(view);
        return view;
    }

    private void setupSearchFunctionality(View view) {
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        ImageView searchIcon = view.findViewById(R.id.searchIcon);
        ImageView micIcon = view.findViewById(R.id.micIcon);

        searchIcon.setOnClickListener(v -> performSearch(searchEditText.getText().toString()));
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString());
                return true;
            }
            return false;
        });

        micIcon.setOnClickListener(v -> promptSpeechInput());
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Sorry, your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSpeechResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (speechResults != null && !speechResults.isEmpty()) {
                String recognizedText = speechResults.get(0);
                performSearch(recognizedText);
            }
        }
    }

    private void performSearch(String query) {
        if (!query.isEmpty()) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
            String userProvince = sharedPreferences.getString("userProvince", "");
            homeViewModel.searchPosts(query, userProvince, selectedCategory).observe(getViewLifecycleOwner(), this::updateUI);
        }
    }

    private void initializeViews(View view) {
        filterAll = view.findViewById(R.id.filterAll);
        filterFoods = view.findViewById(R.id.filterFoods);
        filterItems = view.findViewById(R.id.filterItems);
        pasabuyCardsContainer = view.findViewById(R.id.pasabuyCardsContainer);
        currentLocationTextView = view.findViewById(R.id.currentLocationTextView);
    }

    private void setupFilterButtons() {
        filterAll.setSelected(true);
        updateFilterButtonState(filterAll, R.drawable.all_icon_selected, R.color.white);
        updateFilterButtonState(filterFoods, R.drawable.foods_icon, R.color.cyan_2);
        updateFilterButtonState(filterItems, R.drawable.items_icon, R.color.cyan_2);

        View.OnClickListener filterClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterAll.setSelected(false);
                filterFoods.setSelected(false);
                filterItems.setSelected(false);
                updateFilterButtonState(filterAll, R.drawable.all_icon, R.color.cyan_2);
                updateFilterButtonState(filterFoods, R.drawable.foods_icon, R.color.cyan_2);
                updateFilterButtonState(filterItems, R.drawable.items_icon, R.color.cyan_2);

                v.setSelected(true);
                if (v.getId() == R.id.filterAll) {
                    updateFilterButtonState(filterAll, R.drawable.all_icon_selected, R.color.white);
                    selectedCategory = "All";
                } else if (v.getId() == R.id.filterFoods) {
                    updateFilterButtonState(filterFoods, R.drawable.foods_icon_selected, R.color.white);
                    selectedCategory = "Food";
                } else if (v.getId() == R.id.filterItems) {
                    updateFilterButtonState(filterItems, R.drawable.items_icon_selected, R.color.white);
                    selectedCategory = "Item";
                }

                // Update post list based on selected category
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
                String userProvince = sharedPreferences.getString("userProvince", "");
                homeViewModel.getPosts(userProvince, selectedCategory).observe(getViewLifecycleOwner(), HomeFragment.this::updateUI);
            }
        };

        filterAll.setOnClickListener(filterClickListener);
        filterFoods.setOnClickListener(filterClickListener);
        filterItems.setOnClickListener(filterClickListener);
    }

    private void updateFilterButtonState(LinearLayout filterButton, int iconResId, int textColorResId) {
        ImageView icon = null;
        TextView text = null;

        if (filterButton.getId() == R.id.filterAll) {
            icon = filterButton.findViewById(R.id.filterAllIcon);
            text = filterButton.findViewById(R.id.filterAllText);
        } else if (filterButton.getId() == R.id.filterFoods) {
            icon = filterButton.findViewById(R.id.filterFoodsIcon);
            text = filterButton.findViewById(R.id.filterFoodsText);
        } else if (filterButton.getId() == R.id.filterItems) {
            icon = filterButton.findViewById(R.id.filterItemsIcon);
            text = filterButton.findViewById(R.id.filterItemsText);
        }

        if (icon != null && text != null) {
            icon.setImageResource(iconResId);
            text.setTextColor(ContextCompat.getColor(getContext(), textColorResId));
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
                if (location != null) {
                    updateLocationTextView(location);
                } else {
                    Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateLocationTextView(Location location) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                currentLocationTextView.setText(addressText);

                // Update userProvince in shared preferences
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("userProvince", address.getSubAdminArea());
                editor.apply();

            } else {
                currentLocationTextView.setText("Location not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            currentLocationTextView.setText("Error finding location");
        }
    }

    private void updateUI(List<Post> posts) {
        pasabuyCardsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (posts.isEmpty()) {
            View emptyView = inflater.inflate(R.layout.empty_text, pasabuyCardsContainer, false);
            TextView emptyText = emptyView.findViewById(R.id.textName);

            String message;
            switch (selectedCategory) {
                case "Food":
                    message = "There are no food Pasabuy posts currently in your location.";
                    break;
                case "Item":
                    message = "There are no item Pasabuy posts currently in your location.";
                    break;
                default:
                    message = "There are no Pasabuy posts currently in your location.";
                    break;
            }
            emptyText.setText(message);
            pasabuyCardsContainer.addView(emptyView);
        } else {
            for (Post post : posts) {
                View cardView = inflater.inflate(R.layout.item_pasabuy_post, pasabuyCardsContainer, false);
                populateCardView(cardView, post);
                pasabuyCardsContainer.addView(cardView);
            }
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
        Picasso.get().load(post.getThumbnail()).into(postThumbnail);

        // Retrieve user details
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(post.getUserID()).get().addOnSuccessListener(documentSnapshot -> {
            String fullName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");
            double rating = documentSnapshot.getDouble("rating");
            String profilePictureURL = documentSnapshot.getString("profilePictureURL");

            userFullNameInPost.setText(fullName);
            userRatingsInPost.setText(String.valueOf(rating));
            if (profilePictureURL == null || profilePictureURL.isEmpty()) {
                Glide.with(this).load(R.drawable.placeholder_profile).circleCrop().into(profilePictureImage);
            } else {
                Glide.with(this).load(profilePictureURL).circleCrop().into(profilePictureImage);
            }
        });

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

        // Set click listener to open ViewPostActivity
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewPostActivity.class);
            intent.putExtra("postID", post.getPostID());
            startActivity(intent);
        });
    }
}
