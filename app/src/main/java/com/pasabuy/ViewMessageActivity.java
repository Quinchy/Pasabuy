package com.pasabuy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pasabuy.adapters.MessageAdapter;
import com.pasabuy.models.Message;
import com.pasabuy.viewmodels.ViewMessageViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ViewMessageActivity extends AppCompatActivity {

    private static final String TAG = "ViewMessageActivity";
    private ViewMessageViewModel viewModel;
    private MessageAdapter messageAdapter;
    private String chatUserID;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        setTheme(R.style.ViewPostActivityTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_message);

        viewModel = new ViewModelProvider(this).get(ViewMessageViewModel.class);
        chatUserID = getIntent().getStringExtra("userID");
        currentUserID = getCurrentUserID();
        Log.d(TAG, "Chat User ID: " + chatUserID);

        if (chatUserID.equals(currentUserID)) {
            finish();
            return;
        }

        setupRecyclerView();
        setupBackButton();
        setupFullNameTextView();
        setupSendMessage();

        viewModel.getMessagesLiveData(chatUserID).observe(this, messages -> {
            messageAdapter.setMessages(messages);
            scrollToBottom();
            Log.d(TAG, "Messages updated in RecyclerView");
        });

        loadMessagesFromFirestore();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.viewMessageMessageContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(this, currentUserID, chatUserID);
        recyclerView.setAdapter(messageAdapter);
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButtonViewMessage);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            finish();
        });
    }

    private void setupFullNameTextView() {
        TextView fullNameTextView = findViewById(R.id.viewMessageText);
        fetchFullName(chatUserID, fullNameTextView);
    }

    private void setupSendMessage() {
        EditText messageInput = findViewById(R.id.messageInput);
        ImageButton sendMessageButton = findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(v -> {
            String content = messageInput.getText().toString();
            if (!content.isEmpty()) {
                Log.d(TAG, "Send button clicked with message content: " + content);
                viewModel.sendMessage(chatUserID, content);
                messageInput.setText("");
            }
        });
    }

    private void fetchFullName(String userID, TextView fullNameTextView) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.collection("users").document(userID);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String fullName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");
                fullNameTextView.setText(fullName);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching user details", e));
    }

    private void loadMessagesFromFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        List<Message> allMessages = new ArrayList<>();

        fetchMessages(firestore, "senderID", currentUserID, "receiverID", chatUserID, allMessages);
        fetchMessages(firestore, "senderID", chatUserID, "receiverID", currentUserID, allMessages);
    }

    private void fetchMessages(FirebaseFirestore firestore, String senderField, String senderID, String receiverField, String receiverID, List<Message> allMessages) {
        firestore.collection("messages")
                .whereEqualTo(senderField, senderID)
                .whereEqualTo(receiverField, receiverID)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allMessages.addAll(task.getResult().toObjects(Message.class));
                        mergeAndSortMessages(allMessages);
                        messageAdapter.setMessages(allMessages);
                        Log.d(TAG, "Messages loaded from Firestore where " + senderField + " is " + senderID);
                    } else {
                        Log.e(TAG, "Error loading messages from Firestore", task.getException());
                    }
                });
    }

    private void mergeAndSortMessages(List<Message> messages) {
        messages.sort(Comparator.comparingLong(Message::getTimestamp));
    }

    private void scrollToBottom() {
        RecyclerView recyclerView = findViewById(R.id.viewMessageMessageContainer);
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private String getCurrentUserID() {
        SharedPreferences sharedPreferences = getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userID", null);
    }
}
