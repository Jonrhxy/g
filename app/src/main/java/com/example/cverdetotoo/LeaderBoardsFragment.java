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
                        List<UserData> leaderboardData = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.contains("points")) {
                                String username = document.getString("username");
                                if (username == null) {
                                    username = document.getId();
                                }
                                int score = document.getLong("points") != null
                                        ? document.getLong("points").intValue()
                                        : 0;
                                leaderboardData.add(new UserData(username, score));
                            }
                        }

                        leaderboardData.sort((a, b) -> Integer.compare(b.highScore, a.highScore));
                        updateLeaderboardUI(leaderboardData);
                    } else {
                        Toast.makeText(getContext(),
                                "Error loading leaderboard data: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
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
