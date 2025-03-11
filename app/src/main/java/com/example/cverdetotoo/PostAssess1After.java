package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class PostAssess1After extends AppCompatActivity {

    private Button btnq1done;
    private TextView scoreTextView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_post_assess1_after);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnq1done = findViewById(R.id.post1done);
        scoreTextView = findViewById(R.id.post1score);

        int score = getIntent().getIntExtra("score", 0);
        scoreTextView.setText(String.valueOf(score));

        saveScoreToFirestore(score);


        btnq1done.setOnClickListener(v -> {
            markPreAssessmentCompleted(); // Store completion in Firestore
            navigateToNavbar(score);
        });
    }

    private void saveScoreToFirestore(int score) {
        String username = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : null;

        if (username != null) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("score", score);
            scoreData.put("timestamp", System.currentTimeMillis());
            scoreData.put("isCompleted", true); // Mark quiz as completed
            scoreData.put("username", username);

            db.collection("PostAssess")
                    .document(username)
                    .set(scoreData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(PostAssess1After.this, "Score saved successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(PostAssess1After.this, "Error saving score", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
        }
    }



    private void navigateToNavbar(int score) {
        Intent intent = new Intent(this, navbar.class);
        intent.putExtra("isCompleted", true);
        intent.putExtra("score", score);
        startActivity(intent);
        finish();
    }

    private void markPreAssessmentCompleted() {
        String username = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : null;
        if (username != null) {
            DocumentReference docRef = db.collection("PostAssess").document(username);
            docRef.update("isCompleted", true)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(PostAssess1After.this, "Assessment marked as completed", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(PostAssess1After.this, "Failed to update completion status", Toast.LENGTH_SHORT).show());
        }
    }
}
