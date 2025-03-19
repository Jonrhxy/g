package com.example.cverdetotoo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderBoardsFragment extends Fragment {

    private TextView firstPlaceName, firstPlaceScore, firstPlaceRole;
    private TextView secondPlaceName, secondPlaceScore, secondPlaceRole;
    private TextView thirdPlaceName, thirdPlaceScore, thirdPlaceRole;
    private TextView fourthPlaceName, fourthPlaceScore, fourthPlaceRole;
    private TextView fifthPlaceName, fifthPlaceScore, fifthPlaceRole;
    private TextView sixthPlaceName, sixthPlaceScore, sixthPlaceRole;
    private TextView seventhPlaceName, seventhPlaceScore, seventhPlaceRole;
    private TextView eighthPlaceName, eighthPlaceScore, eighthPlaceRole;
    private TextView ninthPlaceName, ninthPlaceScore, ninthPlaceRole;
    private TextView tenthPlaceName, tenthPlaceScore, tenthPlaceRole;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_leader_boards, container, false);

        // Binding UI elements for top 10 positions
        firstPlaceName = view.findViewById(R.id.tvFirstPlaceName);
        firstPlaceScore = view.findViewById(R.id.tvFirstPlacePoints);
        firstPlaceRole = view.findViewById(R.id.tvFirstPlaceRole);

        secondPlaceName = view.findViewById(R.id.tvSecondPlaceName);
        secondPlaceScore = view.findViewById(R.id.tvSecondPlacePoints);
        secondPlaceRole = view.findViewById(R.id.tvSecondPlaceRole);

        thirdPlaceName = view.findViewById(R.id.tvThirdPlaceName);
        thirdPlaceScore = view.findViewById(R.id.tvThirdPlacePoints);
        thirdPlaceRole = view.findViewById(R.id.tvThirdPlaceRole);

        fourthPlaceName = view.findViewById(R.id.tvFourthPlaceName);
        fourthPlaceScore = view.findViewById(R.id.tvFourthPlacePoints);
        fourthPlaceRole = view.findViewById(R.id.tvFourthPlaceRole);

        fifthPlaceName = view.findViewById(R.id.tvFifthPlaceName);
        fifthPlaceScore = view.findViewById(R.id.tvFifthPlacePoints);
        fifthPlaceRole = view.findViewById(R.id.tvFifthPlaceRole);

        sixthPlaceName = view.findViewById(R.id.tvSixthPlaceName);
        sixthPlaceScore = view.findViewById(R.id.tvSixthPlacePoints);
        sixthPlaceRole = view.findViewById(R.id.tvSixthPlaceRole);

        seventhPlaceName = view.findViewById(R.id.tvSeventhPlaceName);
        seventhPlaceScore = view.findViewById(R.id.tvSeventhPlacePoints);
        seventhPlaceRole = view.findViewById(R.id.tvSeventhPlaceRole);

        eighthPlaceName = view.findViewById(R.id.tvEighthPlaceName);
        eighthPlaceScore = view.findViewById(R.id.tvEighthPlacePoints);
        eighthPlaceRole = view.findViewById(R.id.tvEighthPlaceRole);

        ninthPlaceName = view.findViewById(R.id.tvNinthPlaceName);
        ninthPlaceScore = view.findViewById(R.id.tvNinthPlacePoints);
        ninthPlaceRole = view.findViewById(R.id.tvNinthPlaceRole);

        tenthPlaceName = view.findViewById(R.id.tvTenthPlaceName);
        tenthPlaceScore = view.findViewById(R.id.tvTenthPlacePoints);
        tenthPlaceRole = view.findViewById(R.id.tvTenthPlaceRole);

        db = FirebaseFirestore.getInstance();

        loadLeaderBoardData();

        return view;
    }

    // Method to update points and assign a role
    private void updatePoints(TextView pointsView, TextView roleView, int points) {
        pointsView.setText(String.valueOf(points) + " Points");

        if (points >= 1000) {
            roleView.setText("Eco Champion");
        } else if (points >= 700) {
            roleView.setText("Eco Leader");
        } else if (points >= 500) {
            roleView.setText("Eco Protector");
        } else if (points >= 100) {
            roleView.setText("Eco Rookie");
        } else {
            roleView.setText("Eco Beginner");
        }
    }

    private void loadLeaderBoardData() {
        db.collection("Games")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Temporary list to hold the leaderboard data
                        List<UserData> leaderboardData = new ArrayList<>();

                        // We'll store all the asynchronous sub-fetch tasks here
                        List<Task<?>> tasks = new ArrayList<>();

                        // For each doc in Games (each user)
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // The doc ID is typically the username (or userId)
                            final String userId = document.getId();

                            // If the doc has a "username" field, use it. Otherwise fallback to doc ID
                            String username = document.contains("username")
                                    ? document.getString("username")
                                    : userId;

                            // Main doc points from Games/{username}
                            Long mainPointsLong = document.getLong("points");
                            final int mainPoints = (mainPointsLong != null) ? mainPointsLong.intValue() : 0;

                            // Now fetch additional points from Gamez/{username}/records/BattleEco and CYCF
                            Task<Integer> totalPointsTask = getUserTotalPoints(userId, mainPoints);

                            // Once that sub-task finishes, add an entry to the leaderboard list
                            totalPointsTask.addOnSuccessListener(totalPoints -> {
                                leaderboardData.add(new UserData(username, totalPoints));
                            });

                            tasks.add(totalPointsTask);
                        }

                        // When ALL tasks are complete, we can safely sort and update UI
                        Tasks.whenAll(tasks).addOnSuccessListener(aVoid -> {
                            // Sort descending by score
                            leaderboardData.sort((a, b) -> Integer.compare(b.highScore, a.highScore));
                            updateLeaderboardUI(leaderboardData);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(),
                                    "Error summing leaderboard data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(getContext(),
                                "Error loading leaderboard data: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Helper method to fetch sub-collection points from:
     *   Gamez/{userId}/records/BattleEco -> "points"
     *   Gamez/{userId}/records/CYCF -> "points"
     * Then add them to the given mainPoints and return the total via a Task<Integer>.
     */
    private Task<Integer> getUserTotalPoints(String userId, int mainPoints) {
        // References for sub-collection docs
        DocumentReference battleEcoRef = db.collection("Gamez")
                .document(userId)
                .collection("records")
                .document("BattleEco");

        DocumentReference cycfRef = db.collection("Gamez")
                .document(userId)
                .collection("records")
                .document("CYCF");

        // Fetch each doc
        Task<DocumentSnapshot> battleEcoTask = battleEcoRef.get();
        Task<DocumentSnapshot> cycfTask = cycfRef.get();

        // Combine them with Tasks.whenAllSuccess(...)
        return Tasks.whenAllSuccess(battleEcoTask, cycfTask)
                .continueWith(task -> {
                    // Start total with mainPoints from Games/{userId}
                    int total = mainPoints;

                    DocumentSnapshot battleEcoSnap = battleEcoTask.getResult();
                    if (battleEcoSnap != null && battleEcoSnap.exists()) {
                        Long bePoints = battleEcoSnap.getLong("points");
                        if (bePoints != null) {
                            total += bePoints.intValue();
                        }
                    }

                    DocumentSnapshot cycfSnap = cycfTask.getResult();
                    if (cycfSnap != null && cycfSnap.exists()) {
                        Long cycfPoints = cycfSnap.getLong("points");
                        if (cycfPoints != null) {
                            total += cycfPoints.intValue();
                        }
                    }

                    // Return the final sum
                    return total;
                });
    }


    private void updateLeaderboardUI(List<UserData> leaderboardData) {
        int totalEntries = Math.min(leaderboardData.size(), 10);
        for (int i = 0; i < totalEntries; i++) {
            UserData user = leaderboardData.get(i);
            switch (i) {
                case 0:
                    firstPlaceName.setText(user.username);
                    updatePoints(firstPlaceScore, firstPlaceRole, user.highScore);
                    break;
                case 1:
                    secondPlaceName.setText(user.username);
                    updatePoints(secondPlaceScore, secondPlaceRole, user.highScore);
                    break;
                case 2:
                    thirdPlaceName.setText(user.username);
                    updatePoints(thirdPlaceScore, thirdPlaceRole, user.highScore);
                    break;
                case 3:
                    fourthPlaceName.setText(user.username);
                    updatePoints(fourthPlaceScore, fourthPlaceRole, user.highScore);
                    break;
                case 4:
                    fifthPlaceName.setText(user.username);
                    updatePoints(fifthPlaceScore, fifthPlaceRole, user.highScore);
                    break;
                case 5:
                    sixthPlaceName.setText(user.username);
                    updatePoints(sixthPlaceScore, sixthPlaceRole, user.highScore);
                    break;
                case 6:
                    seventhPlaceName.setText(user.username);
                    updatePoints(seventhPlaceScore, seventhPlaceRole, user.highScore);
                    break;
                case 7:
                    eighthPlaceName.setText(user.username);
                    updatePoints(eighthPlaceScore, eighthPlaceRole, user.highScore);
                    break;
                case 8:
                    ninthPlaceName.setText(user.username);
                    updatePoints(ninthPlaceScore, ninthPlaceRole, user.highScore);
                    break;
                case 9:
                    tenthPlaceName.setText(user.username);
                    updatePoints(tenthPlaceScore, tenthPlaceRole, user.highScore);
                    break;
            }
        }
    }

    public static class UserData {
        public String username;
        public int highScore;

        public UserData() {
        }

        public UserData(String username, int highScore) {
            this.username = username;
            this.highScore = highScore;
        }
    }
}
