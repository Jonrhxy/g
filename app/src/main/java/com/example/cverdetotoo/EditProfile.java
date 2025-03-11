package com.example.cverdetotoo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    private ImageView ivBack;
    private EditText etFirstName, etLastName, etUsername, etEmail, etBirthDate;
    private RadioGroup rgGender;
    private RadioButton rbFemale, rbMale, rbOthers;
    private TextView tvSaveChanges;

    // Firebase instances
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Original data loaded from Firestore (document ID is based on the original username)
    private String originalFirstName;
    private String originalLastName;
    private String originalUsername;  // Document ID used for lookups
    private String originalEmail;
    private String originalBirthDate;
    private String originalGender;

    private Button resetPasswordButton;
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

        // Fetch user data from Firestore using the username as the document ID
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
        etBirthDate = findViewById(R.id.etBirthDate);
        rgGender = findViewById(R.id.rgGender);
        rbFemale = findViewById(R.id.rbFemale);
        rbMale = findViewById(R.id.rbMale);
        rbOthers = findViewById(R.id.rbOthers);
        resetPasswordButton = findViewById(R.id.btnResPass);
        tvSaveChanges = findViewById(R.id.tvSaveChanges);


    }

    /**
     * Set up click listeners for interactive views.
     */
    private void setupListeners() {
        // Back arrow click finishes the activity
        ivBack.setOnClickListener(view -> finish());

        // Show DatePicker when clicking the birth date field
        etBirthDate.setOnClickListener(view -> showDatePickerDialog());


        resetPasswordButton.setOnClickListener(v -> resetPassword());

        // Handle the save changes button click
        tvSaveChanges.setOnClickListener(view -> {
            Log.d(TAG, "Save Changes clicked");
            saveProfileChanges();
        });
    }

    /**
     * Displays a DatePickerDialog to select a birth date.
     */
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day   = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Months are zero-indexed so add one for display purposes
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etBirthDate.setText(date);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Load existing user data from Firestore.
     * This method uses the username (obtained from currentUser.getDisplayName()) as the document ID.
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
                String firstName = documentSnapshot.getString("firstName");
                String lastName  = documentSnapshot.getString("lastName");
                String email     = documentSnapshot.getString("email");
                String username  = documentSnapshot.getString("username");
                String birthDate = documentSnapshot.getString("birthDate");
                String gender    = documentSnapshot.getString("gender");

                if (firstName != null) {
                    etFirstName.setText(firstName);
                    originalFirstName = firstName;
                }
                if (lastName != null) {
                    etLastName.setText(lastName);
                    originalLastName = lastName;
                }
                if (email != null) {
                    etEmail.setText(email);
                    originalEmail = email;
                }
                if (username != null) {
                    etUsername.setText(username);
                    originalUsername = username;
                }
                if (birthDate != null) {
                    etBirthDate.setText(birthDate);
                    originalBirthDate = birthDate;
                }
                if (gender != null) {
                    originalGender = gender;
                    if (gender.equalsIgnoreCase("Female")) {
                        rgGender.check(rbFemale.getId());
                    } else if (gender.equalsIgnoreCase("Male")) {
                        rgGender.check(rbMale.getId());
                    } else if (gender.equalsIgnoreCase("Others")) {
                        rgGender.check(rbOthers.getId());
                    }
                }
            } else {
                Toast.makeText(EditProfile.this, "User data not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(EditProfile.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching user data", e);
            finish();
        });
    }

    /**
     * Validate input and update Firestore only if changes are detected.
     * Regardless of whether the username field changes, this method updates the fields in the existing document.
     */
    private void saveProfileChanges() {
        String updatedFirstName = etFirstName.getText().toString().trim();
        String updatedLastName  = etLastName.getText().toString().trim();
        String updatedUsername  = etUsername.getText().toString().trim();
        String updatedEmail     = etEmail.getText().toString().trim();
        String updatedBirthDate = etBirthDate.getText().toString().trim();
        String updatedGender    = "";

        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == rbFemale.getId()) {
            updatedGender = "Female";
        } else if (selectedId == rbMale.getId()) {
            updatedGender = "Male";
        } else if (selectedId == rbOthers.getId()) {
            updatedGender = "Others";
        }

        if (updatedFirstName.isEmpty() || updatedLastName.isEmpty() || updatedUsername.isEmpty() ||
                updatedEmail.isEmpty() || updatedBirthDate.isEmpty() || updatedGender.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if any field has changed compared to the original data
        if (updatedFirstName.equals(originalFirstName) &&
                updatedLastName.equals(originalLastName) &&
                updatedUsername.equals(originalUsername) &&
                updatedEmail.equals(originalEmail) &&
                updatedBirthDate.equals(originalBirthDate) &&
                updatedGender.equalsIgnoreCase(originalGender)) {
            Toast.makeText(this, "No changes to update.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare the updated data map (update the "username" field as well)
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("firstName", updatedFirstName);
        updatedData.put("lastName", updatedLastName);
        updatedData.put("username", updatedUsername);
        updatedData.put("email", updatedEmail);
        updatedData.put("birthDate", updatedBirthDate);
        updatedData.put("gender", updatedGender);

        // Update the existing document (document ID remains the same as originalUsername)
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
     * Redirects to the Navbar activity.
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

    private void redirectToNavbar() {
        Intent intent = new Intent(EditProfile.this, navbar.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}