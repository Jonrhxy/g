package com.example.cverdetotoo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BattleEcoActivity extends AppCompatActivity {

    // Primary UI elements
    private TextView playerHealthText, computerHealthText, battleLogText;
    private TextView playerShieldText, computerShieldText, playerEnergyText, computerEnergyText;
    private TextView timerText;
    private ImageView aiDrawnCard, aiCharacterImage;
    private ImageView playerDrawnCard, playerCharacterImage;
    private LinearLayout handLayout;
    private Button skipTurnButton;
    private Button restartButton; // Now inside pause panel

    // Pause overlay UI elements
    private ImageButton pauseButton;
    private LinearLayout pausePanel;
    private Button resumeButton, muteButton, instructionsButton;
    private LinearLayout instructionsPanel;
    private Button backButton;

    // Game state variables
    private int playerHealth = 100;
    private int computerHealth;
    private int playerShield = 0, computerShield = 0;
    private int playerEnergy = 3, computerEnergy = 3;
    private int currentBossIndex = 0;
    private long timeRemaining = 300000; // 5 minutes in milliseconds

    // Track if game is over
    private boolean gameIsOver = false;

    // Additional state: Whose turn it is
    private boolean isPlayerTurn = false;

    // Persist last drawn card images (resource IDs)
    private int lastPlayerCardResId = -1;
    private int lastAICardResId = -1;

    // Game objects and card decks
    private PlayerCard playerCard, computer;
    private List<BattleCard> playerDeck;
    private List<BattleCard> aiDeck;
    private List<BattleCard> playerHand;

    private Handler handler = new Handler();
    private MediaPlayer backgroundMusic;

    // Boss and background arrays
    private final int[] BOSS_HEALTHS = {100, 120, 150};
    private final int[] BOSS_IMAGES = { R.drawable.boss1, R.drawable.boss2, R.drawable.boss3 };
    private final int[] BACKGROUNDS = { R.drawable.background1, R.drawable.background2 };

    // Timer variables
    private CountDownTimer gameTimer;
    private long gameStartTime;

    // Energy gain every 30 seconds
    private Handler energyHandler = new Handler();
    private Runnable energyRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isGameActive()) return;
            playerEnergy++;
            computerEnergy++;
            updateUI();
            energyHandler.postDelayed(this, 30000);
        }
    };

    // Total game time constant for eco tip calculations.
    private final int TOTAL_GAME_TIME = 300000; // 5 minutes

    // Eco Tip arrays: texts, durations, and sound resource IDs.
    private String[] ecoTipTexts = {
            "Did you know? Walking 5 km instead of driving can save roughly 1 kg of CO₂ emissions, depending on your vehicle's efficiency.",
            "Did you know? Switching to LED bulbs can reduce your energy consumption by up to 80%, cutting both your energy bills and carbon footprint.",
            "Did you know? Recycling one ton of paper saves about 17 trees and can cut CO₂ emissions by around 3 tons.",
            "Did you know? Using public transportation or carpooling can reduce your carbon footprint by about 30% compared to solo driving.",
            "Did you know? A single mature tree can absorb roughly 21 kg of CO₂ annually, helping to improve air quality."
    };
    private int[] ecoTipDurations = {10500, 10200, 8700, 9200, 9400}; // in milliseconds
    private int[] ecoTipSoundRes = { R.raw.ecotip1, R.raw.ecotip2, R.raw.ecotip3, R.raw.ecotip4, R.raw.ecotip5 };
    private int nextEcoTipIndex = 0; // tracks which eco tip to show next

    // Flag to track mute state.
    private boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force full screen.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_battle_eco);

        // Bind primary UI components.
        playerHealthText = findViewById(R.id.playerHealthText);
        computerHealthText = findViewById(R.id.computerHealthText);
        battleLogText = findViewById(R.id.battleLogText);
        playerShieldText = findViewById(R.id.playerShieldText);
        computerShieldText = findViewById(R.id.computerShieldText);
        playerEnergyText = findViewById(R.id.playerEnergyText);
        computerEnergyText = findViewById(R.id.computerEnergyText);
        timerText = findViewById(R.id.timerText);

        aiDrawnCard = findViewById(R.id.aiDrawnCard);
        aiCharacterImage = findViewById(R.id.aiCharacterImage);
        playerDrawnCard = findViewById(R.id.playerDrawnCard);
        playerCharacterImage = findViewById(R.id.playerCharacterImage);
        handLayout = findViewById(R.id.handLayout);

        skipTurnButton = findViewById(R.id.skipTurnButton);
        restartButton = findViewById(R.id.restartButton);

        // Bind pause overlay components.
        pauseButton = findViewById(R.id.pauseButton);
        pausePanel = findViewById(R.id.pausePanel);
        resumeButton = findViewById(R.id.resumeButton);
        muteButton = findViewById(R.id.muteButton);
        instructionsButton = findViewById(R.id.instructionsButton);
        instructionsPanel = findViewById(R.id.instructionsPanel);
        backButton = findViewById(R.id.backButton);

        // Initially, hide pause and instructions panels.
        pausePanel.setVisibility(View.GONE);
        instructionsPanel.setVisibility(View.GONE);

        // Set listeners.
        pauseButton.setOnClickListener(v -> pauseGame());
        resumeButton.setOnClickListener(v -> resumeGame());
        muteButton.setOnClickListener(v -> {
            if(backgroundMusic != null) {
                if(!isMuted){
                    backgroundMusic.pause();
                    isMuted = true;
                    muteButton.setText("Unmute");
                } else {
                    backgroundMusic.start();
                    isMuted = false;
                    muteButton.setText("Mute");
                }
            }
        });
        instructionsButton.setOnClickListener(v -> {
            pausePanel.setVisibility(View.GONE);
            instructionsPanel.setVisibility(View.VISIBLE);
        });
        backButton.setOnClickListener(v -> {
            instructionsPanel.setVisibility(View.GONE);
            pausePanel.setVisibility(View.VISIBLE);
        });
        skipTurnButton.setOnClickListener(v -> {
            if(!isGameActive()) return;
            battleLogText.setText("Turn skipped due to insufficient energy.");
            handLayout.setVisibility(View.GONE);
            handler.postDelayed(this::processComputerTurn, 1000);
        });
        restartButton.setOnClickListener(v -> restartGame());

        // Initialize background music.
        backgroundMusic = MediaPlayer.create(this, R.raw.music_cardgame);
        backgroundMusic.setLooping(true);
        backgroundMusic.start();

        // Set player character image based on equipped character.
        updatePlayerCharacterImage();

        // Load saved game state.
        loadGameState();

        // Initialize players.
        playerCard = new PlayerCard("Player", playerHealth);
        computer = new PlayerCard("Computer", BOSS_HEALTHS[currentBossIndex]);

        // Set boss image and background.
        aiCharacterImage.setImageResource(BOSS_IMAGES[currentBossIndex]);
        View root = findViewById(R.id.battleEcoRoot);
        root.setBackgroundResource(BACKGROUNDS[0]);

        // Define decks.
        playerDeck = new ArrayList<>();
        playerDeck.add(new BattleCard(CardType.SLASH, 60,
                "Reforest Revival (60 damage, 40 heal, 2 Energy)", R.drawable.card1));
        playerDeck.add(new BattleCard(CardType.HEAL, 30,
                "Nature's Embrace (30 heal, gain 2 Energy, 1 Energy)", R.drawable.card2));
        playerDeck.add(new BattleCard(CardType.PIERCING, 35,
                "Piercing Staff (35 damage, +20 shield, 1 Energy)", R.drawable.card3));
        playerDeck.add(new BattleCard(CardType.ENERGY, 0,
                "Forest Aura (+1 Energy, 0 Energy Cost)", R.drawable.card4));
        playerDeck.add(new BattleCard(CardType.SHIELD, 5,
                "Green Shield (+15 shield, 5 damage, 0 Energy)", R.drawable.card5));
        playerDeck.add(new BattleCard(CardType.SHIELD, 5,
                "Eco Barrier (+20 shield, 5 damage, 1 Energy)", R.drawable.card6));
        playerDeck.add(new BattleCard(CardType.SHIELD, 10,
                "Carbon Guard (+25 shield, 10 damage, 1 Energy)", R.drawable.card7));

        aiDeck = new ArrayList<>();
        aiDeck.add(new BattleCard(CardType.SHIELD, 5,
                "Green Shield (+15 shield, 5 damage, 0 Energy)", R.drawable.card5));
        aiDeck.add(new BattleCard(CardType.SHIELD, 5,
                "Eco Barrier (+20 shield, 5 damage, 1 Energy)", R.drawable.card6));
        aiDeck.add(new BattleCard(CardType.SHIELD, 10,
                "Carbon Guard (+25 shield, 10 damage, 1 Energy)", R.drawable.card7));
        aiDeck.add(new BattleCard(CardType.DAMAGE, 25,
                "Fossil Fury (25 damage, 1 Energy)", R.drawable.card8));
        aiDeck.add(new BattleCard(CardType.SLASH, 45,
                "Pollution Pulse (45 damage, 30 shield reduction, 2 Energy)", R.drawable.card9));
        aiDeck.add(new BattleCard(CardType.DAMAGE, 35,
                "Emissions Eruption (35 damage)", R.drawable.card10));
        aiDeck.add(new BattleCard(CardType.SHIELD, 0,
                "Pollution Moon (+25 shield, 0 Energy)", R.drawable.card11));

        // Restore last drawn card images if available.
        if (lastPlayerCardResId != -1) {
            playerDrawnCard.setImageResource(lastPlayerCardResId);
            playerDrawnCard.setVisibility(View.VISIBLE);
        }
        if (lastAICardResId != -1) {
            aiDrawnCard.setImageResource(lastAICardResId);
            aiDrawnCard.setVisibility(View.VISIBLE);
        }

        // Determine whose turn it is.
        if (playerHand != null && !playerHand.isEmpty() && isPlayerTurn) {
            showHandSelection();
        } else if (!isPlayerTurn) {
            handler.postDelayed(this::processComputerTurn, 1000);
        } else {
            handler.postDelayed(this::rollForFirstTurn, 1000);
        }

        // Record game start time.
        gameStartTime = System.currentTimeMillis();

        // Start game timer.
        gameTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                int minutes = secondsRemaining / 60;
                int seconds = secondsRemaining % 60;
                timerText.setText(String.format("⏰ %02d:%02d", minutes, seconds));

                // Calculate minute index (0-based) that has passed.
                int minuteIndex = (int) ((TOTAL_GAME_TIME - timeRemaining) / 60000);
                if (minuteIndex > nextEcoTipIndex && nextEcoTipIndex < ecoTipTexts.length) {
                    // Show eco tip dialogue with corresponding sound.
                    showEcoTipDialogueUnskippable(ecoTipTexts[nextEcoTipIndex], ecoTipDurations[nextEcoTipIndex],
                            ecoTipSoundRes[nextEcoTipIndex]);
                    nextEcoTipIndex++;
                }
            }
            @Override
            public void onFinish() {
                if(isGameActive()){
                    showNpcDialogue("Time's up! Game Over!", false);
                    gameOver();
                }
            }
        }.start();

        energyHandler.postDelayed(energyRunnable, 30000);
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(backgroundMusic != null && !backgroundMusic.isPlaying() && !isMuted){
            backgroundMusic.start();
        }
        startGameTimer();
        energyHandler.postDelayed(energyRunnable, 30000);
        updatePlayerCharacterImage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(backgroundMusic != null && backgroundMusic.isPlaying()){
            backgroundMusic.pause();
        }
        if (gameIsOver) {
            SharedPreferences prefs = getSharedPreferences("BattleEcoPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
        } else {
            saveGameState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(backgroundMusic != null){
            backgroundMusic.stop();
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if(gameTimer != null){
            gameTimer.cancel();
        }
        energyHandler.removeCallbacks(energyRunnable);
    }

    // ------------------------------------------------------------------------
    // NEW: Eco Tip Dialogue Method (Unskippable with Sound)
    // ------------------------------------------------------------------------
    /**
     * Displays an eco tip dialogue with the provided message.
     * The dialogue is unskippable (close button hidden) and auto-dismisses after durationMs milliseconds.
     * Also plays the provided sound resource.
     *
     * @param ecoTip     The eco tip message.
     * @param durationMs Duration in milliseconds before auto-dismiss.
     * @param soundResId The raw resource ID for the eco tip sound.
     */
    private void showEcoTipDialogueUnskippable(String ecoTip, int durationMs, int soundResId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_npc_explanation, null);
        TextView npcMessage = dialogView.findViewById(R.id.npcMessage);
        TextView npcCloseButton = dialogView.findViewById(R.id.npcCloseButton);
        npcCloseButton.setVisibility(View.GONE); // Hide the close button.
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentFullScreenDialog);
        builder.setView(dialogView);
        final AlertDialog ecoDialog = builder.create();
        ecoDialog.setCanceledOnTouchOutside(false);
        ecoDialog.setCancelable(false);
        if(ecoDialog.getWindow() != null){
            ecoDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        animateText(npcMessage, ecoTip, 0);
        ecoDialog.show();

        // Play the eco tip sound.
        MediaPlayer ecoTipSoundPlayer = MediaPlayer.create(BattleEcoActivity.this, soundResId);
        ecoTipSoundPlayer.start();
        ecoTipSoundPlayer.setOnCompletionListener(mp -> mp.release());

        // Auto-dismiss after durationMs.
        new Handler().postDelayed(() -> ecoDialog.dismiss(), durationMs);
    }

    // ------------------------------------------------------------------------
    // Helper: Update Player Character Image
    // ------------------------------------------------------------------------
    private void updatePlayerCharacterImage() {
        SharedPreferences gamePrefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String selectedCharacterId = gamePrefs.getString("selectedCharacterId", "char001");
        String json = gamePrefs.getString("characters", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<CharacterModel>>() {}.getType();
            List<CharacterModel> characters = gson.fromJson(json, type);
            for (CharacterModel character : characters) {
                if (character.getId().equals(selectedCharacterId)) {
                    playerCharacterImage.setImageResource(character.getImageResId());
                    return;
                }
            }
        }
        playerCharacterImage.setImageResource(R.drawable.main_character);
    }

    // ------------------------------------------------------------------------
    // Check if Game is Active
    // ------------------------------------------------------------------------
    private boolean isGameActive() {
        return (playerHealth > 0 && computerHealth > 0 && !gameIsOver);
    }

    // ------------------------------------------------------------------------
    // Rolling Dice to Decide Who Goes First
    // ------------------------------------------------------------------------
    private void rollForFirstTurn() {
        final Handler rollHandler = new Handler();
        final long startTime = System.currentTimeMillis();
        final int rollDuration = 2000;
        final int rollInterval = 400;

        battleLogText.setText("Rolling dice...");

        rollHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed < rollDuration) {
                    battleLogText.setText(new Random().nextBoolean() ? "Rolling... Player might go first!" : "Rolling... AI might go first!");
                    rollHandler.postDelayed(this, rollInterval);
                } else {
                    boolean playerStarts = new Random().nextBoolean();
                    isPlayerTurn = playerStarts;
                    battleLogText.setText(playerStarts ? "Final result: Player goes first!" : "Final result: AI goes first!");
                    handler.postDelayed(playerStarts ? () -> startPlayerTurn() : () -> processComputerTurn(), 1000);
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // Pause/Resume Logic
    // ------------------------------------------------------------------------
    private void pauseGame() {
        pausePanel.setVisibility(View.VISIBLE);
        if(backgroundMusic != null && backgroundMusic.isPlaying()){
            backgroundMusic.pause();
        }
        if(gameTimer != null){
            gameTimer.cancel();
        }
        energyHandler.removeCallbacks(energyRunnable);
    }

    private void resumeGame() {
        pausePanel.setVisibility(View.GONE);
        if(backgroundMusic != null && !backgroundMusic.isPlaying() && !isMuted){
            backgroundMusic.start();
        }
        startGameTimer();
        energyHandler.postDelayed(energyRunnable, 30000);
    }

    private void startGameTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        gameTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                int minutes = secondsRemaining / 60;
                int seconds = secondsRemaining % 60;
                timerText.setText(String.format("⏰ %02d:%02d", minutes, seconds));

                // Determine how many minutes have passed (0-based)
                int minuteIndex = (int) ((TOTAL_GAME_TIME - timeRemaining) / 60000);
                if (minuteIndex > nextEcoTipIndex && nextEcoTipIndex < ecoTipTexts.length) {
                    showEcoTipDialogueUnskippable(ecoTipTexts[nextEcoTipIndex], ecoTipDurations[nextEcoTipIndex],
                            ecoTipSoundRes[nextEcoTipIndex]);
                    nextEcoTipIndex++;
                }
            }
            @Override
            public void onFinish() {
                if(isGameActive()){
                    showNpcDialogue("Time's up! Game Over!", false);
                    gameOver();
                }
            }
        }.start();
    }

    // ------------------------------------------------------------------------
    // Saving and Loading Game State
    // ------------------------------------------------------------------------
    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences("BattleEcoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("playerHealth", playerHealth);
        editor.putInt("computerHealth", computerHealth);
        editor.putInt("playerShield", playerShield);
        editor.putInt("computerShield", computerShield);
        editor.putInt("playerEnergy", playerEnergy);
        editor.putInt("computerEnergy", computerEnergy);
        editor.putInt("currentBossIndex", currentBossIndex);
        editor.putLong("timeRemaining", timeRemaining);
        editor.putBoolean("isPlayerTurn", isPlayerTurn);
        if(playerHand != null && !playerHand.isEmpty()){
            Gson gson = new Gson();
            String playerHandJson = gson.toJson(playerHand);
            editor.putString("playerHand", playerHandJson);
        } else {
            editor.remove("playerHand");
        }
        editor.putString("battleLog", battleLogText.getText().toString());
        editor.putInt("lastPlayerCardResId", lastPlayerCardResId);
        editor.putInt("lastAICardResId", lastAICardResId);
        editor.apply();
    }

    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences("BattleEcoPrefs", MODE_PRIVATE);
        if(prefs.contains("playerHealth")){
            int savedPlayerHealth = prefs.getInt("playerHealth", 100);
            if(savedPlayerHealth <= 0){
                resetGameState();
            } else {
                playerHealth = savedPlayerHealth;
                computerHealth = prefs.getInt("computerHealth", BOSS_HEALTHS[currentBossIndex]);
                playerShield = prefs.getInt("playerShield", 0);
                computerShield = prefs.getInt("computerShield", 0);
                playerEnergy = prefs.getInt("playerEnergy", 3);
                computerEnergy = prefs.getInt("computerEnergy", 3);
                currentBossIndex = prefs.getInt("currentBossIndex", 0);
                timeRemaining = prefs.getLong("timeRemaining", 300000);
                isPlayerTurn = prefs.getBoolean("isPlayerTurn", false);

                String playerHandJson = prefs.getString("playerHand", null);
                if(playerHandJson != null){
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<BattleCard>>(){}.getType();
                    playerHand = gson.fromJson(playerHandJson, type);
                }
                String savedBattleLog = prefs.getString("battleLog", "");
                battleLogText.setText(savedBattleLog);
                lastPlayerCardResId = prefs.getInt("lastPlayerCardResId", -1);
                lastAICardResId = prefs.getInt("lastAICardResId", -1);
            }
        } else {
            resetGameState();
        }
    }

    private void resetGameState() {
        playerHealth = 100;
        currentBossIndex = 0;
        computerHealth = BOSS_HEALTHS[currentBossIndex];
        playerShield = 0;
        computerShield = 0;
        playerEnergy = 3;
        computerEnergy = 3;
        timeRemaining = 300000; // 5 minutes
        lastPlayerCardResId = -1;
        lastAICardResId = -1;
        if(handLayout != null){
            handLayout.removeAllViews();
        }
        if(playerHand != null){
            playerHand.clear();
        }
    }

    // ------------------------------------------------------------------------
    // Gameplay: Player and AI Turns
    // ------------------------------------------------------------------------
    private void startPlayerTurn() {
        if(!isGameActive()) return;
        isPlayerTurn = true;
        if(playerEnergy <= 0){
            handLayout.setVisibility(View.GONE);
            skipTurnButton.setVisibility(View.VISIBLE);
        } else {
            skipTurnButton.setVisibility(View.GONE);
            if(playerHand == null || playerHand.isEmpty()){
                drawInitialHand();
            }
            showHandSelection();
        }
    }

    private void drawInitialHand() {
        if(!isGameActive()) return;
        playerHand = new ArrayList<>();
        Random random = new Random();
        for(int i = 0; i < 3; i++){
            int index = random.nextInt(playerDeck.size());
            playerHand.add(playerDeck.get(index));
        }
    }

    private void showHandSelection() {
        if(!isGameActive()) return;
        handLayout.removeAllViews();
        handLayout.setVisibility(View.VISIBLE);
        handLayout.bringToFront();
        handLayout.setElevation(100f);
        LayoutInflater inflater = LayoutInflater.from(this);
        for(BattleCard card : playerHand){
            View cardView = inflater.inflate(R.layout.card_item, handLayout, false);
            ImageView cardImage = cardView.findViewById(R.id.cardImage);
            cardImage.setImageResource(card.getImageResId());
            cardView.setOnClickListener(v -> {
                if(!isGameActive()) return;
                battleLogText.setText("");
                playerHand.clear();
                handLayout.removeAllViews();
                processPlayerTurnWithCard(card);
            });
            handLayout.addView(cardView);
        }
    }

    private void processPlayerTurnWithCard(BattleCard card) {
        if(!isGameActive()) return;
        animateDeckDraw(null, playerDrawnCard, card.getImageResId(), () -> {
            lastPlayerCardResId = card.getImageResId();
            applyCardEffect(card, true);
            updateUI();
            afterTurnCheck();
            if(isGameActive()){
                handler.postDelayed(this::processComputerTurn, 2000);
            }
        });
    }

    private void processComputerTurn() {
        if(!isGameActive()) return;
        isPlayerTurn = false;
        if(computerEnergy <= 0){
            battleLogText.setText("AI skipped its turn due to insufficient energy.");
            handler.postDelayed(this::startPlayerTurn, 1000);
            return;
        }
        Random random = new Random();
        BattleCard card = aiDeck.get(random.nextInt(aiDeck.size()));
        battleLogText.setText("");
        animateDeckDraw(null, aiDrawnCard, card.getImageResId(), () -> {
            lastAICardResId = card.getImageResId();
            applyCardEffect(card, false);
            updateUI();
            afterTurnCheck();
            if(isGameActive()){
                handler.postDelayed(this::startPlayerTurn, 1000);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Damage, Card Effects, and Boss Progression
    // ------------------------------------------------------------------------
    private int[] applyDamage(boolean isPlayerTurn, int damage, boolean ignoreShield) {
        int blocked = 0;
        if(isPlayerTurn){
            if(!ignoreShield){
                if(computerShield >= damage){
                    blocked = damage;
                    computerShield -= damage;
                    damage = 0;
                } else {
                    blocked = computerShield;
                    damage -= computerShield;
                    computerShield = 0;
                }
            }
            computerHealth -= damage;
        } else {
            if(!ignoreShield){
                if(playerShield >= damage){
                    blocked = damage;
                    playerShield -= damage;
                    damage = 0;
                } else {
                    blocked = playerShield;
                    damage -= playerShield;
                    playerShield = 0;
                }
            }
            playerHealth -= damage;
        }
        return new int[]{damage, blocked};
    }

    private void applyCardEffect(BattleCard card, boolean isPlayerTurn) {
        if(!isGameActive()) return;
        String logMessage = "";
        int energyCost = extractEnergyCost(card.getName());
        if(isPlayerTurn){
            if(playerEnergy < energyCost) return;
            playerEnergy -= energyCost;
        } else {
            if(computerEnergy < energyCost) return;
            computerEnergy -= energyCost;
        }
        String cardName = card.getName().toLowerCase();

        if(cardName.contains("reforest revival")){
            if(isPlayerTurn){
                int[] result = applyDamage(true, 60, false);
                playerHealth += 40;
                logMessage = "Player used Reforest Revival: dealt " + result[0] + " damage";
                if(result[1] > 0) {
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += " and healed 40 HP.";
            } else {
                int[] result = applyDamage(false, 60, false);
                logMessage = "Computer used Reforest Revival: dealt " + result[0] + " damage";
                if(result[1] > 0) {
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("nature's embrace")){
            if(isPlayerTurn){
                playerHealth += 30;
                playerEnergy += 2;
                logMessage = "Player used Nature's Embrace: healed 30 HP and gained 2 Energy.";
            } else {
                computerHealth += 30;
                computerEnergy += 2;
                logMessage = "Computer used Nature's Embrace: healed 30 HP and gained 2 Energy.";
            }
        }
        else if(cardName.contains("piercing staff")){
            if(isPlayerTurn){
                int[] result = applyDamage(true, 35, true);
                playerShield += 20;
                logMessage = "Player used Piercing Staff: dealt " + result[0] + " damage (ignoring shields) and gained 20 shield.";
            } else {
                int[] result = applyDamage(false, 35, true);
                computerShield += 20;
                logMessage = "Computer used Piercing Staff: dealt " + result[0] + " damage (ignoring shields) and gained 20 shield.";
            }
        }
        else if(cardName.contains("forest aura")){
            if(isPlayerTurn){
                playerEnergy += 1;
                logMessage = "Player used Forest Aura: gained 1 Energy.";
            } else {
                computerEnergy += 1;
                logMessage = "Computer used Forest Aura: gained 1 Energy.";
            }
        }
        else if(cardName.contains("green shield")){
            if(isPlayerTurn){
                playerShield += 15;
                int[] result = applyDamage(true, 5, false);
                logMessage = "Player used Green Shield: gained 15 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                computerShield += 15;
                int[] result = applyDamage(false, 5, false);
                logMessage = "Computer used Green Shield: gained 15 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("eco barrier")){
            if(isPlayerTurn){
                playerShield += 20;
                int[] result = applyDamage(true, 5, false);
                logMessage = "Player used Eco Barrier: gained 20 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                computerShield += 20;
                int[] result = applyDamage(false, 5, false);
                logMessage = "Computer used Eco Barrier: gained 20 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("carbon guard")){
            if(isPlayerTurn){
                playerShield += 25;
                int[] result = applyDamage(true, 10, false);
                logMessage = "Player used Carbon Guard: gained 25 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                computerShield += 25;
                int[] result = applyDamage(false, 10, false);
                logMessage = "Computer used Carbon Guard: gained 25 shield and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("fossil fury")){
            if(isPlayerTurn){
                int[] result = applyDamage(true, 25, false);
                logMessage = "Player used Fossil Fury: dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                int[] result = applyDamage(false, 25, false);
                logMessage = "Computer used Fossil Fury: dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("pollution pulse")){
            int reduction = 30;
            int baseDamage = 45;
            if(isPlayerTurn){
                int shieldReduction = Math.min(computerShield, reduction);
                computerShield -= shieldReduction;
                int extraDamage = reduction - shieldReduction;
                int totalDamage = baseDamage + extraDamage;
                int[] result = applyDamage(true, totalDamage, false);
                logMessage = "Player used Pollution Pulse: reduced enemy shield by " + reduction +
                        " and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                int shieldReduction = Math.min(playerShield, reduction);
                playerShield -= shieldReduction;
                int extraDamage = reduction - shieldReduction;
                int totalDamage = baseDamage + extraDamage;
                int[] result = applyDamage(false, totalDamage, false);
                logMessage = "Computer used Pollution Pulse: reduced player's shield by " + reduction +
                        " and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("emissions eruption")){
            if(isPlayerTurn){
                int[] result = applyDamage(true, 35, false);
                logMessage = "Player used Emissions Eruption: dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                int[] result = applyDamage(false, 35, false);
                logMessage = "Computer used Emissions Eruption: dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        else if(cardName.contains("pollution moon")){
            if(isPlayerTurn){
                playerShield += 25;
                logMessage = "Player used Pollution Moon: gained 25 shield.";
            } else {
                computerShield += 25;
                logMessage = "Computer used Pollution Moon: gained 25 shield.";
            }
        }
        else {
            if(isPlayerTurn){
                int[] result = applyDamage(true, card.getEffectValue(), false);
                logMessage = "Player used " + card.getName() + " and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            } else {
                int[] result = applyDamage(false, card.getEffectValue(), false);
                logMessage = "Computer used " + card.getName() + " and dealt " + result[0] + " damage";
                if(result[1] > 0){
                    logMessage += " (" + result[1] + " blocked by shields)";
                }
                logMessage += ".";
            }
        }
        battleLogText.setText(logMessage);
    }

    /**
     * Extracts the energy cost from the card name.
     */
    private int extractEnergyCost(String cardName) {
        int cost = 0;
        int index = cardName.lastIndexOf("Energy");
        if(index != -1) {
            int i = index - 1;
            while(i >= 0 && Character.isDigit(cardName.charAt(i))) {
                i--;
            }
            try {
                String numStr = cardName.substring(i+1, index).trim();
                cost = Integer.parseInt(numStr);
            } catch (NumberFormatException e) {
                cost = 0;
            }
        }
        return cost;
    }

    private void updateUI() {
        playerHealthText.setText("❤️ " + playerHealth);
        computerHealthText.setText(playerHealth > 0 ? computerHealth + " ❤️" : "0 ❤️");
        playerShieldText.setText("🛡️ " + playerShield);
        computerShieldText.setText(computerShield + "🛡️");
        playerEnergyText.setText("⚡ " + playerEnergy);
        computerEnergyText.setText(computerEnergy + "⚡");
    }

    private void afterTurnCheck() {
        if(playerHealth <= 0){
            showNpcDialogue("Player is defeated!", false);
            gameOver();
        } else if(computerHealth <= 0){
            showNpcDialogue("You defeated the boss!", false);
            proceedToNextBossOrWin();
        }
    }

    private void proceedToNextBossOrWin() {
        if(currentBossIndex == 0){
            storeVictoryInFirestoreIncrement(30);
        } else if(currentBossIndex == 1){
            storeVictoryInFirestoreIncrement(50);
        } else if(currentBossIndex == 2){
            storeVictoryInFirestoreIncrement(80);
        }
        currentBossIndex++;
        if(currentBossIndex < BOSS_HEALTHS.length){
            computerHealth = BOSS_HEALTHS[currentBossIndex];
            aiCharacterImage.setImageResource(BOSS_IMAGES[currentBossIndex]);
            updateUI();
            handler.postDelayed(() -> showBossIntroDialogue(currentBossIndex), 500);
            handler.postDelayed(this::startPlayerTurn, 2000);
        } else {
            if(gameTimer != null){
                gameTimer.cancel();
            }
            long totalTimeTaken = System.currentTimeMillis() - gameStartTime;
            int minutes = (int)(totalTimeTaken / 60000);
            int seconds = (int)((totalTimeTaken % 60000) / 1000);
            showNpcDialogue("Congratulations! You defeated all bosses in " + minutes + "m " + seconds + "s!", true);
            gameOver();
        }
    }

    private void gameOver() {
        gameIsOver = true;
        battleLogText.setText("Game Over!");
    }

    // ------------------------------------------------------------------------
    // Restart Game
    // ------------------------------------------------------------------------
    private void restartGame() {
        SharedPreferences prefs = getSharedPreferences("BattleEcoPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        resetGameState();
        // Clear drawn cards and player's hand.
        lastPlayerCardResId = -1;
        lastAICardResId = -1;
        if(handLayout != null){
            handLayout.removeAllViews();
        }
        if(playerHand != null){
            playerHand.clear();
        }
        Intent intent = new Intent(this, BattleEcoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }

    // ------------------------------------------------------------------------
    // Animations
    // ------------------------------------------------------------------------
    private void animateText(TextView textView, String text, int index) {
        if(index < text.length()){
            textView.setText(text.substring(0, index + 1));
            new Handler().postDelayed(() -> animateText(textView, text, index + 1), 40);
        }
    }

    private void animateDeckDraw(final ImageView deckPreview, final ImageView drawnCard,
                                 final int newImageResId, final Runnable onAnimationEnd) {
        MediaPlayer mp = MediaPlayer.create(BattleEcoActivity.this, R.raw.cardflip);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
        drawnCard.setImageResource(newImageResId);
        drawnCard.setVisibility(View.INVISIBLE);
        ImageView startView = deckPreview;
        if(startView == null){
            if(drawnCard == playerDrawnCard){
                startView = playerCharacterImage;
            } else if(drawnCard == aiDrawnCard){
                startView = aiCharacterImage;
            }
        }
        int[] startPos = new int[2];
        int[] targetPos = new int[2];
        startView.getLocationOnScreen(startPos);
        drawnCard.getLocationOnScreen(targetPos);
        final float deltaX = startPos[0] - targetPos[0];
        final float deltaY = startPos[1] - targetPos[1];
        drawnCard.setTranslationX(deltaX);
        drawnCard.setTranslationY(deltaY);
        drawnCard.setRotationY(0f);
        drawnCard.setVisibility(View.VISIBLE);
        ObjectAnimator flipOut = ObjectAnimator.ofFloat(drawnCard, "rotationY", 0f, 90f);
        flipOut.setDuration(200);
        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                drawnCard.setRotationY(270f);
                ObjectAnimator flipIn = ObjectAnimator.ofFloat(drawnCard, "rotationY", 270f, 360f);
                flipIn.setDuration(200);
                flipIn.start();
            }
        });
        ObjectAnimator translateXAnim = ObjectAnimator.ofFloat(drawnCard, "translationX", deltaX, 0f);
        ObjectAnimator translateYAnim = ObjectAnimator.ofFloat(drawnCard, "translationY", deltaY, 0f);
        translateXAnim.setDuration(400);
        translateYAnim.setDuration(400);
        translateXAnim.start();
        translateYAnim.start();
        flipOut.start();
        translateYAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(onAnimationEnd != null){
                    onAnimationEnd.run();
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // Firestore Scoring
    // ------------------------------------------------------------------------
    private void storeVictoryInFirestoreIncrement(int pointsEarned) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("points", FieldValue.increment(pointsEarned));
        updates.put("highScore", FieldValue.increment(pointsEarned));
        db.collection("Games").document("Jonr")
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Points incremented by " + pointsEarned + "!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ------------------------------------------------------------------------
    // Existing NPC Dialogue Methods
    // ------------------------------------------------------------------------
    private void showNpcDialogue(String message, boolean enableAfterDismiss) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_npc_explanation, null);
        TextView npcMessage = dialogView.findViewById(R.id.npcMessage);
        TextView npcCloseButton = dialogView.findViewById(R.id.npcCloseButton);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentFullScreenDialog);
        builder.setView(dialogView);
        final AlertDialog npcDialog = builder.create();
        npcDialog.setCanceledOnTouchOutside(false);
        npcDialog.setOnShowListener(dialogInterface -> {
            if(npcDialog.getWindow() != null){
                npcDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });
        animateText(npcMessage, message, 0);
        npcCloseButton.setOnClickListener(v -> {
            npcDialog.dismiss();
            if(enableAfterDismiss && isGameActive()){
                startPlayerTurn();
            }
        });
        npcDialog.show();
    }

    private void showNpcDialogue(String message) {
        showNpcDialogue(message, true);
    }

    private void showBossIntroDialogue(int bossIndex) {
        switch (bossIndex) {
            case 0:
                showNpcDialogue("The Smoke Boss emerges! Defeat it quickly before time runs out.", true);
                break;
            case 1:
                showNpcDialogue("The Pollution Boss emerges! Hurry, the clock is ticking.", true);
                break;
            case 2:
                showNpcDialogue("The Volcanic Boss has arrived! Beat it before the 5 minutes are up.", true);
                break;
            default:
                break;
        }
    }
}
