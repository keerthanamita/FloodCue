package com.floodcue.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AuroraCurveView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path topPath = new Path();
    private final Path bottomPath = new Path();

    private float sx;
    private float sy;
    private float strokeScale;

    // ------------------------------------------------------------
    // TARGET DESIGN REFERENCE
    //
    // Designed around your 1536 x 2048 target image.
    // Coordinates are normalized/scaled to whatever phone
    // resolution the app is running on.
    // ------------------------------------------------------------

    public AuroraCurveView(Context context) {
        super(context);
        init();
    }

    public AuroraCurveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraCurveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // Important:
        // We are drawing a static background, not animating it.
        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);

        sx = w / 1536f;
        sy = h / 2048f;

        // Use width/height independently for geometry,
        // but stroke thickness follows the smaller scale
        // so the ribbon doesn't become ridiculously thick.
        strokeScale = Math.min(sx, sy);

        buildPaths();
    }

    // ============================================================
    // BUILD THE TWO MAIN RIBBON CURVES
    // ============================================================

    private void buildPaths() {

        topPath.reset();
        bottomPath.reset();

        // --------------------------------------------------------
        // TOP-RIGHT RIBBON
        //
        // Target:
        //
        //        \~~~~~~
        //          \~~~~~~
        //              \~~~~
        //
        // It starts near the upper-middle and flows toward
        // the right edge.
        // --------------------------------------------------------

        topPath.moveTo(
                X(710),
                Y(-35)
        );

        topPath.cubicTo(
                X(830), Y(10),
                X(950), Y(105),
                X(1090), Y(150)
        );

        topPath.cubicTo(
                X(1235), Y(198),
                X(1350), Y(215),
                X(1565), Y(185)
        );

        // --------------------------------------------------------
        // BOTTOM RIBBON
        //
        // Target:
        //
        // left
        //   \____
        //        \____
        //             \____/
        //                  \____ right
        //
        // Actually the target has a broad smooth valley through
        // the lower-middle rather than a sharp U.
        // --------------------------------------------------------

        bottomPath.moveTo(
                X(-90),
                Y(1585)
        );

        bottomPath.cubicTo(
                X(120), Y(1660),
                X(250), Y(1835),
                X(485), Y(1905)
        );

        bottomPath.cubicTo(
                X(720), Y(1980),
                X(950), Y(1985),
                X(1160), Y(1885)
        );

        bottomPath.cubicTo(
                X(1320), Y(1810),
                X(1440), Y(1695),
                X(1625), Y(1635)
        );
    }

    // ============================================================
    // NORMALIZED TARGET COORDINATES
    // ============================================================

    private float X(float targetX) {
        return targetX * sx;
    }

    private float Y(float targetY) {
        return targetY * sy;
    }

    private float W(float targetWidth) {
        return targetWidth * strokeScale;
    }

    // ============================================================
    // DRAW
    // ============================================================

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        // --------------------------------------------------------
        // BACKGROUND
        // --------------------------------------------------------

        canvas.drawColor(Color.rgb(3, 23, 48));

        // --------------------------------------------------------
        // TOP RIBBON
        // --------------------------------------------------------

        drawTopRibbon(canvas);

        // --------------------------------------------------------
        // BOTTOM RIBBON
        // --------------------------------------------------------

        drawBottomRibbon(canvas);
    }

    // ============================================================
    // TOP RIBBON
    // ============================================================

    private void drawTopRibbon(Canvas canvas) {

        // --------------------------------------------------------
        // 1. LARGE SOFT GLOW
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(150),
                65,
                new int[]{
                        Color.argb(0, 20, 100, 255),
                        Color.argb(65, 20, 145, 255),
                        Color.argb(70, 0, 220, 255),
                        Color.argb(60, 90, 40, 255),
                        Color.argb(0, 100, 20, 255)
                }
        );

        // --------------------------------------------------------
        // 2. OUTER DARK-BLUE RIBBON
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(112),
                230,
                new int[]{
                        Color.rgb(20, 67, 190),
                        Color.rgb(20, 105, 235),
                        Color.rgb(0, 190, 238),
                        Color.rgb(35, 105, 225),
                        Color.rgb(80, 35, 205)
                }
        );

        // --------------------------------------------------------
        // 3. SECOND BLUE LAYER
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(88),
                245,
                new int[]{
                        Color.rgb(18, 74, 210),
                        Color.rgb(15, 143, 245),
                        Color.rgb(0, 207, 238),
                        Color.rgb(40, 130, 245),
                        Color.rgb(83, 45, 225)
                }
        );

        // --------------------------------------------------------
        // 4. CYAN INNER LAYER
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(61),
                255,
                new int[]{
                        Color.rgb(17, 95, 224),
                        Color.rgb(0, 190, 250),
                        Color.rgb(20, 235, 235),
                        Color.rgb(20, 155, 242),
                        Color.rgb(88, 57, 235)
                }
        );

        // --------------------------------------------------------
        // 5. BRIGHT INNER STRIPE
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(30),
                255,
                new int[]{
                        Color.rgb(35, 119, 245),
                        Color.rgb(40, 220, 255),
                        Color.rgb(80, 245, 235),
                        Color.rgb(30, 185, 255),
                        Color.rgb(116, 62, 245)
                }
        );

        // --------------------------------------------------------
        // 6. VERY THIN HIGHLIGHT
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                topPath,
                W(10),
                220,
                new int[]{
                        Color.rgb(85, 170, 255),
                        Color.rgb(100, 250, 255),
                        Color.rgb(120, 255, 245),
                        Color.rgb(80, 190, 255),
                        Color.rgb(155, 90, 255)
                }
        );
    }

    // ============================================================
    // BOTTOM RIBBON
    // ============================================================

    private void drawBottomRibbon(Canvas canvas) {

        // --------------------------------------------------------
        // LARGE GLOW
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(170),
                65,
                new int[]{
                        Color.argb(0, 60, 40, 255),
                        Color.argb(65, 70, 70, 255),
                        Color.argb(75, 0, 210, 255),
                        Color.argb(65, 40, 80, 255),
                        Color.argb(0, 0, 100, 255)
                }
        );

        // --------------------------------------------------------
        // OUTER RIBBON
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(125),
                235,
                new int[]{
                        Color.rgb(89, 35, 215),
                        Color.rgb(38, 72, 225),
                        Color.rgb(10, 150, 245),
                        Color.rgb(0, 190, 235),
                        Color.rgb(20, 75, 220)
                }
        );

        // --------------------------------------------------------
        // BLUE LAYER
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(98),
                245,
                new int[]{
                        Color.rgb(117, 35, 230),
                        Color.rgb(30, 92, 240),
                        Color.rgb(0, 200, 250),
                        Color.rgb(20, 165, 245),
                        Color.rgb(20, 90, 230)
                }
        );

        // --------------------------------------------------------
        // CYAN LAYER
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(68),
                255,
                new int[]{
                        Color.rgb(130, 40, 235),
                        Color.rgb(45, 110, 245),
                        Color.rgb(0, 225, 245),
                        Color.rgb(10, 205, 245),
                        Color.rgb(20, 110, 240)
                }
        );

        // --------------------------------------------------------
        // BRIGHT INNER BAND
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(34),
                255,
                new int[]{
                        Color.rgb(165, 55, 245),
                        Color.rgb(65, 125, 255),
                        Color.rgb(30, 240, 250),
                        Color.rgb(40, 225, 245),
                        Color.rgb(50, 135, 250)
                }
        );

        // --------------------------------------------------------
        // THIN HIGHLIGHT
        // --------------------------------------------------------

        drawGradientStroke(
                canvas,
                bottomPath,
                W(11),
                225,
                new int[]{
                        Color.rgb(210, 80, 255),
                        Color.rgb(110, 170, 255),
                        Color.rgb(120, 255, 255),
                        Color.rgb(100, 235, 255),
                        Color.rgb(100, 180, 255)
                }
        );
    }

    // ============================================================
    // GRADIENT STROKE
    // ============================================================

    private void drawGradientStroke(
            Canvas canvas,
            Path path,
            float width,
            int alpha,
            int[] colors
    ) {

        paint.reset();

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        paint.setStrokeWidth(width);

        // --------------------------------------------------------
        // Gradient is horizontal because the target transitions
        // from blue/cyan into purple as the ribbon moves across
        // the screen.
        // --------------------------------------------------------

        LinearGradient gradient = new LinearGradient(
                0,
                0,
                getWidth(),
                0,
                colors,
                null,
                Shader.TileMode.CLAMP
        );

        paint.setShader(gradient);

        canvas.drawPath(path, paint);

        paint.setShader(null);
    }
}