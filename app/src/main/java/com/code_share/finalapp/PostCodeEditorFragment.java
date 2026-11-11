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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONException;
import org.json.JSONTokener;

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
            codeEditor.evaluateJavascript("window.sendCode()", value -> {
                try {
                    Object decoded = new JSONTokener(value).nextValue();
                    if (!(decoded instanceof String)) {
                        throw new JSONException("Editor returned no code.");
                    }
                    Bundle result = new Bundle();
                    result.putString("code", (String) decoded);
                    getParentFragmentManager().setFragmentResult("code_result", result);
                    NavHostFragment.findNavController(this).navigateUp();
                } catch (JSONException e) {
                    Toast.makeText(requireContext(), "Couldn't read code from the editor.", Toast.LENGTH_SHORT).show();
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
        webSettings.setAllowContentAccess(false);

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

