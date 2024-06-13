package com.pasabuy.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pasabuy.R;
import com.pasabuy.SignInActivity;
import com.pasabuy.MyPostActivity;
import com.pasabuy.MyAccountActivity;
import com.pasabuy.MyRatingsActivity;
import com.pasabuy.OrderHistoryActivity;
import com.pasabuy.HelpAndSupportActivity;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private TextView userFullNameTextView;
    private TextView usernameTextView;
    private ImageView profilePictureImageView;
    private SharedPreferences sharedPreferences;
    private ProfileViewModel profileViewModel;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userFullNameTextView = view.findViewById(R.id.userFullName);
        usernameTextView = view.findViewById(R.id.username);
        profilePictureImageView = view.findViewById(R.id.profilePicture);

        LinearLayout signOutButton = view.findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        // Add click listeners for the specified containers
        LinearLayout myPostSettingContainer = view.findViewById(R.id.myPostSettingContainer);
        myPostSettingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(MyPostActivity.class);
            }
        });

        LinearLayout myAccountSettingContainer = view.findViewById(R.id.myAccountSettingContainer);
        myAccountSettingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(MyAccountActivity.class);
            }
        });

        LinearLayout myRatingsSettingContainer = view.findViewById(R.id.myRatingsSettingContainer);
        myRatingsSettingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(MyRatingsActivity.class);
            }
        });

        LinearLayout orderHistorySettingContainer = view.findViewById(R.id.orderHistorySettingContainer);
        orderHistorySettingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(OrderHistoryActivity.class);
            }
        });

        LinearLayout helpSupportSettingContainer = view.findViewById(R.id.helpSupportSettingContainer);
        helpSupportSettingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(HelpAndSupportActivity.class);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        refreshUserData();
    }

    public void refreshUserData() {
        String userID = sharedPreferences.getString("userID", null);
        if (userID != null) {
            profileViewModel.fetchUserData(userID);
            profileViewModel.getUserLiveData().observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    userFullNameTextView.setText(user.getFirstName() + " " + user.getLastName());
                    usernameTextView.setText("@" + user.getUsername());
                    loadProfilePicture(user.getProfilePictureURL());
                }
            });
        } else {
            // Handle userID not found in shared preferences
        }
    }

    private void loadProfilePicture(String profilePictureURL) {
        if (profilePictureURL == null || profilePictureURL.isEmpty()) {
            Glide.with(this)
                    .load(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(profilePictureImageView);
        } else {
            Glide.with(this)
                    .load(profilePictureURL)
                    .placeholder(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(profilePictureImageView);
        }
    }

    private void signOut() {
        // Update shared preferences to indicate the user is logged out and clear user-specific data
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("userID");  // Remove the user ID
        // Add any other user-specific data removal here
        editor.apply();

        // Redirect to SignInActivity
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(getActivity(), activityClass);
        startActivity(intent);
    }
}
