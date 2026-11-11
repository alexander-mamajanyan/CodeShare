package com.code_share.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText loginInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private boolean navigatingToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        authStateListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                startMainActivity();
            }
        };

        // Initialize views
        loginInput = findViewById(R.id.email_log);
        passwordInput = findViewById(R.id.password_log);
        loginButton = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.registerNow);
        TextView forgotLink = findViewById(R.id.forgot);

        // Set click listeners
        loginButton.setOnClickListener(v -> loginUser());
        forgotLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotActivity.class)));
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(com.code_share.finalapp.LoginActivity.this, com.code_share.finalapp.RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loginUser() {
        String login = loginInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
            loginInput.setError("Enter a valid email address");
            return;
        }

        loginWithEmail(login, password);
    }

    private void loginWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startMainActivity();
                    } else {
                        Toast.makeText(com.code_share.finalapp.LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMainActivity() {
        if (navigatingToMain || isFinishing()) {
            return;
        }
        navigatingToMain = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(authStateListener);
    }
}
