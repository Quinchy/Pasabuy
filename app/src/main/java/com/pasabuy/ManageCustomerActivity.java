package com.pasabuy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.pasabuy.models.Address;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.ManageCustomerViewModel;
import com.squareup.picasso.Picasso;

public class ManageCustomerActivity extends AppCompatActivity {

    private ManageCustomerViewModel manageCustomerViewModel;
    private String orderID;
    private String buyerUserID;

    private ImageView customerProfileImage, orderThumbnailImage;
    private TextView customerFullNameText, customerUsernameText, orderTitleText, orderPriceText, orderQuantityText;
    private EditText customerAddressText;
    private Spinner statusSpinner;
    private Button updateStatusButton;
    private ImageButton clickToMessageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_customer);

        ImageButton backButtonManageCustomer = findViewById(R.id.backButtonManageCustomer);
        backButtonManageCustomer.setOnClickListener(v -> finish());

        manageCustomerViewModel = new ViewModelProvider(this).get(ManageCustomerViewModel.class);

        // Initialize UI components
        customerProfileImage = findViewById(R.id.customerProfileManagePost);
        customerFullNameText = findViewById(R.id.customerFullNameManagePostText);
        customerUsernameText = findViewById(R.id.customerUsernameManagePostText);
        orderTitleText = findViewById(R.id.orderTitleManagePost);
        orderPriceText = findViewById(R.id.orderPriceManagePost);
        orderQuantityText = findViewById(R.id.orderQuantityManagePost);
        customerAddressText = findViewById(R.id.editTextText2);
        statusSpinner = findViewById(R.id.statusCategorySpinnerManagePost);
        updateStatusButton = findViewById(R.id.updateCustomerButton);
        orderThumbnailImage = findViewById(R.id.imageView3);
        clickToMessageButton = findViewById(R.id.clickToMessageManagePostButton);

        // Get orderID from Intent
        Intent intent = getIntent();
        orderID = intent.getStringExtra("orderID");

        // Set up status spinner with values
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        // Fetch order details
        manageCustomerViewModel.fetchOrderDetails(orderID);

        // Observe LiveData
        manageCustomerViewModel.getOrderLiveData().observe(this, new Observer<Order>() {
            @Override
            public void onChanged(Order order) {
                if (order != null) {
                    populateOrderDetails(order);
                    buyerUserID = order.getBuyerID();
                    manageCustomerViewModel.fetchUserDetails(order.getBuyerID());
                    manageCustomerViewModel.fetchPostDetails(order.getPostID());
                }
            }
        });

        manageCustomerViewModel.getUserLiveData().observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null) {
                    populateUserDetails(user);
                    manageCustomerViewModel.fetchAddressDetails(user.getAddressID());

                    // Set the OnClickListener to the profile picture
                    customerProfileImage.setOnClickListener(v -> {
                        Intent profileIntent = new Intent(ManageCustomerActivity.this, ProfileDisplayActivity.class);
                        profileIntent.putExtra("userID", user.getUserID());
                        startActivity(profileIntent);
                    });
                }
            }
        });

        manageCustomerViewModel.getPostLiveData().observe(this, new Observer<Post>() {
            @Override
            public void onChanged(Post post) {
                if (post != null) {
                    populatePostDetails(post);
                }
            }
        });

        manageCustomerViewModel.getAddressLiveData().observe(this, new Observer<Address>() {
            @Override
            public void onChanged(Address address) {
                if (address != null) {
                    populateAddressDetails(address);
                }
            }
        });

        updateStatusButton.setOnClickListener(v -> updateOrderStatus());

        clickToMessageButton.setOnClickListener(v -> {
            Intent messageIntent = new Intent(ManageCustomerActivity.this, ViewMessageActivity.class);
            messageIntent.putExtra("userID", buyerUserID);
            startActivity(messageIntent);
        });
    }

    private void populateOrderDetails(Order order) {
        orderQuantityText.setText("Quantity: " + order.getQuantity());

        // Set the status spinner value
        String[] statuses = getResources().getStringArray(R.array.status_array);
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(order.getStatus())) {
                statusSpinner.setSelection(i);
                break;
            }
        }
    }

    private void populateUserDetails(User user) {
        customerFullNameText.setText(user.getFirstName() + " " + user.getLastName());
        customerUsernameText.setText("@" + user.getUsername());
        if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfilePictureURL())
                    .placeholder(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(customerProfileImage);
        } else {
            Glide.with(this)
                    .load(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(customerProfileImage);
        }
    }

    private void populatePostDetails(Post post) {
        orderTitleText.setText(post.getTitle());
        orderPriceText.setText("₱" + post.getPrice());
        if (post.getThumbnail() != null && !post.getThumbnail().isEmpty()) {
            Picasso.get().load(post.getThumbnail()).into(orderThumbnailImage);
        } else {
            orderThumbnailImage.setImageResource(R.drawable.placeholder_product);
        }
    }

    private void populateAddressDetails(Address address) {
        customerAddressText.setText(address.getHouseStreet() + ", " + address.getBarangay() + ", " + address.getMunicipality() + ", " + address.getProvince());
    }

    private void updateOrderStatus() {
        String newStatus = statusSpinner.getSelectedItem().toString();
        manageCustomerViewModel.updateOrderStatus(orderID, newStatus).observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Order status updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
