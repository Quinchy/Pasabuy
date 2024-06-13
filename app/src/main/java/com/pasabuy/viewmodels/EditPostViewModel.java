package com.pasabuy.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pasabuy.models.Image;
import com.pasabuy.models.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditPostViewModel extends AndroidViewModel {

    private static final String TAG = "EditPostViewModel";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    public MutableLiveData<Post> postLiveData = new MutableLiveData<>();
    public MutableLiveData<List<String>> imageUrlsLiveData = new MutableLiveData<>();
    public MutableLiveData<String> submissionMessage = new MutableLiveData<>();

    public EditPostViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public MutableLiveData<Post> getPostLiveData() {
        return postLiveData;
    }

    public MutableLiveData<List<String>> getImageUrlsLiveData() {
        return imageUrlsLiveData;
    }

    public void fetchPostDetails(String postID) {
        db.collection("posts").document(postID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Post post = documentSnapshot.toObject(Post.class);
                    postLiveData.postValue(post);
                    if (post != null) {
                        fetchImageUrls(post.getImageIDs());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch post details: " + e.getMessage()));
    }

    public void fetchImageUrls(List<String> imageIDs) {
        List<String> imageUrls = new ArrayList<>();
        imageUrlsLiveData.postValue(new ArrayList<>());  // Clear previous data
        for (String imageID : imageIDs) {
            db.collection("images").document(imageID).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String imageUrl = documentSnapshot.getString("imageURL");
                        if (imageUrl != null) {
                            imageUrls.add(imageUrl);
                            if (imageUrls.size() == imageIDs.size()) {
                                imageUrlsLiveData.postValue(new ArrayList<>(imageUrls));  // Create a new list to trigger observer
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch image URL: " + e.getMessage()));
        }
    }

    public void updatePost(Post post, List<Uri> newPhotoUris, List<String> originalImageUrls) {
        List<String> allImageUrls = new ArrayList<>(originalImageUrls);
        List<String> imageIDs = new ArrayList<>(post.getImageIDs());

        if (newPhotoUris.isEmpty()) {
            storeUpdatedPost(post);
        } else {
            for (Uri photoUri : newPhotoUris) {
                String fileName = "images/" + UUID.randomUUID().toString();
                StorageReference storageReference = storage.getReference().child(fileName);

                storageReference.putFile(photoUri)
                        .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            db.collection("images").add(new Image(UUID.randomUUID().toString(), downloadUrl))
                                    .addOnSuccessListener(documentReference -> {
                                        String imageID = documentReference.getId();
                                        imageIDs.add(imageID);
                                        allImageUrls.add(downloadUrl);
                                        if (allImageUrls.size() == newPhotoUris.size() + originalImageUrls.size()) {
                                            post.setImageIDs(imageIDs);
                                            post.setThumbnail(allImageUrls.get(0));
                                            storeUpdatedPost(post);
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
    }

    private void storeUpdatedPost(Post post) {
        db.collection("posts").document(post.getPostID()).set(post)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post updated successfully");
                    submissionMessage.postValue("Post updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update post: " + e.getMessage());
                    submissionMessage.postValue("Failed to update post: " + e.getMessage());
                });
    }
}
