package com.pasabuy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

public class ViewOrderViewModel extends AndroidViewModel {
    private final FirebaseFirestore db;

    public ViewOrderViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<Order> getOrderDetails(String orderID) {
        MutableLiveData<Order> orderLiveData = new MutableLiveData<>();
        db.collection("orders").document(orderID).get().addOnSuccessListener(orderDocument -> {
            if (orderDocument.exists()) {
                Order order = orderDocument.toObject(Order.class);
                orderLiveData.setValue(order);
            }
        });
        return orderLiveData;
    }

    public LiveData<User> getUserDetails(String userID) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        db.collection("users").document(userID).get().addOnSuccessListener(userDocument -> {
            if (userDocument.exists()) {
                User user = userDocument.toObject(User.class);
                userLiveData.setValue(user);
            }
        });
        return userLiveData;
    }

    public LiveData<Post> getPostDetails(String postID) {
        MutableLiveData<Post> postLiveData = new MutableLiveData<>();
        db.collection("posts").document(postID).get().addOnSuccessListener(postDocument -> {
            if (postDocument.exists()) {
                Post post = postDocument.toObject(Post.class);
                postLiveData.setValue(post);
            }
        });
        return postLiveData;
    }

    public LiveData<Boolean> updateOrderStatus(String orderID, String newStatus) {
        MutableLiveData<Boolean> updateStatusLiveData = new MutableLiveData<>();
        db.collection("orders").document(orderID).update("status", newStatus)
                .addOnSuccessListener(aVoid -> updateStatusLiveData.setValue(true))
                .addOnFailureListener(e -> updateStatusLiveData.setValue(false));
        return updateStatusLiveData;
    }
}
