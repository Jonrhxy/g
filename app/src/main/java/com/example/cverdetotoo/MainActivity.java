package com.example.cverdetotoo;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // Constants for SharedPreferences
    public static final String PREFS_NAME = "loginPrefs";
    public static final String PREF_IS_LOGGED_IN = "isLoggedIn";

    // Static flags to track app visibility
    public static boolean isForeground = false;
    public static long lastBackgroundTime = 0;

    private Button getStartBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Start foreground service to handle notifications
        startNotificationService();

        // Check if the user is logged in
        if (isUserLoggedIn()) {
            navigateToNavbar();
            return; // Prevents further execution of this method
        }

        // If not logged in, show the main layout
        setContentView(R.layout.activity_main);
        initializeUI();
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false);
    }

    private void navigateToNavbar() {
        Intent intent = new Intent(MainActivity.this, navbar.class);
        startActivity(intent);
        finish(); // Close MainActivity
    }

    private void initializeUI() {
        getStartBtn = findViewById(R.id.getstartbtn);
        getStartBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signin.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

        // Cancel any notifications when the user returns to the app
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;

        // Record the time when the user leaves the app
        lastBackgroundTime = System.currentTimeMillis();
    }
}
