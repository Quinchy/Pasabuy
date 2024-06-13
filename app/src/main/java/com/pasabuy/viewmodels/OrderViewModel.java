package com.pasabuy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pasabuy.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private FirebaseFirestore db;

    public OrderViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Order>> getOrdersByStatus(String userID, String status) {
        MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
        db.collection("orders")
                .whereEqualTo("buyerID", userID)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> orders = new ArrayList<>();
                    queryDocumentSnapshots.forEach(documentSnapshot -> {
                        Order order = documentSnapshot.toObject(Order.class);
                        orders.add(order);
                    });
                    ordersLiveData.setValue(orders);
                });
        return ordersLiveData;
    }
}
