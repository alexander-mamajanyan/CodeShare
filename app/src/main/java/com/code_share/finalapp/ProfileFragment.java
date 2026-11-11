package com.code_share.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private TextView usernameText;
    private TextView emailText;
    private TextView scoreText;
    private TextView followersCount;
    private TextView followingCount;
    private FloatingActionButton settingsFab;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return view;
        }

        String userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize views
        usernameText = view.findViewById(R.id.username_text);
        emailText = view.findViewById(R.id.email_text);
        scoreText = view.findViewById(R.id.score_text);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        MaterialButton settingsButton = view.findViewById(R.id.settings_button);
        MaterialButton leaderboardsButton = view.findViewById(R.id.leaderboards_button);

        // Set up settings button click listener
        settingsButton.setOnClickListener(v -> {
            SettingsFragment settingsFragment = new SettingsFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)
                    .commit();
        });
        
        // Set up leaderboards button click listener
        leaderboardsButton.setOnClickListener(v -> {
            LeaderboardFragment leaderboardFragment = new LeaderboardFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, leaderboardFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Load user data
        loadUserData();

        return view;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            emailText.setText(email != null ? email : "No email available");
            
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    Integer score = dataSnapshot.child("totalPoints").getValue(Integer.class);
                    String photoUrl = dataSnapshot.child("photoUrl").getValue(String.class);
                    
                    // Check admin status
                    Boolean isAdmin = dataSnapshot.child("isAdmin").getValue(Boolean.class);
                    Boolean isSuperAdmin = dataSnapshot.child("isSuperAdmin").getValue(Boolean.class);
                    boolean isAdminValue = isAdmin != null && isAdmin;
                    boolean isSuperAdminValue = isSuperAdmin != null && isSuperAdmin;
                    
                    // Set up admin profile image
                    View adminProfileLayout = getView().findViewById(R.id.admin_profile_layout);
                    AdminProfileHelper.setupAdminProfileImage(adminProfileLayout, isAdminValue, isSuperAdminValue, photoUrl);
                    
                    if (username != null) {
                        usernameText.setText(username);
                    }
                    
                    if (score != null) {
                        scoreText.setText("Score: " + score);
                    }
                    
                    // Get followers and following counts
                    if (dataSnapshot.hasChild("followers")) {
                        long followers = dataSnapshot.child("followers").getChildrenCount();
                        followersCount.setText(String.valueOf(followers));
                    } else {
                        followersCount.setText("0");
                    }
                    
                    if (dataSnapshot.hasChild("following")) {
                        long following = dataSnapshot.child("following").getChildrenCount();
                        followingCount.setText(String.valueOf(following));
                    } else {
                        followingCount.setText("0");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
} 