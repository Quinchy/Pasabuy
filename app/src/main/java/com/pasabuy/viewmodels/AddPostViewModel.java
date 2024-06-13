package com.pasabuy.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pasabuy.models.Image;
import com.pasabuy.models.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddPostViewModel extends AndroidViewModel {

    private static final String TAG = "AddPostViewModel";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    public MutableLiveData<String> submissionMessage = new MutableLiveData<>();

    public AddPostViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public void submitPost(String userId, String userProvince, String title, String description, double price, Timestamp deadline, String category, List<Uri> photoUris) {
        if (photoUris.isEmpty()) {
            submissionMessage.postValue("Please upload at least one photo.");
            return;
        }
        uploadPhotosAndStorePost(userId, userProvince, title, description, price, deadline, category, photoUris);
    }

    private void uploadPhotosAndStorePost(String userId, String userProvince, String title, String description, double price, Timestamp deadline, String category, List<Uri> photoUris) {
        List<String> imageIDs = new ArrayList<>();
        List<String> downloadUrls = new ArrayList<>();

        for (int i = 0; i < photoUris.size(); i++) {
            Uri photoUri = photoUris.get(i);
            String fileName = "images/" + UUID.randomUUID().toString();
            StorageReference storageReference = storage.getReference().child(fileName);

            Log.d(TAG, "Uploading image to: " + fileName);
            Log.d(TAG, "Image URI: " + photoUri.toString());

            int finalI = i;
            storageReference.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "Image uploaded, URL: " + downloadUrl);

                        DocumentReference newImageRef = db.collection("images").document();
                        String imageID = newImageRef.getId();
                        Image image = new Image(imageID, downloadUrl);
                        newImageRef.set(image)
                                .addOnSuccessListener(documentReference -> {
                                    imageIDs.add(imageID);
                                    downloadUrls.add(downloadUrl);
                                    if (imageIDs.size() == photoUris.size()) {
                                        String thumbnail = downloadUrls.get(0); // Use the first image as the thumbnail
                                        storePost(userId, userProvince, title, description, price, deadline, category, imageIDs, downloadUrls, thumbnail);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to upload image data: " + e.getMessage());
                                    submissionMessage.postValue("Failed to upload image data: " + e.getMessage());
                                });
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image: " + e.getMessage());
                        submissionMessage.postValue("Failed to upload image: " + e.getMessage());
                    });
        }
    }

    private void storePost(String userId, String userProvince, String title, String description, double price, Timestamp deadline, String category, List<String> imageIDs, List<String> downloadUrls, String thumbnail) {
        DocumentReference newPostRef = db.collection("posts").document();
        String postId = newPostRef.getId();

        List<String> joinedUserIDs = new ArrayList<>();
        Post post = new Post(postId, userId, userProvince, title, description, price, deadline, Timestamp.now(), category, imageIDs, joinedUserIDs, thumbnail, "Open");

        newPostRef.set(post)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post submitted successfully");
                    submissionMessage.postValue("Post submitted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit post: " + e.getMessage());
                    submissionMessage.postValue("Failed to submit post: " + e.getMessage());
                });
    }
}
