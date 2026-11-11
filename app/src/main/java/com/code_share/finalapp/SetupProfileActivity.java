package com.code_share.finalapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SetupProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        TextView title = findViewById(R.id.setup_profile_title);
        if (title != null) {
            title.setText("Complete your profile");
        }
    }
}
