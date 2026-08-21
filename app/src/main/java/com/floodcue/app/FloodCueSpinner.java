package com.floodcue.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class FloodCueSpinner extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float rotation = 0;

    public FloodCueSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint.setStyle(Paint.Style.STROKE);

        // Increased thickness
        paint.setStrokeWidth(7f);

        paint.setStrokeCap(Paint.Cap.ROUND);

        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float radius = Math.min(centerX, centerY) - 7;

        /*
         * High-contrast palette:
         * Magenta -> Violet -> Deep Purple
         *
         * These colors stand out from the
         * blue/cyan splash background.
         */
        SweepGradient gradient = new SweepGradient(
                centerX,
                centerY,
                new int[]{
                        0x00FF4FD8,   // transparent
                        0x00FF4FD8,

                        0xFFFF4FD8,   // bright pink
                        0xFFD946EF,   // magenta
                        0xFF8B5CF6,   // violet
                        0xFF6D28D9,   // deep violet

                        0x006D28D9    // fade out
                },
                null
        );

        paint.setShader(gradient);

        canvas.save();

        canvas.rotate(rotation, centerX, centerY);

        canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                -55,
                290,
                false,
                paint
        );

        canvas.restore();
    }

    private void startAnimation() {

        ValueAnimator animator = ValueAnimator.ofFloat(0, 360);

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