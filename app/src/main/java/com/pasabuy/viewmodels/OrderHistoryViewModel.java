package com.pasabuy.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryViewModel extends AndroidViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();

    public OrderHistoryViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    public void fetchOrders(String userId) {
        db.collection("orders")
                .whereEqualTo("buyerID", userId)
                .whereIn("status", List.of("Declined", "Cancelled", "Delivered"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Order> orders = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Order order = document.toObject(Order.class);
                            orders.add(order);
                            // Log the order details
                            Log.d("OrderHistory", "Order ID: " + order.getOrderID() + ", Post ID: " + order.getPostID() + ", Status: " + order.getStatus());
                        }
                        ordersLiveData.setValue(orders);
                    } else {
                        ordersLiveData.setValue(null);
                    }
                });
    }

    public void fetchPostAndUserDetails(Order order, OnDetailsFetchedListener listener) {
        db.collection("posts").document(order.getPostID()).get()
                .addOnSuccessListener(postSnapshot -> {
                    Post post = postSnapshot.toObject(Post.class);
                    db.collection("users").document(post.getUserID()).get()
                            .addOnSuccessListener(userSnapshot -> {
                                User user = userSnapshot.toObject(User.class);
                                listener.onDetailsFetched(order, post, user);
                            });
                });
    }

    public interface OnDetailsFetchedListener {
        void onDetailsFetched(Order order, Post post, User user);
    }
}
