package com.code_share.finalapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;
    
    private CircleImageView profileImageView;
    private Uri imageUri;
    private MaterialButton adminSettingsButton;
    private boolean isAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        // Initialize views
        profileImageView = view.findViewById(R.id.profile_image);
        MaterialButton changeProfileImageButton = view.findViewById(R.id.change_profile_image_button);
        MaterialButton changeUsernameButton = view.findViewById(R.id.change_username_button);
        MaterialButton changePasswordButton = view.findViewById(R.id.change_password_button);
        MaterialButton changeEmailButton = view.findViewById(R.id.change_email_button);
        MaterialButton deleteAccountButton = view.findViewById(R.id.delete_account_button);
        MaterialButton aboutButton = view.findViewById(R.id.about_button);
        MaterialButton privacyButton = view.findViewById(R.id.privacy_button);
        MaterialButton logoutButton = view.findViewById(R.id.logout_button);
        adminSettingsButton = view.findViewById(R.id.admin_settings_button);
        SwitchMaterial darkModeSwitch = view.findViewById(R.id.dark_mode_switch);
        SwitchMaterial notificationsSwitch = view.findViewById(R.id.notifications_switch);
        
        // Check if user is admin
        checkAdminStatus();
        
        // Load current profile image
        loadProfileImage();

        // Set click listeners
        changeProfileImageButton.setOnClickListener(v -> openImagePicker());
        changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        changeEmailButton.setOnClickListener(v -> showChangeEmailDialog());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
        aboutButton.setOnClickListener(v -> showAboutDialog());
        privacyButton.setOnClickListener(v -> showPrivacyPolicy());
        logoutButton.setOnClickListener(v -> logoutUser());
        adminSettingsButton.setOnClickListener(v -> {
            if (isAdmin) {
                navigateToAdminSettings();
            } else {
                Toast.makeText(getContext(), "You don't have permission to access admin settings", Toast.LENGTH_SHORT).show();
            }
        });

        // Set switch listeners
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Implement dark mode toggle
            Toast.makeText(getContext(), "Dark mode: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Implement notifications toggle
            Toast.makeText(getContext(), "Notifications: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void showChangeUsernameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_username, null);
        TextInputEditText newUsernameInput = dialogView.findViewById(R.id.new_username_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Username")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String newUsername = newUsernameInput.getText().toString().trim();
                    if (!newUsername.isEmpty()) {
                        updateUsername(newUsername);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUsername(String newUsername) {
        if (userRef != null) {
            userRef.child("username").setValue(newUsername)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.new_password_input);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirm_password_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString().trim();
                    String newPassword = newPasswordInput.getText().toString().trim();
                    String confirmPassword = confirmPasswordInput.getText().toString().trim();

                    if (!currentPassword.isEmpty() && !newPassword.isEmpty() && !confirmPassword.isEmpty()) {
                        if (newPassword.equals(confirmPassword)) {
                            updatePassword(currentPassword, newPassword);
                        } else {
                            Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String currentPassword, String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(getContext(), "Please log in again before changing your password", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> currentUser.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid1 -> Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChangeEmailDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
        TextInputEditText currentPasswordInput = dialogView.findViewById(R.id.current_password_input);
        TextInputEditText newEmailInput = dialogView.findViewById(R.id.new_email_input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Email")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString().trim();
                    String newEmail = newEmailInput.getText().toString().trim();

                    if (!currentPassword.isEmpty() && !newEmail.isEmpty()) {
                        updateEmail(currentPassword, newEmail);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmail(String currentPassword, String newEmail) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(getContext(), "Please log in again before changing your email", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> currentUser.updateEmail(newEmail)
                        .addOnSuccessListener(aVoid1 -> Toast.makeText(getContext(), "Email updated successfully", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in again before deleting your account", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRef == null) {
            Toast.makeText(getContext(), "Unable to delete account data at the moment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete ru.myitschool.finalproject.User data from Realtime Database
        userRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Delete Firebase Auth account
                    currentUser.delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                requireActivity().finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete ru.myitschool.finalproject.User data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("About App")
                .setMessage("Python Learning App v1.0\n\n" +
                        "A comprehensive app for learning Python programming with interactive lessons, " +
                        "exercises, and a supportive community.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPrivacyPolicy() {
        // TODO: Implement privacy policy view
        Toast.makeText(getContext(), "Privacy policy feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void loadProfileImage() {
        if (currentUser == null || userRef == null) {
            return;
        }

        userRef.child("photoUrl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String photoUrl = dataSnapshot.getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(requireContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImageView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load profile image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (currentUser == null || userRef == null || imageUri == null) {
            return;
        }

        ImgBBService imgBBService = new ImgBBService(requireContext());
        imgBBService.uploadImage(imageUri, new ImgBBService.OnImageUploadListener() {
            @Override
            public void onSuccess(String imageUrl) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(Uri.parse(imageUrl))
                        .build();

                currentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                userRef.child("photoUrl").setValue(imageUrl)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile image in database", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(getContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to upload image: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAdminStatus() {
        if (currentUser == null || userRef == null) {
            return;
        }

        userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean admin = dataSnapshot.getValue(Boolean.class);
                isAdmin = admin != null && admin;

                // Show/hide admin settings button based on admin status
                adminSettingsButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to check admin status", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void navigateToAdminSettings() {
        AdminSettingsFragment fragment = new AdminSettingsFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    
    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
