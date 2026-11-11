package com.code_share.finalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateExerciseFragment extends Fragment {
    private static final String TAG = "CreateExerciseFrag";
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private WebView initialCodeEditor;
    private TextInputEditText testInput;
    private TextInputEditText expectedOutput;
    private TextInputEditText hintsInput;
    private MaterialButton createButton;
    private DatabaseReference pendingExercisesRef;
    private DatabaseReference userRef;
    private DatabaseReference userScoreRef;
    private static final int EXERCISE_COST = 10;
    private WebAppInterface initialCodeInterface;
    private String currentUserId;
    private String currentUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_exercise, container, false);

        // Initialize Firebase references
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to create an exercise", Toast.LENGTH_SHORT).show();
            return view;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pendingExercisesRef = FirebaseDatabase.getInstance().getReference("pending_exercises");
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        userScoreRef = userRef.child("totalPoints");

        // Get current username
        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUsername = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get username: " + databaseError.getMessage());
            }
        });

        // Initialize views
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        initialCodeEditor = view.findViewById(R.id.initial_code_editor);
        testInput = view.findViewById(R.id.test_input);
        expectedOutput = view.findViewById(R.id.expected_output);
        hintsInput = view.findViewById(R.id.hints_input);
        createButton = view.findViewById(R.id.create_button);

        // Initialize ru.myitschool.finalproject.WebAppInterface
        initialCodeInterface = new WebAppInterface(requireContext());

        // Set up code editor
        setupCodeEditor(initialCodeEditor, initialCodeInterface);

        // Set up toolbar navigation
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Set up create button
        createButton.setOnClickListener(v -> checkScoreAndCreate());

        return view;
    }

    private void setupCodeEditor(WebView webView, WebAppInterface webAppInterface) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(webAppInterface, "Android");
        webView.loadUrl("file:///android_asset/codemirror.html");
    }

    private void checkScoreAndCreate() {
        userScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer currentScore = dataSnapshot.getValue(Integer.class);
                if (currentScore == null) {
                    // If totalPoints is null, initialize it to 0 and then proceed
                    currentScore = 0;
                    userScoreRef.setValue(0);
                    Log.d(TAG, "Initialized user score to 0");
                }
                
                Log.d(TAG, "Current score: " + currentScore);

                if (currentScore >= EXERCISE_COST) {
                    getCodeAndCreate(currentScore);
                } else {
                    Toast.makeText(getContext(),
                        "Not enough score! You need " + EXERCISE_COST + " score, but you have " + currentScore,
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to check score: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to check score", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCodeAndCreate(int currentScore) {
        initialCodeEditor.evaluateJavascript("javascript:sendCode()", initialValue -> {
            String initialCode = stripQuotes(initialValue);
            createExercise(currentScore, initialCode);
        });
    }

    private String stripQuotes(String value) {
        if (value == null || value.equals("null")) return "";
        return value.substring(1, value.length() - 1).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private void createExercise(int currentScore, String initialCode) {
        // Get values from inputs
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String hintsText = hintsInput.getText().toString().trim();
        String difficulty = "Medium"; // Default difficulty for community exercises

        // Check if username was loaded
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(getContext(), "Failed to get username. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs
        if (title.isEmpty() || description.isEmpty() || initialCode.isEmpty() || hintsText.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split hints into a list
        List<String> hints = new ArrayList<>(Arrays.asList(hintsText.split("\n")));

        // Get test inputs and outputs
        List<String> testInputs = getTestInputs();
        List<String> expectedOutputs = getExpectedOutputs();

        // Create test cases from inputs and outputs
        List<Exercise.TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < testInputs.size(); i++) {
            List<String> inputs = Arrays.asList(testInputs.get(i));
            String expectedOutput = expectedOutputs.get(i);
            testCases.add(new Exercise.TestCase(inputs, expectedOutput));
        }

        // Create Exercise object with creator info
        String exerciseId = pendingExercisesRef.push().getKey();
        Exercise exercise = new Exercise(
            exerciseId,
            title,
            description,
            difficulty,
            "Community", // Category
            initialCode,
            testCases,  // test cases instead of solution
            hints,
            "solution",  // default method name for community exercises
            EXERCISE_COST, // Score for community exercises
            currentUserId,
            currentUsername
        );

        // Check if exerciseId was generated
        if (exerciseId != null) {
            // Check if user is admin
            userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Boolean isAdmin = dataSnapshot.getValue(Boolean.class);

                    // If user is admin, auto-approve and add directly to community exercises
                    if (isAdmin != null && isAdmin) {
                        exercise.setApproved(true);
                        // Admin - auto-approve and don't deduct points
                        FirebaseDatabase.getInstance().getReference("community_exercises")
                            .child(exerciseId).setValue(exercise)
                            .addOnSuccessListener(aVoid -> {
                                // Log the operation
                                Log.d(TAG, "Exercise created and auto-approved by admin");
                                Toast.makeText(getContext(), "Exercise created and auto-approved!", Toast.LENGTH_SHORT).show();
                                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                                    getParentFragmentManager().popBackStack();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create exercise: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to create exercise: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        // Regular user - submit for approval
                        // Update score first - ensure we're using a transaction to avoid race conditions
                        userScoreRef.runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                Integer score = mutableData.getValue(Integer.class);
                                if (score == null) {
                                    score = 0;
                                }
                                
                                // Deduct the cost
                                score = score - EXERCISE_COST;
                                mutableData.setValue(score);
                                return Transaction.success(mutableData);
                            }
                            
                            @Override
                            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                                // Check if fragment is still attached
                                if (getContext() == null) {
                                    Log.e(TAG, "Fragment not attached to context during transaction completion");
                                    return;
                                }
                                
                                if (databaseError != null) {
                                    Log.e(TAG, "Score transaction failed: " + databaseError.getMessage());
                                    Toast.makeText(getContext(), "Failed to update score", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                if (!committed) {
                                    Log.e(TAG, "Score transaction aborted");
                                    Toast.makeText(getContext(), "Failed to update score", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                // Score updated successfully, now create the exercise
                                Log.d(TAG, "Score updated successfully, new score: " + dataSnapshot.getValue());
                                
                                try {
                                    // Ensure exercise object is valid
                                    if (exercise == null) {
                                        Log.e(TAG, "Exercise object is null");
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Error: Exercise data is missing", Toast.LENGTH_SHORT).show();
                                        }
                                        // Refund the points since we couldn't create the exercise
                                        refundPoints(EXERCISE_COST);
                                        return;
                                    }
                                    
                                    // Ensure exerciseId is valid
                                    if (exerciseId == null || exerciseId.isEmpty()) {
                                        Log.e(TAG, "Exercise ID is null or empty");
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Error: Could not generate exercise ID", Toast.LENGTH_SHORT).show();
                                        }
                                        // Refund the points since we couldn't create the exercise
                                        refundPoints(EXERCISE_COST);
                                        return;
                                    }
                                    
                                    // Log the exercise data for debugging
                                    Log.d(TAG, "Submitting exercise to pending queue: ID=" + exerciseId + ", Title=" + exercise.getTitle());
                                    
                                    // Explicitly set approved to false for non-admin users
                                    exercise.setApproved(false);
                                    
                                    // Double check that pendingExercisesRef is properly initialized
                                    if (pendingExercisesRef == null) {
                                        Log.e(TAG, "pendingExercisesRef is null, reinitializing");
                                        pendingExercisesRef = FirebaseDatabase.getInstance().getReference("pending_exercises");
                                        if (pendingExercisesRef == null) {
                                            Log.e(TAG, "Failed to initialize pendingExercisesRef");
                                            if (getContext() != null) {
                                                Toast.makeText(getContext(), "Error: Database connection failed", Toast.LENGTH_SHORT).show();
                                            }
                                            // Refund the points since we couldn't create the exercise
                                            refundPoints(EXERCISE_COST);
                                            return;
                                        }
                                    }
                                    
                                    // Then create the Exercise in pending queue
                                    pendingExercisesRef.child(exerciseId).setValue(exercise)
                                        .addOnSuccessListener(aVoid2 -> {
                                            if (getContext() == null) return; // Check if fragment is still attached
                                            
                                            Log.d(TAG, "Exercise successfully submitted to pending queue: " + exerciseId);
                                            Toast.makeText(getContext(), "Exercise submitted for approval!", Toast.LENGTH_SHORT).show();
                                            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                                                getParentFragmentManager().popBackStack();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (getContext() == null) return; // Check if fragment is still attached
                                            
                                            Log.e(TAG, "Failed to create exercise: " + e.getMessage());
                                            Toast.makeText(getContext(), "Failed to create exercise: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                            
                                            // Refund the points since we couldn't create the exercise
                                            refundPoints(EXERCISE_COST);
                                        });
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception when creating exercise: " + e.getMessage(), e);
                                    // Refund the points since we couldn't create the exercise
                                    refundPoints(EXERCISE_COST);
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Error creating exercise: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to check admin status: " + databaseError.getMessage());
                    Toast.makeText(getContext(), "Failed to check admin status", Toast.LENGTH_SHORT).show();
                }
            });
        }
        }


    private List<String> getTestInputs() {
        List<String> inputs = new ArrayList<>();
        String testInputText = testInput.getText().toString().trim();
        if (!testInputText.isEmpty()) {
            // Split by newlines and add each line as a test input
            String[] lines = testInputText.split("\n");
            inputs.addAll(Arrays.asList(lines));
        }
        return inputs;
    }
    
    private List<String> getExpectedOutputs() {
        List<String> outputs = new ArrayList<>();
        String expectedOutputText = expectedOutput.getText().toString().trim();
        if (!expectedOutputText.isEmpty()) {
            // Split by newlines and add each line as an expected output
            String[] lines = expectedOutputText.split("\n");
            outputs.addAll(Arrays.asList(lines));
        }
        return outputs;
    }
    
    /**
     * Refunds points to the user when exercise creation fails
     * @param amount Amount of points to refund
     */
    private void refundPoints(int amount) {
        if (userScoreRef == null) {
            Log.e(TAG, "userScoreRef is null, cannot refund points");
            return;
        }
        
        userScoreRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer score = mutableData.getValue(Integer.class);
                if (score == null) {
                    score = amount;
                } else {
                    score += amount;
                }
                mutableData.setValue(score);
                return Transaction.success(mutableData);
            }
            
            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, "Failed to refund points: " + databaseError.getMessage());
                } else if (committed) {
                    Log.d(TAG, "Points refunded successfully: " + amount);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), amount + " points have been refunded", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (initialCodeEditor != null) {
            initialCodeEditor.loadUrl("about:blank");
            initialCodeEditor.clearHistory();
            initialCodeEditor.clearCache(true);
            initialCodeEditor.destroy();
        }
    }
}




