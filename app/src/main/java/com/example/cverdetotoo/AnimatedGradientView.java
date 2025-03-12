package com.example.cverdetotoo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AnimatedGradientView extends View {
    private Paint paint;
    private LinearGradient linearGradient;
    private ValueAnimator animator;
    private float translate;

    // Define your gradient colors
    private int[] colors = new int[] {
            0xFF3AF817, // Green
            0xFF7ED957  // Lighter green (or any other color)
    };

    public AnimatedGradientView(Context context) {
        super(context);
        init();
    }

    public AnimatedGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();

        // Initial gradient setup; actual width and height will be set in onSizeChanged().
        linearGradient = new LinearGradient(
                0, 0, getWidth(), getHeight(),
                colors, null, Shader.TileMode.MIRROR
        );
        paint.setShader(linearGradient);

        // Animate from 0 to 1 over 20 seconds, infinitely
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(20000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            translate = fraction * getWidth();

            // Shift the gradient horizontally based on fraction
            linearGradient = new LinearGradient(
                    translate, 0,
                    translate + getWidth(), getHeight(),
                    colors, null, Shader.TileMode.MIRROR
            );
            paint.setShader(linearGradient);
            invalidate(); // Redraw view
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Fill the entire view with the animated gradient
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
