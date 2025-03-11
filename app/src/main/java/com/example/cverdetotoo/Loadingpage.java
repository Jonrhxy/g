package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class Loadingpage extends AppCompatActivity {

    private ProgressBar progressBar;
    private static final int LOADING_TIME = 3000; // 3 seconds delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadingpage);

        // Initialize ProgressBar from the layout
        progressBar = findViewById(R.id.progressBar);

        // Optionally hide the ProgressBar if it's not needed visually
        progressBar.setVisibility(View.INVISIBLE);

        // Delay for 3 seconds then redirect to navbar activity
        new Handler().postDelayed(() -> navigateTo(navbar.class), LOADING_TIME);
    }

    private void navigateTo(Class<?> destination) {
        Intent intent = new Intent(Loadingpage.this, destination);
        startActivity(intent);
        finish();
    }
}
