package com.pasabuy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Timestamp;
import com.pasabuy.models.Post;
import com.pasabuy.viewmodels.EditPostViewModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {

    private static final String TAG = "EditPostActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 1;

    private LinearLayout photoContainer;
    private List<Uri> photoUris;
    private List<String> originalImageUrls;
    private EditText titleEditText, descriptionEditText, priceEditText;
    private Spinner categorySpinner;
    private DatePicker deadlineDatePicker;
    private TimePicker deadlineTimePicker;
    private Button submitButton;
    private EditPostViewModel editPostViewModel;
    private String postID;
    private Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        editPostViewModel = new ViewModelProvider(this).get(EditPostViewModel.class);
        photoUris = new ArrayList<>();
        originalImageUrls = new ArrayList<>();

        setupUI();

        // Get postID from Intent
        Intent intent = getIntent();
        postID = intent.getStringExtra("postID");

        // Fetch post details
        editPostViewModel.fetchPostDetails(postID);

        // Observe LiveData from ViewModel
        editPostViewModel.getPostLiveData().observe(this, this::populatePostDetails);
        editPostViewModel.getImageUrlsLiveData().observe(this, this::loadPostImages);
        editPostViewModel.submissionMessage.observe(this, message -> {
            Toast.makeText(EditPostActivity.this, message, Toast.LENGTH_SHORT).show();
            if (message.equals("Post updated successfully")) {
                finish(); // Navigate back to the previous screen
            }
        });
    }

    private void setupUI() {
        ImageButton backButtonEditPost = findViewById(R.id.backButtonEditPost);
        backButtonEditPost.setOnClickListener(v -> finish());

        categorySpinner = findViewById(R.id.categorySpinnerEditPost);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.category_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        categorySpinner.setAdapter(adapter);

        deadlineDatePicker = findViewById(R.id.deadlineDatePickerEditPost);
        deadlineTimePicker = findViewById(R.id.deadlineTimePickerEditPost);

        titleEditText = findViewById(R.id.titleEditTextEditPost);
        descriptionEditText = findViewById(R.id.descriptionEditTextEditPost);
        priceEditText = findViewById(R.id.priceEditTextEditPost);

        photoContainer = findViewById(R.id.photoContainerEditPost);
        Button uploadPhotoButton = findViewById(R.id.uploadPhotoButtonEditPost);
        uploadPhotoButton.setOnClickListener(v -> openGallery());

        submitButton = findViewById(R.id.submitButtonEditPost);
        submitButton.setOnClickListener(v -> updatePost());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Pictures"), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
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

    private void addPhotoToContainer(String imageUrl) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View photoCardView = inflater.inflate(R.layout.photo_card, photoContainer, false);

        ImageView imageView = photoCardView.findViewById(R.id.uploadedPhoto);
        Picasso.get().load(imageUrl).into(imageView);

        ImageButton removeButton = photoCardView.findViewById(R.id.removePhotoButton);
        removeButton.setOnClickListener(v -> removePhotoFromContainer(photoCardView, Uri.parse(imageUrl)));

        photoContainer.addView(photoCardView);
    }

    private void removePhotoFromContainer(View view, Uri imageUri) {
        photoContainer.removeView(view);
        photoUris.remove(imageUri);
    }

    private void populatePostDetails(Post post) {
        this.post = post;
        titleEditText.setText(post.getTitle());
        descriptionEditText.setText(post.getDescription());
        priceEditText.setText(String.valueOf(post.getPrice()));

        // Set category spinner
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) categorySpinner.getAdapter();
        int position = adapter.getPosition(post.getCategory());
        categorySpinner.setSelection(position);

        // Set deadline date and time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(post.getDeadline().toDate());
        deadlineDatePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        deadlineTimePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        deadlineTimePicker.setMinute(calendar.get(Calendar.MINUTE));

        // Fetch associated images
        editPostViewModel.fetchImageUrls(post.getImageIDs());
    }

    private void loadPostImages(List<String> imageUrls) {
        photoContainer.removeAllViews();  // Clear existing images
        originalImageUrls = new ArrayList<>(imageUrls);  // Store original image URLs
        for (String imageUrl : imageUrls) {
            addPhotoToContainer(imageUrl);
        }
    }

    private void updatePost() {
        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userID", null);

        if (userId == null) {
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

        post.setTitle(title);
        post.setDescription(description);
        post.setPrice(price);
        post.setCategory(category);
        post.setDeadline(deadline);

        editPostViewModel.updatePost(post, photoUris, originalImageUrls);
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
