package com.pasabuy.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.pasabuy.models.Post;

import java.util.ArrayList;
import java.util.List;

public class MyPostViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Post>> userPostsLiveData;
    private final FirebaseFirestore db;

    public MyPostViewModel(@NonNull Application application) {
        super(application);
        userPostsLiveData = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
        fetchUserPosts();
    }

    public LiveData<List<Post>> getUserPostsLiveData() {
        return userPostsLiveData;
    }

    private void fetchUserPosts() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        String currentUserID = sharedPreferences.getString("userID", "");

        db.collection("posts")
                .whereEqualTo("userID", currentUserID)
                .orderBy("timePosted", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> userPosts = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Post post = document.toObject(Post.class);
                        userPosts.add(post);
                    }
                    userPostsLiveData.setValue(userPosts);
                });
    }
    public void deletePostAndConnections(Post post, OnDeletePostListener listener) {
        String postID = post.getPostID();
        WriteBatch batch = db.batch();

        // Delete the post
        batch.delete(db.collection("posts").document(postID));

        // Delete related orders
        db.collection("orders").whereEqualTo("postID", postID).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        batch.delete(document.getReference());
                    }

                    // Delete related messages
                    db.collection("messages").whereEqualTo("postID", postID).get()
                            .addOnSuccessListener(messageSnapshots -> {
                                for (DocumentSnapshot document : messageSnapshots) {
                                    batch.delete(document.getReference());
                                }

                                // Delete related ratings
                                db.collection("ratings").whereEqualTo("postUserID", post.getUserID()).get()
                                        .addOnSuccessListener(ratingSnapshots -> {
                                            for (DocumentSnapshot document : ratingSnapshots) {
                                                batch.delete(document.getReference());
                                            }

                                            // Commit the batch
                                            batch.commit().addOnSuccessListener(aVoid -> {
                                                listener.onDeleteSuccess();
                                            }).addOnFailureListener(e -> {
                                                listener.onDeleteFailure(e);
                                            });
                                        });
                            });
                });
    }

    public interface OnDeletePostListener {
        void onDeleteSuccess();
        void onDeleteFailure(Exception e);
    }
}
