package com.code_share.finalapp;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

public final class AdminProfileHelper {
    private AdminProfileHelper() {
    }

    public static void setupAdminProfileImage(View adminProfileLayout, boolean isAdmin, boolean isSuperAdmin, @Nullable String photoUrl) {
        if (adminProfileLayout == null) {
            return;
        }

        ImageView profileImage = adminProfileLayout.findViewById(R.id.admin_profile_image);
        ImageView adminBadge = adminProfileLayout.findViewById(R.id.admin_badge);

        if (profileImage != null) {
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(profileImage)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImage);
            } else {
                Glide.with(profileImage)
                        .load(R.drawable.default_profile)
                        .circleCrop()
                        .into(profileImage);
            }
        }

        if (adminBadge != null) {
            adminBadge.setVisibility((isAdmin || isSuperAdmin) ? View.VISIBLE : View.GONE);
        }
    }
}
