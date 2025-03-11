package com.example.cverdetotoo;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // Static flag to track whether the app is visible.
    public static boolean isForeground = false;
    // Timestamp recording when the app went to the background.
    public static long lastBackgroundTime = 0;

    private Button getstartbtn;
    public static final String PREFS_NAME = "loginPrefs";
    public static final String PREF_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar if it exists.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        // Check if user is logged in
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false);
        if (isLoggedIn) {
            // If logged in, start the navbar activity and finish MainActivity.
            Intent intent = new Intent(MainActivity.this, navbar.class);
            startActivity(intent);
            finish();
            return;
        }

        // If not logged in, show MainActivity layout.
        setContentView(R.layout.activity_main);

        // Initialize and set up the "Get Started" button.
        getstartbtn = findViewById(R.id.getstartbtn);
        getstartbtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signin.class);
            startActivity(intent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        // Cancel any notifications when the user returns to the app.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
        // Record the time when the user leaves the app.
        lastBackgroundTime = System.currentTimeMillis();
    }
}
