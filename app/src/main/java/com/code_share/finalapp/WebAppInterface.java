package com.code_share.finalapp;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    private Context mContext;
    private String userCode = "";
    private OnCodeReceivedListener listener;

    public interface OnCodeReceivedListener {
        void onCodeReceived(String code);
    }

    public WebAppInterface(Context c) {
        mContext = c;
    }

    public void setOnCodeReceivedListener(OnCodeReceivedListener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void sendCodeToAndroid(String code) {
        Log.d("WebAppInterface", "Received code from JavaScript");
        try {
            if (code != null && !code.trim().isEmpty()) {
                this.userCode = code.trim();
                Log.d("WebAppInterface", "Code length: " + this.userCode.length());
                
                if (listener != null) {
                    listener.onCodeReceived(this.userCode);
                }
            } else {
                Log.d("WebAppInterface", "Received null or empty code");
                this.userCode = "";
                if (mContext != null) {
                    Toast.makeText(mContext, "No code content received", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.d("WebAppInterface", "Error processing code: " + e.getMessage());
            if (mContext != null) {
                Toast.makeText(mContext, "Error processing code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getUserCode() {
        Log.d("WebAppInterface", "Getting  code, length: " + userCode.length());
        return userCode;
    }
}





