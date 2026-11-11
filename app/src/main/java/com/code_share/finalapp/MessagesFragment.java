package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class MessagesFragment extends Fragment {

    private RecyclerView friendsList;
    private RecyclerView requestsList;
    private FloatingActionButton addFriendFab;
    private LinearLayout requestsContainer;

    private DatabaseReference databaseRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private List<Friend> friends = new ArrayList<>();
    private List<FriendRequest> friendRequests = new ArrayList<>();
    private FriendsAdapter friendsAdapter;
    private FriendRequestsAdapter requestsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Debug information
        if (currentUser != null) {
            System.out.println("Current User ID: " + currentUser.getUid());
            System.out.println("Current User display name: " + currentUser.getDisplayName());
            System.out.println("Current User email: " + currentUser.getEmail());
            
            // Check if display name is set
            if (currentUser.getDisplayName() == null || currentUser.getDisplayName().isEmpty()) {
                System.out.println("WARNING: Display name is not set for current User");
            }
        } else {
            System.out.println("ERROR: No User is currently logged in");
        }

        // Initialize views
        friendsList = view.findViewById(R.id.friends_list);
        addFriendFab = view.findViewById(R.id.add_friend_fab);
        requestsContainer = view.findViewById(R.id.requests_container);
        requestsList = view.findViewById(R.id.requests_list);

        // Set up RecyclerViews
        friendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        
        friendsAdapter = new FriendsAdapter(getContext(), friends, this::onFriendClick, this::onProfileClick);
        requestsAdapter = new FriendRequestsAdapter(friendRequests, this::onRequestAccepted, this::onRequestRejected);
        
        friendsList.setAdapter(friendsAdapter);
        requestsList.setAdapter(requestsAdapter);

        // Set up add friend FAB
        addFriendFab.setOnClickListener(v -> showAddFriendDialog());

        // Load friends list and friend requests
        loadFriends();
        loadFriendRequests();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Add a button to view friend requests
        view.findViewById(R.id.view_requests_button).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,  // enter
                        R.anim.slide_out_left,   // exit
                        R.anim.slide_in_left,    // popEnter
                        R.anim.slide_out_right   // popExit
                    )
                    .replace(R.id.fragment_container, new FriendRequestsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void showAddFriendDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_friend, null);
        TextInputEditText friendUsernameInput = dialogView.findViewById(R.id.friend_username_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_friend)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String friendUsername = friendUsernameInput.getText().toString().trim();
                    if (!friendUsername.isEmpty()) {
                        sendFriendRequest(friendUsername);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void sendFriendRequest(String friendUsername) {
        System.out.println("Searching for User with username: " + friendUsername);
        // Find User by username
        databaseRef.child("users").orderByChild("username").equalTo(friendUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        System.out.println("Search result exists: " + dataSnapshot.exists());
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                String friendId = userSnapshot.getKey();
                                System.out.println("Found User with ID: " + friendId);
                                
                                if (friendId != null && !friendId.equals(currentUser.getUid())) {
                                    // Check if already friends
                                    databaseRef.child("users").child(currentUser.getUid())
                                            .child("friends").child(friendId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    if (snapshot.exists()) {
                                                        Toast.makeText(getContext(), R.string.already_friends, Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    
                                                    // Check if request already exists
                                                    databaseRef.child("users").child(friendId)
                                                            .child("friendRequests").child(currentUser.getUid())
                                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                    if (snapshot.exists()) {
                                                                        Toast.makeText(getContext(), R.string.request_already_sent, Toast.LENGTH_SHORT).show();
                                                                        return;
                                                                    }
                                                                    
                                                                    // Create friend request object
                                                                    String requestId = databaseRef.child("users").child(friendId).child("friendRequests").push().getKey();
                                                                    FriendRequest request = new FriendRequest(
                                                                            requestId,
                                                                            currentUser.getUid(),
                                                                            currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous"
                                                                    );
                                                                    
                                                                    // Send friend request
                                                                    databaseRef.child("users").child(friendId)
                                                                            .child("friendRequests").child(currentUser.getUid())
                                                                            .setValue(request)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                System.out.println("Friend request sent successfully");
                                                                                Toast.makeText(getContext(), R.string.friend_request_sent, Toast.LENGTH_SHORT).show();
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                System.out.println("Failed to send friend request: " + e.getMessage());
                                                                                Toast.makeText(getContext(), R.string.error_adding_friend, Toast.LENGTH_SHORT).show();
                                                                            });
                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError error) {
                                                                    System.out.println("Error checking existing request: " + error.getMessage());
                                                                    Toast.makeText(getContext(), R.string.error_adding_friend, Toast.LENGTH_SHORT).show();
                                                                }
                                                            }); // End of Listener 3 - Semicolon added here
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    System.out.println("Error checking friendship: " + error.getMessage());
                                                    Toast.makeText(getContext(), R.string.error_adding_friend, Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    return;
                                }
                            }
                        }
                        System.out.println("User not found with username: " + friendUsername);
                        Toast.makeText(getContext(), R.string.user_not_found, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        System.out.println("Database error while searching for User: " + databaseError.getMessage());
                        Toast.makeText(getContext(), R.string.error_adding_friend, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFriendRequests() {
        if (currentUser != null) {
            String path = "users/" + currentUser.getUid() + "/friendRequests";
            System.out.println("Loading friend requests from path: " + path);
            
            databaseRef.child("users").child(currentUser.getUid()).child("friendRequests")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            System.out.println("Friend requests data changed. Exists: " + dataSnapshot.exists() + ", Children count: " + dataSnapshot.getChildrenCount());
                            
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
                            System.out.println("Total friend requests loaded: " + friendRequests.size());
                            
                            requestsAdapter.notifyDataSetChanged();
                            requestsContainer.setVisibility(friendRequests.isEmpty() ? View.GONE : View.VISIBLE);
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
        String friendId = request.getSenderId();
        String friendName = request.getName();
        
        // Add to friends list - store only the name as String, not the whole Friend object
        databaseRef.child("users").child(currentUser.getUid())
                .child("friends").child(friendId)
                .setValue(friendName)
                .addOnSuccessListener(aVoid -> {
                    // Also add current user to friend's friends list
                    String currentUserName = currentUser.getDisplayName();
                    if (currentUserName == null || currentUserName.isEmpty()) {
                        currentUserName = "User"; // Fallback name if display name is not set
                    }
                    
                    databaseRef.child("users").child(friendId)
                            .child("friends").child(currentUser.getUid())
                            .setValue(currentUserName);
                    
                    // Delete the request
                    databaseRef.child("users").child(currentUser.getUid())
                            .child("friendRequests").child(request.getId())
                            .removeValue();
                    
                    Toast.makeText(getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                    
                    // Update UI
                    Friend newFriend = new Friend(friendId, friendName);
                    friends.add(newFriend);
                    friendsAdapter.notifyDataSetChanged();
                    friendRequests.remove(request);
                    requestsAdapter.notifyDataSetChanged();
                    
                    // Hide container if no requests
                    if (friendRequests.isEmpty()) {
                        requestsContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error accepting request", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void onRequestRejected(FriendRequest request) {
        // Delete the request
        databaseRef.child("users").child(currentUser.getUid())
                .child("friendRequests").child(request.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
                    // Remove from local list
                    friendRequests.remove(request);
                    requestsAdapter.notifyDataSetChanged();
                    
                    // Hide container if no requests
                    if (friendRequests.isEmpty()) {
                        requestsContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error rejecting request", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFriends() {
        if (currentUser != null) {
            databaseRef.child("users").child(currentUser.getUid()).child("friends")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // Keep AI Assistant as first friend
                            friends.clear();
                            friends.add(new Friend("ai_assistant", "AI Assistant"));
                            
                            for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                                String friendId = friendSnapshot.getKey();
                                String friendName = friendSnapshot.getValue(String.class);
                                if (friendId != null && friendName != null) {
                                    // Fetch friend's profile picture
                                    databaseRef.child("users").child(friendId).child("photoUrl")
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot photoSnapshot) {
                                                    String photoUrl = photoSnapshot.getValue(String.class);
                                                    Friend friend = new Friend(friendId, friendName, photoUrl);
                                                    friends.add(friend);
                                                    friendsAdapter.notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    // Add friend without photo
                                                    friends.add(new Friend(friendId, friendName));
                                                    friendsAdapter.notifyDataSetChanged();
                                                }
                                            });
                                }
                            }
                            friendsAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getContext(), R.string.error_loading_friends, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void onFriendClick(Friend friend) {
        if (friend.getId().equals("ai_assistant")) {
            // Open chat with AI Assistant
            ChatFragment chatFragment = ChatFragment.newInstance("ai_assistant", "AI Assistant");
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
        } else {
            // Open chat with regular friend
            ChatFragment chatFragment = ChatFragment.newInstance(friend.getId(), friend.getName());
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    /**
     * Handle click on a friend's profile image to view their profile
     */
    private void onProfileClick(Friend friend) {
        // Don't open profile for AI Assistant
        if (friend.getId().equals("ai_assistant")) {
            Toast.makeText(getContext(), "AI Assistant doesn't have a profile", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Open the other user's profile
        OtherProfileFragment fragment = OtherProfileFragment.newInstance(friend.getId());
        getParentFragmentManager().beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    // Using imported Friend model class

    // Using imported FriendRequest model class
}
