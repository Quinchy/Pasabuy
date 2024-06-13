package com.pasabuy.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Post;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<List<Post>> posts;
    private final FirebaseFirestore db;

    public HomeViewModel() {
        posts = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Post>> getPosts(String userProvince, String category) {
        loadPosts(userProvince, category);
        return posts;
    }

    public LiveData<List<Post>> searchPosts(String searchString, String userProvince, String category) {
        Query query = db.collection("posts")
                .whereEqualTo("userProvince", userProvince)
                .whereEqualTo("status", "Open");

        if (!category.equals("All")) {
            query = query.whereEqualTo("category", category);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Post> filteredPosts = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Post post = document.toObject(Post.class);
                    if (isPostValid(post) && post.getTitle().toLowerCase().contains(searchString.toLowerCase())) {
                        filteredPosts.add(post);
                    }
                }
                posts.setValue(filteredPosts);
            }
        });
        return posts;
    }

    private void loadPosts(String userProvince, String category) {
        Query query = db.collection("posts")
                .whereEqualTo("userProvince", userProvince)
                .whereEqualTo("status", "Open");

        if (!category.equals("All")) {
            query = query.whereEqualTo("category", category);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Post> postList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Post post = document.toObject(Post.class);
                    if (isPostValid(post)) {
                        postList.add(post);
                    }
                }
                posts.setValue(postList);
            }
        });
    }

    private boolean isPostValid(Post post) {
        long currentTime = System.currentTimeMillis();
        long deadlineTime = post.getDeadline().toDate().getTime();
        return deadlineTime > currentTime;
    }
}
