package com.example.cverdetotoo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.example.cverdetotoo.MainActivity.PREFS_NAME;
import static com.example.cverdetotoo.MainActivity.PREF_IS_LOGGED_IN;

public class Settings extends AppCompatActivity {

    // Declare views
    private ImageView ivBack;
    private CardView cardEditProfile, cardTermsConditions, cardAboutUs, cardDeleteAccount,cardActivityLog;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize the views
        ivBack = findViewById(R.id.ivBack);
        cardEditProfile = findViewById(R.id.cardEditProfile);
        cardTermsConditions = findViewById(R.id.cardTermsConditions);
        cardAboutUs = findViewById(R.id.cardAboutUs);
        cardDeleteAccount = findViewById(R.id.cardDeleteAccount);
        btnLogout = findViewById(R.id.btnLogout);
        cardActivityLog = findViewById(R.id.cardActivityLog);

        // Back arrow: simply finish the activity
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Edit Profile: navigate to EditProfileActivity
        cardEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, EditProfile.class);
                startActivity(intent);
            }
        });

        // Terms & Conditions: navigate to TermsAndCon activity
        cardTermsConditions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, TermsAndCon.class);
                startActivity(intent);
            }
        });

        // About Us: navigate to AboutUs activity
        cardAboutUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, AboutUs.class);
                startActivity(intent);
            }
        });

        // Delete Account: schedule deletion (save deletion schedule to Firestore using username as document ID)
        cardDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmAndScheduleDeletion();
            }
        });

        // Logout button: perform logout action
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutUser();
            }
        });

        cardActivityLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.this, ActivityLog.class);
                startActivity(intent);
            }
        });
    }


    /**
     * Displays a confirmation dialog to schedule account deletion.
     */
    private void confirmAndScheduleDeletion() {
        new AlertDialog.Builder(this)
                .setTitle("Schedule Account Deletion")
                .setMessage("Do you want to schedule your account for deletion in 30 days? " +
                        "This will save a deletion schedule in Firestore using your username. " +
                        "You can cancel by signing in before then.")
                .setPositiveButton("Schedule Deletion", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scheduleAccountDeletion();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Schedules deletion by saving a deletion timestamp (30 days from now) and a flag into Firestore.
     * The document is stored under the "users" collection using the user's username (display name) as the document ID.
     */
    private void scheduleAccountDeletion() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(Settings.this, "No user is currently signed in.", Toast.LENGTH_LONG).show();
            return;
        }

        // Use username (display name) instead of userId
        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) {
            Toast.makeText(Settings.this, "Username not available. Cannot schedule deletion.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Calculate deletion date: 30 days from now
        long delayMillis = 30L * 24 * 60 * 60 * 1000; // 30 days in milliseconds
        Date deletionDate = new Date(System.currentTimeMillis() + delayMillis);

        // Data to save: deletion timestamp and flag
        Map<String, Object> data = new HashMap<>();
        data.put("scheduledDeletion", deletionDate);
        data.put("deletionScheduled", true);

        // Save the schedule to Firestore using set() with merge options,
        // storing the document under the "users" collection using the username as the document ID.
        db.collection("users").document(username)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(Settings.this, "Account scheduled for deletion on: " + deletionDate.toString(), Toast.LENGTH_LONG).show();
                        logoutUser();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Settings.this, "Failed to schedule deletion: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Logs out the user and navigates to MainActivity.
     */
    private void logoutUser() {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();

        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(Settings.this, gso).signOut();

        // Update shared preferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, false);
        editor.apply();

        // Navigate to MainActivity
        Intent intent = new Intent(Settings.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}