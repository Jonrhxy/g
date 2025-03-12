package com.example.cverdetotoo;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * A custom TextView that animates text in a typewriter style and synchronizes with audio playback.
 */
public class TypeWriterView extends AppCompatTextView {

    private CharSequence mText;
    private int mIndex;
    private Handler mHandler = new Handler();
    private MediaPlayer mMediaPlayer; // Holds the MediaPlayer instance for audio playback

    // Runnable that syncs text display with the audio playback
    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null && mText != null) {
                int currentPosition = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                int textLength = mText.length();
                // Calculate the number of characters to display based on audio progress.
                int charactersToShow = (int) ((currentPosition / (double) duration) * textLength);
                if (charactersToShow > mIndex) {
                    mIndex = charactersToShow;
                    // Ensure we do not exceed the text length.
                    if (mIndex > mText.length()) {
                        mIndex = mText.length();
                    }
                    setText(mText.subSequence(0, mIndex));
                }
                if (mIndex < mText.length()) {
                    mHandler.postDelayed(this, 50);
                }
            }
        }
    };

    public TypeWriterView(Context context) {
        super(context);
        initTypeWriter();
    }

    public TypeWriterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTypeWriter();
    }

    /**
     * Initializes the TypeWriterView with default text if none is provided in XML.
     */
    private void initTypeWriter() {
        // Post a runnable to ensure the view is fully inflated before starting animation.
        post(() -> {
            if (TextUtils.isEmpty(getText())) {
                animateText("Hey there, dive into epic videos, uncover cool trivia, and crush your goals to score big points today!");
            } else {
                CharSequence existingText = getText();
                setText(""); // Clear text before animating.
                animateText(existingText);
            }
        });
    }

    /**
     * Begins animating the given text with a typewriter effect that is synchronized to audio playback.
     * The audio's progress is polled to determine how much text should be displayed.
     *
     * @param text The text to animate.
     */
    public void animateText(CharSequence text) {
        mText = text;
        mIndex = 0;
        setText("");
        mHandler.removeCallbacks(syncRunnable);

        // Release any previous MediaPlayer.
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // Create and start MediaPlayer for the sound (mascot_aivoice1, now ~7.2 seconds long).
        mMediaPlayer = MediaPlayer.create(getContext(), R.raw.mascot_aivoice1);
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            // Start the synchronization runnable.
            mHandler.post(syncRunnable);
            mMediaPlayer.setOnCompletionListener(mp -> {
                // Ensure full text is displayed when audio finishes.
                setText(mText);
                mp.release();
                mMediaPlayer = null;
            });
        }
    }

    /**
     * Optionally set a custom delay between each character.
     * (This method is less used now since text is synced to audio progress.)
     *
     * @param millis Delay in milliseconds.
     */
    public void setCharacterDelay(long millis) {
        // Not used in the sync-based implementation.
    }

    /**
     * When the window visibility changes, pause the audio if the view is not visible.
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        } else {
            if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(syncRunnable);
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
