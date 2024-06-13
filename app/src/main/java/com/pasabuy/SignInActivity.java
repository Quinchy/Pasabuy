package com.pasabuy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.pasabuy.viewmodels.SignInViewModel;

public class SignInActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private ImageButton showPasswordIcon;
    private Button signInButton;
    private SignInViewModel signInViewModel;
    private FirebaseAuth auth;
    private boolean isPasswordVisible = false;
    private boolean isSigningIn = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        auth = FirebaseAuth.getInstance();
        initializeViews();
        signInViewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        setupObservers();
        setupUI();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        showPasswordIcon = findViewById(R.id.showPasswordIcon);
        signInButton = findViewById(R.id.signInButton);
    }

    private void setupObservers() {
        signInViewModel.isLoginSuccessful().observe(this, this::handleLoginSuccess);
        signInViewModel.getLoginError().observe(this, this::showErrorMessage);
    }

    private void handleLoginSuccess(Boolean isSuccess) {
        if (isSuccess != null && isSuccess) {
            String userId = signInViewModel.getUserId().getValue();
            if (userId != null) {
                saveUserInfo(userId);
                updateNewUserFlag();
                saveLoginState(true);
                navigateToPasabuyApp();
            }
        }
        resetSigningInFlag();
    }

    private void showErrorMessage(String error) {
        showToast(error);
        resetSigningInFlag();
    }

    private void resetSigningInFlag() {
        isSigningIn = false;
    }

    private void saveLoginState(boolean isLoggedIn) {
        SharedPreferences prefs = getSharedPreferences("PasabuyApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", isLoggedIn);
        editor.apply();
    }

    private void saveUserInfo(String userId) {
        SharedPreferences prefs = getSharedPreferences("PasabuyApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userID", userId);
        editor.apply();
    }

    private void updateNewUserFlag() {
        SharedPreferences prefs = getSharedPreferences("PasabuyApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isNewUser", false);
        editor.apply();
    }

    private void setupUI() {
        TextView signUpLink = findViewById(R.id.signUpLink);
        signUpLink.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        ImageButton backButtonSignIn = findViewById(R.id.backButtonSignIn);
        backButtonSignIn.setOnClickListener(v -> finish());

        signInButton.setOnClickListener(v -> handleSignIn());

        showPasswordIcon.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void handleSignIn() {
        if (isSigningIn) {
            return;
        }
        isSigningIn = true;

        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (validateInputs(username, password)) {
            signInViewModel.signIn(username, password);
            handler.postDelayed(() -> isSigningIn = false, 3000);
        } else {
            showToast("Username and Password cannot be empty");
            isSigningIn = false;
        }
    }

    private boolean validateInputs(String username, String password) {
        return !username.isEmpty() && !password.isEmpty();
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.setInputType(129); // 129: TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
            showPasswordIcon.setImageResource(R.drawable.open_eye_icon);
        } else {
            passwordEditText.setInputType(145); // 145: TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            showPasswordIcon.setImageResource(R.drawable.hide_eye_icon);
        }
        passwordEditText.setTypeface(ResourcesCompat.getFont(this, R.font.sd_pro_rounded_regular));
        passwordEditText.setSelection(passwordEditText.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void navigateToPasabuyApp() {
        startActivity(new Intent(SignInActivity.this, PasabuyAppActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 200);
        toast.show();
    }
}