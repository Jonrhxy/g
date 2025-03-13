package com.example.cverdetotoo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class PostAsses24 extends AppCompatActivity {

    private int score = 0;

    // Timer-related variables
    private static final long TOTAL_TIME = 20000; // 20 seconds in milliseconds
    private CountDownTimer countDownTimer;
    private TextView timerTextView;

    // The correct answer is assumed to be prebtn1d
    private int correctAnswerId = R.id.prebtn1a;
    private RadioGroup radioGroup;
    private Button submitButton, next11Button;
    private boolean answered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_post_asses24);

        // Retrieve score from previous activity if available
        score = getIntent().getIntExtra("score", 0);

        // Initialize views
        timerTextView = findViewById(R.id.posttimer24);  // Initialize timer TextView
        radioGroup = findViewById(R.id.radiopostQ24);
        submitButton = findViewById(R.id.postq24);
        next11Button = findViewById(R.id.postnext24);

        // Disable the next button until an answer is submitted
        next11Button.setEnabled(false);

        // Start the 20-second timer
        startTimer();

        // Set listener for the submit button click
        submitButton.setOnClickListener(v -> {
            if (!answered) {
                int selectedId = radioGroup.getCheckedRadioButtonId();

                if (selectedId == -1) {
                    // No answer selected
                    Toast.makeText(PostAsses24.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Cancel the timer
                countDownTimer.cancel();

                // Reset the colors of all radio buttons (in case user is retrying)
                resetRadioButtonColors(radioGroup);

                if (selectedId == correctAnswerId) {
                    // Correct answer: update the selected radio button color to green
                    RadioButton selectedRadioButton = findViewById(selectedId);
                    selectedRadioButton.setTextColor(Color.GREEN);
                    selectedRadioButton.setButtonTintList(ColorStateList.valueOf(Color.GREEN));
                    Toast.makeText(PostAsses24.this, "Correct!", Toast.LENGTH_SHORT).show();
                    score++;  // Increase score
                } else {
                    // Wrong answer: mark the selected radio button red
                    RadioButton selectedRadioButton = findViewById(selectedId);
                    selectedRadioButton.setTextColor(Color.RED);
                    selectedRadioButton.setButtonTintList(ColorStateList.valueOf(Color.RED));

                    // Also highlight the correct answer in green
                    RadioButton correctRadioButton = findViewById(correctAnswerId);
                    correctRadioButton.setTextColor(Color.GREEN);
                    correctRadioButton.setButtonTintList(ColorStateList.valueOf(Color.GREEN));
                    Toast.makeText(PostAsses24.this, "Incorrect!", Toast.LENGTH_SHORT).show();
                }

                // Mark as answered, disable further selections, and enable next button
                answered = true;
                disableRadioGroup();
                submitButton.setEnabled(false);
                next11Button.setEnabled(true);
            }
        });

        // Set listener for the next11 button click to redirect to PreAsses12.class
        next11Button.setOnClickListener(v -> {
            // Only proceed if answer has been submitted
            if (answered) {
                Intent intent = new Intent(PostAsses24.this, PostAsses25.class);
                intent.putExtra("score", score);  // Passing the cumulative score to the next activity
                startActivity(intent);
            } else {
                Toast.makeText(PostAsses24.this, "Please submit your answer first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Starts the countdown timer for 20 seconds.
     */
    private void startTimer() {
        countDownTimer = new CountDownTimer(TOTAL_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                timerTextView.setText("Time left: " + secondsLeft + "s");
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Time's up!");
                disableRadioGroup();
                // Automatically select and show the correct answer when time runs out
                radioGroup.check(correctAnswerId);
                answered = true;
                submitButton.setEnabled(false);
                next11Button.setEnabled(true); // Enable next button on auto-submission
                Toast.makeText(PostAsses24.this, "Time is up! Correct answer is shown.", Toast.LENGTH_SHORT).show();

                // Highlight the correct answer in green
                RadioButton correctRadioButton = findViewById(correctAnswerId);
                correctRadioButton.setTextColor(Color.GREEN);
                correctRadioButton.setButtonTintList(ColorStateList.valueOf(Color.GREEN));
            }
        }.start();
    }

    /**
     * Disables all RadioButtons in the RadioGroup to prevent further changes.
     */
    private void disableRadioGroup() {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            View child = radioGroup.getChildAt(i);
            child.setEnabled(false);
        }
    }

    /**
     * Resets the text color and button tint of all RadioButtons in the RadioGroup.
     */
    private void resetRadioButtonColors(RadioGroup radioGroup) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            if (radioGroup.getChildAt(i) instanceof RadioButton) {
                RadioButton rb = (RadioButton) radioGroup.getChildAt(i);
                rb.setTextColor(Color.BLACK);  // Reset text color to black
                rb.setButtonTintList(null);     // Reset to default tint
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
