package com.pasabuy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.User;

public class ProfileViewModel extends AndroidViewModel {

    private final FirebaseFirestore db;
    private final MutableLiveData<User> userLiveData;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        userLiveData = new MutableLiveData<>();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public void fetchUserData(String userID) {
        db.collection("users").document(userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String userId = document.getString("userID");
                    String firstName = document.getString("firstName");
                    String lastName = document.getString("lastName");
                    String username = document.getString("username");
                    String password = document.getString("password");
                    String addressID = document.getString("addressID");
                    String profilePictureURL = document.getString("profilePictureURL");
                    double rating = document.getDouble("rating");
                    int ratingCount = document.getLong("ratingCount").intValue();

                    User user = new User(userId, firstName, lastName, username, password, addressID, profilePictureURL, rating, ratingCount);
                    userLiveData.postValue(user);
                } else {
                    // Handle document not found
                }
            } else {
                // Handle task failure
            }
        });
    }
}
