package com.floodcue.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class AuroraBackgroundView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final PorterDuffXfermode screenMode =
            new PorterDuffXfermode(PorterDuff.Mode.SCREEN);

    private final PorterDuffXfermode multiplyMode =
            new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);

    public AuroraBackgroundView(Context context) {
        super(context);
        initialize();
    }

    public AuroraBackgroundView(
            Context context,
            AttributeSet attrs) {

        super(context, attrs);
        initialize();
    }

    public AuroraBackgroundView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {

        /*
         * BlurMaskFilter is used to reproduce the CSS
         * filter: blur(...) effect.
         *
         * Software rendering keeps this compatible
         * with minSdk 24.
         */
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        boolean darkMode = isDarkMode();

        if (darkMode) {
            drawDarkPolaris(canvas, width, height);
        } else {
            drawLightPolaris(canvas, width, height);
        }
    }

    // ============================================================
    // DARK POLARIS
    // ============================================================

    private void drawDarkPolaris(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Original CSS:
         *
         * body {
         *     background-color: #100e0b;
         * }
         */
        canvas.drawColor(
                Color.rgb(16, 14, 11)
        );

        /*
         * Layer 1
         *
         * conic-gradient(
         *     from 160deg at 50% 50%,
         *     #06b6d4,
         *     #2563eb,
         *     #7c3aed,
         *     #a855f7,
         *     #22d3ee,
         *     #06b6d4
         * )
         *
         * screen
         * blur(155px)
         * opacity(0.68)
         */
        drawConicLayer(
                canvas,
                width,
                height,
                screenMode,
                0.68f,
                155f
        );

        /*
         * Layer 2
         *
         * radial-gradient(
         *     circle at 50% 50%,
         *     rgba(207,250,254,0.18) 0%,
         *     rgba(103,232,249,0.08) 22%,
         *     transparent 54%
         * )
         *
         * screen
         * blur(63px)
         * opacity(0.9)
         */
        drawCenterGlow(
                canvas,
                width,
                height,
                screenMode,
                0.90f,
                63f
        );

        /*
         * Layer 3
         *
         * radial-gradient(
         *     circle at 50% 50%,
         *     rgba(0,0,0,0.82) 0%,
         *     rgba(0,0,0,0.42) 38%,
         *     transparent 62%
         * )
         *
         * multiply
         * blur(45px)
         * opacity(0.9)
         */
        drawDarkCenter(
                canvas,
                width,
                height,
                multiplyMode,
                0.90f,
                45f
        );
    }

    // ============================================================
    // LIGHT POLARIS
    // ============================================================

    private void drawLightPolaris(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Light base.
         *
         * The original specification says that SCREEN
         * layers should become MULTIPLY on a light surface.
         */
        canvas.drawColor(
                Color.rgb(250, 250, 250)
        );

        /*
         * Layer 1:
         *
         * screen -> multiply
         */
        drawConicLayer(
                canvas,
                width,
                height,
                multiplyMode,
                0.68f,
                155f
        );

        /*
         * Layer 2:
         *
         * screen -> multiply
         */
        drawCenterGlow(
                canvas,
                width,
                height,
                multiplyMode,
                0.90f,
                63f
        );

        /*
         * Layer 3 remains multiply.
         */
        drawDarkCenter(
                canvas,
                width,
                height,
                multiplyMode,
                0.90f,
                45f
        );
    }

    // ============================================================
    // POLARIS LAYER 1
    // CSS CONIC GRADIENT
    // ============================================================

    private void drawConicLayer(
            Canvas canvas,
            int width,
            int height,
            PorterDuffXfermode blendMode,
            float opacity,
            float blurDp) {

        float centerX = width * 0.50f;
        float centerY = height * 0.50f;

        /*
         * Android SweepGradient is the native equivalent
         * of a conic gradient.
         *
         * CSS:
         *
         * from 160deg at 50% 50%
         */
        SweepGradient sweepGradient =
                new SweepGradient(
                        centerX,
                        centerY,

                        new int[]{
                                Color.rgb(6, 182, 212),    // #06b6d4
                                Color.rgb(37, 99, 235),    // #2563eb
                                Color.rgb(124, 58, 237),   // #7c3aed
                                Color.rgb(168, 85, 247),   // #a855f7
                                Color.rgb(34, 211, 238),   // #22d3ee
                                Color.rgb(6, 182, 212)     // #06b6d4
                        },

                        new float[]{
                                0.0f,
                                0.20f,
                                0.40f,
                                0.60f,
                                0.80f,
                                1.0f
                        }
                );

        /*
         * SweepGradient starts at 0 degrees.
         *
         * We rotate the canvas by 160 degrees so that
         * the visual starting angle matches:
         *
         * CSS "from 160deg"
         */
        canvas.save();

        canvas.rotate(
                160f,
                centerX,
                centerY
        );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(sweepGradient);

        /*
         * CSS opacity: 0.68
         *
         * 0.68 * 255 = 173
         */
        paint.setAlpha(
                (int) (255f * opacity)
        );

        /*
         * CSS filter: blur(155px)
         *
         * We convert the requested CSS-like size
         * to Android density units.
         */
        paint.setMaskFilter(
                new android.graphics.BlurMaskFilter(
                        dp(blurDp),
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                )
        );

        /*
         * CSS mix-blend-mode:
         *
         * screen in dark mode
         * multiply in light mode
         */
        paint.setXfermode(blendMode);

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        paint.setXfermode(null);
        paint.setMaskFilter(null);
        paint.setShader(null);

        canvas.restore();
    }

    // ============================================================
    // POLARIS LAYER 2
    // CENTER RADIAL GLOW
    // ============================================================

    private void drawCenterGlow(
            Canvas canvas,
            int width,
            int height,
            PorterDuffXfermode blendMode,
            float opacity,
            float blurDp) {

        float centerX = width * 0.50f;
        float centerY = height * 0.50f;

        /*
         * CSS:
         *
         * radial-gradient(
         *     circle at 50% 50%,
         *
         *     rgba(207,250,254,0.18) 0%,
         *     rgba(103,232,249,0.08) 22%,
         *     transparent 54%
         * )
         */

        RadialGradient radialGradient =
                new RadialGradient(
                        centerX,
                        centerY,

                        /*
                         * Radius controls the 54% outer boundary.
                         */
                        Math.min(width, height) * 0.54f,

                        new int[]{
                                Color.argb(
                                        46,
                                        207,
                                        250,
                                        254
                                ),

                                Color.argb(
                                        20,
                                        103,
                                        232,
                                        249
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{
                                0.0f,
                                0.22f,
                                0.54f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(radialGradient);

        /*
         * CSS opacity: 0.9
         */
        paint.setAlpha(
                (int) (255f * opacity)
        );

        /*
         * CSS blur(63px)
         */
        paint.setMaskFilter(
                new android.graphics.BlurMaskFilter(
                        dp(blurDp),
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(blendMode);

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        paint.setXfermode(null);
        paint.setMaskFilter(null);
        paint.setShader(null);
    }

    // ============================================================
    // POLARIS LAYER 3
    // DARK CENTER / MULTIPLY
    // ============================================================

    private void drawDarkCenter(
            Canvas canvas,
            int width,
            int height,
            PorterDuffXfermode blendMode,
            float opacity,
            float blurDp) {

        float centerX = width * 0.50f;
        float centerY = height * 0.50f;

        /*
         * CSS:
         *
         * radial-gradient(
         *     circle at 50% 50%,
         *     rgba(0,0,0,0.82) 0%,
         *     rgba(0,0,0,0.42) 38%,
         *     transparent 62%
         * )
         */

        RadialGradient radialGradient =
                new RadialGradient(
                        centerX,
                        centerY,

                        Math.min(width, height) * 0.62f,

                        new int[]{
                                Color.argb(
                                        209,
                                        0,
                                        0,
                                        0
                                ),

                                Color.argb(
                                        107,
                                        0,
                                        0,
                                        0
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{
                                0.0f,
                                0.38f,
                                0.62f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(radialGradient);

        /*
         * CSS opacity: 0.9
         */
        paint.setAlpha(
                (int) (255f * opacity)
        );

        /*
         * CSS blur(45px)
         */
        paint.setMaskFilter(
                new android.graphics.BlurMaskFilter(
                        dp(blurDp),
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                )
        );

        /*
         * CSS mix-blend-mode: multiply
         */
        paint.setXfermode(blendMode);

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        paint.setXfermode(null);
        paint.setMaskFilter(null);
        paint.setShader(null);
    }

    // ============================================================
    // DARK MODE DETECTION
    // ============================================================

    private boolean isDarkMode() {

        int uiMode =
                getResources()
                        .getConfiguration()
                        .uiMode;

        return
                (uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;
    }

    // ============================================================
    // DP CONVERSION
    // ============================================================

    private float dp(float value) {

        return value *
                getResources()
                        .getDisplayMetrics()
                        .density;
    }
}