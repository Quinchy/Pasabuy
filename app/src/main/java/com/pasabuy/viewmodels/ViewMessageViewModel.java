package com.pasabuy.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.models.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewMessageViewModel extends AndroidViewModel {

    private static final String TAG = "ViewMessageViewModel";
    private final DatabaseReference messagesRef;
    private final MutableLiveData<List<Message>> messagesLiveData;
    private final String currentUserID;

    public ViewMessageViewModel(@NonNull Application application) {
        super(application);
        messagesLiveData = new MutableLiveData<>();
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");

        SharedPreferences sharedPreferences = application.getSharedPreferences("PasabuyApp", Context.MODE_PRIVATE);
        currentUserID = sharedPreferences.getString("userID", null);
        Log.d(TAG, "Current User ID: " + currentUserID);
    }

    public LiveData<List<Message>> getMessagesLiveData(String chatUserID) {
        Log.d(TAG, "Getting messages for chat user ID: " + chatUserID);
        loadMessages(chatUserID);
        return messagesLiveData;
    }

    private void loadMessages(String chatUserID) {
        messagesRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null && ((message.getSenderID().equals(currentUserID) && message.getReceiverID().equals(chatUserID)) ||
                            (message.getSenderID().equals(chatUserID) && message.getReceiverID().equals(currentUserID)))) {
                        messages.add(message);
                    }
                }
                // Sort messages by timestamp
                Collections.sort(messages, Comparator.comparingLong(Message::getTimestamp));
                messagesLiveData.setValue(messages);
                Log.d(TAG, "Messages loaded and sorted: " + messages.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load messages", databaseError.toException());
            }
        });
    }

    public void sendMessage(String chatUserID, String content) {
        Log.d(TAG, "Sending message to chat user ID: " + chatUserID + " with content: " + content);
        String messageID = messagesRef.push().getKey();
        if (messageID != null) {
            long timestamp = System.currentTimeMillis();
            Message message = new Message(messageID, currentUserID, chatUserID, content, timestamp, "sent");

            // Save to Firestore
            FirebaseFirestore.getInstance().collection("messages").document(messageID).set(message);

            // Save to Realtime Database
            messagesRef.child(messageID).setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Message sent successfully");
                } else {
                    Log.e(TAG, "Failed to send message", task.getException());
                }
            });
        }
    }
}
