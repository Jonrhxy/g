package com.example.cverdetotoo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

public class ProfileFragment extends Fragment {

    // Coins UI
    private TextView textCoinValue, Coins;
    // Game Points UI
    private TextView textPointsValue;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Character selection UI
    private ImageView imageSelectedCharacter;
    private TextView textSelectedCharacterName;
    private RecyclerView recyclerUnlockedChars;
    private ImageView shopIcon;

    // Chart UI
    private BarChart barChart;
    private RadioGroup radioGroupTimeRange;

    // Tower UI
    private ImageView imageTower;          // tower background
    private FrameLayout towerTopCircle;    // optional top circle container
    private TextView textTowerSteps;       // displays total steps
    private TextView textMilestone50, textMilestone200, textMilestone500,
            textMilestone1000, textMilestone3000, textMilestone5000, textMilestone10000;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout file you posted: activity_profile.xml
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1) Top bar & greeting
        FrameLayout settingsLayout = view.findViewById(R.id.settingsLayout);
        settingsLayout.setOnClickListener(v -> startActivity(new Intent(getActivity(), Settings.class)));

        TextView textSeeAll = view.findViewById(R.id.textSeeAll);
        textSeeAll.setOnClickListener(v -> getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.constraintLayout, new BadgesFragment())
                .commit());

        TextView textGreeting = view.findViewById(R.id.textGreeting);
        if (mAuth.getCurrentUser() != null) {
            String username = mAuth.getCurrentUser().getDisplayName();
            if (username != null && !username.isEmpty()) {
                DocumentReference userDocRef = db.collection("users").document(username);
                userDocRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String firstName = task.getResult().getString("firstName");
                        if (firstName != null && !firstName.isEmpty()) {
                            textGreeting.setText("Hi, " + firstName + "!");
                        } else {
                            textGreeting.setText("Hi!");
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                        textGreeting.setText("Hi!");
                    }
                });
            } else {
                textGreeting.setText("Hi!");
            }
        } else {
            textGreeting.setText("Hi, Guest!");
        }

        // 2) Activity log button
        Button activityLog = view.findViewById(R.id.btnActivityLog);
        activityLog.setOnClickListener(v -> startActivity(new Intent(getActivity(), ActivityLog.class)));

        // 3) Coins & Points
        textCoinValue = view.findViewById(R.id.textCoinValue);
        Coins = view.findViewById(R.id.totalcoins);
        fetchCoinPoints();

        textPointsValue = view.findViewById(R.id.textTotalPoints);
        fetchGamePoints();

        // 4) Shop icon
        shopIcon = view.findViewById(R.id.imageShop);
        shopIcon.setOnClickListener(v -> startActivity(new Intent(getActivity(), MiniShopActivity.class)));

        ImageView coinIcon = view.findViewById(R.id.imageCoinIcon);
        coinIcon.setOnClickListener(null);

        // 5) Character selection
        imageSelectedCharacter = view.findViewById(R.id.imageSelectedCharacter);
        textSelectedCharacterName = view.findViewById(R.id.textSelectedCharacterName);
        recyclerUnlockedChars = view.findViewById(R.id.recyclerUnlockedChars);

        // Load characters from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        List<CharacterModel> allCharacters = loadCharactersFromStorage(prefs);
        String selectedCharacterId = prefs.getString("selectedCharacterId", "char001");
        CharacterModel selectedCharacter = findSelectedCharacter(allCharacters, selectedCharacterId);
        if (selectedCharacter != null) {
            imageSelectedCharacter.setImageResource(selectedCharacter.getImageResId());
            textSelectedCharacterName.setText(selectedCharacter.getName());
        }

        // Build unlocked character list
        List<CharacterModel> unlockedList = new ArrayList<>();
        for (CharacterModel c : allCharacters) {
            if (c.isUnlocked()) {
                unlockedList.add(c);
            }
        }
        UnlockedCharAdapter adapter = new UnlockedCharAdapter(
                unlockedList,
                character -> {
                    // Update selected character
                    prefs.edit().putString("selectedCharacterId", character.getId()).apply();
                    imageSelectedCharacter.setImageResource(character.getImageResId());
                    textSelectedCharacterName.setText(character.getName());
                }
        );
        recyclerUnlockedChars.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        recyclerUnlockedChars.setAdapter(adapter);

        // 6) Optional bottom button
        Button bottomButton = view.findViewById(R.id.bottomButton);
        bottomButton.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Bottom button clicked", Toast.LENGTH_SHORT).show()
        );

        // 7) BarChart for weekly/monthly
        barChart = view.findViewById(R.id.barChart);
        radioGroupTimeRange = view.findViewById(R.id.radioGroupTimeRange);
        radioGroupTimeRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioWeekly) {
                fetchTrackingDataForBar(true);
            } else if (checkedId == R.id.radioMonthly) {
                fetchTrackingDataForBar(false);
            }
        });
        // Default to weekly view
        fetchTrackingDataForBar(true);

        // 8) Tower UI references
        imageTower = view.findViewById(R.id.imageTower);
        textTowerSteps = view.findViewById(R.id.textTowerSteps);

        // Milestone labels
        textMilestone50 = view.findViewById(R.id.textMilestone50);
        textMilestone200 = view.findViewById(R.id.textMilestone200);
        textMilestone500 = view.findViewById(R.id.textMilestone500);
        textMilestone1000 = view.findViewById(R.id.textMilestone1000);
        textMilestone3000 = view.findViewById(R.id.textMilestone3000);
        textMilestone5000 = view.findViewById(R.id.textMilestone5000);
        textMilestone10000 = view.findViewById(R.id.textMilestone10000);

        // Fetch tower steps
        fetchTotalStepsForTower();

        // 9) Notification icon
        ImageView imageNotification = view.findViewById(R.id.imageNotification);
        imageNotification.setClickable(true);
        imageNotification.setFocusable(true);
        imageNotification.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Opening Messenger...", Toast.LENGTH_SHORT).show();
            String messengerLink = "https://m.me/9335554949869793?is_ai=1";
            Intent messengerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(messengerLink));
            messengerIntent.setPackage("com.facebook.orca");
            startActivity(messengerIntent);
        });
    }

    // ------------------------------------------------------------------
    // Fetch & display weekly/monthly data in the BarChart
    // ------------------------------------------------------------------
    private void fetchTrackingDataForBar(boolean isWeekly) {
        if (mAuth.getCurrentUser() == null) return;
        String username = mAuth.getCurrentUser().getDisplayName();
        if (username == null || username.isEmpty()) return;

        db.collection("Games")
                .document(username)
                .collection("trackingwalk")
                .orderBy("date")
                .get()
                .addOnSuccessListener(queryDocSnapshots -> {
                    List<DocumentSnapshot> docs = queryDocSnapshots.getDocuments();
                    Log.d("BarData", "Total docs: " + docs.size());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    Map<String, DocumentSnapshot> dataMap = new HashMap<>();
                    for (DocumentSnapshot doc : docs) {
                        String dateStr = doc.getString("date");
                        if (dateStr != null) {
                            dataMap.put(dateStr, doc);
                        }
                    }

                    if (isWeekly) {
                        // Show current week (Mon → Sun)
                        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                        List<BarEntry> stepsEntries = new ArrayList<>();
                        List<BarEntry> co2Entries = new ArrayList<>();

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                        for (int i = 0; i < 7; i++) {
                            String dateKey = sdf.format(cal.getTime());
                            float stepsVal = 0f;
                            float co2Val = 0f;
                            if (dataMap.containsKey(dateKey)) {
                                DocumentSnapshot snap = dataMap.get(dateKey);
                                Long steps = snap.getLong("stepsSoFar");
                                Double co2 = snap.getDouble("co2Saved");
                                if (steps != null) stepsVal = steps.floatValue();
                                if (co2 == null) co2 = 0.0;
                                co2Val = co2.floatValue();
                            }
                            stepsEntries.add(new BarEntry(i, stepsVal));
                            co2Entries.add(new BarEntry(i + 0.5f, co2Val));
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        BarDataSet dsSteps = new BarDataSet(stepsEntries, "Steps");
                        dsSteps.setAxisDependency(YAxis.AxisDependency.LEFT);
                        dsSteps.setColor(Color.BLUE);
                        dsSteps.setValueTextColor(Color.WHITE);
                        dsSteps.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return String.format(Locale.getDefault(), "%.0f", value);
                            }
                        });

                        BarDataSet dsCo2 = new BarDataSet(co2Entries, "CO₂ Saved (kg)");
                        dsCo2.setAxisDependency(YAxis.AxisDependency.RIGHT);
                        dsCo2.setColor(Color.GREEN);
                        dsCo2.setValueTextColor(Color.WHITE);
                        dsCo2.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return String.format(Locale.getDefault(), "%.3f", value);
                            }
                        });

                        BarData barData = new BarData(dsSteps, dsCo2);
                        barData.setBarWidth(0.45f);
                        setupBarChart(labels, barData, "Weekly Steps & CO₂ (Mon→Sun)");
                    } else {
                        // Monthly mode: last 30 days grouped by week
                        Calendar today = Calendar.getInstance();
                        Calendar startCal = Calendar.getInstance();
                        startCal.add(Calendar.DAY_OF_YEAR, -29);
                        Map<String, float[]> weekData = new TreeMap<>();
                        Calendar tempCal = (Calendar) startCal.clone();
                        SimpleDateFormat sdfLabel = new SimpleDateFormat("MM/dd", Locale.getDefault());

                        while (!tempCal.after(today)) {
                            Calendar mondayCal = (Calendar) tempCal.clone();
                            mondayCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                            String mondayKey = sdf.format(mondayCal.getTime());
                            if (!weekData.containsKey(mondayKey)) {
                                // [0]: sum of CO₂, [1]: sum of Steps
                                weekData.put(mondayKey, new float[]{0f, 0f});
                            }
                            String dayKey = sdf.format(tempCal.getTime());
                            if (dataMap.containsKey(dayKey)) {
                                DocumentSnapshot snap = dataMap.get(dayKey);
                                Long steps = snap.getLong("stepsSoFar");
                                Double co2 = snap.getDouble("co2Saved");
                                if (co2 == null) co2 = 0.0;
                                float[] current = weekData.get(mondayKey);
                                current[0] += co2.floatValue();
                                current[1] += (steps != null) ? steps : 0f;
                            }
                            tempCal.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        int size = weekData.size();
                        String[] labels = new String[size];
                        List<BarEntry> stepsEntries = new ArrayList<>();
                        List<BarEntry> co2Entries = new ArrayList<>();
                        int index = 0;

                        for (Map.Entry<String, float[]> entry : weekData.entrySet()) {
                            try {
                                Date mondayDate = sdf.parse(entry.getKey());
                                labels[index] = "Week of\n" + sdfLabel.format(mondayDate);
                            } catch (ParseException e) {
                                labels[index] = "Week of\n" + entry.getKey();
                            }
                            float sumCo2 = entry.getValue()[0];
                            float sumSteps = entry.getValue()[1];
                            stepsEntries.add(new BarEntry(index, sumSteps));
                            co2Entries.add(new BarEntry(index + 0.5f, sumCo2));
                            index++;
                        }

                        BarDataSet dsSteps = new BarDataSet(stepsEntries, "Steps");
                        dsSteps.setAxisDependency(YAxis.AxisDependency.LEFT);
                        dsSteps.setColor(Color.BLUE);
                        dsSteps.setValueTextColor(Color.WHITE);
                        dsSteps.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return String.format(Locale.getDefault(), "%.0f", value);
                            }
                        });

                        BarDataSet dsCo2 = new BarDataSet(co2Entries, "CO₂ Saved (kg)");
                        dsCo2.setAxisDependency(YAxis.AxisDependency.RIGHT);
                        dsCo2.setColor(Color.GREEN);
                        dsCo2.setValueTextColor(Color.WHITE);
                        dsCo2.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return String.format(Locale.getDefault(), "%.3f", value);
                            }
                        });

                        BarData barData = new BarData(dsSteps, dsCo2);
                        barData.setBarWidth(0.45f);
                        setupBarChart(labels, barData, "Monthly Progress (30d grouped by week)");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("BarData", "Error fetching tracking data", e);
                });
    }

    // ------------------------------------------------------------------
    // Configure the BarChart UI
    // ------------------------------------------------------------------
    private void setupBarChart(String[] labels, BarData barData, String descriptionText) {
        barChart.clear();
        barChart.setData(barData);
        barChart.notifyDataSetChanged();
        barChart.invalidate();

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(labels.length, true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                if (i >= 0 && i < labels.length) {
                    return labels[i];
                }
                return "";
            }
        });

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(true);
        rightAxis.setTextColor(Color.GREEN);
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f);

        Legend legend = barChart.getLegend();
        legend.setTextColor(Color.WHITE);

        barChart.getDescription().setText(descriptionText);
        barChart.getDescription().setTextColor(Color.WHITE);
    }

    // ------------------------------------------------------------------
    // Fetch total steps, display them on tower, highlight milestones
    // ------------------------------------------------------------------
    private void fetchTotalStepsForTower() {
        if (mAuth.getCurrentUser() == null) return;
        String username = mAuth.getCurrentUser().getDisplayName();
        if (username == null || username.isEmpty()) return;

        db.collection("Games")
                .document(username)
                .collection("trackingwalk")
                .get()
                .addOnSuccessListener(queryDocSnapshots -> {
                    long totalSteps = 0;
                    for (DocumentSnapshot doc : queryDocSnapshots) {
                        Long steps = doc.getLong("stepsSoFar");
                        if (steps != null) {
                            totalSteps += steps;
                        }
                    }
                    Log.d("TowerDebug", "User total steps: " + totalSteps);

                    // Update the tower orb text
                    if (textTowerSteps != null) {
                        textTowerSteps.setText(String.valueOf(totalSteps));
                    }

                    // Highlight milestone labels
                    highlightMilestones(totalSteps);

                    // If totalSteps >= 10,000, award points
                    if (totalSteps >= 10000) {
                        awardPointsForStepGoal(username);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TowerDebug", "Failed to fetch steps for tower", e);
                });
    }

    // Highlight each milestone if user has reached it
    private void highlightMilestones(long totalSteps) {
        // White by default
        int defaultColor = Color.WHITE;
        // Reached color: bright green
        int reachedColor = Color.parseColor("#00FF00");

        // Reset all to white
        textMilestone50.setTextColor(defaultColor);
        textMilestone200.setTextColor(defaultColor);
        textMilestone500.setTextColor(defaultColor);
        textMilestone1000.setTextColor(defaultColor);
        textMilestone3000.setTextColor(defaultColor);
        textMilestone5000.setTextColor(defaultColor);
        textMilestone10000.setTextColor(defaultColor);

        if (totalSteps >= 50)   textMilestone50.setTextColor(reachedColor);
        if (totalSteps >= 200)  textMilestone200.setTextColor(reachedColor);
        if (totalSteps >= 500)  textMilestone500.setTextColor(reachedColor);
        if (totalSteps >= 1000) textMilestone1000.setTextColor(reachedColor);
        if (totalSteps >= 3000) textMilestone3000.setTextColor(reachedColor);
        if (totalSteps >= 5000) textMilestone5000.setTextColor(reachedColor);
        if (totalSteps >= 10000) textMilestone10000.setTextColor(reachedColor);
    }

    // Award 1,000 points if steps >= 10,000 and user has < 1000 points
    private void awardPointsForStepGoal(String username) {
        DocumentReference gameDoc = db.collection("Games").document(username);
        gameDoc.get().addOnSuccessListener(docSnap -> {
            if (docSnap.exists()) {
                Long currentPoints = docSnap.getLong("points");
                if (currentPoints == null || currentPoints < 1000) {
                    gameDoc.update("points", 1000)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("TowerDebug", "Awarded 1000 points for reaching 10000 steps.");
                                Toast.makeText(getActivity(),
                                        "Congratulations! You reached 10,000 steps and earned 1000 points!",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TowerDebug", "Failed to award points", e);
                            });
                } else {
                    Log.d("TowerDebug", "User already has " + currentPoints + " points; not awarding.");
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("TowerDebug", "Error reading game document", e);
        });
    }

    // ------------------------------------------------------------------
    // Fetch coin points from Firestore: Gamez/username & subcollection "records"
    // ------------------------------------------------------------------
    private void fetchCoinPoints() {
        if (mAuth.getCurrentUser() != null) {
            String username = mAuth.getCurrentUser().getDisplayName();
            if (username != null && !username.isEmpty()) {
                final long[] totalCoins = {0};

                // 1) Main doc: Gamez/username
                db.collection("Gamez")
                        .document(username)
                        .get()
                        .addOnSuccessListener(docSnap -> {
                            if (docSnap.exists()) {
                                Long mainCoins = docSnap.getLong("coins");
                                if (mainCoins != null) {
                                    totalCoins[0] += mainCoins;
                                }
                            }
                            // 2) Subcollection: Gamez/username/records
                            db.collection("Gamez")
                                    .document(username)
                                    .collection("records")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        for (DocumentSnapshot snap : querySnapshot) {
                                            if (snap.exists()) {
                                                Long recordCoins = snap.getLong("coins");
                                                if (recordCoins != null) {
                                                    totalCoins[0] += recordCoins;
                                                }
                                            }
                                        }
                                        textCoinValue.setText(String.valueOf(totalCoins[0]));
                                        Coins.setText(String.valueOf(totalCoins[0]));
                                    })
                                    .addOnFailureListener(e -> {
                                        textCoinValue.setText(String.valueOf(totalCoins[0]));
                                        Coins.setText(String.valueOf(totalCoins[0]));
                                        Toast.makeText(getActivity(),
                                                "Failed to fetch records coins: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            textCoinValue.setText("0");
                            Coins.setText("0");
                            Toast.makeText(getActivity(),
                                    "Failed to fetch main doc coins: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                textCoinValue.setText("0");
                Coins.setText("0");
            }
        } else {
            textCoinValue.setText("0");
            Coins.setText("0");
        }
    }

    private void fetchGamePoints() {
        if (mAuth.getCurrentUser() != null) {
            String username = mAuth.getCurrentUser().getDisplayName();
            if (username != null && !username.isEmpty()) {
                // We'll store the final total in this array
                final long[] totalPoints = {0};

                // 1) Create references to each document:
                //    - Games/{username} (main doc)
                //    - Gamez/{username}/records/BattleEco
                //    - Gamez/{username}/records/CYCF
                DocumentReference gamesDocRef = db.collection("Games")
                        .document(username); // e.g. "Games/Jonr"

                DocumentReference battleEcoRef = db.collection("Gamez")
                        .document(username)
                        .collection("records")
                        .document("BattleEco"); // "Gamez/Jonr/records/BattleEco"

                DocumentReference cycfRef = db.collection("Gamez")
                        .document(username)
                        .collection("records")
                        .document("CYCF"); // "Gamez/Jonr/records/CYCF"

                // 2) Fetch all three in parallel
                Task<DocumentSnapshot> gamesTask = gamesDocRef.get();
                Task<DocumentSnapshot> battleEcoTask = battleEcoRef.get();
                Task<DocumentSnapshot> cycfTask = cycfRef.get();

                // 3) When all tasks succeed, sum up their "points"
                Tasks.whenAllSuccess(gamesTask, battleEcoTask, cycfTask)
                        .addOnSuccessListener(tasks -> {
                            // tasks is a List of the results in the same order
                            DocumentSnapshot gamesSnap = (DocumentSnapshot) tasks.get(0);
                            DocumentSnapshot battleEcoSnap = (DocumentSnapshot) tasks.get(1);
                            DocumentSnapshot cycfSnap = (DocumentSnapshot) tasks.get(2);

                            // Games/{username} -> "points"
                            if (gamesSnap.exists()) {
                                Long mainPoints = gamesSnap.getLong("points");
                                if (mainPoints != null) {
                                    totalPoints[0] += mainPoints;
                                }
                            }

                            // Gamez/{username}/records/BattleEco -> "points"
                            if (battleEcoSnap.exists()) {
                                Long battleEcoPoints = battleEcoSnap.getLong("points");
                                if (battleEcoPoints != null) {
                                    totalPoints[0] += battleEcoPoints;
                                }
                            }

                            // Gamez/{username}/records/CYCF -> "points"
                            if (cycfSnap.exists()) {
                                Long cycfPoints = cycfSnap.getLong("points");
                                if (cycfPoints != null) {
                                    totalPoints[0] += cycfPoints;
                                }
                            }

                            // Finally, update the TextView
                            textPointsValue.setText(String.valueOf(totalPoints[0]));
                        })
                        .addOnFailureListener(e -> {
                            textPointsValue.setText("0");
                            Toast.makeText(getActivity(),
                                    "Failed to fetch points: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Username is null or empty
                textPointsValue.setText("0");
            }
        } else {
            // No logged-in user
            textPointsValue.setText("0");
        }
    }


    // ------------------------------------------------------------------
    // Load characters from SharedPreferences (for the selection system)
    // ------------------------------------------------------------------
    private List<CharacterModel> loadCharactersFromStorage(SharedPreferences prefs) {
        String json = prefs.getString("characters", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<CharacterModel>>() {}.getType();
            return gson.fromJson(json, type);
        } else {
            // If no data found, create a default list
            List<CharacterModel> list = new ArrayList<>();
            list.add(new CharacterModel("char001", "Green Warrior", R.drawable.main_character, 100, true));
            list.add(new CharacterModel("char002", "Solar Knight", R.drawable.character_solar, 200, false));
            list.add(new CharacterModel("char003", "Wind Mage", R.drawable.character_wind, 300, false));
            list.add(new CharacterModel("char004", "Nature Knight", R.drawable.character_nature, 250, false));
            return list;
        }
    }

    private CharacterModel findSelectedCharacter(List<CharacterModel> allChars, String selectedId) {
        for (CharacterModel c : allChars) {
            if (c.getId().equals(selectedId)) {
                return c;
            }
        }
        return null;
    }
}
