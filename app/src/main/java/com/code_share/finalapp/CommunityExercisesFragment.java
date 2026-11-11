package com.code_share.finalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CommunityExercisesFragment extends Fragment {
    private static final String TAG = "CommunityExercisesFrag";
    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private List<Exercise> exercises;
    private DatabaseReference communityExercisesRef;
    private MaterialButton pendingExercisesButton;
    private String currentUserId;
    private boolean isAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community_exercises, container, false);
        recyclerView = view.findViewById(R.id.exercise_list);
        pendingExercisesButton = view.findViewById(R.id.pending_exercises_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        exercises = new ArrayList<>();
        adapter = new ExerciseAdapter(exercises, this::onExerciseClick);
        recyclerView.setAdapter(adapter);
        
        // Initialize Firebase references
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to view exercises", Toast.LENGTH_SHORT).show();
            return view;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        communityExercisesRef = FirebaseDatabase.getInstance().getReference("community_exercises");
        
        // Check if user is admin
        checkAdminStatus();
        
        // Set up button click listeners
        pendingExercisesButton.setOnClickListener(v -> {
            if (isAdmin) {
                PendingExercisesFragment fragment = new PendingExercisesFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Only admins can access pending exercises", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Load approved exercises
        loadCommunityExercises();
        
        return view;
    }

    private void checkAdminStatus() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean admin = dataSnapshot.getValue(Boolean.class);
                isAdmin = admin != null && admin;
                
                // Show/hide pending exercises button based on admin status
                pendingExercisesButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to check admin status: " + databaseError.getMessage());
            }
        });
    }
    
    private void loadCommunityExercises() {
        communityExercisesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                exercises.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Exercise exercise = snapshot.getValue(Exercise.class);
                    if (exercise != null && exercise.isApproved()) {
                        exercises.add(exercise);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load exercises: " + databaseError.getMessage());
            }
        });
    }


    private void onExerciseClick(Exercise exercise) {
        ExerciseWorkspaceFragment fragment = new ExerciseWorkspaceFragment();
        Bundle args = new Bundle();
        args.putParcelable("exercise", exercise);
        fragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
} 



