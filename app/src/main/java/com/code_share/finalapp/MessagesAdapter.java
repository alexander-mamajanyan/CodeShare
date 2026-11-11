package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TEXT = 0;
    private static final int VIEW_TYPE_CODE = 1;
    
    private List<Message> messages;
    private String currentUserId;

    public MessagesAdapter(List<Message> messages) {
        this.messages = messages;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = currentUser != null ? currentUser.getUid() : "";
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return "code".equals(message.getType()) ? VIEW_TYPE_CODE : VIEW_TYPE_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_CODE) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_code_message, parent, false);
            return new CodeMessageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false);
            return new TextMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        
        if (holder instanceof TextMessageViewHolder) {
            TextMessageViewHolder textHolder = (TextMessageViewHolder) holder;
            textHolder.messageText.setText(message.getText());
            textHolder.timeText.setText(message.getFormattedTime());
            setupMessageLayout(textHolder.messageContainer, message);
        } else if (holder instanceof CodeMessageViewHolder) {
            CodeMessageViewHolder codeHolder = (CodeMessageViewHolder) holder;
            codeHolder.exerciseTitle.setText("📝 Exercise: " + message.getText());
            codeHolder.timeText.setText(message.getFormattedTime());
            setupMessageLayout(codeHolder.messageContainer, message);
            
            // Set up WebView for code display
            WebSettings webSettings = codeHolder.codeView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setDefaultTextEncodingName("utf-8");
            
            // Set transparent background
            codeHolder.codeView.setBackgroundColor(0x00000000);
            
            // Load the HTML file
            codeHolder.codeView.loadUrl("file:///android_asset/code_message.html");
            
            // Wait for the page to load before setting the code
            codeHolder.codeView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // Escape the code properly for JavaScript
                    String escapedCode = message.getCode()
                        .replace("\\", "\\\\")
                        .replace("`", "\\`")
                        .replace("$", "\\$");
                    
                    // Set the code in the WebView
                    view.evaluateJavascript(
                        "initEditor(`" + escapedCode + "`);",
                        null
                    );
                }
            });
            
            // Make code Message clickable
            codeHolder.messageContainer.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("title", message.getExerciseId());
                bundle.putString("code", message.getCode());
                
                CodeEditorFragment codeEditorFragment = new CodeEditorFragment();
                codeEditorFragment.setArguments(bundle);
                
                Navigation.findNavController(v).navigate(R.id.codeEditorFragment);
            });
        }
    }

    private void setupMessageLayout(ViewGroup container, Message message) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        
        if (message.getSenderId().equals(currentUserId)) {
            container.setBackgroundResource(R.drawable.message_sent_background);
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.setMarginStart(100);
            params.setMarginEnd(8);
        } else {
            container.setBackgroundResource(R.drawable.message_received_background);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.setMarginStart(8);
            params.setMarginEnd(100);
        }
        
        container.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        ViewGroup messageContainer;

        TextMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            messageContainer = itemView.findViewById(R.id.message_container);
        }
    }

    static class CodeMessageViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseTitle;
        TextView timeText;
        WebView codeView;
        ViewGroup messageContainer;

        CodeMessageViewHolder(View itemView) {
            super(itemView);
            exerciseTitle = itemView.findViewById(R.id.exercise_title);
            timeText = itemView.findViewById(R.id.time_text);
            codeView = itemView.findViewById(R.id.code_view);
            messageContainer = itemView.findViewById(R.id.message_container);
        }
    }
}
