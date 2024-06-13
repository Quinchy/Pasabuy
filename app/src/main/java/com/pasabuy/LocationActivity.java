package com.pasabuy;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.pasabuy.viewmodels.LocationViewModel;

import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String TAG = "LocationActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private MapView mapView;
    private GoogleMap googleMap;
    private PlacesClient placesClient;
    private TextView locationTextView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SharedPreferences sharedPreferences;
    private boolean locationPermissionGranted = false;
    private LatLng currentLatLng;
    private LocationViewModel locationViewModel;

    private String firstName, lastName, username, password, houseStreet, barangay, municipality, province;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Initialize the Places API
        Places.initialize(getApplicationContext(), "AIzaSyAUb2kNzStJEZCmnKVGazm_3fXjlFxPOoU");
        placesClient = Places.createClient(this);

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("PasabuyApp", MODE_PRIVATE);

        // Retrieve user data from intent
        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("lastName");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        houseStreet = getIntent().getStringExtra("houseStreet");
        barangay = getIntent().getStringExtra("barangay");
        municipality = getIntent().getStringExtra("municipality");
        province = getIntent().getStringExtra("province");

        // Initialize views
        mapView = findViewById(R.id.mapView);
        locationTextView = findViewById(R.id.locationTextView);
        ImageButton backButtonLocation = findViewById(R.id.backButtonLocation);
        Button setLocationButton = findViewById(R.id.setLocationButton);

        // Initialize ViewModel
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);

        // Observe ViewModel LiveData
        locationViewModel.currentLocationText.observe(this, locationTextView::setText);
        locationViewModel.currentLatLng.observe(this, latLng -> {
            if (googleMap != null) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(latLng).title(locationViewModel.currentLocationText.getValue()));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        });

        locationViewModel.userRegistrationMessage.observe(this, message -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            if (message.equals("User registered successfully!")) {
                // Proceed to PasabuyAppActivity
                Intent intent = new Intent(LocationActivity.this, PasabuyAppActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Set button listeners
        backButtonLocation.setOnClickListener(v -> finish());
        setLocationButton.setOnClickListener(v -> {
            if (currentLatLng != null) {
                locationViewModel.geocodeLocation(currentLatLng, new Geocoder(this, Locale.getDefault()));
                locationViewModel.registerUserWithLocation(firstName, lastName, username, password, houseStreet, barangay, municipality, province);
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize location client and callback
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    locationViewModel.geocodeLocation(currentLatLng, new Geocoder(LocationActivity.this, Locale.getDefault()));
                }
            }
        };

        // Initialize map
        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        Log.d(TAG, "MapView initialized and getMapAsync called");

        // Request location permission and get current location
        getLocationPermission();
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if permission is denied
            }
        }
    }

    private void getCurrentLocation() {
        if (locationPermissionGranted) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    locationViewModel.geocodeLocation(currentLatLng, new Geocoder(LocationActivity.this, Locale.getDefault()));
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        Log.d(TAG, "GoogleMap is ready");
        updateLocationUI();

        // Add a marker in a default location if currentLatLng is not found
        if (currentLatLng == null) {
            LatLng defaultLocation = new LatLng(-34, 151);
            googleMap.addMarker(new MarkerOptions().position(defaultLocation).title("Marker in Sydney"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
        }
    }

    private void updateLocationUI() {
        if (googleMap == null) {
            Log.d(TAG, "GoogleMap is null");
            return;
        }
        try {
            if (locationPermissionGranted) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                getCurrentLocation();
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }
}
