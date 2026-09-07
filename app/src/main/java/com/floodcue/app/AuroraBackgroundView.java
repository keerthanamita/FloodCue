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

    private final Paint backgroundPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint glowPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint auroraPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path topAurora = new Path();
    private final Path bottomAurora = new Path();

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

    private void init() {

        /*
         * BlurMaskFilter needs software rendering.
         */
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        glowPaint.setAntiAlias(true);
        auroraPaint.setAntiAlias(true);
        backgroundPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        boolean darkMode =
                (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        drawBackground(canvas, width, height, darkMode);

        drawTopAurora(canvas, width, height, darkMode);

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

        if(darkMode) {

            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height,
                            new int[]{
                                    0xFF06152F,
                                    0xFF071B38,
                                    0xFF06152F
                            },
                            new float[]{
                                    0f,
                                    0.5f,
                                    1f
                            },
                            Shader.TileMode.CLAMP
                    );

            backgroundPaint.setShader(gradient);

        } else {

            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height,
                            new int[]{
                                    0xFFF8FBFF,
                                    0xFFF2F9FF,
                                    0xFFF8FBFF
                            },
                            new float[]{
                                    0f,
                                    0.5f,
                                    1f
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
    }

    // ============================================================
    // TOP AURORA
    // ============================================================

    private void drawTopAurora(
            Canvas canvas,
            float width,
            float height,
            boolean darkMode) {

        topAurora.reset();

        /*
         * IMPORTANT:
         *
         * This is intentionally a LARGE filled aurora region.
         * It is NOT a thin stroke.
         *
         * The shape occupies roughly the upper 25-30%
         * of the screen.
         */

        topAurora.moveTo(
                -width * 0.10f,
                -height * 0.05f
        );

        topAurora.cubicTo(
                width * 0.08f,
                height * 0.10f,
                width * 0.18f,
                height * 0.18f,
                width * 0.34f,
                height * 0.15f
        );

        topAurora.cubicTo(
                width * 0.50f,
                height * 0.12f,
                width * 0.55f,
                -height * 0.02f,
                width * 0.70f,
                height * 0.04f
        );

        topAurora.cubicTo(
                width * 0.85f,
                height * 0.10f,
                width * 0.95f,
                height * 0.20f,
                width * 1.10f,
                height * 0.16f
        );

        topAurora.lineTo(
                width * 1.10f,
                -height * 0.08f
        );

        topAurora.close();

        if (darkMode) {

            // ----------------------------------------------------
            // SOFT OUTER GLOW
            // ----------------------------------------------------

            glowPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height * 0.25f,
                            new int[]{
                                    0x995D20FF,
                                    0x884B6CFF,
                                    0x8800D9FF,
                                    0x6600BFFF
                            },
                            null,
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

            glowPaint.clearShadowLayer();
            glowPaint.setMaskFilter(null);

            // ----------------------------------------------------
            // MAIN AURORA
            // ----------------------------------------------------

            auroraPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width,
                            0,
                            new int[]{
                                    0xFF692BFF,
                                    0xFF315CFF,
                                    0xFF078BFF,
                                    0xFF00CFE8
                            },
                            new float[]{
                                    0f,
                                    0.35f,
                                    0.68f,
                                    1f
                            },
                            Shader.TileMode.CLAMP
                    )
            );

            auroraPaint.setAlpha(215);

        } else {

            // ----------------------------------------------------
            // LIGHT MODE
            // ----------------------------------------------------

            glowPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width,
                            height * 0.25f,
                            new int[]{
                                    0x557B2CFF,
                                    0x443C8DFF,
                                    0x4400CFFF,
                                    0x3338BDF8
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

            auroraPaint.setShader(
                    new LinearGradient(
                            0,
                            0,
                            width,
                            0,
                            new int[]{
                                    0x667B2CFF,
                                    0x553C8DFF,
                                    0x5500CFFF,
                                    0x4438BDF8
                            },
                            null,
                            Shader.TileMode.CLAMP
                    )
            );

            auroraPaint.setAlpha(120);
        }

        canvas.drawPath(
                topAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);
    }

    // ============================================================
    // BOTTOM AURORA
    // ============================================================

    private void drawBottomAurora(
            Canvas canvas,
            float width,
            float height,
            boolean darkMode) {

        bottomAurora.reset();

        /*
         * Large flowing aurora at the bottom.
         *
         * It should remain behind the Sign Up section,
         * rather than crossing through the entire screen.
         */

        bottomAurora.moveTo(
                -width * 0.10f,
                height * 1.05f
        );

        bottomAurora.cubicTo(
                width * 0.08f,
                height * 0.90f,
                width * 0.20f,
                height * 0.84f,
                width * 0.36f,
                height * 0.91f
        );

        bottomAurora.cubicTo(
                width * 0.51f,
                height * 0.98f,
                width * 0.58f,
                height * 1.08f,
                width * 0.72f,
                height * 0.99f
        );

        bottomAurora.cubicTo(
                width * 0.87f,
                height * 0.90f,
                width * 0.96f,
                height * 0.82f,
                width * 1.10f,
                height * 0.88f
        );

        bottomAurora.lineTo(
                width * 1.10f,
                height * 1.08f
        );

        bottomAurora.close();

        if (darkMode) {

            // ----------------------------------------------------
            // SOFT GLOW
            // ----------------------------------------------------

            glowPaint.setShader(
                    new LinearGradient(
                            0,
                            height * 0.78f,
                            width,
                            height,
                            new int[]{
                                    0x995D20FF,
                                    0x884B6CFF,
                                    0x8800D9FF,
                                    0x6600BFFF
                            },
                            null,
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

            // ----------------------------------------------------
            // MAIN AURORA
            // ----------------------------------------------------

            auroraPaint.setShader(
                    new LinearGradient(
                            0,
                            height * 0.85f,
                            width,
                            height,
                            new int[]{
                                    0xFF642BFF,
                                    0xFF315CFF,
                                    0xFF078BFF,
                                    0xFF00CFE8
                            },
                            new float[]{
                                    0f,
                                    0.35f,
                                    0.70f,
                                    1f
                            },
                            Shader.TileMode.CLAMP
                    )
            );

            auroraPaint.setAlpha(215);

        } else {

            glowPaint.setShader(
                    new LinearGradient(
                            0,
                            height * 0.80f,
                            width,
                            height,
                            new int[]{
                                    0x557B2CFF,
                                    0x443C8DFF,
                                    0x4400CFFF,
                                    0x3338BDF8
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

            auroraPaint.setShader(
                    new LinearGradient(
                            0,
                            height * 0.85f,
                            width,
                            height,
                            new int[]{
                                    0x667B2CFF,
                                    0x553C8DFF,
                                    0x5500CFFF,
                                    0x4438BDF8
                            },
                            null,
                            Shader.TileMode.CLAMP
                    )
            );

            auroraPaint.setAlpha(120);
        }

        canvas.drawPath(
                bottomAurora,
                auroraPaint
        );

        auroraPaint.setShader(null);
    }
}