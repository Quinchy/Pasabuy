package com.pasabuy.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pasabuy.models.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final FirebaseFirestore db;

    public MessageViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Message>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public void loadMessages(String currentUserID) {
        List<Message> messages = new ArrayList<>();
        fetchMessages(currentUserID, "senderID", messages);
        fetchMessages(currentUserID, "receiverID", messages);
    }

    private void fetchMessages(String userID, String field, List<Message> messages) {
        db.collection("messages")
        .whereEqualTo(field, userID)
        .get()
        .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    messages.add(document.toObject(Message.class));
                }
                messagesLiveData.setValue(messages);
            }
        });
    }
}
