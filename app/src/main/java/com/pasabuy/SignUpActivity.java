package com.pasabuy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class SignUpActivity extends AppCompatActivity {
    private EditText firstNameEditText, lastNameEditText, usernameEditText, passwordEditText;
    private EditText houseStreetEditText, barangayEditText, municipalityEditText, provinceEditText;
    private Button signUpButton;
    private ImageButton showPasswordIcon;
    private boolean isPasswordVisible = false;
    private boolean isSigningUp = false;
    private Handler handler = new Handler();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupUI();
    }

    private void initializeViews() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        houseStreetEditText = findViewById(R.id.houseNumberEditText);
        barangayEditText = findViewById(R.id.barangayEditText);
        municipalityEditText = findViewById(R.id.municipalityEditText);
        provinceEditText = findViewById(R.id.provinceEditText);
        signUpButton = findViewById(R.id.signUpButton);
        showPasswordIcon = findViewById(R.id.showPasswordIcon);
    }

    private void setupUI() {
        ImageButton backButtonSignUp = findViewById(R.id.backButtonSignUp);
        backButtonSignUp.setOnClickListener(v -> finish());

        signUpButton.setOnClickListener(v -> {
            if (isSigningUp) {
                return; // Prevent spamming the sign-up button
            }
            isSigningUp = true;

            if (areFieldsValid()) {
                checkUsernameAvailability(usernameEditText.getText().toString().trim());
            } else {
                showToast("Please fill in all required fields and ensure password is at least 8 characters.");
                isSigningUp = false;
            }

            // Reset the signing up flag after a delay to prevent spamming
            handler.postDelayed(() -> isSigningUp = false, 3000); // Adjust delay as needed
        });

        showPasswordIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordEditText.setInputType(129); // 129: TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
                showPasswordIcon.setImageResource(R.drawable.open_eye_icon);
            } else {
                passwordEditText.setInputType(145); // 145: TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showPasswordIcon.setImageResource(R.drawable.hide_eye_icon);
            }
            passwordEditText.setTypeface(ResourcesCompat.getFont(this, R.font.sd_pro_rounded_regular)); // Set the custom font
            passwordEditText.setSelection(passwordEditText.getText().length()); // To move the cursor to the end
            isPasswordVisible = !isPasswordVisible;
        });
    }

    private void checkUsernameAvailability(String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            showToast("Username is already taken. Please choose another one.");
                            isSigningUp = false;
                        } else {
                            proceedToNextActivity();
                        }
                    } else {
                        showToast("Error checking username availability. Please try again.");
                        isSigningUp = false;
                    }
                });
    }

    private void proceedToNextActivity() {
        Intent intent = new Intent(SignUpActivity.this, LocationActivity.class);
        intent.putExtra("firstName", firstNameEditText.getText().toString().trim());
        intent.putExtra("lastName", lastNameEditText.getText().toString().trim());
        intent.putExtra("username", usernameEditText.getText().toString().trim());
        intent.putExtra("password", passwordEditText.getText().toString().trim());
        intent.putExtra("houseStreet", houseStreetEditText.getText().toString().trim());
        intent.putExtra("barangay", barangayEditText.getText().toString().trim());
        intent.putExtra("municipality", municipalityEditText.getText().toString().trim());
        intent.putExtra("province", provinceEditText.getText().toString().trim());
        startActivity(intent);
    }

    private boolean areFieldsValid() {
        boolean isValid = true;
        if (firstNameEditText.getText().toString().trim().isEmpty() ||
                lastNameEditText.getText().toString().trim().isEmpty() ||
                usernameEditText.getText().toString().trim().isEmpty() ||
                passwordEditText.getText().toString().trim().isEmpty() ||
                barangayEditText.getText().toString().trim().isEmpty() ||
                municipalityEditText.getText().toString().trim().isEmpty() ||
                provinceEditText.getText().toString().trim().isEmpty()) {
            isValid = false;
        }
        if (passwordEditText.getText().toString().trim().length() < 8) {
            isValid = false;
        }
        return isValid;
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 200); // Adjust the vertical offset as needed
        toast.show();
    }
}
