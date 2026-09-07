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
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class AuroraBackgroundView extends View {

    private final Paint paint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final PorterDuffXfermode screenMode =
            new PorterDuffXfermode(
                    PorterDuff.Mode.SCREEN
            );

    private final PorterDuffXfermode multiplyMode =
            new PorterDuffXfermode(
                    PorterDuff.Mode.MULTIPLY
            );

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
         * Required for BlurMaskFilter and PorterDuff
         * blending.
         */
        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );

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
            drawDarkAurora(canvas, width, height);
        } else {
            drawLightAurora(canvas, width, height);
        }
    }

    // ============================================================
    // DARK AURORA
    // ============================================================

    private void drawDarkAurora(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Deep FloodCue background.
         */
        canvas.drawColor(
                Color.rgb(6, 10, 25)
        );

        // --------------------------------------------------------
        // BASE COLOUR FIELD
        // --------------------------------------------------------

        drawConicAurora(
                canvas,
                width,
                height,
                screenMode,
                0.39f,
                120f
        );

        // --------------------------------------------------------
        // LARGE ATMOSPHERIC CLOUDS
        // --------------------------------------------------------

        // Violet left
        drawRadialGlow(
                canvas,
                width * 0.07f,
                height * 0.43f,
                width * 0.72f,
                Color.rgb(124, 58, 237),
                0.56f,
                108f,
                screenMode
        );

        // Electric blue right
        drawRadialGlow(
                canvas,
                width * 0.93f,
                height * 0.29f,
                width * 0.69f,
                Color.rgb(37, 99, 235),
                0.53f,
                116f,
                screenMode
        );

        // Purple bottom
        drawRadialGlow(
                canvas,
                width * 0.57f,
                height * 0.96f,
                width * 0.70f,
                Color.rgb(168, 85, 247),
                0.36f,
                94f,
                screenMode
        );

        // Cyan upper-left
        drawRadialGlow(
                canvas,
                width * 0.18f,
                height * 0.08f,
                width * 0.37f,
                Color.rgb(34, 211, 238),
                0.27f,
                64f,
                screenMode
        );

        // Magenta lower-right
        drawRadialGlow(
                canvas,
                width * 0.97f,
                height * 0.83f,
                width * 0.44f,
                Color.rgb(147, 51, 234),
                0.23f,
                76f,
                screenMode
        );

        // --------------------------------------------------------
        // SECONDARY COLOUR LAYERS
        // --------------------------------------------------------

        /*
         * Cyan-blue transition.
         */
        drawRadialGlow(
                canvas,
                width * 0.66f,
                height * 0.06f,
                width * 0.30f,
                Color.rgb(56, 189, 248),
                0.17f,
                58f,
                screenMode
        );

        /*
         * Indigo lower-left.
         */
        drawRadialGlow(
                canvas,
                width * 0.01f,
                height * 0.78f,
                width * 0.35f,
                Color.rgb(79, 70, 229),
                0.17f,
                67f,
                screenMode
        );

        /*
         * Cyan lower atmosphere.
         */
        drawRadialGlow(
                canvas,
                width * 0.35f,
                height * 1.04f,
                width * 0.47f,
                Color.rgb(6, 182, 212),
                0.13f,
                82f,
                screenMode
        );

        /*
         * Indigo center-top transition.
         */
        drawRadialGlow(
                canvas,
                width * 0.50f,
                height * 0.20f,
                width * 0.39f,
                Color.rgb(99, 102, 241),
                0.13f,
                74f,
                screenMode
        );

        /*
         * Blue right edge.
         */
        drawRadialGlow(
                canvas,
                width * 1.03f,
                height * 0.51f,
                width * 0.31f,
                Color.rgb(14, 165, 233),
                0.14f,
                70f,
                screenMode
        );

        /*
         * Magenta lower transition.
         */
        drawRadialGlow(
                canvas,
                width * 0.74f,
                height * 1.02f,
                width * 0.37f,
                Color.rgb(192, 38, 211),
                0.10f,
                72f,
                screenMode
        );

        // --------------------------------------------------------
        // NEW: SMALLER COLOUR POCKETS
        // --------------------------------------------------------

        /*
         * Soft violet pocket near upper-right.
         */
        drawRadialGlow(
                canvas,
                width * 0.82f,
                height * 0.17f,
                width * 0.22f,
                Color.rgb(139, 92, 246),
                0.09f,
                54f,
                screenMode
        );

        /*
         * Soft cyan pocket near middle-left.
         */
        drawRadialGlow(
                canvas,
                width * 0.15f,
                height * 0.58f,
                width * 0.25f,
                Color.rgb(34, 211, 238),
                0.085f,
                56f,
                screenMode
        );

        /*
         * Blue-violet pocket near lower-middle.
         */
        drawRadialGlow(
                canvas,
                width * 0.49f,
                height * 0.78f,
                width * 0.28f,
                Color.rgb(96, 165, 250),
                0.075f,
                58f,
                screenMode
        );

        /*
         * Small magenta atmospheric pocket.
         */
        drawRadialGlow(
                canvas,
                width * 0.84f,
                height * 0.65f,
                width * 0.25f,
                Color.rgb(217, 70, 239),
                0.065f,
                60f,
                screenMode
        );

        // --------------------------------------------------------
        // AURORA BANDS
        // --------------------------------------------------------

        drawAuroraBand(
                canvas,
                width,
                height,
                0.30f,
                Color.rgb(34, 211, 238),
                0.075f,
                42f
        );

        drawAuroraBand(
                canvas,
                width,
                height,
                0.63f,
                Color.rgb(124, 58, 237),
                0.065f,
                48f
        );

        /*
         * Very faint magenta band near bottom.
         */
        drawAuroraBand(
                canvas,
                width,
                height,
                0.87f,
                Color.rgb(192, 38, 211),
                0.045f,
                44f
        );

        // --------------------------------------------------------
        // ATMOSPHERIC ARCS
        // --------------------------------------------------------

        drawAuroraArc(
                canvas,
                width,
                height,
                true
        );

        drawSecondaryArc(
                canvas,
                width,
                height
        );

        drawColorRing(
                canvas,
                width,
                height
        );

        // --------------------------------------------------------
        // CENTER DEPTH
        // --------------------------------------------------------

        drawCenterDepth(
                canvas,
                width,
                height,
                0.50f,
                58f
        );

        // --------------------------------------------------------
        // FINAL VIGNETTE
        // --------------------------------------------------------

        drawVignette(
                canvas,
                width,
                height,
                true
        );
    }

    // ============================================================
    // LIGHT AURORA
    // ============================================================

    private void drawLightAurora(
            Canvas canvas,
            int width,
            int height) {

        canvas.drawColor(
                Color.rgb(246, 248, 253)
        );

        drawConicAurora(
                canvas,
                width,
                height,
                multiplyMode,
                0.22f,
                120f
        );

        drawRadialGlow(
                canvas,
                width * 0.07f,
                height * 0.43f,
                width * 0.72f,
                Color.rgb(124, 58, 237),
                0.20f,
                108f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.93f,
                height * 0.29f,
                width * 0.69f,
                Color.rgb(37, 99, 235),
                0.18f,
                116f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.57f,
                height * 0.96f,
                width * 0.70f,
                Color.rgb(168, 85, 247),
                0.13f,
                94f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.18f,
                height * 0.08f,
                width * 0.37f,
                Color.rgb(34, 211, 238),
                0.11f,
                64f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.97f,
                height * 0.83f,
                width * 0.44f,
                Color.rgb(147, 51, 234),
                0.09f,
                76f,
                multiplyMode
        );

        // Secondary layers

        drawRadialGlow(
                canvas,
                width * 0.66f,
                height * 0.06f,
                width * 0.30f,
                Color.rgb(56, 189, 248),
                0.065f,
                58f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.01f,
                height * 0.78f,
                width * 0.35f,
                Color.rgb(79, 70, 229),
                0.06f,
                67f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.35f,
                height * 1.04f,
                width * 0.47f,
                Color.rgb(6, 182, 212),
                0.05f,
                82f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.50f,
                height * 0.20f,
                width * 0.39f,
                Color.rgb(99, 102, 241),
                0.05f,
                74f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 1.03f,
                height * 0.51f,
                width * 0.31f,
                Color.rgb(14, 165, 233),
                0.05f,
                70f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.74f,
                height * 1.02f,
                width * 0.37f,
                Color.rgb(192, 38, 211),
                0.04f,
                72f,
                multiplyMode
        );

        // Smaller pockets

        drawRadialGlow(
                canvas,
                width * 0.82f,
                height * 0.17f,
                width * 0.22f,
                Color.rgb(139, 92, 246),
                0.035f,
                54f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.15f,
                height * 0.58f,
                width * 0.25f,
                Color.rgb(34, 211, 238),
                0.032f,
                56f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.49f,
                height * 0.78f,
                width * 0.28f,
                Color.rgb(96, 165, 250),
                0.028f,
                58f,
                multiplyMode
        );

        drawRadialGlow(
                canvas,
                width * 0.84f,
                height * 0.65f,
                width * 0.25f,
                Color.rgb(217, 70, 239),
                0.025f,
                60f,
                multiplyMode
        );

        // Bands

        drawAuroraBand(
                canvas,
                width,
                height,
                0.30f,
                Color.rgb(34, 211, 238),
                0.025f,
                42f
        );

        drawAuroraBand(
                canvas,
                width,
                height,
                0.63f,
                Color.rgb(124, 58, 237),
                0.022f,
                48f
        );

        drawAuroraBand(
                canvas,
                width,
                height,
                0.87f,
                Color.rgb(192, 38, 211),
                0.015f,
                44f
        );

        drawAuroraArc(
                canvas,
                width,
                height,
                false
        );

        drawSecondaryArc(
                canvas,
                width,
                height
        );

        drawColorRing(
                canvas,
                width,
                height
        );

        drawLightCenterDepth(
                canvas,
                width,
                height
        );
    }

    // ============================================================
    // MAIN CONIC FIELD
    // ============================================================

    private void drawConicAurora(
            Canvas canvas,
            int width,
            int height,
            PorterDuffXfermode blendMode,
            float opacity,
            float blurDp) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.50f;

        SweepGradient gradient =
                new SweepGradient(
                        centerX,
                        centerY,

                        new int[]{

                                Color.rgb(6, 182, 212),
                                Color.rgb(34, 211, 238),
                                Color.rgb(37, 99, 235),
                                Color.rgb(79, 70, 229),
                                Color.rgb(124, 58, 237),
                                Color.rgb(168, 85, 247),
                                Color.rgb(192, 38, 211),
                                Color.rgb(34, 211, 238),
                                Color.rgb(6, 182, 212)
                        },

                        new float[]{

                                0.00f,
                                0.10f,
                                0.22f,
                                0.34f,
                                0.48f,
                                0.62f,
                                0.74f,
                                0.87f,
                                1.00f
                        }
                );

        canvas.save();

        canvas.rotate(
                160f,
                centerX,
                centerY
        );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(gradient);

        paint.setAlpha(
                (int) (255f * opacity)
        );

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(blurDp),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(blendMode);

        canvas.drawRect(
                -dp(120),
                -dp(120),
                width + dp(120),
                height + dp(120),
                paint
        );

        clearPaint();

        canvas.restore();
    }

    // ============================================================
    // RADIAL GLOW
    // ============================================================

    private void drawRadialGlow(
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

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        int inner =
                Color.argb(
                        alpha,
                        red,
                        green,
                        blue
                );

        int middle =
                Color.argb(
                        (int) (alpha * 0.46f),
                        red,
                        green,
                        blue
                );

        int outer =
                Color.argb(
                        (int) (alpha * 0.12f),
                        red,
                        green,
                        blue
                );

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                inner,
                                middle,
                                outer,
                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.32f,
                                0.68f,
                                1.00f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(blurDp),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(blendMode);

        canvas.drawRect(
                0,
                0,
                getWidth(),
                getHeight(),
                paint
        );

        clearPaint();
    }

    // ============================================================
    // AURORA BAND
    // ============================================================

    private void drawAuroraBand(
            Canvas canvas,
            int width,
            int height,
            float yPosition,
            int color,
            float opacity,
            float blurDp) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * yPosition;

        float radius =
                width * 0.75f;

        int alpha =
                (int) (255f * opacity);

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.argb(
                                        alpha,
                                        Color.red(color),
                                        Color.green(color),
                                        Color.blue(color)
                                ),

                                Color.argb(
                                        (int) (alpha * 0.40f),
                                        Color.red(color),
                                        Color.green(color),
                                        Color.blue(color)
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.42f,
                                1.00f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(blurDp),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                isDarkMode()
                        ? screenMode
                        : multiplyMode
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
                height * 0.08f;

        float radius =
                Math.max(width, height) * 0.92f;

        int cyanAlpha =
                dark ? 29 : 17;

        int violetAlpha =
                dark ? 24 : 14;

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
                                0.66f,
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
                        dp(dark ? 25f : 18f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark
                        ? screenMode
                        : multiplyMode
        );

        paint.setAlpha(
                dark ? 215 : 160
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
    // SECONDARY ARC
    // ============================================================

    private void drawSecondaryArc(
            Canvas canvas,
            int width,
            int height) {

        float centerX =
                width * 0.47f;

        float centerY =
                height * 0.30f;

        float radius =
                Math.max(width, height) * 0.72f;

        boolean dark =
                isDarkMode();

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.TRANSPARENT,
                                Color.TRANSPARENT,

                                Color.argb(
                                        dark ? 15 : 8,
                                        96,
                                        165,
                                        250
                                ),

                                Color.argb(
                                        dark ? 12 : 7,
                                        168,
                                        85,
                                        247
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.69f,
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
                        dp(19f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark
                        ? screenMode
                        : multiplyMode
        );

        paint.setAlpha(
                dark ? 205 : 145
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
    // SUBTLE COLOR RING
    // ============================================================

    private void drawColorRing(
            Canvas canvas,
            int width,
            int height) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.54f;

        float radius =
                Math.min(width, height) * 0.67f;

        boolean dark =
                isDarkMode();

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.TRANSPARENT,
                                Color.TRANSPARENT,

                                Color.argb(
                                        dark ? 11 : 5,
                                        34,
                                        211,
                                        238
                                ),

                                Color.argb(
                                        dark ? 8 : 4,
                                        168,
                                        85,
                                        247
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.73f,
                                0.79f,
                                0.82f,
                                0.86f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(dark ? 14f : 10f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark
                        ? screenMode
                        : multiplyMode
        );

        paint.setAlpha(
                dark ? 190 : 120
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
    // DARK CENTER DEPTH
    // ============================================================

    private void drawCenterDepth(
            Canvas canvas,
            int width,
            int height,
            float opacity,
            float blurDp) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.51f;

        float radius =
                Math.min(width, height) * 0.60f;

        int alpha =
                (int) (255f * opacity);

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.argb(
                                        (int) (alpha * 0.54f),
                                        3,
                                        7,
                                        18
                                ),

                                Color.argb(
                                        (int) (alpha * 0.30f),
                                        5,
                                        9,
                                        23
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.45f,
                                0.78f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(blurDp),
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
    // LIGHT CENTER
    // ============================================================

    private void drawLightCenterDepth(
            Canvas canvas,
            int width,
            int height) {

        float centerX =
                width * 0.50f;

        float centerY =
                height * 0.51f;

        float radius =
                Math.min(width, height) * 0.58f;

        RadialGradient gradient =
                new RadialGradient(
                        centerX,
                        centerY,
                        radius,

                        new int[]{

                                Color.argb(
                                        18,
                                        255,
                                        255,
                                        255
                                ),

                                Color.argb(
                                        10,
                                        255,
                                        255,
                                        255
                                ),

                                Color.TRANSPARENT
                        },

                        new float[]{

                                0.00f,
                                0.48f,
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
                Math.max(width, height) * 0.80f;

        int alpha =
                dark ? 72 : 20;

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
                                0.61f,
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
    // DARK MODE
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
    // DP
    // ============================================================

    private float dp(float value) {

        return value *
                getResources()
                        .getDisplayMetrics()
                        .density;
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    private void clearPaint() {

        paint.setXfermode(null);
        paint.setShader(null);
        paint.setMaskFilter(null);
        paint.setAlpha(255);
    }
}