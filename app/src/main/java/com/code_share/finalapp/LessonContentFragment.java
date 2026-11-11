package com.code_share.finalapp;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
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

import org.json.JSONObject;

public class LessonContentFragment extends Fragment {
    private static final String LESSON_THEME_CSS =
            ":root{color-scheme:dark}" +
            "html,body{background:#0B1020!important;color:#A7B1C8!important}" +
            "body{box-sizing:border-box;font-family:sans-serif!important;line-height:1.7!important;" +
            "max-width:860px;margin:0 auto!important;padding:20px 18px 40px!important}" +
            "h1,h2,h3,h4{color:#F5F7FF!important;line-height:1.3!important}" +
            "h1{font-size:1.8em!important;margin:0 0 20px!important}" +
            "h2{font-size:1.35em!important;margin-top:30px!important}" +
            "h3{font-size:1.1em!important;margin-top:22px!important}" +
            "a{color:#A99BFF!important}" +
            "strong{color:#C5B9FF!important}" +
            "pre{box-sizing:border-box;max-width:100%;overflow-x:auto;background:#151D30!important;" +
            "color:#F5F7FF!important;border:1px solid #34415D!important;border-radius:12px!important;" +
            "padding:14px!important;white-space:pre!important}" +
            "code{font-family:monospace!important;color:#B8F36B;background:#1C2740!important;" +
            "border-radius:4px;padding:2px 5px}" +
            "pre code{background:transparent!important;padding:0!important;color:#B8F36B}" +
            ".note,blockquote{background:#151D30!important;color:#A7B1C8!important;" +
            "border-left:4px solid #B8F36B!important;border-radius:0 10px 10px 0;padding:12px 16px!important}" +
            "table{display:block;max-width:100%;overflow-x:auto;border-collapse:collapse}" +
            "th,td{border:1px solid #34415D;padding:8px;color:#A7B1C8}" +
            "th{background:#151D30;color:#F5F7FF}" +
            "img{max-width:100%;height:auto}" +
            ".tok-keyword{color:#C792EA;font-weight:600}" +
            ".tok-string{color:#C3E88D}" +
            ".tok-number{color:#F78C6C}" +
            ".tok-comment{color:#7886A3;font-style:italic}" +
            ".tok-variableName{color:#82AAFF}" +
            ".tok-definition{color:#82AAFF;font-weight:600}" +
            ".tok-typeName{color:#FFCB6B}" +
            ".tok-operator{color:#89DDFF}" +
            ".tok-punctuation{color:#A7B1C8}" +
            ".tok-bool{color:#F78C6C}" +
            ".lesson-playground-row{display:flex;justify-content:flex-end;margin:-8px 0 24px}" +
            ".lesson-playground-button{border:1px solid #B8F36B;border-radius:10px;" +
            "background:#B8F36B;color:#0B1020;padding:10px 14px;font-size:14px;font-weight:700}";

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
                webView.addJavascriptInterface(new LessonPlaygroundBridge(), "LessonAndroid");
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
                        String script = "(function(){var s=document.createElement('style');s.textContent="
                                + JSONObject.quote(LESSON_THEME_CSS)
                                + ";document.head.appendChild(s);var h=document.createElement('script');"
                                + "h.src='file:///android_asset/code-editor.js';"
                                + "h.onload=function(){if(window.highlightPythonCodeBlocks)"
                                + "window.highlightPythonCodeBlocks(document);"
                                + "if(window.addLessonPlaygroundButtons)"
                                + "window.addLessonPlaygroundButtons(document)};"
                                + "document.head.appendChild(h);})();";
                        view.evaluateJavascript(script, null);
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

    public final class LessonPlaygroundBridge {
        @JavascriptInterface
        public void openPlayground(String code) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                CodeEditorFragment playground = new CodeEditorFragment();
                Bundle args = new Bundle();
                args.putBoolean("playground", true);
                args.putString("code", code == null ? "" : code);
                playground.setArguments(args);
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, playground)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }
}
