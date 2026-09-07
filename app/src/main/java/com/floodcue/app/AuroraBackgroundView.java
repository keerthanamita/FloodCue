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
         * Software rendering is intentionally used because
         * BlurMaskFilter + PorterDuff blending are required.
         *
         * Works with minSdk 24.
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
            drawDarkAurora(canvas, width, height);
        } else {
            drawLightAurora(canvas, width, height);
        }
    }

    // ============================================================
    // DARK THEME
    // ============================================================

    private void drawDarkAurora(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Deep navy/black foundation.
         *
         * Slightly cooler than the original #100e0b because
         * FloodCue's blue/cyan/violet palette looks better
         * against a cool emergency-app background.
         */
        canvas.drawColor(Color.rgb(8, 12, 27));

        /*
         * --------------------------------------------------------
         * 1. MAIN AURORA FIELD
         * --------------------------------------------------------
         *
         * Broad conic field creates the overall atmospheric
         * colour movement.
         */
        drawConicAurora(
                canvas,
                width,
                height,
                screenMode,
                0.42f,
                120f
        );

        /*
         * --------------------------------------------------------
         * 2. LEFT VIOLET CLOUD
         * --------------------------------------------------------
         */
        drawRadialGlow(
                canvas,
                width * 0.12f,
                height * 0.43f,
                width * 0.72f,
                Color.rgb(124, 58, 237),
                0.62f,
                105f,
                screenMode
        );

        /*
         * --------------------------------------------------------
         * 3. RIGHT BLUE/CYAN CLOUD
         * --------------------------------------------------------
         */
        drawRadialGlow(
                canvas,
                width * 0.90f,
                height * 0.31f,
                width * 0.67f,
                Color.rgb(37, 99, 235),
                0.58f,
                115f,
                screenMode
        );

        /*
         * --------------------------------------------------------
         * 4. LOWER VIOLET AURA
         * --------------------------------------------------------
         */
        drawRadialGlow(
                canvas,
                width * 0.58f,
                height * 0.92f,
                width * 0.70f,
                Color.rgb(168, 85, 247),
                0.40f,
                90f,
                screenMode
        );

        /*
         * --------------------------------------------------------
         * 5. CYAN HIGHLIGHT
         * --------------------------------------------------------
         *
         * Small enough to create depth without becoming a
         * distracting bright spot behind the login form.
         */
        drawRadialGlow(
                canvas,
                width * 0.24f,
                height * 0.16f,
                width * 0.34f,
                Color.rgb(34, 211, 238),
                0.30f,
                62f,
                screenMode
        );

        /*
         * --------------------------------------------------------
         * 6. SUBTLE MAGENTA EDGE
         * --------------------------------------------------------
         */
        drawRadialGlow(
                canvas,
                width * 0.94f,
                height * 0.78f,
                width * 0.42f,
                Color.rgb(147, 51, 234),
                0.25f,
                72f,
                screenMode
        );

        /*
         * --------------------------------------------------------
         * 7. SOFT AURORA ARC
         * --------------------------------------------------------
         *
         * This is intentionally subtle.
         * It gives the background some "drama" without looking
         * like a decorative neon ring.
         */
        drawAuroraArc(
                canvas,
                width,
                height,
                true
        );

        /*
         * --------------------------------------------------------
         * 8. CENTRAL DEPTH
         * --------------------------------------------------------
         *
         * Darkens the middle slightly.
         *
         * This is important because your FloodCue login content
         * will occupy the center.
         */
        drawCenterDepth(
                canvas,
                width,
                height,
                0.54f,
                55f
        );

        /*
         * --------------------------------------------------------
         * 9. VERY SOFT EDGE VIGNETTE
         * --------------------------------------------------------
         */
        drawVignette(
                canvas,
                width,
                height,
                true
        );
    }

    // ============================================================
    // LIGHT THEME
    // ============================================================

    private void drawLightAurora(
            Canvas canvas,
            int width,
            int height) {

        /*
         * Clean cool-white foundation.
         */
        canvas.drawColor(Color.rgb(246, 248, 253));

        /*
         * Main coloured field.
         *
         * Multiply is used instead of SCREEN because SCREEN
         * becomes too washed out on light backgrounds.
         */
        drawConicAurora(
                canvas,
                width,
                height,
                multiplyMode,
                0.25f,
                120f
        );

        /*
         * LEFT VIOLET
         */
        drawRadialGlow(
                canvas,
                width * 0.10f,
                height * 0.38f,
                width * 0.72f,
                Color.rgb(124, 58, 237),
                0.23f,
                105f,
                multiplyMode
        );

        /*
         * RIGHT BLUE
         */
        drawRadialGlow(
                canvas,
                width * 0.91f,
                height * 0.29f,
                width * 0.67f,
                Color.rgb(37, 99, 235),
                0.20f,
                115f,
                multiplyMode
        );

        /*
         * LOWER VIOLET
         */
        drawRadialGlow(
                canvas,
                width * 0.60f,
                height * 0.93f,
                width * 0.70f,
                Color.rgb(168, 85, 247),
                0.14f,
                90f,
                multiplyMode
        );

        /*
         * CYAN HIGHLIGHT
         */
        drawRadialGlow(
                canvas,
                width * 0.22f,
                height * 0.15f,
                width * 0.34f,
                Color.rgb(6, 182, 212),
                0.13f,
                62f,
                multiplyMode
        );

        /*
         * MAGENTA EDGE
         */
        drawRadialGlow(
                canvas,
                width * 0.95f,
                height * 0.78f,
                width * 0.42f,
                Color.rgb(147, 51, 234),
                0.11f,
                72f,
                multiplyMode
        );

        /*
         * Light version of the subtle arc.
         */
        drawAuroraArc(
                canvas,
                width,
                height,
                false
        );

        /*
         * Slight central neutralization.
         *
         * Keeps the login fields readable against the colourful
         * surroundings.
         */
        drawLightCenterDepth(
                canvas,
                width,
                height
        );
    }

    // ============================================================
    // CONIC AURORA
    // ============================================================

    private void drawConicAurora(
            Canvas canvas,
            int width,
            int height,
            PorterDuffXfermode blendMode,
            float opacity,
            float blurDp) {

        float centerX = width * 0.50f;
        float centerY = height * 0.50f;

        SweepGradient gradient =
                new SweepGradient(
                        centerX,
                        centerY,

                        new int[]{

                                Color.rgb(6, 182, 212),

                                Color.rgb(37, 99, 235),

                                Color.rgb(79, 70, 229),

                                Color.rgb(124, 58, 237),

                                Color.rgb(168, 85, 247),

                                Color.rgb(34, 211, 238),

                                Color.rgb(6, 182, 212)
                        },

                        new float[]{

                                0.00f,
                                0.16f,
                                0.32f,
                                0.50f,
                                0.68f,
                                0.84f,
                                1.00f
                        }
                );

        canvas.save();

        /*
         * Rotate the colour field.
         */
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
                -dp(80),
                -dp(80),
                width + dp(80),
                height + dp(80),
                paint
        );

        clearPaint();

        canvas.restore();
    }

    // ============================================================
    // RADIAL ATMOSPHERIC GLOW
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

        int alpha = (int) (255f * opacity);

        int innerColor = Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );

        int middleAlpha = (int) (alpha * 0.48f);

        int middleColor = Color.argb(
                middleAlpha,
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
                                0.38f,
                                1.0f
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
    // SUBTLE AURORA ARC
    // ============================================================

    private void drawAuroraArc(
            Canvas canvas,
            int width,
            int height,
            boolean dark) {

        float centerX = width * 0.50f;

        /*
         * The arc is created using a very large radial gradient
         * positioned partly outside the screen.
         *
         * This creates the impression of a glowing atmospheric
         * curve rather than a visible circle.
         */
        float centerY = height * 0.08f;

        float radius =
                Math.max(width, height) * 0.92f;

        int cyanAlpha = dark ? 26 : 18;
        int violetAlpha = dark ? 22 : 14;

        RadialGradient arcGradient =
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
                                0.67f,
                                0.77f,
                                0.80f,
                                0.84f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setShader(arcGradient);

        paint.setMaskFilter(
                new BlurMaskFilter(
                        dp(dark ? 24f : 18f),
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        paint.setXfermode(
                dark ? screenMode : multiplyMode
        );

        paint.setAlpha(
                dark ? 220 : 180
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

        float centerX = width * 0.50f;
        float centerY = height * 0.51f;

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
    // LIGHT CENTER DEPTH
    // ============================================================

    private void drawLightCenterDepth(
            Canvas canvas,
            int width,
            int height) {

        float centerX = width * 0.50f;
        float centerY = height * 0.51f;

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
    // EDGE VIGNETTE
    // ============================================================

    private void drawVignette(
            Canvas canvas,
            int width,
            int height,
            boolean dark) {

        float centerX = width * 0.50f;
        float centerY = height * 0.50f;

        float radius =
                Math.max(width, height) * 0.78f;

        int alpha = dark ? 80 : 24;

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
                                0.62f,
                                1.00f
                        },

                        Shader.TileMode.CLAMP
                );

        paint.reset();
        paint.setAntiAlias(true);
        paint.setShader(gradient);

        paint.setXfermode(
                dark ? multiplyMode : multiplyMode
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
    // DP
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