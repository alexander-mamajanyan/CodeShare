package com.code_share.finalapp;

import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Content;
import android.os.Handler;
import android.os.Looper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIAssistant {
    private static final String TAG = "AIAssistant";
    private final GenerativeModelFutures model;
    private final Handler mainHandler;
    private final Executor executor;

    
    public interface ResponseCallback {
        void onResponse(String response);
    }

    public AIAssistant() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        this.model = apiKey.isEmpty()
            ? null
            : GenerativeModelFutures.from(new GenerativeModel("gemini-1.5-flash", apiKey));
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getCodeAssistance(String code, String prompt, ResponseCallback callback) {
        if (model == null) {
            mainHandler.post(() -> callback.onResponse(
                "AI assistance is not configured. Set the GEMINI_API_KEY environment variable and rebuild."
            ));
            return;
        }

        String fullPrompt;
        if (code != null && !code.isEmpty()) {
            fullPrompt = String.format(
                "You are a helpful coding assistant. Here is the code to analyze:\n\n```\n%s\n```\n\n%s",
                code, prompt
            );
        } else {
            fullPrompt = prompt;
        }

        Content content = new Content.Builder()
            .addText(fullPrompt)
            .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                mainHandler.post(() -> {
                    if (text != null && !text.isEmpty()) {
                        callback.onResponse(text);
                    } else {
                        callback.onResponse("I apologize, but I couldn't generate a response. Please try again.");
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error generating response", t);
                mainHandler.post(() -> {
                    callback.onResponse("Error: " + t.getMessage() + ". Please try again.");
                });
            }
        }, executor);
    }
}



