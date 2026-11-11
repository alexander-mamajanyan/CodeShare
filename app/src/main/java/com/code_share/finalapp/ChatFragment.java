package com.code_share.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView messagesList;
    private TextInputEditText messageInput;
    private MaterialButton sendButton;
    private TextView chatFriendName;
    private TextView chatFriendStatus;

    private DatabaseReference databaseRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String friendId;
    private String friendName;

    private List<Message> messages = new ArrayList<>();
    private MessagesAdapter messagesAdapter;

    public static ChatFragment newInstance(String friendId, String friendName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("friendId", friendId);
        args.putString("friendName", friendName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get friend details from arguments
        if (getArguments() != null) {
            friendId = getArguments().getString("friendId");
            friendName = getArguments().getString("friendName");
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        messagesList = view.findViewById(R.id.messages_list);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        chatFriendName = view.findViewById(R.id.chat_friend_name);
        chatFriendStatus = view.findViewById(R.id.chat_friend_status);

        // Set up back button
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        // Set friend name
        chatFriendName.setText(friendName);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);
        
        messagesAdapter = new MessagesAdapter(messages);
        messagesList.setAdapter(messagesAdapter);

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();

        return view;
    }

    private void loadMessages() {
        if (currentUser != null && friendId != null) {
            String conversationId = getConversationId(currentUser.getUid(), friendId);
            databaseRef.child("conversations").child(conversationId).child("messages")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            messages.clear();
                            for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                Message message = messageSnapshot.getValue(Message.class);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }
                            messagesAdapter.updateMessages(messages);
                            messagesList.scrollToPosition(messages.size() - 1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getContext(), R.string.error_loading_messages, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void sendMessage() {
        if (messageInput.getText() == null) return;

        String messageText = messageInput.getText().toString().trim();
        if (!TextUtils.isEmpty(messageText)) {
            if (friendId.equals("ai_assistant")) {
                // Handle AI messages
                String conversationId = getConversationId(currentUser.getUid(), "ai_assistant");
                
                // Save Message to Firebase
                Message userMessage = new Message(messageText, currentUser.getUid());
                databaseRef.child("conversations").child(conversationId).child("messages")
                    .push().setValue(userMessage);
                
                messageInput.setText("");

                // Get code from Message if it's a code Message
                String codeToAnalyze = "";
                if (messages.size() > 1) {
                    Message lastMessage = messages.get(messages.size() - 2);
                    if (lastMessage.getType() != null && lastMessage.getType().equals("code")) {
                        codeToAnalyze = lastMessage.getCode();
                    }
                }

                // Show typing indicator
                chatFriendStatus.setText("Typing...");
                
                // Get AI response
                AIAssistant aiAssistant = new AIAssistant();
                aiAssistant.getCodeAssistance(codeToAnalyze, messageText, response -> {
                    requireActivity().runOnUiThread(() -> {
                        // Save AI response to Firebase
                        Message aiMessage = new Message(response, "ai_assistant");
                        databaseRef.child("conversations").child(conversationId).child("messages")
                            .push().setValue(aiMessage)
                            .addOnSuccessListener(aVoid -> {
                                chatFriendStatus.setText("Online");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), R.string.error_sending_message, Toast.LENGTH_SHORT).show();
                                chatFriendStatus.setText("Online");
                            });
                    });
                });
            } else {
                // Regular chat Message
                String conversationId = getConversationId(currentUser.getUid(), friendId);
                Message newMessage = new Message(messageText, currentUser.getUid());
                
                databaseRef.child("conversations").child(conversationId).child("messages")
                    .push().setValue(newMessage)
                    .addOnSuccessListener(aVoid -> {
                        messageInput.setText("");
                        messagesList.scrollToPosition(messages.size() - 1);
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(getContext(), R.string.error_sending_message, Toast.LENGTH_SHORT).show()
                    );
            }
        }
    }

    private String getConversationId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}
