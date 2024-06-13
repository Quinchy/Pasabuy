package com.pasabuy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.pasabuy.models.Address;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.MyAccountViewModel;

public class MyAccountActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private MyAccountViewModel viewModel;
    private ImageView profilePicture;
    private EditText usernameEditText, passwordEditText, firstNameEditText, lastNameEditText,
            addressEditText, barangayEditText, municipalityEditText, provinceEditText;
    private String userID;
    private Uri selectedImageUri;
    private String savedAddressID;
    private String savedProfilePictureURL;
    private double savedRating;
    private int savedRatingCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        viewModel = new ViewModelProvider(this).get(MyAccountViewModel.class);

        ImageButton backButtonMyAccount = findViewById(R.id.backButtonMyAccount);
        backButtonMyAccount.setOnClickListener(v -> finish());

        profilePicture = findViewById(R.id.profilePicture);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        addressEditText = findViewById(R.id.addressEditText);
        barangayEditText = findViewById(R.id.barangayEditText);
        municipalityEditText = findViewById(R.id.municipalityEditText);
        provinceEditText = findViewById(R.id.provinceEditText);

        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", null);

        if (userID != null) {
            viewModel.loadUserData(userID);
            observeViewModel();
        }

        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> openGallery());

        Button removeButton = findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> removeProfilePicture());

        ImageButton showPasswordIcon = findViewById(R.id.showPasswordIcon);
        showPasswordIcon.setOnClickListener(v -> togglePasswordVisibility());

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> saveUserData());

        Button deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                savedAddressID = user.getAddressID(); // Save the addressID
                savedProfilePictureURL = user.getProfilePictureURL(); // Save the profilePictureURL
                savedRating = user.getRating(); // Save the rating
                savedRatingCount = user.getRatingCount(); // Save the ratingCount
                if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfilePictureURL())
                            .placeholder(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(profilePicture);
                } else {
                    Glide.with(this)
                            .load(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(profilePicture);
                }
                usernameEditText.setText(user.getUsername());
                passwordEditText.setText(user.getPassword());
                firstNameEditText.setText(user.getFirstName());
                lastNameEditText.setText(user.getLastName());
            }
        });

        viewModel.getAddressLiveData().observe(this, address -> {
            if (address != null) {
                addressEditText.setText(address.getHouseStreet());
                barangayEditText.setText(address.getBarangay());
                municipalityEditText.setText(address.getMunicipality());
                provinceEditText.setText(address.getProvince());
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(profilePicture);
        }
    }

    private void removeProfilePicture() {
        selectedImageUri = null;
        profilePicture.setImageResource(R.drawable.placeholder_profile);
    }

    private void togglePasswordVisibility() {
        if (passwordEditText.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordEditText.setSelection(passwordEditText.getText().length());
    }
    private void saveUserData() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String houseStreet = addressEditText.getText().toString();
        String barangay = barangayEditText.getText().toString();
        String municipality = municipalityEditText.getText().toString();
        String province = provinceEditText.getText().toString();

        User user = new User(userID, firstName, lastName, username, password, savedAddressID, savedProfilePictureURL, savedRating, savedRatingCount); // Use saved values
        Address address = new Address(savedAddressID, houseStreet, barangay, municipality, province); // Use savedAddressID

        viewModel.saveUserData(user, address, selectedImageUri);

        Toast.makeText(MyAccountActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MyAccountActivity.this, PasabuyAppActivity.class);
        intent.putExtra("loadProfileFragment", true);
        startActivity(intent);
        finish();  // Exit the activity after saving
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.confirm_delete_popup, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button yesButton = dialogView.findViewById(R.id.yesButton);
        Button noButton = dialogView.findViewById(R.id.noButton);

        yesButton.setOnClickListener(v -> {
            deleteUserAccount();
            dialog.dismiss();
        });

        noButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void deleteUserAccount() {
        viewModel.deleteUserAccount(userID);
        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

        // Sign out user and remove user-related data from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("userID");
        editor.remove("isLoggedIn");
        editor.remove("userProvince");
        editor.apply();

        // Redirect to login screen
        startActivity(new Intent(MyAccountActivity.this, SignInActivity.class));
        finish();
    }
}