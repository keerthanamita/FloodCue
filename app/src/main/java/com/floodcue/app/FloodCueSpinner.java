package com.floodcue.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FloodCueSpinner extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float rotation = 0f;

    // Spinner settings
    private static final float START_ANGLE = -45f;
    private static final float SWEEP_ANGLE = 285f;

    // Colors
    private static final int COLOR_BLUE = Color.rgb(22, 139, 255);
    private static final int COLOR_CYAN = Color.rgb(0, 217, 255);

    public FloodCueSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float radius = Math.min(centerX, centerY) - 5f;

        canvas.save();

        // Rotate spinner
        canvas.rotate(rotation, centerX, centerY);

        /*
         * Draw the spinner as many small arc segments.
         *
         * This allows us to change the thickness gradually,
         * creating the broad-to-thin effect.
         */
        int segments = 70;

        float segmentAngle = SWEEP_ANGLE / segments;

        for (int i = 0; i < segments; i++) {

            float progress = (float) i / (segments - 1);

            // Broad at the beginning
            // Thin at the end
            float strokeWidth = 7f - (5.2f * progress);

            // Two-color transition
            int color = interpolateColor(
                    COLOR_BLUE,
                    COLOR_CYAN,
                    progress
            );

            paint.setColor(color);
            paint.setStrokeWidth(strokeWidth);

            float angle = START_ANGLE + (i * segmentAngle);

            canvas.drawArc(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    angle,
                    segmentAngle + 0.8f,
                    false,
                    paint
            );
        }

        canvas.restore();
    }

    /**
     * Smoothly changes from blue to cyan.
     */
    private int interpolateColor(int startColor, int endColor, float fraction) {

        int startRed = Color.red(startColor);
        int startGreen = Color.green(startColor);
        int startBlue = Color.blue(startColor);

        int endRed = Color.red(endColor);
        int endGreen = Color.green(endColor);
        int endBlue = Color.blue(endColor);

        int red = (int) (startRed + (endRed - startRed) * fraction);
        int green = (int) (startGreen + (endGreen - startGreen) * fraction);
        int blue = (int) (startBlue + (endBlue - startBlue) * fraction);

        return Color.rgb(red, green, blue);
    }

    private void startAnimation() {

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);

        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);

        animator.addUpdateListener(animation -> {

            rotation = (float) animation.getAnimatedValue();

            invalidate();
        });

        animator.start();
    }
}