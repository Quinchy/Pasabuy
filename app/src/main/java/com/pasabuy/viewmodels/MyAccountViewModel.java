package com.pasabuy.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pasabuy.models.Address;
import com.pasabuy.models.User;

public class MyAccountViewModel extends AndroidViewModel {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Address> addressLiveData = new MutableLiveData<>();

    public MyAccountViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Address> getAddressLiveData() {
        return addressLiveData;
    }

    public void loadUserData(String userID) {
        db.collection("users").document(userID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                userLiveData.setValue(user);
                loadAddressData(user.getAddressID());
            }
        });
    }

    private void loadAddressData(String addressID) {
        db.collection("addresses").document(addressID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Address address = documentSnapshot.toObject(Address.class);
                addressLiveData.setValue(address);
            }
        });
    }

    public void saveUserData(User user, Address address, Uri profilePictureUri) {
        if (profilePictureUri != null) {
            uploadProfilePicture(user.getUserID(), profilePictureUri, newProfilePictureUrl -> {
                user.setProfilePictureURL(newProfilePictureUrl);
                checkAndSaveUserAndAddress(user, address);
            });
        } else {
            checkAndSaveUserAndAddress(user, address);
        }
    }

    private void checkAndSaveUserAndAddress(User user, Address address) {
        DocumentReference userRef = db.collection("users").document(user.getUserID());
        DocumentReference addressRef = db.collection("addresses").document(address.getAddressID());

        db.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            if (!userSnapshot.exists()) {
                throw new RuntimeException("User document does not exist.");
            }

            DocumentSnapshot addressSnapshot = transaction.get(addressRef);
            if (addressSnapshot.exists()) {
                Address existingAddress = addressSnapshot.toObject(Address.class);
                if (!existingAddress.equals(address)) {
                    transaction.set(addressRef, address); // Update address only if it has changed
                }
            } else {
                transaction.set(addressRef, address); // Create new address if it doesn't exist
            }
            transaction.set(userRef, user); // Always update user document
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Transaction success
        }).addOnFailureListener(e -> {
            // Transaction failure
        });
    }

    private void uploadProfilePicture(String userID, Uri profilePictureUri, OnSuccessListener<String> onSuccessListener) {
        StorageReference profilePictureRef = storage.getReference().child("profile_pictures/" + userID + ".jpg");
        profilePictureRef.putFile(profilePictureUri).addOnSuccessListener(taskSnapshot ->
                profilePictureRef.getDownloadUrl().addOnSuccessListener(uri -> onSuccessListener.onSuccess(uri.toString()))
        );
    }

    public void deleteUserAccount(String userID) {
        // Delete profile picture from storage
        storage.getReference().child("profile_pictures/" + userID + ".jpg").delete();

        db.collection("users").document(userID).delete();
        db.collection("addresses").document(userID).delete();
        db.collection("posts").whereEqualTo("userID", userID).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                db.collection("posts").document(documentSnapshot.getId()).delete();
            }
        });
        db.collection("orders").whereEqualTo("buyerID", userID).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                db.collection("orders").document(documentSnapshot.getId()).delete();
            }
        });
    }

    public interface OnSuccessListener<T> {
        void onSuccess(T result);
    }
}
