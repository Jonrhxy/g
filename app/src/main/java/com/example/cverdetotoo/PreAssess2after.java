package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class PreAssess2after extends AppCompatActivity {

    private Button btnq1done;
    private TextView scoreTextView; // For displaying the numeric score and correct answers
    private FirebaseFirestore db;   // Firestore instance
    private FirebaseAuth auth;      // Firebase Authentication instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Initialize Firebase before using any Firebase service
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_pre_assess2after);

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize button and TextView
        btnq1done = findViewById(R.id.pre2done);
        scoreTextView = findViewById(R.id.pre2score);

        // Retrieve the score passed from the previous activity
        int score = getIntent().getIntExtra("score", 0);

        // Display only the numeric score in the TextView
        scoreTextView.setText(String.valueOf(score));

        // Save the score to Firestore
        saveScoreToFirestore(score);



        // Set click listener for the button
        btnq1done.setOnClickListener(v -> {
            if (validateBeforeRedirect()) {
                navigateToNavbar(score);
            } else {
                Toast.makeText(PreAssess2after.this, "Validation failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Save score to Firestore
    private void saveScoreToFirestore(int score) {
        // Get the current user from Firebase Authentication
        String username = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : null;

        if (username != null) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("score", score);
            scoreData.put("timestamp", System.currentTimeMillis());
            scoreData.put("username", username);

            db.collection("PreAssess")
                    .document(username)
                    .set(scoreData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(PreAssess2after.this, "Score saved successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(PreAssess2after.this, "Error saving score", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
        }
    }

    // Fetch all correct answers from Firestore (stored in the "quiz" collection)
      // Validation logic before navigating
    private boolean validateBeforeRedirect() {
        // Replace with your actual validation logic if needed
        return true;
    }

    // Navigate to the next activity (video1) and pass necessary data
    private void navigateToNavbar(int score) {
        Intent intent = new Intent(this, video1.class);
        intent.putExtra("isQuizDone", true);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }
}
