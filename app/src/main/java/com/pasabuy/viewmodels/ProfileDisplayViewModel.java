package com.pasabuy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

import java.util.ArrayList;
import java.util.List;

public class ProfileDisplayViewModel extends AndroidViewModel {
    private final MutableLiveData<User> user;
    private final MutableLiveData<List<Post>> posts;
    private final FirebaseFirestore db;

    public ProfileDisplayViewModel(@NonNull Application application) {
        super(application);
        user = new MutableLiveData<>();
        posts = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<User> getUser(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            User userData = documentSnapshot.toObject(User.class);
            user.setValue(userData);
        });
        return user;
    }

    public LiveData<List<Post>> getPosts(String userId, String category) {
        db.collection("posts")
                .whereEqualTo("userID", userId)
                .whereEqualTo("status", "Open")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Post> postList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            if (category.equals("All") || post.getCategory().equals(category)) {
                                postList.add(post);
                            }
                        }
                        posts.setValue(postList);
                    }
                });
        return posts;
    }
}
