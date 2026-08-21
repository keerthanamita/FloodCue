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

        // Thicker spinner
        paint.setStrokeWidth(5.5f);

        paint.setStrokeCap(Paint.Cap.ROUND);

        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float radius = Math.min(centerX, centerY) - 6;

        /*
         * Two strong contrasting colors.
         * The transparent portions create the
         * thin/fading end similar to MX Player.
         */
        SweepGradient gradient = new SweepGradient(
                centerX,
                centerY,
                new int[]{
                        0x0000D9FF,   // transparent cyan
                        0x0000D9FF,   // transparent cyan
                        0xFF00D9FF,   // bright cyan
                        0xFF168BFF,   // blue
                        0xFF005BFF,   // deeper blue
                        0x00168BFF    // fade out
                },
                null
        );

        paint.setShader(gradient);

        canvas.save();

        canvas.rotate(rotation, centerX, centerY);

        /*
         * Long arc with rounded ends.
         * The gradient makes one side appear
         * thicker/brighter and the other side fade.
         */
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