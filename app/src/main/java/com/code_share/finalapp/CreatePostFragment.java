package com.code_share.finalapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class CreatePostFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextInputEditText descriptionInput;
    private MaterialButton attachImageButton;
    private MaterialButton attachCodeButton;
    private MaterialButton postButton;
    private WebView codeEditor;
    private ImageView selectedImage;
    private String codeContent;
    private Uri imageUri;
    private DatabaseReference postsRef;
    private StorageReference storageRef;
    private WebAppInterface webAppInterface;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        storageRef = FirebaseStorage.getInstance().getReference().child("post_images");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);
        
        // Initialize views
        descriptionInput = view.findViewById(R.id.postDescriptionInput);
        attachImageButton = view.findViewById(R.id.attachImageButton);
        attachCodeButton = view.findViewById(R.id.attachCodeButton);
        postButton = view.findViewById(R.id.postButton);
        codeEditor = view.findViewById(R.id.code_editor);
        selectedImage = view.findViewById(R.id.selectedImage);
        
        // Initialize WebAppInterface
        webAppInterface = new WebAppInterface(requireContext());
        webAppInterface.setOnCodeReceivedListener(code -> {
            codeContent = code;
            Log.d("CreatePostFragment", "Code received, length: " + (code != null ? code.length() : 0));
        });
        
        // Setup code editor
        setupCodeEditor();

        attachImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        attachCodeButton.setOnClickListener(v -> {
            if (codeEditor.getVisibility() == View.GONE) {
                codeEditor.setVisibility(View.VISIBLE);
                attachCodeButton.setText("Remove Code");
                // Load initial code editor content
                codeEditor.evaluateJavascript("javascript:initializeEditor()", null);
            } else {
                codeEditor.setVisibility(View.GONE);
                attachCodeButton.setText("Attach Code");
                codeContent = null;
            }
        });

        postButton.setOnClickListener(v -> createPost());

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    imageUri = data.getData();
                    if (selectedImage != null) {
                        selectedImage.setImageURI(imageUri);
                        selectedImage.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e("CreatePostFragment", "Error setting image: " + e.getMessage());
                    Toast.makeText(getContext(), "Error setting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                Toast.makeText(getContext(), "Image selection cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupCodeEditor() {
        WebSettings webSettings = codeEditor.getSettings();
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

        codeEditor.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Initialize the editor after page load
                codeEditor.evaluateJavascript("javascript:initializeEditor()", null);
                
                // Set up a timer to periodically capture the code content
                view.evaluateJavascript(
                    "setInterval(function() { " +
                    "    if (typeof editor !== 'undefined' && editor) { " +
                    "        Android.sendCodeToAndroid(editor.getValue()); " +
                    "    }" +
                    "}, 2000);", null);
            }
        });
        
        codeEditor.setWebChromeClient(new WebChromeClient());
        codeEditor.addJavascriptInterface(webAppInterface, "Android");
        codeEditor.loadUrl("file:///android_asset/codemirror.html");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (codeEditor != null) {
            codeEditor.loadUrl("about:blank");
            codeEditor.clearHistory();
            codeEditor.clearCache(true);
            codeEditor.destroy();
        }
    }

    private void createPost() {
        if (getContext() == null) {
            Toast.makeText(getContext(), "Context is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please log in to create a post", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = descriptionInput != null ? descriptionInput.getText().toString().trim() : "";
        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        postButton.setEnabled(false);

        if (codeEditor.getVisibility() == View.VISIBLE) {
            try {
                // First check if we already have the code content from the WebAppInterface
                if (codeContent != null && !codeContent.isEmpty()) {
                    Log.d("CreatePostFragment", "Using code from WebAppInterface, length: " + codeContent.length());
                    if (imageUri != null) {
                        uploadImageAndSavePost(description, codeContent);
                    } else {
                        savePost(description, codeContent, null);
                    }
                    return;
                }
                
                // If we don't have the code yet, try to get it directly from the editor
                codeEditor.evaluateJavascript("javascript:getCode()", value -> {
                    try {
                        if (value != null && !value.equals("null")) {
                            // Process the code content
                            String processedCode = value.substring(1, value.length() - 1)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\'", "'")
                                    .replace("\\\\", "\\");
                            
                            // Store the processed code for future use
                            codeContent = processedCode;
                            
                            Log.d("CreatePostFragment", "Processed code from JS, length: " + processedCode.length());
                            if (imageUri != null) {
                                uploadImageAndSavePost(description, processedCode);
                            } else {
                                savePost(description, processedCode, null);
                            }
                        } else {
                            // No code content found
                            Log.d("CreatePostFragment", "No code content found from JS call");
                            if (imageUri != null) {
                                uploadImageAndSavePost(description, null);
                            } else {
                                savePost(description, null, null);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CreatePostFragment", "Error processing code: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error processing code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        postButton.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                Log.e("CreatePostFragment", "Error getting code: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Error getting code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                postButton.setEnabled(true);
            }
        } else {
            if (imageUri != null) {
                uploadImageAndSavePost(description, null);
            } else {
                savePost(description, null, null);
            }
        }
    }

    private void uploadImageAndSavePost(String description, String code) {
        if (imageUri == null) {
            savePost(description, code, null);
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Upload image using ImgBB
        ImgBBService imgBBService = new ImgBBService(getContext());
        imgBBService.uploadImage(imageUri, new ImgBBService.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                progressDialog.dismiss();
                savePost(description, code, imageUrl);
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Log.e("CreatePostFragment", "Error uploading image: " + error);
                Toast.makeText(getContext(), "Error uploading image: " + error, Toast.LENGTH_SHORT).show();
                postButton.setEnabled(true);
            }
        });
    }

    private void savePost(String description, String code, String imageUrl) {
        try {
            String postId = UUID.randomUUID().toString();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            Post post = new Post();
            post.setId(postId);
            post.setUserId(userId);
            post.setDescription(description);
            post.setTimestamp(System.currentTimeMillis());
            
            if (code != null && !code.isEmpty()) {
                post.setCodeContent(code);
            }
            
            if (imageUrl != null) {
                post.setImageUrl(imageUrl);
            }
            
            String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            post.setUserName(displayName != null ? displayName : "Anonymous");
            
            if (FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                post.setUserAvatar(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString());
            }

            postsRef.child(postId).setValue(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post created successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate back to community tab
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CreatePostFragment", "Error saving ru.myitschool.finalproject.Post: " + e.getMessage());
                    Toast.makeText(getContext(), "Error creating ru.myitschool.finalproject.Post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    postButton.setEnabled(true);
                });
        } catch (Exception e) {
            Log.e("CreatePostFragment", "Error in savePost: " + e.getMessage());
            Toast.makeText(getContext(), "Error creating ru.myitschool.finalproject.Post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            postButton.setEnabled(true);
        }
    }
} 




