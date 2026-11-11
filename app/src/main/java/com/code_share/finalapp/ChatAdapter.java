package com.code_share.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages = new ArrayList<>();

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.senderText.setText(message.getSender());
        holder.messageText.setText(message.getMessage());

        // Align AI messages to the left, User messages to the right
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.messageText.getLayoutParams();
        if (message.isAI()) {
            params.leftMargin = 0;
            params.rightMargin = holder.itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.chat_message_margin);
        } else {
            params.leftMargin = holder.itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.chat_message_margin);
            params.rightMargin = 0;
        }
        holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderText;
        TextView messageText;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.sender_text);
            messageText = itemView.findViewById(R.id.message_text);
        }
    }
} 




