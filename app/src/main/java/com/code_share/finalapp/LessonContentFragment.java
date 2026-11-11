package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LessonContentFragment extends Fragment {

    private WebView webView;
    private TextView titleView;
    private TextView contentView;
    private String lessonTitle;
    private String lessonContent;
    private String htmlFile;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson_content, container, false);

        // Initialize views
        webView = view.findViewById(R.id.webview);
        titleView = view.findViewById(R.id.lesson_title);
        contentView = view.findViewById(R.id.lesson_content);

        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            lessonTitle = args.getString("title");
            lessonContent = args.getString("content");
            htmlFile = args.getString("html_file");

            // Set title
            titleView.setText(lessonTitle);

            // Set content
            if (htmlFile != null && !htmlFile.isEmpty()) {
                // Load HTML content
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.getSettings().setAllowFileAccess(true);
                webView.getSettings().setAllowContentAccess(true);
                webView.getSettings().setAllowFileAccessFromFileURLs(true);
                webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setDisplayZoomControls(false);
                webView.getSettings().setSupportZoom(true);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.getSettings().setDefaultTextEncodingName("UTF-8");
                webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                webView.setVerticalScrollBarEnabled(true);
                webView.setHorizontalScrollBarEnabled(false);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        view.setVisibility(View.VISIBLE);
                        contentView.setVisibility(View.GONE);
                        // Force layout recalculation
                        view.requestLayout();
                    }
                });
                webView.loadUrl("file:///android_asset/" + htmlFile);
            } else {
                // Display text content
                contentView.setText(lessonContent);
                contentView.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
            }
        }

        // Initialize Firebase and mark ru.myitschool.finalproject.Lesson as completed
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            markLessonAsCompleted();
        }

        return view;
    }

    private void markLessonAsCompleted() {
        if (lessonTitle != null && userRef != null) {
            userRef.child("completed_lessons").child(lessonTitle).setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully marked as completed
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to mark ru.myitschool.finalproject.Lesson as completed", Toast.LENGTH_SHORT).show();
                    });
        }
    }
} 




