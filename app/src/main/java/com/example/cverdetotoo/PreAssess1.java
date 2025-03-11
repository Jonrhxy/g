package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PreAssess1 extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove the action bar (header)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_pre_assess1);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Find the start button by its ID
        Button startButton = findViewById(R.id.prebtnstart1);

        // Set an OnClickListener on the button to perform the Firestore check
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPreAssessData1();
            }
        });
    }

    /**
     * Checks Firestore for existing pre-assessment data based on the user's username.
     * If data exists, redirects to Homepage; otherwise, navigates to PreAsses11.
     */
    private void checkPreAssessData1() {
        // Retrieve current user's username from FirebaseAuth
        String username = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (username == null || username.isEmpty()) {
            // Fallback handling if display name is not set, you may use UID or show an error message
            username = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // Reference to the user's pre-assessment document in the "PreAssess" collection
        DocumentReference docRef = db.collection("PreAssess").document(username);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Data exists, redirect to Homepage
                    Intent intent = new Intent(PreAssess1.this, video1.class);
                    startActivity(intent);
                } else {
                    // No data exists, redirect to PreAsses11 to collect data
                    Intent intent = new Intent(PreAssess1.this, PreAsses11.class);
                    startActivity(intent);
                }
            } else {
                // In case of an error, optionally log the error and redirect to PreAsses11
                Intent intent = new Intent(PreAssess1.this, PreAsses11.class);
                startActivity(intent);
            }
        });
    }
}
