package com.code_share.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private Context context;
    private List<Friend> friends;
    private OnFriendClickListener listener;
    private OnProfileClickListener profileListener;
    private DatabaseReference usersRef;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }
    
    public interface OnProfileClickListener {
        void onProfileClick(Friend friend);
    }

    public FriendsAdapter(List<Friend> friends, OnFriendClickListener listener) {
        this.friends = friends;
        this.listener = listener;
        this.profileListener = null;
        this.context = null;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }
    
    public FriendsAdapter(List<Friend> friends, OnFriendClickListener listener, OnProfileClickListener profileListener) {
        this.friends = friends;
        this.listener = listener;
        this.profileListener = profileListener;
        this.context = null;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }
    
    public FriendsAdapter(Context context, List<Friend> friends, OnFriendClickListener listener, OnProfileClickListener profileListener) {
        this.context = context;
        this.friends = friends;
        this.listener = listener;
        this.profileListener = profileListener;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.friendName.setText(friend.getName());
        holder.itemView.setOnClickListener(v -> listener.onFriendClick(friend));
        
        // Set up profile image click listener if available
        if (profileListener != null) {
            holder.profileImage.setOnClickListener(v -> profileListener.onProfileClick(friend));
        }
        
        // Set default profile image
        if (context == null) {
            context = holder.itemView.getContext();
        }
        
        // Load profile image if available
        if (friend.getId().equals("ai_assistant")) {
            // Special case for AI Assistant
            Glide.with(context)
                .load(R.drawable.default_profile)
                .circleCrop()
                .into(holder.profileImage);
        } else if (friend.getPhotoUrl() != null && !friend.getPhotoUrl().isEmpty()) {
            // Use cached photo URL
            Glide.with(context)
                .load(friend.getPhotoUrl())
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .circleCrop()
                .into(holder.profileImage);
        } else {
            // Fetch photo URL from database
            usersRef.child(friend.getId()).child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String photoUrl = dataSnapshot.getValue(String.class);
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        friend.setPhotoUrl(photoUrl);
                        Glide.with(context)
                            .load(photoUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .circleCrop()
                            .into(holder.profileImage);
                    } else {
                        Glide.with(context)
                            .load(R.drawable.default_profile)
                            .circleCrop()
                            .into(holder.profileImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Glide.with(context)
                        .load(R.drawable.default_profile)
                        .circleCrop()
                        .into(holder.profileImage);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        CircleImageView profileImage;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friend_name);
            profileImage = itemView.findViewById(R.id.friend_profile_image);
        }
    }
} 





