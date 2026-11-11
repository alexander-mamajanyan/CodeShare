package com.code_share.finalapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONTokener;

public class CodeEditorFragment extends Fragment {

    private WebView codeEditor;
    private MaterialButton runCodeButton;
    private TextView output;
    private TextInputEditText programInput;
    private String initialCode;
    private String exerciseTitle;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;
    private Exercise exercise;
    private String currentCode;
    private ExecutorService pythonExecutor;
    private Handler mainHandler;
    private boolean keyboardVisible;
    private boolean hasRunOutput;
    private boolean playground;
    private int editorPageHeight;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    private View editorRootView;
    private View programInputLayout;
    private View editorActions;
    private TextView runtimeLabel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        boolean isPlayground = args != null && args.getBoolean("playground", false);
        setHasOptionsMenu(args == null
                || (!args.getBoolean("embedded", false) && !isPlayground));
    }

    public boolean isPlayground() {
        Bundle args = getArguments();
        return args != null && args.getBoolean("playground", false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isPlayground()) {
            return;
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (!getParentFragmentManager().popBackStackImmediate()) {
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new LessonFragment())
                                    .commit();
                        }
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_code_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            requireActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_code_editor, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to use the editor", Toast.LENGTH_SHORT).show();
            return rootView;
        }
        String userId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize UI elements
        codeEditor = rootView.findViewById(R.id.codeEditor);
        runCodeButton = rootView.findViewById(R.id.run_button);
        output = rootView.findViewById(R.id.output);
        programInput = rootView.findViewById(R.id.program_input);
        programInputLayout = rootView.findViewById(R.id.program_input_layout);
        editorActions = rootView.findViewById(R.id.editor_actions);
        runtimeLabel = rootView.findViewById(R.id.runtime_label);
        editorRootView = rootView;
        MaterialButton shareButton = rootView.findViewById(R.id.share_code_button);
        pythonExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        float displayDensity = rootView.getResources().getDisplayMetrics().density;
        keyboardLayoutListener = () -> {
            if (editorRootView != rootView) return;
            int currentHeight = rootView.getHeight();
            if (currentHeight == 0) return;
            if (editorPageHeight == 0) {
                editorPageHeight = currentHeight;
                return;
            }
            int keyboardThreshold = (int) (120 * displayDensity);
            boolean visible = editorPageHeight - currentHeight > keyboardThreshold;
            if (!visible) {
                editorPageHeight = currentHeight;
            }
            updateKeyboardLayout(visible);
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        // Set up toolbar
        MaterialToolbar toolbar = rootView.findViewById(R.id.toolbar);
        boolean embedded = getArguments() != null && getArguments().getBoolean("embedded", false);
        playground = getArguments() != null && getArguments().getBoolean("playground", false);
        ActionBar actionBar = null;
        if (embedded) {
            toolbar.setVisibility(View.GONE);
        } else {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setTitle(playground ? "Python playground" : "Python workspace");
            }
        }
        if (playground) {
            shareButton.setVisibility(View.GONE);
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
            ((MainActivity) requireActivity()).setBottomNavigationVisible(false);
        }

        // Get Exercise data from arguments
        Bundle args = getArguments();
        if (args != null) {
            // Check for both "exercise" and "Exercise" keys to ensure backward compatibility
            exercise = args.getParcelable("exercise");
            if (exercise == null) {
                exercise = args.getParcelable("Exercise");
            }
            
            if (exercise != null) {
                exerciseTitle = exercise.getTitle();
                initialCode = exercise.getCode();
                
                // Set toolbar title
                if (actionBar != null && !embedded) {
                    actionBar.setTitle(exerciseTitle);
                }
            } else {
                initialCode = args.getString("code", "");
            }
        }

        setupWebView(codeEditor, initialCode);

        runCodeButton.setOnClickListener(v -> runCode());
        shareButton.setOnClickListener(v -> showShareDialog());

        return rootView;
    }

    private void updateKeyboardLayout(boolean visible) {
        keyboardVisible = visible;
        if (runtimeLabel == null) return;
        boolean enteringProgramInput = programInput != null && programInput.hasFocus();
        runtimeLabel.setVisibility(visible ? View.GONE : View.VISIBLE);
        editorActions.setVisibility(visible ? View.GONE : View.VISIBLE);
        programInputLayout.setVisibility(visible && !enteringProgramInput ? View.GONE : View.VISIBLE);
        output.setVisibility(visible || !hasRunOutput ? View.GONE : View.VISIBLE);
    }

    private void setupWebView(WebView webView, String initialCode) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (initialCode != null) {
                    String js = "window.editor.setValue(" + org.json.JSONObject.quote(initialCode) + ")";
                    view.evaluateJavascript(js, null);
                }
            }
        });

        webView.loadUrl("file:///android_asset/codemirror.html");
    }

    private void runCode() {
        if (codeEditor == null || pythonExecutor == null || pythonExecutor.isShutdown()) {
            return;
        }
        runCodeButton.setEnabled(false);
        runCodeButton.setText("Loading code...");
        hasRunOutput = true;
        output.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE);
        output.setText("Preparing Python...");
        codeEditor.evaluateJavascript("window.getCode()", value -> {
            if (!isAdded() || output == null) return;

            try {
                Object decoded = new JSONTokener(value).nextValue();
                if (!(decoded instanceof String)) {
                    throw new JSONException("Editor returned no code.");
                }
                currentCode = (String) decoded;
            } catch (JSONException e) {
                finishRun("Couldn't read the code from the editor.");
                return;
            }

            String code = currentCode;
            String input = programInput.getText() == null ? "" : programInput.getText().toString();
            Context applicationContext = requireContext().getApplicationContext();
            runCodeButton.setText("Running...");
            pythonExecutor.execute(() -> {
                RunResult result;
                try {
                    result = runPython(applicationContext, code, input);
                } catch (RuntimeException e) {
                    result = new RunResult("Python couldn't run this code: " + e.getMessage(), false);
                }
                RunResult completedResult = result;
                mainHandler.post(() -> {
                    if (!isAdded() || output == null) return;
                    finishRun(completedResult.output);
                    if (completedResult.allTestsPassed) {
                        recordSolvedExercise();
                    }
                });
            });
        });
    }

    private RunResult runPython(Context context, String code, String input) {
        List<Exercise.TestCase> testCases = exercise == null ? null : exercise.getTestCases();
        if (testCases == null || testCases.isEmpty()) {
            return new RunResult(PythonExecutor.execute(context, code, input), false);
        }

        String methodName = exercise.getSolutionMethodName();
        if (methodName == null || methodName.trim().isEmpty()) {
            methodName = "solution";
        }

        StringBuilder results = new StringBuilder();
        int passedTests = 0;
        for (int i = 0; i < testCases.size(); i++) {
            Exercise.TestCase testCase = testCases.get(i);
            String result = PythonExecutor.executeMethod(context, code, methodName, testCase.getInputs());
            boolean passed = result.trim().equals(
                    testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput().trim());
            if (passed) passedTests++;

            results.append("Test ").append(i + 1).append(": ")
                    .append(passed ? "PASSED" : "FAILED").append('\n');
            if (testCase.getInputs() != null && !testCase.getInputs().isEmpty()) {
                results.append("Input: ").append(String.join(", ", testCase.getInputs())).append('\n');
            }
            results.append("Output: ").append(result.trim().isEmpty() ? "(no output)" : result.trim()).append("\n\n");
        }

        results.append(passedTests).append(" of ").append(testCases.size()).append(" tests passed.");
        return new RunResult(results.toString(), passedTests == testCases.size());
    }

    private void finishRun(String result) {
        if (output != null) {
            output.setText(result);
            hasRunOutput = true;
            output.setVisibility(keyboardVisible ? View.GONE : View.VISIBLE);
        }
        if (runCodeButton != null) {
            runCodeButton.setEnabled(true);
            runCodeButton.setText("Run code");
        }
    }

    private void recordSolvedExercise() {
        if (exercise == null || exercise.getId() == null || userRef == null) {
            Toast.makeText(requireContext(), "All tests passed.", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = android.util.Base64.encodeToString(
                exercise.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
        DatabaseReference solvedRef = userRef.child("practiceProgress").child("solved").child(key);
        solvedRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() != null) {
                    return Transaction.abort();
                }
                Map<String, Object> solved = new HashMap<>();
                solved.put("title", exercise.getTitle() == null ? "Exercise" : exercise.getTitle());
                solved.put("category", exercise.getCategory() == null ? "Other" : exercise.getCategory());
                solved.put("completedAt", System.currentTimeMillis());
                currentData.setValue(solved);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (error != null) {
                    Toast.makeText(requireContext(), "Tests passed, but progress couldn't be saved.", Toast.LENGTH_LONG).show();
                } else if (committed) {
                    Toast.makeText(requireContext(), "Exercise completed. Progress updated.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static final class RunResult {
        final String output;
        final boolean allTestsPassed;

        RunResult(String output, boolean allTestsPassed) {
            this.output = output;
            this.allTestsPassed = allTestsPassed;
        }
    }

    private void showShareDialog() {
        codeEditor.evaluateJavascript("window.getCode()", value -> {
            final String codeText;
            try {
                Object decoded = new JSONTokener(value).nextValue();
                if (!(decoded instanceof String)) {
                    throw new JSONException("Editor returned no code.");
                }
                codeText = (String) decoded;
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Couldn't read code from the editor.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Please sign in to share code.", Toast.LENGTH_SHORT).show();
                return;
            }
            final String sharedTitle = exercise != null && exercise.getTitle() != null
                    ? exercise.getTitle() : "Shared Code";
            String currentUserId = auth.getCurrentUser().getUid();
            DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUserId)
                    .child("friends");

            friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;

                    List<String> friendIds = new ArrayList<>();
                    List<String> friendNames = new ArrayList<>();
                    for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                        String friendId = friendSnapshot.getKey();
                        String friendName = friendSnapshot.getValue(String.class);
                        if (friendId != null && friendName != null) {
                            friendIds.add(friendId);
                            friendNames.add(friendName);
                        }
                    }

                    if (friendIds.isEmpty()) {
                        Toast.makeText(requireContext(), "Add a friend before sharing code.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean[] checkedItems = new boolean[friendIds.size()];
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Share code with")
                            .setMultiChoiceItems(friendNames.toArray(new String[0]), checkedItems,
                                    (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                            .setPositiveButton("Share", (dialog, which) -> {
                                boolean selected = false;
                                for (boolean checked : checkedItems) selected |= checked;
                                if (!selected) {
                                    Toast.makeText(requireContext(), "Select at least one friend.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                for (int i = 0; i < checkedItems.length; i++) {
                                    if (!checkedItems[i]) continue;
                                    String conversationId = getConversationId(currentUserId, friendIds.get(i));
                                    Message codeMessage = new Message(sharedTitle, currentUserId, sharedTitle, codeText);
                                    codeMessage.setType("code");
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("conversations")
                                            .child(conversationId)
                                            .child("messages")
                                            .push()
                                            .setValue(codeMessage)
                                            .addOnSuccessListener(aVoid -> {
                                                if (isAdded()) {
                                                    Toast.makeText(requireContext(), "Code shared.", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(error -> {
                                                if (isAdded()) {
                                                    Toast.makeText(requireContext(),
                                                            "Couldn't share code: " + error.getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Couldn't load friends: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private String getConversationId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    @Override
    public void onDestroyView() {
        if (editorRootView != null && keyboardLayoutListener != null) {
            ViewTreeObserver observer = editorRootView.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnGlobalLayoutListener(keyboardLayoutListener);
            }
        }
        editorRootView = null;
        keyboardLayoutListener = null;
        programInputLayout = null;
        editorActions = null;
        runtimeLabel = null;
        if (playground && isAdded()) {
            ((MainActivity) requireActivity()).setBottomNavigationVisible(true);
        }
        if (pythonExecutor != null) {
            pythonExecutor.shutdownNow();
            pythonExecutor = null;
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
        if (codeEditor != null) {
            codeEditor.stopLoading();
            codeEditor.setWebChromeClient(null);
            codeEditor.setWebViewClient(null);
            ViewParent parent = codeEditor.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(codeEditor);
            }
            codeEditor.destroy();
            codeEditor = null;
        }
        runCodeButton = null;
        output = null;
        programInput = null;
        super.onDestroyView();
    }
}
