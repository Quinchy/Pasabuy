package com.pasabuy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.viewmodels.AddPostViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddPostActivity extends AppCompatActivity {

    private static final String TAG = "AddPostActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 1;

    private LinearLayout photoContainer;
    private List<Uri> photoUris;
    private EditText titleEditText, descriptionEditText, priceEditText;
    private Spinner categorySpinner;
    private DatePicker deadlineDatePicker;
    private TimePicker deadlineTimePicker;
    private FirebaseFirestore db;
    private AddPostViewModel addPostViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        db = FirebaseFirestore.getInstance();
        photoUris = new ArrayList<>();
        addPostViewModel = new ViewModelProvider(this).get(AddPostViewModel.class);

        setupUI();
        setupObservers();
    }

    @SuppressLint("WrongViewCast")
    private void setupUI() {
        ImageButton backButtonAddPost = findViewById(R.id.backButtonAddPost);
        backButtonAddPost.setOnClickListener(v -> finish());

        categorySpinner = findViewById(R.id.categorySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.category_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        categorySpinner.setAdapter(adapter);

        deadlineDatePicker = findViewById(R.id.deadlineDatePicker);
        deadlineTimePicker = findViewById(R.id.deadlineTimePicker);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceEditText = findViewById(R.id.priceEditText);

        photoContainer = findViewById(R.id.photoContainerAddPost);
        Button uploadPhotoButton = findViewById(R.id.uploadPhotoButtonAddPost);
        uploadPhotoButton.setOnClickListener(v -> openGallery());

        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> submitPost());
    }

    private void setupObservers() {
        addPostViewModel.submissionMessage.observe(this, message -> {
            Toast.makeText(AddPostActivity.this, message, Toast.LENGTH_SHORT).show();
            if (message.equals("Post submitted successfully")) {
                finish(); // Navigate back to the previous screen
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    // Multiple images selected
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        if (imageUri != null) {
                            Log.d(TAG, "Image selected: " + imageUri.toString());
                            photoUris.add(imageUri);
                            addPhotoToContainer(imageUri);
                        } else {
                            Log.e(TAG, "Image URI is null");
                        }
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        Log.d(TAG, "Image selected: " + imageUri.toString());
                        photoUris.add(imageUri);
                        addPhotoToContainer(imageUri);
                    } else {
                        Log.e(TAG, "Image URI is null");
                    }
                }
            } else {
                Log.e(TAG, "Image selection failed or canceled");
            }
        }
    }

    private void addPhotoToContainer(Uri imageUri) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View photoCardView = inflater.inflate(R.layout.photo_card, photoContainer, false);

        ImageView imageView = photoCardView.findViewById(R.id.uploadedPhoto);
        imageView.setImageURI(imageUri);

        ImageButton removeButton = photoCardView.findViewById(R.id.removePhotoButton);
        removeButton.setOnClickListener(v -> removePhotoFromContainer(photoCardView, imageUri));

        photoContainer.addView(photoCardView);
    }

    private void removePhotoFromContainer(View view, Uri imageUri) {
        photoContainer.removeView(view);
        photoUris.remove(imageUri);
    }

    private void submitPost() {
        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userID", null);
        String userProvince = sharedPreferences.getString("userProvince", null);

        if (userId == null || userProvince == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        Timestamp deadline = getFormattedDeadline();

        if (title.isEmpty() || description.isEmpty() || priceString.isEmpty() || category.isEmpty() || deadline == null) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price.", Toast.LENGTH_SHORT).show();
            return;
        }

        addPostViewModel.submitPost(userId, userProvince, title, description, price, deadline, category, photoUris);
    }

    private Timestamp getFormattedDeadline() {
        int day = deadlineDatePicker.getDayOfMonth();
        int month = deadlineDatePicker.getMonth();
        int year = deadlineDatePicker.getYear();
        int hour = deadlineTimePicker.getHour();
        int minute = deadlineTimePicker.getMinute();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute);

        return new Timestamp(calendar.getTime());
    }
}
