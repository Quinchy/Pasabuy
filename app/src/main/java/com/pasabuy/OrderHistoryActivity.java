package com.pasabuy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.pasabuy.models.Order;
import com.pasabuy.models.Post;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.OrderHistoryViewModel;

import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private OrderHistoryViewModel orderHistoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        ImageButton backButtonOrderHistory = findViewById(R.id.backButtonOrderHistory);
        backButtonOrderHistory.setOnClickListener(v -> finish());

        orderHistoryViewModel = new ViewModelProvider(this).get(OrderHistoryViewModel.class);

        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userID", "");

        if (!userId.isEmpty()) {
            orderHistoryViewModel.fetchOrders(userId);
            orderHistoryViewModel.getOrdersLiveData().observe(this, orders -> {
                if (orders != null) {
                    populateOrderHistory(orders);
                }
            });
        }

        ImageView informationOrderHistory = findViewById(R.id.informationOrderHistory);
        informationOrderHistory.setOnClickListener(v -> showTooltip(v, "This section displays your order history, including orders that have been cancelled, declined, or delivered."));
    }

    private void showTooltip(View anchorView, String message) {
        View tooltipView = LayoutInflater.from(this).inflate(R.layout.tooltip_layout, null);
        TextView tooltipText = tooltipView.findViewById(R.id.tooltipText);
        tooltipText.setText(message);

        final PopupWindow tooltipPopup = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        tooltipView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorWidth = anchorView.getWidth();
        int xOffset = anchorWidth + 350;
        int yOffset = -(tooltipHeight / 12);

        tooltipPopup.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] - xOffset, location[1] + yOffset);

        tooltipView.setOnTouchListener((v, event) -> {
            tooltipPopup.dismiss();
            return true;
        });
    }

    private void populateOrderHistory(List<Order> orders) {
        LinearLayout orderHistoryContainer = findViewById(R.id.orderHistoryContainer);
        orderHistoryContainer.removeAllViews();

        if (orders.isEmpty()) {
            View emptyView = LayoutInflater.from(this).inflate(R.layout.empty_text, orderHistoryContainer, false);
            TextView emptyText = emptyView.findViewById(R.id.textName);
            emptyText.setText("No order history available.");
            orderHistoryContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            orderHistoryViewModel.fetchPostAndUserDetails(order, (orderDetails, post, user) -> {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_history, orderHistoryContainer, false);

                if (orderHistoryContainer.getChildCount() > 0) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemView.getLayoutParams();
                    params.topMargin = -10;
                    itemView.setLayoutParams(params);
                }

                updateOrderHistoryItem(itemView, orderDetails, post, user);
                orderHistoryContainer.addView(itemView);
            });
        }
    }

    private void updateOrderHistoryItem(View itemView, Order order, Post post, User user) {
        TextView orderTitle = itemView.findViewById(R.id.order_title);
        ImageView orderImage = itemView.findViewById(R.id.order_image);
        TextView orderName = itemView.findViewById(R.id.order_name);
        TextView orderPrice = itemView.findViewById(R.id.order_price);
        TextView orderQuantity = itemView.findViewById(R.id.order_quantity);
        TextView orderTotal = itemView.findViewById(R.id.order_total);

        orderTitle.setText(getOrderStatusText(order.getStatus()));
        Glide.with(this)
                .load(post.getThumbnail())
                .placeholder(R.drawable.placeholder_product)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(20)))
                .into(orderImage);
        orderName.setText(post.getTitle());
        orderPrice.setText(String.format("₱ %.2f", post.getPrice()));
        orderQuantity.setText(String.format("Quantity: %d", order.getQuantity()));
        orderTotal.setText(String.format("Total: ₱ %.2f", order.getTotalPrice()));
    }

    private String getOrderStatusText(String status) {
        switch (status) {
            case "Declined":
                return "Order Declined";
            case "Cancelled":
                return "Order Cancelled";
            case "Delivered":
                return "Order Delivered";
            default:
                return "Order";
        }
    }
}
