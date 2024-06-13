package com.pasabuy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pasabuy.fragments.HomeFragment;
import com.pasabuy.fragments.MessagesFragment;
import com.pasabuy.fragments.OrdersFragment;
import com.pasabuy.fragments.ProfileFragment;

public class PasabuyAppActivity extends AppCompatActivity {
    private LinearLayout navHomeLayout, navOrdersLayout, navMessagesLayout, navProfileLayout;
    private ImageView navHome, navOrders, navMessages, navProfile;
    private FloatingActionButton fab;

    private Fragment homeFragment;
    private Fragment ordersFragment;
    private Fragment messagesFragment;
    private Fragment profileFragment;

    private Fragment activeFragment;
    private static final String ACTIVE_FRAGMENT_KEY = "activeFragment";

    private GlobalBroadcastReceiver globalBroadcastReceiver;

    private Handler handler = new Handler();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasabuy_app);

        homeFragment = new HomeFragment();
        ordersFragment = new OrdersFragment();
        messagesFragment = new MessagesFragment();
        profileFragment = new ProfileFragment();

        setupUI();

        // Add all fragments initially and show the default one
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, homeFragment, "home").hide(homeFragment);
        transaction.add(R.id.container, ordersFragment, "orders").hide(ordersFragment);
        transaction.add(R.id.container, messagesFragment, "messages").hide(messagesFragment);
        transaction.add(R.id.container, profileFragment, "profile").hide(profileFragment);

        if (savedInstanceState != null) {
            activeFragment = getSupportFragmentManager().getFragment(savedInstanceState, ACTIVE_FRAGMENT_KEY);
            transaction.show(activeFragment).commit();
            updateIcons(getActiveFragmentId());
        } else {
            if (getIntent().getBooleanExtra("openOrdersFragment", false)) {
                transaction.show(ordersFragment).commit();
                activeFragment = ordersFragment;
                updateIcons(R.id.nav_orders);
            } else {
                transaction.show(homeFragment).commit();
                activeFragment = homeFragment;
                updateIcons(R.id.nav_home);
            }
        }

        // Initialize and register the broadcast receiver
        globalBroadcastReceiver = new GlobalBroadcastReceiver();
        IntentFilter filter = new IntentFilter("com.pasabuy.SHOW_POPUP");
        registerReceiver(globalBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        // Send broadcast to show popup after sign in
        sendBroadcast(new Intent("com.pasabuy.SHOW_POPUP"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (globalBroadcastReceiver != null) {
            unregisterReceiver(globalBroadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we need to open the ProfileFragment
        if (getIntent().getBooleanExtra("loadProfileFragment", false)) {
            switchFragment(profileFragment);
            updateIcons(R.id.nav_profile);
            // Reset the extra to avoid re-triggering
            getIntent().removeExtra("loadProfileFragment");
            // Refresh the ProfileFragment to load new data
            if (profileFragment instanceof ProfileFragment) {
                ((ProfileFragment) profileFragment).refreshUserData();
            }
        } else {
            // Reload the current fragment if needed
            if (activeFragment != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.detach(activeFragment);
                transaction.attach(activeFragment);
                transaction.commit();
            }

            // Check if we need to open the OrdersFragment
            if (getIntent().getBooleanExtra("openOrdersFragment", false)) {
                switchFragment(ordersFragment);
                updateIcons(R.id.nav_orders);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeFragment != null) {
            getSupportFragmentManager().putFragment(outState, ACTIVE_FRAGMENT_KEY, activeFragment);
        }
    }

    private void setupUI() {
        navHomeLayout = findViewById(R.id.nav_home_layout);
        navOrdersLayout = findViewById(R.id.nav_orders_layout);
        navMessagesLayout = findViewById(R.id.nav_messages_layout);
        navProfileLayout = findViewById(R.id.nav_profile_layout);

        navHome = findViewById(R.id.nav_home);
        navOrders = findViewById(R.id.nav_orders);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);

        fab = findViewById(R.id.fab);

        navHomeLayout.setOnClickListener(new DebouncedClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                switchFragment(homeFragment);
                updateIcons(R.id.nav_home);
            }
        });

        navOrdersLayout.setOnClickListener(new DebouncedClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                switchFragment(ordersFragment);
                updateIcons(R.id.nav_orders);
            }
        });

        navMessagesLayout.setOnClickListener(new DebouncedClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                switchFragment(messagesFragment);
                updateIcons(R.id.nav_messages);
                // Call loadMessages() method of MessagesFragment
                ((MessagesFragment) messagesFragment).loadMessages();
            }
        });

        navProfileLayout.setOnClickListener(new DebouncedClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                switchFragment(profileFragment);
                updateIcons(R.id.nav_profile);
                // Refresh the ProfileFragment to load new data
                if (profileFragment instanceof ProfileFragment) {
                    ((ProfileFragment) profileFragment).refreshUserData();
                }
            }
        });

        fab.setOnClickListener(new DebouncedClickListener() {
            @Override
            public void onDebouncedClick(View view) {
                Intent intent = new Intent(PasabuyAppActivity.this, AddPostActivity.class);
                startActivity(intent);
            }
        });
    }

    private synchronized void switchFragment(Fragment fragment) {
        if (fragment == activeFragment) return;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(activeFragment);
        transaction.show(fragment);
        transaction.commit();
        activeFragment = fragment;
    }

    private void updateIcons(int selectedId) {
        navHome.setImageResource(selectedId == R.id.nav_home ? R.drawable.home_icon_filled : R.drawable.home_icon);
        navOrders.setImageResource(selectedId == R.id.nav_orders ? R.drawable.order_icon_filled : R.drawable.order_icon);
        navMessages.setImageResource(selectedId == R.id.nav_messages ? R.drawable.message_icon_filled : R.drawable.message_icon);
        navProfile.setImageResource(selectedId == R.id.nav_profile ? R.drawable.profile_icon_filled : R.drawable.profile_icon);
    }

    private int getActiveFragmentId() {
        if (activeFragment instanceof HomeFragment) {
            return R.id.nav_home;
        } else if (activeFragment instanceof OrdersFragment) {
            return R.id.nav_orders;
        } else if (activeFragment instanceof MessagesFragment) {
            return R.id.nav_messages;
        } else if (activeFragment instanceof ProfileFragment) {
            return R.id.nav_profile;
        }
        return R.id.nav_home; // default
    }

    private abstract class DebouncedClickListener implements View.OnClickListener {
        private static final long MIN_CLICK_INTERVAL = 1000L;
        private long lastClickTime;

        @Override
        public void onClick(View v) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                lastClickTime = currentTime;
                onDebouncedClick(v);
            }
        }

        public abstract void onDebouncedClick(View v);
    }
}
