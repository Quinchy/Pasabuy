package com.pasabuy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.ViewOrderViewModel;
import com.squareup.picasso.Picasso;

public class ViewOrderActivity extends AppCompatActivity {

    private TextView orderStatusProductTitle;
    private TextView orderStatusProductPrice;
    private TextView orderStatusProductQuantity;
    private ImageView orderStatusProductImage;

    private ImageView initialLine;
    private ImageView orderConfirmationIcon;
    private ImageView line1;
    private ImageView orderBuyingIcon;
    private ImageView line2;
    private ImageView orderDeliveringIcon;
    private ImageView line3;
    private ImageView orderDeliveredIcon;

    private TextView orderConfirmationTextView;
    private TextView orderBuyingTextView;
    private TextView orderDeliveringTextView;
    private TextView orderDeliveredTextView;

    private ImageView profilePictureImageOrderStatus;
    private TextView userFullNameOrderStatus;
    private TextView userRatingsOrderStatus;

    private ImageButton backButtonOrderStatus;
    private ImageButton messageButtonOrderStatus;
    private Button cancelButtonOrderStatus;

    private ViewOrderViewModel viewOrderViewModel;
    private String orderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);

        viewOrderViewModel = new ViewModelProvider(this).get(ViewOrderViewModel.class);

        initializeViews();
        orderID = getIntent().getStringExtra("orderID");
        loadOrderDetails(orderID);

        backButtonOrderStatus.setOnClickListener(v -> onBackPressed());
        cancelButtonOrderStatus.setOnClickListener(v -> showCancelConfirmationDialog());
    }

    private void initializeViews() {
        orderStatusProductTitle = findViewById(R.id.orderStatusProductTitle);
        orderStatusProductPrice = findViewById(R.id.orderStatusProductPrice);
        orderStatusProductQuantity = findViewById(R.id.forderStatusProductQuantity);
        orderStatusProductImage = findViewById(R.id.orderStatusProductImage);

        initialLine = findViewById(R.id.initialLine);
        orderConfirmationIcon = findViewById(R.id.orderConfirmationIcon);
        line1 = findViewById(R.id.line1);
        orderBuyingIcon = findViewById(R.id.orderBuyingIcon);
        line2 = findViewById(R.id.line2);
        orderDeliveringIcon = findViewById(R.id.orderDeliveringIcon);
        line3 = findViewById(R.id.line3);
        orderDeliveredIcon = findViewById(R.id.orderDeliveredIcon);

        orderConfirmationTextView = findViewById(R.id.orderConfirmationTextView);
        orderBuyingTextView = findViewById(R.id.orderBuyingTextView);
        orderDeliveringTextView = findViewById(R.id.orderDeliveringTextView);
        orderDeliveredTextView = findViewById(R.id.orderDeliveredTextView);

        profilePictureImageOrderStatus = findViewById(R.id.profilePictureImageOrderStatus);
        userFullNameOrderStatus = findViewById(R.id.userFullNameOrderStatus);
        userRatingsOrderStatus = findViewById(R.id.userRatingsOrderStatus);

        backButtonOrderStatus = findViewById(R.id.backButtonOrderStatus);
        messageButtonOrderStatus = findViewById(R.id.messageButtonOrderStatus);
        cancelButtonOrderStatus = findViewById(R.id.button2);
    }

    private void loadOrderDetails(String orderID) {
        viewOrderViewModel.getOrderDetails(orderID).observe(this, order -> {
            if (order != null) {
                orderStatusProductQuantity.setText("Quantity: " + order.getQuantity());
                loadPostDetails(order.getPostID(), order.getStatus());

                // Remove the cancel button if the order status is "Buying" or "Delivering"
                if ("Buying".equals(order.getStatus()) || "Delivering".equals(order.getStatus())) {
                    cancelButtonOrderStatus.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadPostDetails(String postID, String orderStatus) {
        viewOrderViewModel.getPostDetails(postID).observe(this, post -> {
            if (post != null) {
                orderStatusProductTitle.setText(post.getTitle());
                orderStatusProductPrice.setText("₱" + post.getPrice());
                Picasso.get().load(post.getThumbnail()).into(orderStatusProductImage);
                updateOrderStatusUI(orderStatus);
                loadSellerDetails(post.getUserID());
            }
        });
    }

    private void updateOrderStatusUI(String status) {
        switch (status) {
            case "Confirmed":
                initialLine.setImageResource(R.drawable.initial_line_done_icon);
                orderConfirmationIcon.setImageResource(R.drawable.order_confirmation_done_icon);
                orderConfirmationTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));
                break;
            case "Buying":
                initialLine.setImageResource(R.drawable.initial_line_done_icon);
                orderConfirmationIcon.setImageResource(R.drawable.order_confirmation_done_icon);
                orderConfirmationTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line1.setImageResource(R.drawable.line_done_icon);
                orderBuyingIcon.setImageResource(R.drawable.order_buying_done_icon);
                orderBuyingTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));
                break;
            case "Delivering":
                initialLine.setImageResource(R.drawable.initial_line_done_icon);
                orderConfirmationIcon.setImageResource(R.drawable.order_confirmation_done_icon);
                orderConfirmationTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line1.setImageResource(R.drawable.line_done_icon);
                orderBuyingIcon.setImageResource(R.drawable.order_buying_done_icon);
                orderBuyingTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line2.setImageResource(R.drawable.line_done_icon);
                orderDeliveringIcon.setImageResource(R.drawable.order_delivering_ready_icon);
                orderDeliveringTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));
                break;
            case "Delivered":
                initialLine.setImageResource(R.drawable.initial_line_done_icon);
                orderConfirmationIcon.setImageResource(R.drawable.order_confirmation_done_icon);
                orderConfirmationTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line1.setImageResource(R.drawable.line_done_icon);
                orderBuyingIcon.setImageResource(R.drawable.order_buying_done_icon);
                orderBuyingTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line2.setImageResource(R.drawable.line_done_icon);
                orderDeliveringIcon.setImageResource(R.drawable.order_delivering_ready_icon);
                orderDeliveringTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));

                line3.setImageResource(R.drawable.line_done_icon);
                orderDeliveredIcon.setImageResource(R.drawable.order_delivered_confirmed_icon);
                orderDeliveredTextView.setTypeface(getResources().getFont(R.font.sd_pro_rounded_semibold));
                break;
        }
    }

    private void loadSellerDetails(String sellerID) {
        viewOrderViewModel.getUserDetails(sellerID).observe(this, user -> {
            if (user != null) {
                String fullName = user.getFirstName() + " " + user.getLastName();
                userFullNameOrderStatus.setText(fullName);
                userRatingsOrderStatus.setText(String.valueOf(user.getRating()));
                if (user.getProfilePictureURL() != null && !user.getProfilePictureURL().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfilePictureURL())
                            .placeholder(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(profilePictureImageOrderStatus);
                } else {
                    Glide.with(this)
                            .load(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(profilePictureImageOrderStatus);
                }

                // Set the OnClickListener to the message button
                messageButtonOrderStatus.setOnClickListener(v -> {
                    Intent messageIntent = new Intent(ViewOrderActivity.this, ViewMessageActivity.class);
                    messageIntent.putExtra("userID", user.getUserID());
                    startActivity(messageIntent);
                });
            }
        });
    }

    private void showCancelConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.confirm_delete_popup, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button yesButton = dialogView.findViewById(R.id.yesButton);
        Button noButton = dialogView.findViewById(R.id.noButton);

        dialogTitle.setText("Cancel Order");
        dialogMessage.setText("Are you sure you want to cancel this order? This action cannot be undone.");

        AlertDialog alertDialog = builder.create();

        yesButton.setOnClickListener(v -> {
            updateOrderStatusToCancel();
            alertDialog.dismiss();
        });

        noButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void updateOrderStatusToCancel() {
        viewOrderViewModel.updateOrderStatus(orderID, "Cancelled").observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Order status updated to Cancel", Toast.LENGTH_SHORT).show();
                // Redirect to OrdersFragment in PasabuyAppActivity
                Intent intent = new Intent(ViewOrderActivity.this, PasabuyAppActivity.class);
                intent.putExtra("openOrdersFragment", true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
