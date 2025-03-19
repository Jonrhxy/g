package com.example.cverdetotoo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MiniShopActivity extends AppCompatActivity {

    private TextView textShopTitle, textUserCoins;
    private RecyclerView recyclerCharacters;
    private List<CharacterModel> characterList;
    private int userCoins;
    private SharedPreferences prefs;
    private FirebaseFirestore db;

    private CharacterShopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mini_shop_activity);

        textShopTitle = findViewById(R.id.textShopTitle);
        textUserCoins = findViewById(R.id.textUserCoins);
        recyclerCharacters = findViewById(R.id.recyclerCharacters);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        // Load local coins (default: 500 if Firestore fetch fails)
        userCoins = prefs.getInt("coins", 500);
        textUserCoins.setText("Coins: " + userCoins);

        // 1) Fetch from Firestore (BattleEco & CYCF) and update userCoins
        fetchCoinsFromFirestore();

        // 2) Load the character list from SharedPreferences (merging missing defaults if needed)
        characterList = loadCharactersFromStorage();
        recyclerCharacters.setLayoutManager(new LinearLayoutManager(this));

        // 3) Get the currently selected character ID (or default to "char001")
        String selectedCharacterId = prefs.getString("selectedCharacterId", "char001");

        // 4) Initialize adapter
        adapter = new CharacterShopAdapter(
                characterList,
                userCoins,
                selectedCharacterId,
                new CharacterShopAdapter.OnCharacterActionListener() {
                    @Override
                    public void onBuyClicked(CharacterModel character) {
                        // Check if user has enough coins
                        if (userCoins >= character.getCost()) {
                            int cost = character.getCost();
                            userCoins -= cost;
                            textUserCoins.setText("Coins: " + userCoins);
                            prefs.edit().putInt("coins", userCoins).apply();

                            // Deduct the purchase cost from Firestore (BattleEco doc as example)
                            deductCostFromBattleEco(cost);

                            // Mark the character as unlocked and update local storage
                            character.setUnlocked(true);
                            saveCharactersToStorage(characterList);

                            // Refresh adapter so the item now shows "Equip"
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(MiniShopActivity.this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onSelectClicked(CharacterModel character) {
                        // Mark this character as selected in SharedPreferences
                        prefs.edit().putString("selectedCharacterId", character.getId()).apply();
                        Toast.makeText(MiniShopActivity.this, character.getName() + " equipped!", Toast.LENGTH_SHORT).show();

                        // Update the adapter's selectedCharacterId and refresh the list
                        adapter.setSelectedCharacterId(character.getId());
                        adapter.notifyDataSetChanged();
                        finish(); // Return to previous screen
                    }
                }
        );
        recyclerCharacters.setAdapter(adapter);
    }

    /**
     * Fetch coins by summing from:
     *  - Gamez/{username}/records/BattleEco
     *  - Gamez/{username}/records/CYCF
     * Then update userCoins and the UI.
     */
    private void fetchCoinsFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // No logged-in user, keep local coins
            return;
        }
        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) {
            return;
        }

        // References for the docs we want to read (using "coins" field now)
        DocumentReference battleEcoRef = db.collection("Gamez")
                .document(username)
                .collection("records")
                .document("BattleEco");
        DocumentReference cycfRef = db.collection("Gamez")
                .document(username)
                .collection("records")
                .document("CYCF");

        // Parallel fetch
        Task<DocumentSnapshot> battleEcoTask = battleEcoRef.get();
        Task<DocumentSnapshot> cycfTask = cycfRef.get();

        Tasks.whenAllSuccess(battleEcoTask, cycfTask)
                .addOnSuccessListener(tasks -> {
                    long total = 0;

                    // tasks are in the same order as passed
                    DocumentSnapshot battleEcoSnap = (DocumentSnapshot) tasks.get(0);
                    DocumentSnapshot cycfSnap = (DocumentSnapshot) tasks.get(1);

                    if (battleEcoSnap.exists()) {
                        Long coinsBE = battleEcoSnap.getLong("coins");
                        if (coinsBE != null) total += coinsBE;
                    }
                    if (cycfSnap.exists()) {
                        Long coinsCYCF = cycfSnap.getLong("coins");
                        if (coinsCYCF != null) total += coinsCYCF;
                    }

                    userCoins = (int) total;
                    textUserCoins.setText("Coins: " + userCoins);
                    prefs.edit().putInt("coins", userCoins).apply();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MiniShopActivity.this,
                            "Failed to fetch coins: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deduct cost from the BattleEco document (using "coins" field).
     * Adjust if you want to deduct from CYCF or both.
     */
    private void deductCostFromBattleEco(int cost) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) return;

        db.collection("Gamez")
                .document(username)
                .collection("records")
                .document("BattleEco")
                .update("coins", FieldValue.increment(-cost))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(MiniShopActivity.this,
                                "Purchased item. Deducted " + cost + " coins.",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(MiniShopActivity.this,
                                "Error updating Firestore coins: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }



    /**
     * Returns the full list of default characters.
     */
    private List<CharacterModel> getDefaultCharacters() {
        List<CharacterModel> defaults = new ArrayList<>();
        defaults.add(new CharacterModel("char01", "Green Warrior", R.drawable.main_character, 100, true)); // unlocked by default
        defaults.add(new CharacterModel("char02", "Solar Knight", R.drawable.character_solar, 200, false));
        defaults.add(new CharacterModel("char03", "Wind Mage", R.drawable.character_wind, 300, false));
        defaults.add(new CharacterModel("char04", "Nature Knight", R.drawable.character_nature, 250, false));
        defaults.add(new CharacterModel("char05", "Zephyr Elves", R.drawable.character_zephyr, 150, false));
        defaults.add(new CharacterModel("char06", "Solis Earth Hero", R.drawable.character_solis, 150, false));
        defaults.add(new CharacterModel("char07", "Aero Air Guardian", R.drawable.character_aeron, 150, false));
        return defaults;
    }

    /**
     * Loads the character list from SharedPreferences.
     * If stored data exists, merges any missing default characters.
     */
    private List<CharacterModel> loadCharactersFromStorage() {
        List<CharacterModel> storedList;
        String json = prefs.getString("characters", null);
        if (json != null) {
            storedList = new Gson().fromJson(json, new TypeToken<List<CharacterModel>>() {}.getType());
        } else {
            storedList = new ArrayList<>();
        }

        // Merge defaults
        List<CharacterModel> defaultList = getDefaultCharacters();
        for (CharacterModel defaultChar : defaultList) {
            boolean found = false;
            for (CharacterModel storedChar : storedList) {
                if (storedChar.getId().equals(defaultChar.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                storedList.add(defaultChar);
            }
        }

        // Save merged list
        saveCharactersToStorage(storedList);
        return storedList;
    }

    private void saveCharactersToStorage(List<CharacterModel> list) {
        String json = new Gson().toJson(list);
        prefs.edit().putString("characters", json).apply();
    }
}
