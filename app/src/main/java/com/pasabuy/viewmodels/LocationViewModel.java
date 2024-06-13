package com.pasabuy.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;
import com.pasabuy.models.User;
import com.pasabuy.utils.FirestoreUtils;

import java.io.IOException;
import java.util.List;

public class LocationViewModel extends AndroidViewModel {

    public MutableLiveData<String> userRegistrationMessage = new MutableLiveData<>();
    public MutableLiveData<Boolean> isRegistrationComplete = new MutableLiveData<>();
    public MutableLiveData<String> currentLocationText = new MutableLiveData<>();
    public MutableLiveData<LatLng> currentLatLng = new MutableLiveData<>();

    public LocationViewModel(@NonNull Application application) {
        super(application);
    }

    public void geocodeLocation(LatLng latLng, Geocoder geocoder) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                currentLocationText.postValue(addressText);
                currentLatLng.postValue(latLng);
            } else {
                currentLocationText.postValue("Location not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            currentLocationText.postValue("Error finding location");
        }
    }

    public void registerUserWithLocation(String firstName, String lastName, String username, String password,
                                         String houseStreet, String barangay, String municipality, String province) {
        FirestoreUtils.getUsersCollection()
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        userRegistrationMessage.postValue("Username already taken.");
                    } else {
                        com.pasabuy.models.Address address = new com.pasabuy.models.Address(null, houseStreet, barangay, municipality, province);
                        FirestoreUtils.createAddress(address, addressId -> {
                            if (addressId != null) {
                                address.setAddressID(addressId); // Set the address ID once it is generated
                                User user = new User(null, firstName, lastName, username, password, addressId, "", 0.0, 0);
                                FirestoreUtils.createUser(user, userId -> {
                                    if (userId != null) {
                                        user.setUserID(userId); // Set the user ID once it is generated

                                        // Save userID and userProvince in SharedPreferences
                                        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString("userID", userId);
                                        editor.putString("userProvince", province);
                                        editor.apply();

                                        userRegistrationMessage.postValue("User registered successfully!");
                                        isRegistrationComplete.postValue(true);
                                    } else {
                                        userRegistrationMessage.postValue("Failed to register user.");
                                    }
                                });
                            } else {
                                userRegistrationMessage.postValue("Failed to register address.");
                            }
                        });
                    }
                });
    }
}
