package com.example.cverdetotoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.widget.TextView;
import android.content.SharedPreferences;

public class ActivityLog extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Set up the Toolbar (Red Header)
        Toolbar toolbar = findViewById(R.id.toolbar_activity_log);
        setSupportActionBar(toolbar);

        // Enable Back Button (Optional)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Retrieve and display the actual finish time
        SharedPreferences prefs = getSharedPreferences("GameStats", MODE_PRIVATE);
        String allRecords = prefs.getString("game_records", "No game records found.");

        TextView gameTimeTextView = findViewById(R.id.game_time_text);
        gameTimeTextView.setText(allRecords);

    }
}
