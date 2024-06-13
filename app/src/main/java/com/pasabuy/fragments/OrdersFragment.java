package com.pasabuy.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.R;
import com.pasabuy.ViewOrderActivity;
import com.pasabuy.models.Order;
import com.pasabuy.viewmodels.OrderViewModel;
import com.squareup.picasso.Picasso;
import java.util.List;

public class OrdersFragment extends Fragment {

    private LinearLayout filterPending;
    private LinearLayout filterBuying;
    private LinearLayout filterDelivering;
    private LinearLayout orderCardsContainer;
    private OrderViewModel orderViewModel;
    private String selectedStatus = "Pending";

    public OrdersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        initializeViews(view);
        setupFilterButtons();
        loadOrders("Pending");
        return view;
    }

    private void initializeViews(View view) {
        filterPending = view.findViewById(R.id.orderFilterPending);
        filterBuying = view.findViewById(R.id.orderFilterBuying);
        filterDelivering = view.findViewById(R.id.orderFilterDelivering);
        orderCardsContainer = view.findViewById(R.id.orderCardsContainer);
    }

    private void setupFilterButtons() {
        filterPending.setSelected(true);
        updateFilterButtonState(filterPending, R.drawable.pending_selected_icon, R.color.white);
        updateFilterButtonState(filterBuying, R.drawable.buying_icon, R.color.cyan_2);
        updateFilterButtonState(filterDelivering, R.drawable.delivering_icon, R.color.cyan_2);

        View.OnClickListener filterClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterPending.setSelected(false);
                filterBuying.setSelected(false);
                filterDelivering.setSelected(false);
                updateFilterButtonState(filterPending, R.drawable.pending_icon, R.color.cyan_2);
                updateFilterButtonState(filterBuying, R.drawable.buying_icon, R.color.cyan_2);
                updateFilterButtonState(filterDelivering, R.drawable.delivering_icon, R.color.cyan_2);

                v.setSelected(true);
                if (v.getId() == R.id.orderFilterPending) {
                    updateFilterButtonState(filterPending, R.drawable.pending_selected_icon, R.color.white);
                    selectedStatus = "Pending";
                } else if (v.getId() == R.id.orderFilterBuying) {
                    updateFilterButtonState(filterBuying, R.drawable.buying_selected_icon, R.color.white);
                    selectedStatus = "Buying";
                } else if (v.getId() == R.id.orderFilterDelivering) {
                    updateFilterButtonState(filterDelivering, R.drawable.delivering_selected_icon, R.color.white);
                    selectedStatus = "Delivering";
                }

                // Update order list based on selected status
                loadOrders(selectedStatus);
            }
        };

        filterPending.setOnClickListener(filterClickListener);
        filterBuying.setOnClickListener(filterClickListener);
        filterDelivering.setOnClickListener(filterClickListener);
    }

    private void updateFilterButtonState(LinearLayout filterButton, int iconResId, int textColorResId) {
        ImageView icon = null;
        TextView text = null;

        if (filterButton.getId() == R.id.orderFilterPending) {
            icon = filterButton.findViewById(R.id.orderFilterPendingIcon);
            text = filterButton.findViewById(R.id.orderFilterPendingText);
        } else if (filterButton.getId() == R.id.orderFilterBuying) {
            icon = filterButton.findViewById(R.id.orderFilterBuyingIcon);
            text = filterButton.findViewById(R.id.orderFilterBuyingText);
        } else if (filterButton.getId() == R.id.orderFilterDelivering) {
            icon = filterButton.findViewById(R.id.orderFilterDeliveringIcon);
            text = filterButton.findViewById(R.id.orderFilterDeliveringText);
        }

        if (icon != null && text != null) {
            icon.setImageResource(iconResId);
            text.setTextColor(ContextCompat.getColor(getContext(), textColorResId));
        }
    }

    private void loadOrders(String status) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        String userID = sharedPreferences.getString("userID", "");

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        orderViewModel.getOrdersByStatus(userID, status).observe(getViewLifecycleOwner(), this::updateUI);
    }

    private void updateUI(List<Order> orders) {
        orderCardsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (orders.isEmpty()) {
            View emptyView = inflater.inflate(R.layout.empty_text, orderCardsContainer, false);
            TextView emptyText = emptyView.findViewById(R.id.textName);
            String message = "You have no " + selectedStatus.toLowerCase() + " orders";
            emptyText.setText(message);
            orderCardsContainer.addView(emptyView);
        } else {
            for (Order order : orders) {
                View cardView = inflater.inflate(R.layout.item_pasabuy_order, orderCardsContainer, false);
                populateCardView(cardView, order);
                orderCardsContainer.addView(cardView);
            }
        }
    }


    private void populateCardView(View cardView, Order order) {
        TextView orderProductTitle = cardView.findViewById(R.id.orderProductTitle);
        TextView orderProductPrice = cardView.findViewById(R.id.orderProductPrice);
        TextView orderProductQuantity = cardView.findViewById(R.id.orderProductQuantity);
        TextView orderProductStatus = cardView.findViewById(R.id.orderProductStatus);
        ImageView orderProductImage = cardView.findViewById(R.id.orderProductImage);

        // Fetch post details to display in the order card
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(order.getPostID()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String title = documentSnapshot.getString("title");
                double price = documentSnapshot.getDouble("price");
                String thumbnail = documentSnapshot.getString("thumbnail");

                orderProductTitle.setText(title);
                orderProductPrice.setText("₱" + price);
                Picasso.get().load(thumbnail).into(orderProductImage);
            }
        });

        orderProductQuantity.setText("Quantity: " + order.getQuantity());
        orderProductStatus.setText(order.getStatus());
        orderProductStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.cyan_2));

        // Set click listener to handle further actions if needed
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewOrderActivity.class);
            intent.putExtra("orderID", order.getOrderID());
            startActivity(intent);
        });
    }
}
