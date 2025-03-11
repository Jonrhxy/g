package com.example.cverdetotoo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class video2 extends AppCompatActivity {

    private VideoView storyVideo;
    private ImageButton closeButton;
    private ProgressBar progressBar;
    private Handler progressHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video1);

        // Initialize the VideoView, close button, and progress bar
        storyVideo = findViewById(R.id.story_video);
        closeButton = findViewById(R.id.close_button);
        progressBar = findViewById(R.id.progress_bar);

        // Load the video from the raw folder (replace 'vid1intro' with your actual file name)
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.vid1intro);
        storyVideo.setVideoURI(videoUri);

        // Set a listener to know when the video is ready to play
        storyVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // Set the progress bar maximum to the video duration
                progressBar.setMax(storyVideo.getDuration());

                // Start playing the video
                storyVideo.start();

                // Begin updating the progress bar
                updateProgressBar();
            }
        });

        // Set a listener to redirect when the video completes
        storyVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Redirect to PostAssess1 activity after video finishes
                Intent intent = new Intent(video2.this, PostAssess1.class);
                startActivity(intent);
                finish(); // Optional: finish current activity if you don't want users to return here
            }
        });

        // Close the video view when the close button is pressed
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // Runnable to update the progress bar every 100ms
    private void updateProgressBar() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (storyVideo != null && storyVideo.isPlaying()) {
                    // Update the progress bar to match the current video position
                    progressBar.setProgress(storyVideo.getCurrentPosition());
                    progressHandler.postDelayed(this, 100);
                }
            }
        }, 100);
    }
}
