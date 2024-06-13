package com.pasabuy.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pasabuy.R;
import com.pasabuy.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENDER = 1;
    private static final int VIEW_TYPE_RECEIVER = 2;

    private final Context context;
    private final String currentUserID;
    private List<Message> messages;

    public MessageAdapter(Context context, String currentUserID, String chatUserID) {
        this.context = context;
        this.currentUserID = currentUserID;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.getSenderID().equals(currentUserID) ? VIEW_TYPE_SENDER : VIEW_TYPE_RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_TYPE_SENDER ? R.layout.item_your_message : R.layout.item_sender_message;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return viewType == VIEW_TYPE_SENDER ? new SenderViewHolder(view) : new ReceiverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENDER) {
            ((SenderViewHolder) holder).bind(message);
        } else {
            ((ReceiverViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    class SenderViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;

        SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.your_message_text);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
        }
    }

    class ReceiverViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final ImageView senderAvatar;

        ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.sender_message_text);
            senderAvatar = itemView.findViewById(R.id.sender_avatar);
        }

        void bind(Message message) {
            messageText.setText(message.getContent());
            loadSenderProfilePicture(message.getSenderID(), senderAvatar);
        }

        private void loadSenderProfilePicture(String senderID, ImageView imageView) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference userRef = firestore.collection("users").document(senderID);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profilePictureURL = documentSnapshot.getString("profilePictureURL");
                    Glide.with(context)
                            .load(profilePictureURL == null || profilePictureURL.isEmpty() ? R.drawable.placeholder_profile : profilePictureURL)
                            .placeholder(R.drawable.placeholder_profile)
                            .circleCrop()
                            .into(imageView);
                }
            }).addOnFailureListener(e -> Log.e("MessageAdapter", "Error fetching user details", e));
        }
    }
}
