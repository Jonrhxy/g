package com.example.cverdetotoo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.cverdetotoo.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ActivityResultLauncher<Intent> preAssessLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment.
        View root = inflater.inflate(R.layout.activity_home, container, false);

        // Fetch the welcome popup data.
        fetchPopupDataAndShow();

        // Check if the account deletion is scheduled and show the prompt if so.
        checkDeletionSchedule();

        // Register the ActivityResultLauncher for PreAssess activities.
        preAssessLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // Launch video1 activity after successful PreAssess.
                            Intent videoIntent = new Intent(getActivity(), video1.class);
                            startActivity(videoIntent);
                        }
                    }
                }
        );

        // Set click listeners for video cards.
        CardView vid1Card = root.findViewById(R.id.vid1);
        CardView vid2Card = root.findViewById(R.id.vid2);
        CardView vid4Card = root.findViewById(R.id.vid4);
        vid1Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), PreAssess1.class);
                preAssessLauncher.launch(intent);
            }
        });
        vid2Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), PreAssess2.class);
                preAssessLauncher.launch(intent);
            }
        });
        vid4Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), videoQCU.class);
                preAssessLauncher.launch(intent);
            }
        });

        // Set click listeners for trivia cards.
        CardView trivia1Card = root.findViewById(R.id.trivia1);
        CardView trivia2Card = root.findViewById(R.id.trivia2);
        CardView trivia3Card = root.findViewById(R.id.trivia3);
        CardView trivia4Card = root.findViewById(R.id.trivia4);

        trivia1Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent triviaIntent = new Intent(getActivity(), StoryActivity11.class);
                startActivity(triviaIntent);
            }
        });
        trivia2Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent triviaIntent = new Intent(getActivity(), StoryActivity22.class);
                startActivity(triviaIntent);
            }
        });
        trivia3Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent triviaIntent = new Intent(getActivity(), StoryActivity33.class);
                startActivity(triviaIntent);
            }
        });
        trivia4Card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent triviaIntent = new Intent(getActivity(), StoryActivity44.class);
                startActivity(triviaIntent);
            }
        });

        return root;
    }

    /**
     * Fetches the Firestore document for the current user (using their username from FirebaseAuth)
     * and shows the welcome popup only if it has not already been marked as shown.
     */
    private void fetchPopupDataAndShow() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated; cannot fetch popup data.");
            return;
        }

        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username is not available; cannot fetch popup data.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Use the username as the document ID.
        DocumentReference popupRef = db.collection("popups").document(username);

        popupRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Boolean popupShown = documentSnapshot.exists() ? documentSnapshot.getBoolean("popup5") : null;
                        if (Boolean.TRUE.equals(popupShown)) {
                            Log.d(TAG, "Popup already shown for username: " + username);
                        } else {
                            Log.d(TAG, "Popup not shown yet for username: " + username + ". Launching popup.");
                            Intent intent = new Intent(getActivity(), popupWelcome.class);
                            startActivity(intent);
                            popupRef.set(Collections.singletonMap("popup5", true));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error fetching popup data.", e);
                        // Fallback: show the popup and update Firestore.
                        Intent intent = new Intent(getActivity(), popupWelcome.class);
                        startActivity(intent);
                        popupRef.set(Collections.singletonMap("popup5", true));
                    }
                });
    }

    /**
     * Checks Firestore for a deletion schedule using the current user's username.
     * If a deletion schedule exists, shows a prompt.
     */
    private void checkDeletionSchedule() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username not available for deletion schedule check.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(username)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Boolean deletionScheduled = documentSnapshot.getBoolean("deletionScheduled");
                            if (deletionScheduled != null && deletionScheduled) {
                                showDeletionPrompt();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking deletion schedule", e));
    }

    /**
     * Shows a prompt asking whether to continue with deletion or cancel it.
     */
    private void showDeletionPrompt() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Account Deletion Scheduled")
                .setMessage("Your account is scheduled for deletion. Do you want to continue with deletion or cancel it?")
                .setCancelable(false)
                .setPositiveButton("Continue Deletion", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getActivity(), "Continuing deletion. You will be logged out.", Toast.LENGTH_LONG).show();
                        logoutUser();
                    }
                })
                .setNegativeButton("Cancel Deletion", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDeletionSchedule();
                    }
                })
                .show();
    }

    /**
     * Cancels the deletion schedule by updating Firestore.
     */
    private void cancelDeletionSchedule() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("deletionScheduled", false);
        data.put("scheduledDeletion", null);

        db.collection("users").document(username)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Deletion schedule cancelled.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to cancel deletion schedule: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Logs out the user and redirects to MainActivity.
     */
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(getActivity(), gso).signOut();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hide the action bar for this fragment.
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }
}