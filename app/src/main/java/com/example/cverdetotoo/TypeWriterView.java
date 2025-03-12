package com.example.cverdetotoo;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * A custom TextView that animates text in a typewriter style.
 */
public class TypeWriterView extends AppCompatTextView {

    private CharSequence mText;
    private int mIndex;
    private long mDelay = 50; // Default delay in ms

    private Handler mHandler = new Handler();

    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(mText.subSequence(0, mIndex++));
            if (mIndex <= mText.length()) {
                mHandler.postDelayed(characterAdder, mDelay);
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
     * Initializes the TypeWriterView with a default text if none is set.
     */
    private void initTypeWriter() {
        // Use post(...) to ensure the view is fully inflated before animating.
        post(() -> {
            // If XML doesn't provide text, use a default message.
            // Otherwise, animate whatever text is currently set.
            if (TextUtils.isEmpty(getText())) {
                animateText("Hey there, dive into epic videos, nuncover cool trivia, nand crush your goals to score big points today!");
            } else {
                // Animate the text that was set via XML (android:text="...")
                CharSequence existingText = getText();
                setText(""); // clear before we animate
                animateText(existingText);
            }
        });
    }

    /**
     * Begins animating the given text with a typewriter effect.
     */
    public void animateText(CharSequence text) {
        mText = text;
        mIndex = 0;
        setText("");
        mHandler.removeCallbacks(characterAdder);
        mHandler.postDelayed(characterAdder, mDelay);
    }

    /**
     * Set how long (in milliseconds) to wait before adding each character.
     */
    public void setCharacterDelay(long millis) {
        mDelay = millis;
    }
}
