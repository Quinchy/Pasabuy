package com.pasabuy.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentReference;
import com.pasabuy.models.Address;
import com.pasabuy.models.User;

public class FirestoreUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static CollectionReference getUsersCollection() {
        return db.collection("users");
    }

    public static Query authenticateUser(String username, String password) {
        return db.collection("users")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password);
    }

    public interface FirestoreCallback<T> {
        void onCallback(T result);
    }

    public static void createUser(User user, FirestoreCallback<String> callback) {
        DocumentReference userDocRef = db.collection("users").document();
        user.setUserID(userDocRef.getId());
        userDocRef.set(user)
                .addOnSuccessListener(aVoid -> callback.onCallback(userDocRef.getId()))
                .addOnFailureListener(e -> callback.onCallback(null));
    }

    public static void createAddress(Address address, FirestoreCallback<String> callback) {
        DocumentReference addressDocRef = db.collection("addresses").document();
        address.setAddressID(addressDocRef.getId());
        addressDocRef.set(address)
                .addOnSuccessListener(aVoid -> callback.onCallback(addressDocRef.getId()))
                .addOnFailureListener(e -> callback.onCallback(null));
    }
}
