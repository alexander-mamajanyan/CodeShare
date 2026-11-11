package com.code_share.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private EditText loginInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        loginInput = findViewById(R.id.email_log);
        passwordInput = findViewById(R.id.password_log);
        loginButton = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.registerNow);

        // Set click listeners
        loginButton.setOnClickListener(v -> loginUser());
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

        // Check if login is email or username
        if (login.contains("@")) {
            // Login with email
            loginWithEmail(login, password);
        } else {
            // Login with username
            loginWithUsername(login, password);
        }
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

    private void loginWithUsername(String username, String password) {
        // Query the database to find the ru.myitschool.finalproject.User with the given username
        databaseRef.child("users").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Get the first User with this username
                            String email = null;
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                email = userSnapshot.child("email").getValue(String.class);
                                break;
                            }
                            if (email != null) {
                                // Login with the found email
                                loginWithEmail(email, password);
                            } else {
                                Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(com.code_share.finalapp.LoginActivity.this, com.code_share.finalapp.MainActivity.class);
        startActivity(intent);
        finish();
    }
}


