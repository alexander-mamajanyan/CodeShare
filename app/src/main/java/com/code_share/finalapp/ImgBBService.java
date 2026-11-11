package com.code_share.finalapp;

import android.content.Context;
import android.net.Uri;

public class ImgBBService {
    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    private final Context context;

    public ImgBBService(Context context) {
        this.context = context.getApplicationContext();
    }

    public void uploadImage(Uri imageUri, OnImageUploadListener listener) {
        if (imageUri == null) {
            if (listener != null) {
                listener.onError("No image selected");
            }
            return;
        }

        if (listener != null) {
            listener.onSuccess(imageUri.toString());
        }
    }
}
