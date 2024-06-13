package com.pasabuy.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Address;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

public class ManageCustomerViewModel extends AndroidViewModel {

    private static final String TAG = "ManageCustomerViewModel";

    private FirebaseFirestore db;
    private MutableLiveData<Order> orderLiveData = new MutableLiveData<>();
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private MutableLiveData<Post> postLiveData = new MutableLiveData<>();
    private MutableLiveData<Address> addressLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> updateStatusLiveData = new MutableLiveData<>();

    public ManageCustomerViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public MutableLiveData<Order> getOrderLiveData() {
        return orderLiveData;
    }

    public MutableLiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public MutableLiveData<Post> getPostLiveData() {
        return postLiveData;
    }

    public MutableLiveData<Address> getAddressLiveData() {
        return addressLiveData;
    }

    public void fetchOrderDetails(String orderID) {
        db.collection("orders").document(orderID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Order order = documentSnapshot.toObject(Order.class);
                    orderLiveData.postValue(order);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch order details: " + e.getMessage()));
    }

    public void fetchUserDetails(String userID) {
        db.collection("users").document(userID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    userLiveData.postValue(user);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user details: " + e.getMessage()));
    }

    public void fetchPostDetails(String postID) {
        db.collection("posts").document(postID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Post post = documentSnapshot.toObject(Post.class);
                    postLiveData.postValue(post);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch post details: " + e.getMessage()));
    }

    public void fetchAddressDetails(String addressID) {
        db.collection("addresses").document(addressID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Address address = documentSnapshot.toObject(Address.class);
                    addressLiveData.postValue(address);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch address details: " + e.getMessage()));
    }

    public MutableLiveData<Boolean> updateOrderStatus(String orderID, String newStatus) {
        db.collection("orders").document(orderID).update("status", newStatus)
                .addOnSuccessListener(aVoid -> updateStatusLiveData.postValue(true))
                .addOnFailureListener(e -> updateStatusLiveData.postValue(false));
        return updateStatusLiveData;
    }
}
