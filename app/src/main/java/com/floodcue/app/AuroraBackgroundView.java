package com.floodcue.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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

    public AuroraBackgroundView(Context context, AttributeSet attrs) {
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
         * Required for BlurMaskFilter and PorterDuff blending.
         */
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setWillNotDraw(false);

        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (isDarkMode()) {
            drawDarkMode(canvas, width, height);
        } else {
            drawLightMode(canvas, width, height);
        }
    }

    // ============================================================
    // DARK MODE
    // ============================================================

    private void drawDarkMode(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Deep navy foundation.
         */
        canvas.drawColor(
                Color.rgb(8, 12, 27)
        );

        /*
         * MAIN CYAN ATMOSPHERE
         */
        drawGlow(
                canvas,
                width * 0.12f,
                height * 0.18f,
                width * 0.70f,
                Color.rgb(6, 182, 212),
                0.30f,
                90f,
                screenMode
        );

        /*
         * LARGE LEFT VIOLET
         */
        drawGlow(
                canvas,
                width * 0.05f,
                height * 0.48f,
                width * 0.75f,
                Color.rgb(124, 58, 237),
                0.48f,
                110f,
                screenMode
        );

        /*
         * LARGE RIGHT BLUE
         */
        drawGlow(
                canvas,
                width * 0.94f,
                height * 0.32f,
                width * 0.70f,
                Color.rgb(37, 99, 235),
                0.46f,
                115f,
                screenMode
        );

        /*
         * LOWER VIOLET
         */
        drawGlow(
                canvas,
                width * 0.58f,
                height * 0.95f,
                width * 0.72f,
                Color.rgb(168, 85, 247),
                0.34f,
                95f,
                screenMode
        );

        /*
         * SMALL CYAN HIGHLIGHT
         */
        drawGlow(
                canvas,
                width * 0.25f,
                height * 0.05f,
                width * 0.38f,
                Color.rgb(34, 211, 238),
                0.25f,
                55f,
                screenMode
        );

        /*
         * LOWER RIGHT MAGENTA.
         */
        drawGlow(
                canvas,
                width * 0.96f,
                height * 0.80f,
                width * 0.42f,
                Color.rgb(147, 51, 234),
                0.22f,
                70f,
                screenMode
        );

        /*
         * SUBTLE BLUE SECONDARY CLOUD.
         */
        drawGlow(
                canvas,
                width * 0.68f,
                height * 0.08f,
                width * 0.30f,
                Color.rgb(56, 189, 248),
                0.15f,
                55f,
                screenMode
        );

        /*
         * SUBTLE LEFT LOWER HAZE.
         */
        drawGlow(
                canvas,
                width * 0.02f,
                height * 0.82f,
                width * 0.32f,
                Color.rgb(99, 102, 241),
                0.14f,
                65f,
                screenMode
        );

        /*
         * ATMOSPHERIC ARC.
         */
        drawAuroraArc(
                canvas,
                width,
                height,
                true
        );

        /*
         * SECOND ARC.
         */
        drawSecondaryArc(
                canvas,
                width,
                height,
                true
        );

        /*
         * QUIETER CENTER.
         */
        drawCenterDepth(
                canvas,
                width,
                height
        );

        /*
         * EDGE VIGNETTE.
         */
        drawVignette(
                canvas,
                width,
                height,
                true
        );
    }

    // ============================================================
    // LIGHT MODE - ARCTIC FROST
    // ============================================================

    private void drawLightMode(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Arctic Frost base.
         *
         * This intentionally replaces the dark background with
         * the warm-cool off-white used by the Arctic Frost design.
         */
        canvas.drawColor(
                Color.rgb(250, 248, 242)
        );

        /*
         * --------------------------------------------------------
         * 1. MAIN FROST FIELD
         * --------------------------------------------------------
         *
         * Soft cyan wash across the upper area.
         */
        drawGlow(
                canvas,
                width * 0.18f,
                height * 0.08f,
                width * 0.82f,
                Color.rgb(207, 250, 254),
                0.52f,
                55f,
                null
        );

        /*
         * --------------------------------------------------------
         * 2. LEFT CYAN
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 0.04f,
                height * 0.30f,
                width * 0.70f,
                Color.rgb(6, 182, 212),
                0.22f,
                115f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 3. RIGHT SKY BLUE
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 0.96f,
                height * 0.25f,
                width * 0.68f,
                Color.rgb(14, 165, 233),
                0.20f,
                125f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 4. LOWER BLUE
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 0.76f,
                height * 0.92f,
                width * 0.72f,
                Color.rgb(56, 189, 248),
                0.17f,
                125f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 5. SOFT VIOLET
         * --------------------------------------------------------
         *
         * Adds just enough colour variation so the light theme
         * doesn't become a plain cyan background.
         */
        drawGlow(
                canvas,
                width * 0.10f,
                height * 0.78f,
                width * 0.50f,
                Color.rgb(129, 140, 248),
                0.12f,
                110f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 6. UPPER RIGHT FROST
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 0.78f,
                height * 0.05f,
                width * 0.40f,
                Color.rgb(165, 243, 252),
                0.25f,
                70f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 7. LOWER CYAN HAZE
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 0.28f,
                height * 1.02f,
                width * 0.52f,
                Color.rgb(34, 211, 238),
                0.11f,
                105f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 8. SOFT VIOLET EDGE
         * --------------------------------------------------------
         */
        drawGlow(
                canvas,
                width * 1.00f,
                height * 0.72f,
                width * 0.38f,
                Color.rgb(167, 139, 250),
                0.10f,
                85f,
                multiplyMode
        );

        /*
         * --------------------------------------------------------
         * 9. MAIN ARCTIC ARC
         * --------------------------------------------------------
         */
        drawAuroraArc(
                canvas,
                width,
                height,
                false
        );

        /*
         * --------------------------------------------------------
         * 10. SECONDARY ARC
         * --------------------------------------------------------
         */
        drawSecondaryArc(
                canvas,
                width,
                height,
                false
        );

        /*
         * --------------------------------------------------------
         * 11. SOFT CENTER LIGHT
         * --------------------------------------------------------
         *
         * Keeps the content area clean and glass-like.
         */
        drawLightCenterGlow(
                canvas,
                width,
                height
        );

        /*
         * --------------------------------------------------------
         * 12. VERY SOFT EDGE DARKENING
         * --------------------------------------------------------
         */
        drawVignette(
                canvas,
                width,
                height,
                false
        );
    }

    // ============================================================
    // RADIAL GLOW
    // ============================================================

    private void drawGlow(
            Canvas canvas,
            float centerX,
            float centerY,
            float radius,
            int color,
            float opacity,
            float blurDp,
            PorterDuffXfermode blendMode) {

        int alpha =
                (int) (255f * opacity);

        int innerColor =
                Color.argb(
                        alpha,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                );

        int middleColor =
                Color.argb(
                        (int) (alpha * 0.45f),
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                );

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{
                                innerColor,
                                middleColor,
                                Color.TRANSPARENT
                        },

                        new float[]{
                                0.0f,
                                0.40f,
                                1.0f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(gradient);

        if (blurDp > 0) {

            paint.setMaskFilter(
                    new BlurMaskFilter(
                            dp(blurDp),
                            BlurMaskFilter.Blur.NORMAL
                    )
            );
        }

        if (blendMode != null) {
            paint.setXfermode(blendMode);
        }

        canvas.drawRect(
                -dp(80),
                -dp(80),
                getWidth() + dp(80),
                getHeight() + dp(80),
                paint
        );

        clearPaint();
    }

    // ============================================================
    // MAIN AURORA ARC
    // ============================================================

    private void drawAuroraArc(
            Canvas canvas,
            int width,
            int height,
            boolean dark) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.04f;

        float radius =
                Math.max(width, height) * 0.95f;

        int cyanAlpha =
                dark ? 28 : 20;

        int violetAlpha =
                dark ? 22 : 15;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.TRANSPARENT,
                                Color.TRANSPARENT,

                                Color.argb(
                                        cyanAlpha,
                                        34,
                                        211,
                                        238
                                ),

                                Color.argb(
                                        violetAlpha,
                                        124,
                                        58,
                                        237
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.68f,
                                0.76f,
                                0.80f,
                                0.85f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(dark ? 25f : 20f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark
                        ? screenMode
                        : multiplyMode
        );

        paint.setAlpha(
                dark ? 220 : 180
        );

        canvas.drawRect(
                -dp(50),
                -dp(50),
                width + dp(50),
                height + dp(50),
                paint
        );

        clearPaint();
    }

    // ============================================================
    // SECONDARY ARC
    // ============================================================

    private void drawSecondaryArc(
            Canvas canvas,
            int width,
            int height,
            boolean dark) {

        float centerX =
                width * 0.48f;

        float centerY =
                height * 0.31f;

        float radius =
                Math.max(width, height) * 0.73f;

        int blueAlpha =
                dark ? 15 : 9;

        int violetAlpha =
                dark ? 12 : 7;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.TRANSPARENT,
                                Color.TRANSPARENT,

                                Color.argb(
                                        blueAlpha,
                                        96,
                                        165,
                                        250
                                ),

                                Color.argb(
                                        violetAlpha,
                                        168,
                                        85,
                                        247
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.70f,
                                0.76f,
                                0.80f,
                                0.85f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(dark ? 18f : 16f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark
                        ? screenMode
                        : multiplyMode
        );

        paint.setAlpha(
                dark ? 210 : 160
        );

        canvas.drawRect(
                -dp(50),
                -dp(50),
                width + dp(50),
                height + dp(50),
                paint
        );

        clearPaint();
    }

    // ============================================================
    // DARK CENTER DEPTH
    // ============================================================

    private void drawCenterDepth(
            Canvas canvas,
            int width,
            int height) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.52f;

        float radius =
                Math.min(width, height) * 0.62f;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.argb(
                                        65,
                                        3,
                                        7,
                                        18
                                ),

                                Color.argb(
                                        35,
                                        5,
                                        9,
                                        23
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.46f,
                                0.80f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(55f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(multiplyMode);

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        clearPaint();
    }

    // ============================================================
    // LIGHT CENTER GLOW
    // ============================================================

    private void drawLightCenterGlow(
            Canvas canvas,
            int width,
            int height) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.50f;

        float radius =
                Math.min(width, height) * 0.62f;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.argb(
                                        55,
                                        255,
                                        255,
                                        255
                                ),

                                Color.argb(
                                        25,
                                        255,
                                        255,
                                        255
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.42f,
                                0.78f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(45f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        /*
         * Normal blending is intentionally used here.
         * It creates a soft glass-like clear region.
         */
        paint.setXfermode(null);

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        clearPaint();
    }

    // ============================================================
    // VIGNETTE
    // ============================================================

    private void drawVignette(
            Canvas canvas,
            int width,
            int height,
            boolean dark) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.50f;

        float radius =
                Math.max(width, height) * 0.82f;

        int alpha =
                dark ? 65 : 18;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.TRANSPARENT,
                                Color.TRANSPARENT,

                                Color.argb(
                                        alpha,
                                        0,
                                        0,
                                        0
                                )
                        },

                        new float[]{

                                0.00f,
                                0.60f,
                                1.00f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setXfermode(
                multiplyMode
        );

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        clearPaint();
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
                (uiMode &
                        Configuration.UI_MODE_NIGHT_MASK)
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

    // ============================================================
    // PAINT CLEANUP
    // ============================================================

    private void clearPaint() {

        paint.setXfermode(null);
        paint.setShader(null);
        paint.setMaskFilter(null);
        paint.setAlpha(255);
    }
}