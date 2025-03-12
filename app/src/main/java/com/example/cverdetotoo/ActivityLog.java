package com.example.cverdetotoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

public class ActivityLog extends AppCompatActivity {

    private static final String TAG = "ActivityLog";
    private LinearLayout logContainer;
    private TextView textDate;
    private FirebaseFirestore db;
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_activity_log);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set dynamic date in header (e.g., "March 03")
        textDate = findViewById(R.id.textDate);
        String currentDate = new SimpleDateFormat("MMMM dd", Locale.getDefault()).format(new Date());
        textDate.setText(currentDate);

        // Get the container where cards will be added
        logContainer = findViewById(R.id.logContainer);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Retrieve game records from SharedPreferences (for your card game)
        SharedPreferences gamePrefs = getSharedPreferences("GameStats", MODE_PRIVATE);
        String gameRecords = gamePrefs.getString("game_records", "No game records found.");

        // Get current user displayName for Firestore path
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = "unknown";
        if(currentUser != null && currentUser.getDisplayName() != null){
            displayName = currentUser.getDisplayName();
        }
        final String finalDisplayName = displayName;
        Log.d(TAG, "Fetching records for user: " + finalDisplayName);

        // Initial update of logs
        updateLogs(finalDisplayName, gameRecords);

        // Set up a handler to refresh logs every 5 minutes (300,000 ms)
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateLogs(finalDisplayName, gameRecords);
                refreshHandler.postDelayed(this, 300000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 300000);
    }

    /**
     * Update the log container with game records and tracking walk records.
     */
    private void updateLogs(String displayName, String gameRecords) {
        // Clear the container
        logContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Add Game Records card
        MaterialCardView gameCard = (MaterialCardView) inflater.inflate(R.layout.card_log_item, logContainer, false);
        TextView gameHeaderTitle = gameCard.findViewById(R.id.cardHeaderTitle);
        TextView gameHeaderContent = gameCard.findViewById(R.id.cardHeaderContent);
        gameHeaderTitle.setText("Game Records");
        gameHeaderContent.setText(gameRecords);
        logContainer.addView(gameCard);

        // Add a header for Tracking Walk Records
        MaterialCardView trackingHeaderCard = (MaterialCardView) inflater.inflate(R.layout.card_log_item, logContainer, false);
        TextView trackingHeaderTitle = trackingHeaderCard.findViewById(R.id.cardHeaderTitle);
        TextView trackingHeaderContent = trackingHeaderCard.findViewById(R.id.cardHeaderContent);
        trackingHeaderTitle.setText("Tracking Walk Records");
        trackingHeaderContent.setText(""); // No additional content
        logContainer.addView(trackingHeaderCard);

        // Fetch tracking walk records from Firestore
        db.collection("Games")
                .document(displayName)
                .collection("trackingwalk")
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    // Group documents by "date" using a TreeMap for sorted order
                    Map<String, List<DocumentSnapshot>> dateMap = new TreeMap<>();
                    for (DocumentSnapshot doc : docs) {
                        String date = doc.getString("date");
                        if (date == null) {
                            date = "N/A";
                        }
                        if (!dateMap.containsKey(date)) {
                            dateMap.put(date, new ArrayList<>());
                        }
                        dateMap.get(date).add(doc);
                    }
                    if (dateMap.isEmpty()) {
                        // No records found card
                        MaterialCardView noRecordsCard = (MaterialCardView) inflater.inflate(R.layout.card_log_item, logContainer, false);
                        TextView noRecordsTitle = noRecordsCard.findViewById(R.id.cardHeaderTitle);
                        TextView noRecordsContent = noRecordsCard.findViewById(R.id.cardHeaderContent);
                        noRecordsTitle.setText("No Tracking Records");
                        noRecordsContent.setText("");
                        logContainer.addView(noRecordsCard);
                    } else {
                        // For each date group, add a header and then each record
                        for (String dateKey : dateMap.keySet()) {
                            // Add date header card
                            MaterialCardView dateHeaderCard = (MaterialCardView) inflater.inflate(R.layout.card_log_item, logContainer, false);
                            TextView dateHeaderTitle = dateHeaderCard.findViewById(R.id.cardHeaderTitle);
                            TextView dateHeaderContent = dateHeaderCard.findViewById(R.id.cardHeaderContent);
                            dateHeaderTitle.setText("Date: " + dateKey);
                            dateHeaderContent.setText("");
                            logContainer.addView(dateHeaderCard);

                            List<DocumentSnapshot> dateDocs = dateMap.get(dateKey);
                            for (DocumentSnapshot doc : dateDocs) {
                                String time = doc.getString("time");
                                String distance = doc.getString("distanceSoFarKm");
                                Long steps = doc.getLong("stepsSoFar");
                                Double co2Saved = doc.getDouble("co2Saved");

                                String recordStr = String.format(Locale.getDefault(),
                                        "Time: %s\nDistance: %s km\nSteps: %s\nCO₂ Saved: %.2f kg",
                                        time != null ? time : "N/A",
                                        distance != null ? distance : "N/A",
                                        (steps != null ? steps.toString() : "N/A"),
                                        (co2Saved != null ? co2Saved : 0.0));

                                // Add a card for each tracking record
                                MaterialCardView recordCard = (MaterialCardView) inflater.inflate(R.layout.card_log_item, logContainer, false);
                                TextView recHeader = recordCard.findViewById(R.id.cardHeaderTitle);
                                TextView recContent = recordCard.findViewById(R.id.cardHeaderContent);
                                recHeader.setText(""); // no header for record detail
                                recContent.setText(recordStr);
                                logContainer.addView(recordCard);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tracking records: " + e.getMessage());
                    Toast.makeText(ActivityLog.this, "Error fetching tracking records.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}
