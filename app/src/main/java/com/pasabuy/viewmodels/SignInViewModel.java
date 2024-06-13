package com.pasabuy.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.utils.FirestoreUtils;

public class SignInViewModel extends ViewModel {
    private final MutableLiveData<Boolean> loginSuccessful = new MutableLiveData<>();
    private final MutableLiveData<String> loginError = new MutableLiveData<>();
    private final MutableLiveData<String> userId = new MutableLiveData<>();

    public LiveData<Boolean> isLoginSuccessful() {
        return loginSuccessful;
    }

    public LiveData<String> getLoginError() {
        return loginError;
    }

    public LiveData<String> getUserId() {
        return userId;
    }

    public void signIn(String username, String password) {
        Query query = FirestoreUtils.authenticateUser(username, password);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userId.postValue(document.getId());
                }
                loginSuccessful.postValue(true);
            } else {
                handleSignInFailure(task.getResult() != null && task.getResult().isEmpty());
            }
        });
    }

    private void handleSignInFailure(boolean isEmptyResult) {
        if (isEmptyResult) {
            loginError.postValue("Invalid username or password");
        } else {
            loginError.postValue("Error signing in. Please try again.");
        }
    }
}
