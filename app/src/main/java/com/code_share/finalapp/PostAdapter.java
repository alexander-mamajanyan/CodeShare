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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private Context context;
    private List<Post> posts;
    private OnPostInteractionListener listener;
    private String currentUserId;
    private final Set<String> expandedCodePosts = new HashSet<>();

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

        // Set user name and post text
        holder.userName.setText(post.getUserName());
        String title = post.getTitle();
        if (title == null || title.trim().isEmpty()) {
            holder.postTitle.setVisibility(View.GONE);
        } else {
            holder.postTitle.setText(title);
            holder.postTitle.setVisibility(View.VISIBLE);
        }

        // Set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(post.getTimestamp()));
        holder.timestamp.setText(formattedDate);

        // Keep legacy post descriptions readable when no title was saved.
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
        String code = post.getCodeContent();
        if (code != null && !code.isEmpty()) {
            String codeKey = getCodePostKey(post);
            boolean expanded = expandedCodePosts.contains(codeKey);
            int lineCount = code.split("\n", -1).length;
            boolean expandable = lineCount > 7 || code.length() > 600;
            holder.codeView.setVisibility(View.VISIBLE);
            setupCodeView(holder.codeView, code);
            setCodeViewHeight(holder.codeView, lineCount, expanded);
            holder.codeViewIndicator.setVisibility(expandable ? View.VISIBLE : View.GONE);
            holder.codeViewIndicator.setText(expanded ? "Show less" : "Expand code");
            holder.codeViewIndicator.setOnClickListener(v -> {
                if (expandedCodePosts.contains(codeKey)) {
                    expandedCodePosts.remove(codeKey);
                } else {
                    expandedCodePosts.add(codeKey);
                }
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(currentPosition);
                }
            });
        } else {
            holder.codeView.setVisibility(View.GONE);
            holder.codeViewIndicator.setVisibility(View.GONE);
            holder.codeViewIndicator.setOnClickListener(null);
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
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(false);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        String htmlContent = "<!DOCTYPE html>" +
            "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<style>html,body{margin:0;background:#0B1020;color:#F5F7FF}" +
            "pre{box-sizing:border-box;margin:0;padding:14px;overflow:auto;white-space:pre;" +
            "font:13px/21px monospace}code{font:inherit}.tok-keyword{color:#C792EA;font-weight:600}" +
            ".tok-string{color:#C3E88D}.tok-number{color:#F78C6C}" +
            ".tok-comment{color:#7886A3;font-style:italic}.tok-variableName{color:#82AAFF}" +
            ".tok-definition{color:#82AAFF;font-weight:600}.tok-typeName{color:#FFCB6B}" +
            ".tok-operator{color:#89DDFF}.tok-punctuation{color:#A7B1C8}" +
            ".tok-bool{color:#F78C6C}</style></head><body><pre><code>"
            + escapeHtml(code) +
            "</code></pre><script src=\"file:///android_asset/code-editor.js\"></script>" +
            "<script>if(window.highlightPythonCodeBlocks)" +
            "window.highlightPythonCodeBlocks(document)</script></body></html>";

        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
    }

    private void setCodeViewHeight(WebView webView, int lineCount, boolean expanded) {
        int density = context.getResources().getDisplayMetrics().densityDpi;
        int collapsedHeight = 176 * density / 160;
        int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.6f);
        int expandedHeight = Math.min(maxHeight, Math.max(collapsedHeight,
                (lineCount * 21 + 28) * density / 160));
        ViewGroup.LayoutParams params = webView.getLayoutParams();
        params.height = expanded ? expandedHeight : collapsedHeight;
        webView.setLayoutParams(params);
    }

    private String getCodePostKey(Post post) {
        if (post.getId() != null && !post.getId().isEmpty()) {
            return post.getId();
        }
        return post.getUserId() + "_" + post.getTimestamp();
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
        TextView postTitle;
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
            postTitle = itemView.findViewById(R.id.post_title);
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
