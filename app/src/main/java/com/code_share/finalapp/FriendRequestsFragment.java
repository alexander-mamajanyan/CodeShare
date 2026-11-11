package com.code_share.finalapp;

import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestsFragment extends Fragment {
    private RecyclerView requestsList;
    private TextView emptyText;
    private FriendRequestsAdapter adapter;
    private List<FriendRequest> friendRequests;
    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        friendRequests = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);
        
        requestsList = view.findViewById(R.id.requests_list);
        emptyText = view.findViewById(R.id.empty_text);
        
        adapter = new FriendRequestsAdapter(friendRequests, this::onRequestAccepted, this::onRequestRejected);
        requestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsList.setAdapter(adapter);
        
        loadFriendRequests();
        
        return view;
    }

    private void loadFriendRequests() {
        if (currentUser != null) {
            databaseRef.child("users").child(currentUser.getUid()).child("friendRequests")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            friendRequests.clear();
                            for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                                String senderId = requestSnapshot.getKey();
                                FriendRequest request = requestSnapshot.getValue(FriendRequest.class);
                                if (request != null && senderId != null) {
                                    request.setId(senderId); // Ensure the ID is set correctly
                                    friendRequests.add(request);
                                    System.out.println("Added friend request from: " + request.getName());
                                }
                            }
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            System.out.println("Error loading friend requests: " + databaseError.getMessage());
                            Toast.makeText(getContext(), R.string.error_loading_friends, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void onRequestAccepted(FriendRequest request) {
        if (currentUser == null) return;
        
        String friendId = request.getId();
        String friendName = request.getName();
        
        System.out.println("Accepting friend request from: " + friendName + " (ID: " + friendId + ")");
        
        // Add to friends list for both users
        databaseRef.child("users").child(currentUser.getUid())
                .child("friends").child(friendId)
                .setValue(friendName)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Added friend to current ru.myitschool.finalproject.User's list");
                    databaseRef.child("users").child(friendId)
                            .child("friends").child(currentUser.getUid())
                            .setValue(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous")
                            .addOnSuccessListener(aVoid1 -> {
                                System.out.println("Added current ru.myitschool.finalproject.User to friend's list");
                                // Remove friend request
                                databaseRef.child("users").child(currentUser.getUid())
                                        .child("friendRequests").child(friendId)
                                        .removeValue()
                                        .addOnSuccessListener(aVoid2 -> {
                                            System.out.println("Removed friend request");
                                            Toast.makeText(getContext(), R.string.friend_added, Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            System.out.println("Error removing friend request: " + e.getMessage());
                                            Toast.makeText(getContext(), R.string.error_accepting_request, Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                System.out.println("Error adding friend to other ru.myitschool.finalproject.User: " + e.getMessage());
                                Toast.makeText(getContext(), R.string.error_accepting_request, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error adding friend: " + e.getMessage());
                    Toast.makeText(getContext(), R.string.error_accepting_request, Toast.LENGTH_SHORT).show();
                });
    }

    private void onRequestRejected(FriendRequest request) {
        if (currentUser == null) return;
        
        String friendId = request.getId();
        System.out.println("Rejecting friend request from: " + request.getName() + " (ID: " + friendId + ")");
        
        // Remove friend request
        databaseRef.child("users").child(currentUser.getUid())
                .child("friendRequests").child(friendId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Successfully rejected friend request");
                    Toast.makeText(getContext(), R.string.request_rejected, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error rejecting friend request: " + e.getMessage());
                    Toast.makeText(getContext(), R.string.error_rejecting_request, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (friendRequests.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            requestsList.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            requestsList.setVisibility(View.VISIBLE);
        }
    }
} 




