package com.code_share.finalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateExerciseFragment extends Fragment {
    private static final String TAG = "CreateExerciseFrag";

    private TextInputEditText titleInput;
    private TextInputEditText topicInput;
    private TextInputEditText descriptionInput;
    private WebView initialCodeEditor;
    private TextInputEditText testInput;
    private TextInputEditText expectedOutput;
    private TextInputEditText hintsInput;
    private MaterialButton createButton;
    private DatabaseReference pendingExercisesRef;
    private DatabaseReference userRef;
    private WebAppInterface initialCodeInterface;
    private String currentUserId;
    private String currentUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_exercise, container, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to create an exercise.", Toast.LENGTH_SHORT).show();
            return view;
        }

        currentUserId = currentUser.getUid();
        pendingExercisesRef = FirebaseDatabase.getInstance().getReference("pending_exercises");
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get username: " + error.getMessage());
            }
        });

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        titleInput = view.findViewById(R.id.title_input);
        topicInput = view.findViewById(R.id.topic_input);
        descriptionInput = view.findViewById(R.id.description_input);
        initialCodeEditor = view.findViewById(R.id.initial_code_editor);
        testInput = view.findViewById(R.id.test_input);
        expectedOutput = view.findViewById(R.id.expected_output);
        hintsInput = view.findViewById(R.id.hints_input);
        createButton = view.findViewById(R.id.create_button);

        initialCodeInterface = new WebAppInterface(requireContext());
        setupCodeEditor(initialCodeEditor);
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        createButton.setOnClickListener(v -> getCodeAndCreate());
        return view;
    }

    private void setupCodeEditor(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(initialCodeInterface, "Android");
        webView.loadUrl("file:///android_asset/codemirror.html");
    }

    private void getCodeAndCreate() {
        if (initialCodeEditor == null) return;
        createButton.setEnabled(false);
        initialCodeEditor.evaluateJavascript("window.sendCode()", value -> {
            final String initialCode;
            try {
                Object decoded = new JSONTokener(value).nextValue();
                if (!(decoded instanceof String)) {
                    throw new JSONException("Editor returned no code.");
                }
                initialCode = ((String) decoded).trim();
            } catch (JSONException e) {
                createButton.setEnabled(true);
                showMessage("Couldn't read the starter code.");
                return;
            }
            createExercise(initialCode);
        });
    }

    private void createExercise(String initialCode) {
        String title = textOf(titleInput);
        String topic = textOf(topicInput);
        String description = textOf(descriptionInput);
        String hintsText = textOf(hintsInput);
        List<String> testInputs = splitLines(textOf(testInput));
        List<String> expectedOutputs = splitLines(textOf(expectedOutput));

        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            createButton.setEnabled(true);
            showMessage("Your username is still loading. Try again in a moment.");
            return;
        }
        if (title.isEmpty() || topic.isEmpty() || description.isEmpty() || initialCode.isEmpty() || hintsText.isEmpty()) {
            createButton.setEnabled(true);
            showMessage("Fill in the title, topic, description, starter code, and at least one hint.");
            return;
        }
        if (testInputs.isEmpty() || testInputs.size() != expectedOutputs.size()) {
            createButton.setEnabled(true);
            showMessage("Add the same number of test inputs and expected outputs.");
            return;
        }

        List<Exercise.TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < testInputs.size(); i++) {
            testCases.add(new Exercise.TestCase(
                    Arrays.asList(testInputs.get(i)),
                    expectedOutputs.get(i)));
        }

        String exerciseId = pendingExercisesRef.push().getKey();
        if (exerciseId == null) {
            createButton.setEnabled(true);
            showMessage("Couldn't create an exercise ID. Please try again.");
            return;
        }

        Exercise exercise = new Exercise(
                exerciseId,
                title,
                description,
                "Medium",
                topic,
                initialCode,
                testCases,
                splitLines(hintsText),
                "solution",
                0,
                currentUserId,
                currentUsername
        );

        createButton.setEnabled(false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            failCreate(new IllegalStateException("Please sign in again."));
            return;
        }
        currentUser.getIdToken(true)
                .addOnSuccessListener(token -> {
                    boolean isAdmin = Boolean.TRUE.equals(token.getClaims().get("admin"));
                    if (isAdmin) {
                        exercise.setApproved(true);
                        FirebaseDatabase.getInstance().getReference("community_exercises")
                                .child(exerciseId)
                                .setValue(exercise)
                                .addOnSuccessListener(unused -> finishCreate("Exercise published."))
                                .addOnFailureListener(error -> failCreate(error));
                    } else {
                        exercise.setApproved(false);
                        pendingExercisesRef.child(exerciseId)
                                .setValue(exercise)
                                .addOnSuccessListener(unused -> finishCreate("Exercise submitted for review."))
                                .addOnFailureListener(error -> failCreate(error));
                    }
                })
                .addOnFailureListener(this::failCreate);
    }

    private void finishCreate(String message) {
        if (!isAdded()) return;
        createButton.setEnabled(true);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void failCreate(Exception error) {
        Log.e(TAG, "Failed to create exercise", error);
        if (!isAdded()) return;
        createButton.setEnabled(true);
        Toast.makeText(requireContext(), "Couldn't create exercise: " + error.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static List<String> splitLines(String value) {
        List<String> lines = new ArrayList<>();
        if (!value.isEmpty()) {
            lines.addAll(Arrays.asList(value.split("\\r?\\n")));
        }
        return lines;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (initialCodeEditor != null) {
            initialCodeEditor.loadUrl("about:blank");
            initialCodeEditor.destroy();
            initialCodeEditor = null;
        }
    }
}
