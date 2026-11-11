package com.code_share.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private Context context;
    private List<Post> posts;
    private OnPostInteractionListener listener;
    private String currentUserId;

    public interface OnPostInteractionListener {
        void onLikeClicked(Post post);
        void onCommentClicked(Post post);
        void onShareClicked(Post post);
        void onMenuClicked(Post post, View anchor);
    }

    public PostAdapter(Context context, List<Post> posts, OnPostInteractionListener listener, String currentUserId) {
        this.context = context;
        this.posts = posts;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        
        // Load User avatar
        if (post.getUserAvatar() != null && !post.getUserAvatar().isEmpty()) {
            Glide.with(context)
                .load(post.getUserAvatar())
                .circleCrop()
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(holder.userAvatar);
        } else {
            // Try to fetch the user's profile picture from the database
            FirebaseDatabase.getInstance().getReference("users")
                .child(post.getUserId())
                .child("photoUrl")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String photoUrl = dataSnapshot.getValue(String.class);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Update the post's avatar URL for future reference
                            post.setUserAvatar(photoUrl);
                            
                            // Load the image
                            Glide.with(context)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.userAvatar);
                        } else {
                            // Load default profile image
                            Glide.with(context)
                                .load(R.drawable.default_profile)
                                .circleCrop()
                                .into(holder.userAvatar);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Load default profile image on error
                        Glide.with(context)
                            .load(R.drawable.default_profile)
                            .circleCrop()
                            .into(holder.userAvatar);
                    }
                });
        }

        // Set User name (in header and description)
        holder.userName.setText(post.getUserName());
        holder.userNameDescription.setText(post.getUserName());

        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(post.getTimestamp()));
        holder.timestamp.setText(formattedDate);

        // Set content
        holder.content.setText(post.getDescription());

        // Handle Post image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(post.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Handle code content
        if (post.getCodeContent() != null && !post.getCodeContent().isEmpty()) {
            holder.codeView.setVisibility(View.VISIBLE);
            setupCodeView(holder.codeView, post.getCodeContent());
            
            // Make the code view clickable to show full code in a dialog
            holder.codeView.setOnClickListener(v -> {
                showCodeDialog(post);
            });
            
            // Add a "View Code" text indicator
            holder.codeViewIndicator.setVisibility(View.VISIBLE);
            holder.codeViewIndicator.setOnClickListener(v -> {
                showCodeDialog(post);
            });
        } else {
            holder.codeView.setVisibility(View.GONE);
            holder.codeViewIndicator.setVisibility(View.GONE);
        }

        // Set counts and like state
        String likeCountText = post.getLikeCount() + " likes";
        holder.likeCount.setText(likeCountText);
        
        // Update like button state
        if (post.isLikedBy(currentUserId)) {
            holder.likeButton.setImageResource(R.drawable.ic_like_filled);
        } else {
            holder.likeButton.setImageResource(R.drawable.ic_like_outline);
        }

        String commentCountText = "View all " + post.getCommentCount() + " comments";
        holder.commentCount.setText(commentCountText);

        // Set click listeners
        holder.likeButton.setOnClickListener(v -> listener.onLikeClicked(post));
        holder.commentButton.setOnClickListener(v -> listener.onCommentClicked(post));
        holder.shareButton.setOnClickListener(v -> listener.onShareClicked(post));
        holder.menuButton.setOnClickListener(v -> listener.onMenuClicked(post, v));
    }

    private void setupCodeView(WebView webView, String code) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Escape the code for JavaScript
        String escapedCode = code
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");

        // Create HTML content with CodeMirror
        String htmlContent = "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<link rel=\"stylesheet\" href=\"file:///android_asset/codemirror.css\">" +
            "<script src=\"file:///android_asset/codemirror.js\"></script>" +
            "<script src=\"file:///android_asset/javascript.js\"></script>" +
            "<style>" +
            "body { margin: 0; padding: 0; background: #1e1e1e; }" +
            ".CodeMirror { height: auto; font-size: 14px; }" +
            ".CodeMirror-scroll { max-height: 300px; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<script>" +
            "var editor = CodeMirror(document.body, {" +
            "  value: \"" + escapedCode + "\"," +
            "  mode: \"javascript\"," +
            "  theme: \"monokai\"," +
            "  readOnly: true," +
            "  lineNumbers: true," +
            "  lineWrapping: true" +
            "});" +
            "</script>" +
            "</body>" +
            "</html>";

        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
    }
    
    /**
     * Shows a dialog with the post's code content using syntax highlighting
     */
    private void showCodeDialog(Post post) {
        // Create a dialog to show the code content
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle("Code by " + post.getUserName())
            .setPositiveButton("Close", null);
        
        // Create a custom view for the dialog
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_view_code, null);
        WebView codeWebView = view.findViewById(R.id.code_web_view);
        TextView creatorInfoTextView = view.findViewById(R.id.creator_info_text_view);
        
        // Configure WebView for code highlighting
        WebSettings webSettings = codeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // Set the creator info
        String creatorInfo = "Posted by: " + post.getUserName() + "\n" +
                           "Date: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(post.getTimestamp()));
        creatorInfoTextView.setText(creatorInfo);
        
        // Prepare code with syntax highlighting using highlight.js
        String code = post.getCodeContent();
        String highlightedCode = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/styles/atom-one-dark.min.css\">\n" +
                "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.7.0/highlight.min.js\"></script>\n" +
                "    <script>hljs.highlightAll();</script>\n" +
                "    <style>\n" +
                "        body { background-color: #2E3440; margin: 12px; }\n" +
                "        pre { margin: 0; }\n" +
                "        code { font-family: 'Courier New', monospace; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <pre><code class=\"python\">" + escapeHtml(code) + "</code></pre>\n" +
                "</body>\n" +
                "</html>";
        
        codeWebView.loadDataWithBaseURL(null, highlightedCode, "text/html", "UTF-8", null);
        
        // Show the dialog
        builder.setView(view).show();
    }
    
    /**
     * Helper method to escape HTML special characters
     */
    private String escapeHtml(String html) {
        if (html == null) return "";
        return html.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updatePosts(List<Post> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView userName;
        TextView userNameDescription;
        TextView timestamp;
        TextView content;
        ImageView postImage;
        WebView codeView;
        TextView codeViewIndicator;
        ImageButton likeButton;
        TextView likeCount;
        ImageButton commentButton;
        TextView commentCount;
        ImageButton shareButton;
        ImageButton menuButton;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            userName = itemView.findViewById(R.id.user_name);
            userNameDescription = itemView.findViewById(R.id.user_name_description);
            timestamp = itemView.findViewById(R.id.post_timestamp);
            content = itemView.findViewById(R.id.post_content);
            postImage = itemView.findViewById(R.id.post_image);
            codeView = itemView.findViewById(R.id.code_view);
            codeViewIndicator = itemView.findViewById(R.id.code_view_indicator);
            likeButton = itemView.findViewById(R.id.like_button);
            likeCount = itemView.findViewById(R.id.like_count);
            commentButton = itemView.findViewById(R.id.comment_button);
            commentCount = itemView.findViewById(R.id.comment_count);
            shareButton = itemView.findViewById(R.id.share_button);
            menuButton = itemView.findViewById(R.id.post_menu);
        }
    }
}
