package com.pasabuy.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.R;
import com.pasabuy.ViewMessageActivity;
import com.pasabuy.models.Message;
import com.pasabuy.models.User;
import com.pasabuy.viewmodels.MessageViewModel;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private MessageViewModel messageViewModel;
    private LinearLayout userMessagesContainer;
    private String currentUserID;
    private EditText searchMessagesEditText;
    private List<User> usersList = new ArrayList<>();
    private Map<String, Message> latestMessagesMap = new HashMap<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    public MessagesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        userMessagesContainer = view.findViewById(R.id.userMessagesContainer);
        searchMessagesEditText = view.findViewById(R.id.searchMessagesEditText);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        currentUserID = sharedPreferences.getString("userID", "");

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        loadMessages();

        searchMessagesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterMessages(s.toString());
                handler.postDelayed(searchRunnable, 300); // Debounce delay of 300ms
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    public void loadMessages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        latestMessagesMap.clear();

        db.collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            processMessage(document.toObject(Message.class));
                        }
                        loadUsers();
                    }
                });
    }

    private void processMessage(Message message) {
        String otherUserID = getOtherUserID(message);
        if (otherUserID != null && (!latestMessagesMap.containsKey(otherUserID) ||
                latestMessagesMap.get(otherUserID).getTimestamp() < message.getTimestamp())) {
            latestMessagesMap.put(otherUserID, message);
        }
    }

    private String getOtherUserID(Message message) {
        if (message.getSenderID().equals(currentUserID)) {
            return message.getReceiverID();
        } else if (message.getReceiverID().equals(currentUserID)) {
            return message.getSenderID();
        } else {
            return null;
        }
    }

    private void loadUsers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        usersList.clear();

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            usersList.add(document.toObject(User.class));
                        }
                        displayMessages(latestMessagesMap);
                    }
                });
    }

    private synchronized void displayMessages(Map<String, Message> latestMessagesMap) {
        userMessagesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (latestMessagesMap.isEmpty()) {
            addEmptyView(inflater);
        } else {
            addMessagesToContainer(inflater, latestMessagesMap);
        }
    }

    private void addEmptyView(LayoutInflater inflater) {
        View emptyView = inflater.inflate(R.layout.empty_text, userMessagesContainer, false);
        TextView emptyText = emptyView.findViewById(R.id.textName);
        emptyText.setText("You have no messages");
        userMessagesContainer.addView(emptyView);
    }

    private void addMessagesToContainer(LayoutInflater inflater, Map<String, Message> latestMessagesMap) {
        List<Map.Entry<String, Message>> messageList = new ArrayList<>(latestMessagesMap.entrySet());
        Collections.sort(messageList, (e1, e2) -> Long.compare(e2.getValue().getTimestamp(), e1.getValue().getTimestamp()));

        for (Map.Entry<String, Message> entry : messageList) {
            addMessageItem(entry.getKey(), entry.getValue());
        }
    }

    private void addMessageItem(String otherUserID, Message latestMessage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(otherUserID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User receiver = documentSnapshot.toObject(User.class);
                    if (receiver != null) {
                        addUserMessageToContainer(latestMessage, receiver);
                    }
                });
    }

    private void addUserMessageToContainer(Message message, User receiver) {
        View item = getLayoutInflater().inflate(R.layout.item_user_messages, userMessagesContainer, false);
        populateMessageItem(item, message, receiver);
        userMessagesContainer.addView(item);
    }

    private void populateMessageItem(View item, Message message, User receiver) {
        TextView fullNameText = item.findViewById(R.id.fullNameTextMessage);
        TextView userTextMessage = item.findViewById(R.id.userTextMessage);
        TextView timeLastMessageSent = item.findViewById(R.id.timeLastMessageSent);
        ImageView receiverProfilePicture = item.findViewById(R.id.receiverProfilePictureImageMessages);

        fullNameText.setText(receiver.getFirstName() + " " + receiver.getLastName());
        loadProfilePicture(receiver.getProfilePictureURL(), receiverProfilePicture);
        setMessageContent(message, receiver, userTextMessage);
        timeLastMessageSent.setText(getTimeAgo(message.getTimestamp()));

        setItemLayoutParams(item);

        item.setOnClickListener(v -> navigateToViewMessageActivity(message, receiver));
    }

    private void loadProfilePicture(String profilePictureURL, ImageView receiverProfilePicture) {
        if (profilePictureURL == null || profilePictureURL.isEmpty()) {
            Glide.with(this)
                    .load(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(receiverProfilePicture);
        } else {
            Glide.with(this)
                    .load(profilePictureURL)
                    .placeholder(R.drawable.placeholder_profile)
                    .circleCrop()
                    .into(receiverProfilePicture);
        }
    }

    private void setMessageContent(Message message, User receiver, TextView userTextMessage) {
        if (message.getSenderID().equals(currentUserID)) {
            userTextMessage.setText("You: " + message.getContent());
        } else {
            userTextMessage.setText(receiver.getFirstName() + ": " + message.getContent());
        }
    }

    private void setItemLayoutParams(View item) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (userMessagesContainer.getChildCount() > 0) {
            layoutParams.topMargin = -10; // Set top margin to -10dp for subsequent items
        }
        item.setLayoutParams(layoutParams);
    }

    private void navigateToViewMessageActivity(Message message, User receiver) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("messages")
                .document(message.getMessageID())
                .update("status", "Read");

        Intent intent = new Intent(getActivity(), ViewMessageActivity.class);
        intent.putExtra("userID", receiver.getUserID());
        startActivity(intent);
    }

    private String getTimeAgo(long timeMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timeMillis;

        if (diff < 60 * 1000) {
            return "Just now";
        } else if (diff < 2 * 60 * 1000) {
            return "1 minute ago";
        } else if (diff < 50 * 60 * 1000) {
            return diff / (60 * 1000) + " minutes ago";
        } else if (diff < 90 * 60 * 1000) {
            return "1 hour ago";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return diff / (60 * 60 * 1000) + " hours ago";
        } else if (diff < 48 * 60 * 60 * 1000) {
            return "Yesterday";
        } else {
            return diff / (24 * 60 * 60 * 1000) + " days ago";
        }
    }

    private void filterMessages(String query) {
        Map<String, Message> filteredMessagesMap = new HashMap<>();

        for (User user : usersList) {
            String fullName = user.getFirstName() + " " + user.getLastName();
            if (fullName.toLowerCase().contains(query.toLowerCase())) {
                Message message = latestMessagesMap.get(user.getUserID());
                if (message != null) {
                    filteredMessagesMap.put(user.getUserID(), message);
                }
            }
        }

        displayMessages(filteredMessagesMap);
    }
}
