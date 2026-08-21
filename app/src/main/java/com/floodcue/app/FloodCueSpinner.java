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

        // Thick but still elegant
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
         * High-contrast warm palette
         * against the blue/cyan splash.
         */
        SweepGradient gradient = new SweepGradient(
                centerX,
                centerY,
                new int[]{
                        0x00FF6B35,
                        0x00FFD166,
                        0xFFFFD166,
                        0xFFFF6B35
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