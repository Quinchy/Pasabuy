package com.pasabuy.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

import java.util.ArrayList;
import java.util.List;

public class ManagePostViewModel extends AndroidViewModel {
    private final MutableLiveData<Post> postLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> imageUrlsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db;

    public ManagePostViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<Post> getPostLiveData() {
        return postLiveData;
    }

    public LiveData<List<String>> getImageUrlsLiveData() {
        return imageUrlsLiveData;
    }

    public LiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    public LiveData<User> fetchUserDetails(String userID) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        db.collection("users").document(userID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                userLiveData.setValue(user);
                Log.d("ManagePostViewModel", "User details retrieved: " + user);
            }
        }).addOnFailureListener(e -> {
            Log.e("ManagePostViewModel", "Error fetching user details", e);
        });
        return userLiveData;
    }

    public void fetchPostDetails(String postID) {
        db.collection("posts").document(postID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Post post = documentSnapshot.toObject(Post.class);
                postLiveData.setValue(post);
                fetchImageUrls(post.getImageIDs());
                Log.d("ManagePostViewModel", "Post details retrieved: " + post);
            }
        }).addOnFailureListener(e -> {
            Log.e("ManagePostViewModel", "Error fetching post details", e);
        });
    }

    public void fetchOrders(String postID) {
        db.collection("orders").whereEqualTo("postID", postID).get().addOnSuccessListener(querySnapshot -> {
            List<Order> orders = new ArrayList<>();
            querySnapshot.forEach(documentSnapshot -> {
                Order order = documentSnapshot.toObject(Order.class);
                orders.add(order);
            });
            ordersLiveData.setValue(orders);
            Log.d("ManagePostViewModel", "Orders retrieved: " + orders);
        }).addOnFailureListener(e -> {
            Log.e("ManagePostViewModel", "Error fetching orders", e);
        });
    }

    public LiveData<Boolean> updateOrderStatus(String orderID, String status) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        db.collection("orders").document(orderID).update("status", status).addOnSuccessListener(aVoid -> {
            resultLiveData.setValue(true);
            Log.d("ManagePostViewModel", "Order status updated to: " + status);
        }).addOnFailureListener(e -> {
            resultLiveData.setValue(false);
            Log.e("ManagePostViewModel", "Error updating order status", e);
        });
        return resultLiveData;
    }

    private void fetchImageUrls(List<String> imageIDs) {
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
                        Log.d("ManagePostViewModel", "Image URLs retrieved: " + imageUrls);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e("ManagePostViewModel", "Error fetching image URLs", e);
            });
        }
    }
}
