package com.pasabuy.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Address;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

import java.util.ArrayList;
import java.util.List;

public class ViewPostViewModel extends AndroidViewModel {
    private final MutableLiveData<Post> postLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> imageUrlsLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db;

    public ViewPostViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<Post> getPostLiveData() {
        return postLiveData;
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<List<String>> getImageUrlsLiveData() {
        return imageUrlsLiveData;
    }

    public void fetchPostDetails(String postID) {
        db.collection("posts").document(postID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Post post = documentSnapshot.toObject(Post.class);
                postLiveData.setValue(post);
                fetchImageUrls(post.getImageIDs());
            }
        }).addOnFailureListener(e -> {
            // Handle error
            Log.e("ViewPostViewModel", "Error fetching post details", e);
        });
    }

    public void fetchUserDetails(String userID) {
        db.collection("users").document(userID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                userLiveData.setValue(user);
            }
        }).addOnFailureListener(e -> {
            // Handle error
            Log.e("ViewPostViewModel", "Error fetching user details", e);
        });
    }

    public void fetchImageUrls(List<String> imageIDs) {
        if (imageIDs == null || imageIDs.isEmpty()) {
            imageUrlsLiveData.setValue(new ArrayList<>());
            return;
        }

        List<String> imageUrls = new ArrayList<>();
        for (String imageID : imageIDs) {
            db.collection("images").document(imageID).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageURL = documentSnapshot.getString("imageURL");
                    if (imageURL != null) {
                        imageUrls.add(imageURL);
                    }
                    if (imageUrls.size() == imageIDs.size()) {
                        imageUrlsLiveData.setValue(imageUrls);
                    }
                }
            }).addOnFailureListener(e -> {
                // Handle error
                Log.e("ViewPostViewModel", "Error fetching image URLs", e);
            });
        }
    }
}
