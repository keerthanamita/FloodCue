package com.floodcue.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AuroraBackgroundView extends View {

    // ============================================================
    // PAINTS
    // ============================================================

    private final Paint backgroundPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint auroraPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint glowPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint softGlowPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    // ============================================================
    // PATHS
    // ============================================================

    private final Path topAurora = new Path();
    private final Path topAurora2 = new Path();
    private final Path bottomAurora = new Path();
    private final Path bottomAurora2 = new Path();

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public AuroraBackgroundView(Context context) {
        super(context);
        init();
    }

    public AuroraBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraBackgroundView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init();
    }

    // ============================================================
    // INITIALIZATION
    // ============================================================

    private void init() {

        /*
         * BlurMaskFilter requires software rendering.
         *
         * The rest of the background remains lightweight because
         * we are drawing simple paths and gradients.
         */
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        backgroundPaint.setAntiAlias(true);

        auroraPaint.setAntiAlias(true);
        glowPaint.setAntiAlias(true);
        softGlowPaint.setAntiAlias(true);

        auroraPaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);
        softGlowPaint.setStyle(Paint.Style.FILL);

        setWillNotDraw(false);
    }

    // ============================================================
    // DRAW
    // ============================================================

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        boolean darkMode =
                (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        // --------------------------------------------------------
        // 1. BASE BACKGROUND
        // --------------------------------------------------------

        drawBackground(canvas, width, height, darkMode);

        // --------------------------------------------------------
        // 2. TOP AURORA
        // --------------------------------------------------------

        drawTopAurora(canvas, width, height, darkMode);

        // --------------------------------------------------------
        // 3. BOTTOM AURORA
        // --------------------------------------------------------

        drawBottomAurora(canvas, width, height, darkMode);
    }

    // ============================================================
    // BACKGROUND
    // ============================================================

    private void drawBackground(
            Canvas canvas,
            float width,
            float height,
            boolean darkMode) {

        backgroundPaint.setShader(null);

        if (darkMode) {

            /*
             * Deep navy background.
             *
             * The center is intentionally almost uniform.
             * This keeps the login fields and text readable.
             */
            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            new int[]{
                                    0xFF07162F,
                                    0xFF06152D,
                                    0xFF06152F,
                                    0xFF07142C
                            },
                            new float[]{
                                    0.0f,
                                    0.30f,
                                    0.70f,
                                    1.0f
                            },
                            Shader.TileMode.CLAMP
                    );

            backgroundPaint.setShader(gradient);

        } else {

            /*
             * Very clean near-white background.
             *
             * A tiny amount of blue is retained so the light mode
             * doesn't look like plain #FFFFFF.
             */
            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            new int[]{
                                    0xFFF9FCFF,
                                    0xFFF6FAFF,
                                    0xFFF9FCFF,
                                    0xFFF8FBFF
                            },
                            new float[]{
                                    0.0f,
                                    0.35f,
                                    0.70f,
                                    1.0f
                            },
                            Shader.TileMode.CLAMP
                    );

            backgroundPaint.setShader(gradient);
        }

        canvas.drawRect(
                0,
                0,
                width,
                height,
                backgroundPaint
        );

        backgroundPaint.setShader(null);
    }

    // ============================================================
    // TOP AURORA
    // ============================================================

    private void drawTopAurora(
            Canvas canvas,
            float width,
            float height,
            boolean darkMode) {

        /*
         * --------------------------------------------------------
         * MAIN TOP CURVE
         * --------------------------------------------------------
         *
         * This is deliberately kept near the top edge.
         *
         * The center of the screen remains clear.
         */

        topAurora.reset();

        topAurora.moveTo(
                -width * 0.12f,
                -height * 0.02f
        );

        topAurora.cubicTo(
                width * 0.06f,
                height * 0.07f,
                width * 0.15f,
                height * 0.13f,
                width * 0.30f,
                height * 0.11f
        );

        topAurora.cubicTo(
                width * 0.48f,
                height * 0.09f,
                width * 0.53f,
                height * 0.015f,
                width * 0.68f,
                height * 0.035f
        );

        topAurora.cubicTo(
                width * 0.84f,
                height * 0.055f,
                width * 0.95f,
                height * 0.13f,
                width * 1.12f,
                height * 0.08f
        );

        topAurora.lineTo(
                width * 1.12f,
                -height * 0.12f
        );

        topAurora.lineTo(
                -width * 0.12f,
                -height * 0.12f
        );

        topAurora.close();

        // --------------------------------------------------------
        // SECOND TOP CURVE
        // --------------------------------------------------------

        topAurora2.reset();

        topAurora2.moveTo(
                -width * 0.12f,
                height * 0.075f
        );

        topAurora2.cubicTo(
                width * 0.08f,
                height * 0.15f,
                width * 0.20f,
                height * 0.18f,
                width * 0.36f,
                height * 0.145f
        );

        topAurora2.cubicTo(
                width * 0.53f,
                height * 0.11f,
                width * 0.62f,
                height * 0.025f,
                width * 0.78f,
                height * 0.075f
        );

        topAurora2.cubicTo(
                width * 0.92f,
                height * 0.12f,
                width * 1.02f,
                height * 0.19f,
                width * 1.12f,
                height * 0.15f
        );

        topAurora2.lineTo(
                width * 1.12f,
                height * 0.03f
        );

        topAurora2.cubicTo(
                width * 0.98f,
                height * 0.08f,
                width * 0.88f,
                height * 0.015f,
                width * 0.76f,
                0
        );

        topAurora2.cubicTo(
                width * 0.60f,
                -height * 0.025f,
                width * 0.50f,
                height * 0.065f,
                width * 0.35f,
                height * 0.085f
        );

        topAurora2.cubicTo(
                width * 0.18f,
                height * 0.105f,
                width * 0.08f,
                height * 0.035f,
                -width * 0.12f,
                0
        );

        topAurora2.close();

        if (darkMode) {

            drawDarkTopAurora(canvas, width, height);

        } else {

            drawLightTopAurora(canvas, width, height);
        }
    }

    // ============================================================
    // DARK TOP AURORA
    // ============================================================

    private void drawDarkTopAurora(
            Canvas canvas,
            float width,
            float height) {

        // --------------------------------------------------------
        // OUTER BLUR
        // --------------------------------------------------------

        glowPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        height * 0.22f,
                        new int[]{
                                0xB34A16FF,
                                0xA52E57FF,
                                0x9A147DFF,
                                0x9900CFFF
                        },
                        new float[]{
                                0.0f,
                                0.35f,
                                0.70f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        glowPaint.setMaskFilter(
                new BlurMaskFilter(
                        35f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                topAurora,
                glowPaint
        );

        glowPaint.setMaskFilter(null);
        glowPaint.setShader(null);

        // --------------------------------------------------------
        // MAIN TOP AURORA
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        0,
                        new int[]{
                                0xFF6824FF,
                                0xFF473BFF,
                                0xFF176DFF,
                                0xFF00CFF0
                        },
                        new float[]{
                                0.0f,
                                0.32f,
                                0.68f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(125);

        canvas.drawPath(
                topAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND SOFT BAND
        // --------------------------------------------------------

        softGlowPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        height * 0.18f,
                        new int[]{
                                0x005D2BFF,
                                0x884A4DFF,
                                0x6600AFFF,
                                0x0000E5FF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        softGlowPaint.setMaskFilter(
                new BlurMaskFilter(
                        22f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                topAurora2,
                softGlowPaint
        );

        softGlowPaint.setMaskFilter(null);
        softGlowPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND BAND COLOR
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        0,
                        new int[]{
                                0x006C28FF,
                                0x664E54FF,
                                0x55009EFF,
                                0x4400D8FF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(150);

        canvas.drawPath(
                topAurora2,
                auroraPaint
        );

        auroraPaint.setShader(null);
    }

    // ============================================================
    // LIGHT TOP AURORA
    // ============================================================

    private void drawLightTopAurora(
            Canvas canvas,
            float width,
            float height) {

        // --------------------------------------------------------
        // SOFT OUTER GLOW
        // --------------------------------------------------------

        glowPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        height * 0.22f,
                        new int[]{
                                0x507B42FF,
                                0x484D82FF,
                                0x4500CFFF,
                                0x3038C8FF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        glowPaint.setMaskFilter(
                new BlurMaskFilter(
                        30f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                topAurora,
                glowPaint
        );

        glowPaint.setMaskFilter(null);
        glowPaint.setShader(null);

        // --------------------------------------------------------
        // MAIN LIGHT AURORA
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        0,
                        new int[]{
                                0x657E4CFF,
                                0x554E8CFF,
                                0x5500CFFF,
                                0x4038BDF8
                        },
                        new float[]{
                                0.0f,
                                0.35f,
                                0.70f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(130);

        canvas.drawPath(
                topAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND SOFT LIGHT BAND
        // --------------------------------------------------------

        softGlowPaint.setShader(
                new LinearGradient(
                        0,
                        0,
                        width,
                        height * 0.18f,
                        new int[]{
                                0x004E5EFF,
                                0x445D83FF,
                                0x4400CFFF,
                                0x3038CFFF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        softGlowPaint.setMaskFilter(
                new BlurMaskFilter(
                        20f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                topAurora2,
                softGlowPaint
        );

        softGlowPaint.setMaskFilter(null);
        softGlowPaint.setShader(null);
    }

    // ============================================================
    // BOTTOM AURORA
    // ============================================================

    private void drawBottomAurora(
            Canvas canvas,
            float width,
            float height,
            boolean darkMode) {

        /*
         * --------------------------------------------------------
         * MAIN BOTTOM CURVE
         * --------------------------------------------------------
         */

        bottomAurora.reset();

        bottomAurora.moveTo(
                -width * 0.12f,
                height * 1.08f
        );

        bottomAurora.cubicTo(
                width * 0.04f,
                height * 0.94f,
                width * 0.16f,
                height * 0.87f,
                width * 0.31f,
                height * 0.91f
        );

        bottomAurora.cubicTo(
                width * 0.48f,
                height * 0.95f,
                width * 0.56f,
                height * 1.04f,
                width * 0.70f,
                height * 0.98f
        );

        bottomAurora.cubicTo(
                width * 0.86f,
                height * 0.91f,
                width * 0.97f,
                height * 0.83f,
                width * 1.12f,
                height * 0.89f
        );

        bottomAurora.lineTo(
                width * 1.12f,
                height * 1.12f
        );

        bottomAurora.close();

        // --------------------------------------------------------
        // SECOND BOTTOM CURVE
        // --------------------------------------------------------

        bottomAurora2.reset();

        bottomAurora2.moveTo(
                -width * 0.12f,
                height * 1.00f
        );

        bottomAurora2.cubicTo(
                width * 0.06f,
                height * 0.91f,
                width * 0.18f,
                height * 0.82f,
                width * 0.34f,
                height * 0.87f
        );

        bottomAurora2.cubicTo(
                width * 0.50f,
                height * 0.92f,
                width * 0.57f,
                height * 1.01f,
                width * 0.72f,
                height * 0.94f
        );

        bottomAurora2.cubicTo(
                width * 0.88f,
                height * 0.87f,
                width * 0.98f,
                height * 0.78f,
                width * 1.12f,
                height * 0.84f
        );

        bottomAurora2.lineTo(
                width * 1.12f,
                height * 1.04f
        );

        bottomAurora2.close();

        if (darkMode) {

            drawDarkBottomAurora(
                    canvas,
                    width,
                    height
            );

        } else {

            drawLightBottomAurora(
                    canvas,
                    width,
                    height
            );
        }
    }

    // ============================================================
    // DARK BOTTOM AURORA
    // ============================================================

    private void drawDarkBottomAurora(
            Canvas canvas,
            float width,
            float height) {

        // --------------------------------------------------------
        // OUTER GLOW
        // --------------------------------------------------------

        glowPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.76f,
                        width,
                        height,
                        new int[]{
                                0xA85722FF,
                                0xA14354FF,
                                0x920078FF,
                                0x8800CFFF
                        },
                        new float[]{
                                0.0f,
                                0.35f,
                                0.70f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        glowPaint.setMaskFilter(
                new BlurMaskFilter(
                        40f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                bottomAurora,
                glowPaint
        );

        glowPaint.setMaskFilter(null);
        glowPaint.setShader(null);

        // --------------------------------------------------------
        // MAIN BOTTOM AURORA
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.85f,
                        width,
                        height,
                        new int[]{
                                0xFF6727FF,
                                0xFF443CFF,
                                0xFF126EFF,
                                0xFF00CFE8
                        },
                        new float[]{
                                0.0f,
                                0.35f,
                                0.70f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(125);

        canvas.drawPath(
                bottomAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND BOTTOM GLOW
        // --------------------------------------------------------

        softGlowPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.80f,
                        width,
                        height,
                        new int[]{
                                0x005F20FF,
                                0x754B4DFF,
                                0x6600AFFF,
                                0x5500DFFF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        softGlowPaint.setMaskFilter(
                new BlurMaskFilter(
                        24f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                bottomAurora2,
                softGlowPaint
        );

        softGlowPaint.setMaskFilter(null);
        softGlowPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND BOTTOM BAND
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.82f,
                        width,
                        height,
                        new int[]{
                                0x005C2BFF,
                                0x554E55FF,
                                0x4400AFFF,
                                0x3300D8FF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(150);

        canvas.drawPath(
                bottomAurora2,
                auroraPaint
        );

        auroraPaint.setShader(null);
    }

    // ============================================================
    // LIGHT BOTTOM AURORA
    // ============================================================

    private void drawLightBottomAurora(
            Canvas canvas,
            float width,
            float height) {

        // --------------------------------------------------------
        // SOFT OUTER GLOW
        // --------------------------------------------------------

        glowPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.78f,
                        width,
                        height,
                        new int[]{
                                0x557B3FFF,
                                0x4C4F82FF,
                                0x4600CFFF,
                                0x3338C8FF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        glowPaint.setMaskFilter(
                new BlurMaskFilter(
                        30f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                bottomAurora,
                glowPaint
        );

        glowPaint.setMaskFilter(null);
        glowPaint.setShader(null);

        // --------------------------------------------------------
        // MAIN LIGHT AURORA
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.84f,
                        width,
                        height,
                        new int[]{
                                0x687B4AFF,
                                0x584E89FF,
                                0x5500CFFF,
                                0x4038BDF8
                        },
                        new float[]{
                                0.0f,
                                0.35f,
                                0.70f,
                                1.0f
                        },
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(125);

        canvas.drawPath(
                bottomAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND SOFT BAND
        // --------------------------------------------------------

        softGlowPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.80f,
                        width,
                        height,
                        new int[]{
                                0x004E5EFF,
                                0x455D83FF,
                                0x4400CFFF,
                                0x3038CFFF
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        softGlowPaint.setMaskFilter(
                new BlurMaskFilter(
                        20f,
                        BlurMaskFilter.Blur.NORMAL
                )
        );

        canvas.drawPath(
                bottomAurora2,
                softGlowPaint
        );

        softGlowPaint.setMaskFilter(null);
        softGlowPaint.setShader(null);

        // --------------------------------------------------------
        // SECOND LIGHT BAND
        // --------------------------------------------------------

        auroraPaint.setShader(
                new LinearGradient(
                        0,
                        height * 0.82f,
                        width,
                        height,
                        new int[]{
                                0x006C58FF,
                                0x454D8CFF,
                                0x4400CFFF,
                                0x3038BDF8
                        },
                        null,
                        Shader.TileMode.CLAMP
                )
        );

        auroraPaint.setAlpha(100);

        canvas.drawPath(
                bottomAurora2,
                auroraPaint
        );

        auroraPaint.setShader(null);
    }
}