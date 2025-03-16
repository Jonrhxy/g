package com.example.cverdetotoo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Example ProfileFragment that fetches multiple fields:
 *  - date (e.g., "20250315")
 *  - time (e.g., "15:48")
 *  - co2Saved (double)
 *  - distanceSoFarKm (double)
 *  - stepsSoFar (long)
 *
 * and displays them on a single multi-line chart.
 */
public class ProfileFragment extends Fragment {

    private TextView textCoinValue, Coins;
    private TextView textPointsValue; // For game points
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView imageSelectedCharacter;
    private TextView textSelectedCharacterName;
    private RecyclerView recyclerUnlockedChars;
    private ImageView shopIcon;

    // Our multi-line chart
    private LineChart lineChart;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Basic UI setup
        FrameLayout settingsLayout = view.findViewById(R.id.settingsLayout);
        settingsLayout.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), Settings.class));
        });

        TextView textSeeAll = view.findViewById(R.id.textSeeAll);
        textSeeAll.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.constraintLayout, new BadgesFragment())
                    .commit();
        });

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

        Button activityLog = view.findViewById(R.id.btnActivityLog);
        activityLog.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ActivityLog.class));
        });

        // Coins & Points
        textCoinValue = view.findViewById(R.id.textCoinValue);
        Coins = view.findViewById(R.id.totalcoins);
        fetchCoinPoints();

        textPointsValue = view.findViewById(R.id.textTotalPoints);
        fetchGamePoints();

        shopIcon = view.findViewById(R.id.imageShop);
        shopIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MiniShopActivity.class);
            startActivity(intent);
        });

        ImageView coinIcon = view.findViewById(R.id.imageCoinIcon);
        coinIcon.setOnClickListener(null);

        imageSelectedCharacter = view.findViewById(R.id.imageSelectedCharacter);
        textSelectedCharacterName = view.findViewById(R.id.textSelectedCharacterName);
        recyclerUnlockedChars = view.findViewById(R.id.recyclerUnlockedChars);

        // Load characters from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        List<CharacterModel> allCharacters = loadCharactersFromStorage(prefs);

        // Show selected character
        String selectedCharacterId = prefs.getString("selectedCharacterId", "char001");
        CharacterModel selectedCharacter = findSelectedCharacter(allCharacters, selectedCharacterId);
        if (selectedCharacter != null) {
            imageSelectedCharacter.setImageResource(selectedCharacter.getImageResId());
            textSelectedCharacterName.setText(selectedCharacter.getName());
        }

        // Build unlocked list
        List<CharacterModel> unlockedList = new ArrayList<>();
        for (CharacterModel c : allCharacters) {
            if (c.isUnlocked()) {
                unlockedList.add(c);
            }
        }

        UnlockedCharAdapter adapter = new UnlockedCharAdapter(unlockedList, character -> {
            prefs.edit().putString("selectedCharacterId", character.getId()).apply();
            imageSelectedCharacter.setImageResource(character.getImageResId());
            textSelectedCharacterName.setText(character.getName());
        });
        recyclerUnlockedChars.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        recyclerUnlockedChars.setAdapter(adapter);

        // Optional bottom button
        Button bottomButton = view.findViewById(R.id.bottomButton);
        bottomButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Bottom button clicked", Toast.LENGTH_SHORT).show();
        });

        // Initialize the line chart
        lineChart = view.findViewById(R.id.lineChart);

        // Fetch multi-field data from Firestore
        fetchTrackingWalkData();

        // ======= New Code: Re-enable ImageNotification click to launch Messenger =======
        ImageView imageNotification = view.findViewById(R.id.imageNotification);
        imageNotification.setClickable(true);
        imageNotification.setFocusable(true);
        imageNotification.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Opening Messenger...", Toast.LENGTH_SHORT).show();
            String messengerLink = "https://m.me/9335554949869793?is_ai=1";
            Intent messengerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(messengerLink));
            // Force the intent to be handled by Messenger app (uncomment if you want to force it)
            messengerIntent.setPackage("com.facebook.orca");
            startActivity(messengerIntent);
        });
    }

    // -------------------------------------------------
    // 1) Fetch coin points
    // -------------------------------------------------
    private void fetchCoinPoints() {
        if (mAuth.getCurrentUser() != null) {
            String username = mAuth.getCurrentUser().getDisplayName();
            if (username != null && !username.isEmpty()) {
                final long[] totalCoins = {0};

                db.collection("Games")
                        .document(username)
                        .get()
                        .addOnSuccessListener(docSnap -> {
                            if (docSnap.exists()) {
                                Long mainCoins = docSnap.getLong("coins");
                                if (mainCoins != null) {
                                    totalCoins[0] += mainCoins;
                                }
                            }
                            textCoinValue.setText(String.valueOf(totalCoins[0]));
                            Coins.setText(String.valueOf(totalCoins[0]));
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
        }
    }

    // -------------------------------------------------
    // 2) Fetch game points
    // -------------------------------------------------
    private void fetchGamePoints() {
        if (mAuth.getCurrentUser() != null) {
            String username = mAuth.getCurrentUser().getDisplayName();
            if (username != null && !username.isEmpty()) {
                final long[] totalPoints = {0};

                db.collection("Games")
                        .document(username)
                        .get()
                        .addOnSuccessListener(docSnap -> {
                            if (docSnap.exists()) {
                                Long mainPoints = docSnap.getLong("points");
                                if (mainPoints != null) {
                                    totalPoints[0] += mainPoints;
                                }
                            }
                            textPointsValue.setText(String.valueOf(totalPoints[0]));
                        })
                        .addOnFailureListener(e -> {
                            textPointsValue.setText("0");
                            Toast.makeText(getActivity(),
                                    "Failed to fetch main doc points: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            } else {
                textPointsValue.setText("0");
            }
        } else {
            textPointsValue.setText("0");
        }
    }

    // -------------------------------------------------
    // 3) Fetch data from "trackingwalk" and build multi-line chart
    // -------------------------------------------------
    private void fetchTrackingWalkData() {
        if (mAuth.getCurrentUser() == null) return;
        String username = mAuth.getCurrentUser().getDisplayName();
        if (username == null || username.isEmpty()) return;

        db.collection("Games")
                .document(username)
                .collection("trackingwalk")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double[] weeklyCo2 = new double[7];
                    String[] dayLabels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                        String dateStr = snapshot.getString("date");
                        if (dateStr == null) continue;

                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                            Date date = sdf.parse(dateStr);
                            if (date == null) continue;

                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);
                            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                            Double co2Val = snapshot.getDouble("co2Saved");
                            if (co2Val != null) {
                                weeklyCo2[dayOfWeek - 1] = co2Val;
                            }
                        } catch (ParseException e) {
                            Log.e("FirestoreDebug", "Error parsing date: " + dateStr, e);
                        }
                    }

                    List<Entry> co2Entries = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        co2Entries.add(new Entry(i, (float) weeklyCo2[i]));
                    }

                    LineDataSet co2DataSet = new LineDataSet(co2Entries, "CO2 Saved (kg)");
                    styleDataSet(co2DataSet, Color.GREEN);
                    co2DataSet.setValueTextColor(Color.WHITE); // Make data point text white

                    LineData lineData = new LineData(co2DataSet);

                    lineChart.setData(lineData);

                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            if (value >= 0 && value < dayLabels.length) {
                                return dayLabels[(int) value];
                            }
                            return "";
                        }
                    });
                    xAxis.setLabelCount(7, true);
                    xAxis.setTextColor(Color.WHITE);

                    YAxis leftAxis = lineChart.getAxisLeft();
                    leftAxis.setDrawGridLines(true);
                    leftAxis.setGridColor(Color.LTGRAY);
                    leftAxis.setAxisLineColor(Color.TRANSPARENT);
                    leftAxis.setLabelCount(4, true);
                    leftAxis.setTextColor(Color.WHITE);

                    lineChart.getAxisRight().setEnabled(false);

                    double totalCo2 = 0;
                    for (double co2 : weeklyCo2) {
                        totalCo2 += co2;
                    }

                    DecimalFormat df = new DecimalFormat("#.##");
                    String totalCo2Str = df.format(totalCo2);

                    lineChart.getDescription().setText("Total CO2 reduced this week: " + totalCo2Str + " kg");
                    lineChart.getDescription().setTextColor(Color.WHITE);
                    lineChart.getDescription().setEnabled(true);
                    lineChart.getDescription().setPosition(lineChart.getWidth() / 2, 20);
                    lineChart.getDescription().setTextAlign(Paint.Align.CENTER);

                    lineChart.getLegend().setEnabled(false);

                    lineChart.invalidate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(),
                            "Failed to fetch trackingwalk data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreDebug", "Error: ", e);
                });
    }
    /**
     * Simple helper to style each DataSet with cubic lines, no circle, etc.
     */
    private void styleDataSet(LineDataSet dataSet, int color) {
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(3f);
        dataSet.setValueTextSize(8f);

        // For a smooth Strava-like curve:
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // If you don't want circles or values, comment these out:
        // dataSet.setDrawCircles(false);
        // dataSet.setDrawValues(false);
    }

    /**
     * Parse a date string like "20250315" into a float for sorting.
     * Adjust to your actual format if needed (e.g., "yyyy-MM-dd").
     */
    private float parseDateToFloat(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date = sdf.parse(dateStr);
            if (date != null) {
                // Convert to "days since epoch"
                return (float) (date.getTime() / (1000 * 60 * 60 * 24));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0f;
    }

    /**
     * Convert the float date value (days since epoch) back to a label, e.g. "Mar 15".
     */
    private String floatToDateLabel(float value) {
        long millis = (long) (value * (1000 * 60 * 60 * 24));
        Date date = new Date(millis);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
        return sdf.format(date);
    }

    /**
     * If you want to parse "15:48" -> 15*60 + 48 = 948 (minutes),
     * so you can display or chart it numerically.
     */
    private float parseTimeToFloat(String timeStr) {
        if (timeStr == null) return 0f;
        String[] parts = timeStr.split(":");
        if (parts.length == 2) {
            try {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                return hour * 60 + minute;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 0f;
    }

    // Load characters from SharedPreferences
    private List<CharacterModel> loadCharactersFromStorage(SharedPreferences prefs) {
        String json = prefs.getString("characters", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<CharacterModel>>() {}.getType();
            return gson.fromJson(json, type);
        } else {
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
