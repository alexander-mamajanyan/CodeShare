package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class PostCodeEditorFragment extends Fragment {
    private WebView codeEditor;
    private Button saveButton;
    private WebAppInterface webAppInterface;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_code_editor, container, false);

        codeEditor = view.findViewById(R.id.code_editor);
        saveButton = view.findViewById(R.id.save_button);

        // Initialize WebAppInterface
        webAppInterface = new WebAppInterface(requireContext());

        // Configure WebView
        setupWebView(codeEditor);

        // Set up save button
        saveButton.setOnClickListener(v -> {
            codeEditor.evaluateJavascript("javascript:sendCode()", code -> {
                if (code != null && !code.equals("null")) {
                    String codeText = code.substring(1, code.length() - 1);
                    codeText = codeText.replace("\\n", "\n").replace("\\\"", "\"");
                    
                    Bundle result = new Bundle();
                    result.putString("code", codeText);
                    getParentFragmentManager().setFragmentResult("code_result", result);
                    
                    NavHostFragment.findNavController(this).navigateUp();
                }
            });
        });

        return view;
    }

    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
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

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(webAppInterface, "Android");
        webView.loadUrl("file:///android_asset/codemirror.html");
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
} 




