package com.pasabuy.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Rating;

import java.util.ArrayList;
import java.util.List;

public class MyRatingsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Rating>> ratings;
    private final FirebaseFirestore db;

    public MyRatingsViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        ratings = new MutableLiveData<>();
    }

    public LiveData<List<Rating>> getRatings(String userId) {
        db.collection("ratings")
                .whereEqualTo("ratedByUserID", userId)
                .whereGreaterThan("rating", 0)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Rating> ratingList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Rating rating = document.toObject(Rating.class);
                            ratingList.add(rating);
                        }
                        ratings.setValue(ratingList);
                    } else {
                        // Handle errors
                    }
                });
        return ratings;
    }
}
