package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    private ImageView ivBack;
    private EditText etFirstName, etLastName, etUsername, etEmail;
    private TextView tvSaveChanges;
    private Button resetPasswordButton;

    // Firebase instances
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Original data loaded from Firestore
    private String originalFirstName, originalLastName, originalUsername;
    private String originalEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        setupListeners();

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch user data from Firestore
        loadUserData();
    }

    /**
     * Initialize all views from the layout.
     */
    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        resetPasswordButton = findViewById(R.id.btnResPass);
        tvSaveChanges = findViewById(R.id.tvSaveChanges);
    }

    /**
     * Set up click listeners for interactive views.
     */
    private void setupListeners() {
        ivBack.setOnClickListener(view -> finish());
        resetPasswordButton.setOnClickListener(v -> resetPassword());
        tvSaveChanges.setOnClickListener(view -> saveProfileChanges());
    }

    /**
     * Load existing user data from Firestore.
     */
    private void loadUserData() {
        String docId = currentUser.getDisplayName();
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "User data is incomplete", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        DocumentReference docRef = db.collection("users").document(docId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                originalFirstName = documentSnapshot.getString("firstName");
                originalLastName = documentSnapshot.getString("lastName");
                originalEmail = documentSnapshot.getString("email");
                originalUsername = documentSnapshot.getString("username");

                etFirstName.setText(originalFirstName);
                etLastName.setText(originalLastName);
                etEmail.setText(originalEmail);
                etUsername.setText(originalUsername);
            } else {
                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching user data", e);
            finish();
        });
    }

    /**
     * Validate input and update Firestore only if changes are detected.
     */
    private void saveProfileChanges() {
        String updatedFirstName = etFirstName.getText().toString().trim();
        String updatedLastName  = etLastName.getText().toString().trim();
        String updatedUsername  = etUsername.getText().toString().trim();
        String updatedEmail     = etEmail.getText().toString().trim();

        if (updatedFirstName.isEmpty() || updatedLastName.isEmpty() || updatedUsername.isEmpty() ||
                updatedEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (updatedFirstName.equals(originalFirstName) &&
                updatedLastName.equals(originalLastName) &&
                updatedUsername.equals(originalUsername) &&
                updatedEmail.equals(originalEmail)) {
            Toast.makeText(this, "No changes to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("firstName", updatedFirstName);
        updatedData.put("lastName", updatedLastName);
        updatedData.put("username", updatedUsername);
        updatedData.put("email", updatedEmail);

        DocumentReference docRef = db.collection("users").document(originalUsername);
        docRef.set(updatedData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfile.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    redirectToNavbar();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfile.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating profile", e);
                });
    }

    /**
     * Initiates the reset password process.
     */
    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Enter your registered email.");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email.");
            etEmail.requestFocus();
            return;
        }

        sendPasswordResetEmail(email);
    }

    /**
     * Sends a password reset email using Firebase Auth.
     */
    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditProfile.this, "Reset email sent successfully. Check your inbox.", Toast.LENGTH_SHORT).show();
                        redirectToNavbar();
                    } else {
                        Toast.makeText(EditProfile.this, "Error sending reset email. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Redirects to the Navbar activity.
     */
    private void redirectToNavbar() {
        Intent intent = new Intent(EditProfile.this, navbar.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
