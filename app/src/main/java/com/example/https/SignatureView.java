package com.example.https;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {

    private static final float STROKE_WIDTH = 4f;
    private static final int COLOR_INK = Color.parseColor("#1a237e");

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private final Paint mPaint = new Paint();

    private float mLastX, mLastY;
    private boolean mHasFirma = false;

    // ─────────────────────────────────────────
    public SignatureView(Context ctx) { super(ctx); init(); }
    public SignatureView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public SignatureView(Context ctx, AttributeSet a, int d) { super(ctx, a, d); init(); }

    private void init() {
        mPaint.setAntiAlias(true);
        mPaint.setColor(COLOR_INK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        setBackgroundColor(Color.WHITE);
    }

    // Crear bitmap al tener tamaño
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBitmap.eraseColor(Color.WHITE);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Dibujar firma acumulada
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

        // Línea guía
        Paint guide = new Paint();
        guide.setColor(Color.parseColor("#C5CAE9"));
        guide.setStrokeWidth(1f);
        float y = getHeight() * 0.75f;
        canvas.drawLine(20, y, getWidth() - 20, y, guide);

        // Texto si no hay firma
        if (!mHasFirma) {
            Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
            txt.setColor(Color.parseColor("#BDBDBD"));
            txt.setTextSize(32f);
            txt.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Firme aquí", getWidth() / 2f, getHeight() / 2f, txt);
        }

        // Marco
        Paint border = new Paint();
        border.setColor(COLOR_INK);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2f);
        canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, border);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true); // 🔥 aquí SOLO
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false); // 🔥 liberar scroll
                break;
        }

        if (mCanvas == null) return true;

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                mHasFirma = true;
                mLastX = x;
                mLastY = y;
                mCanvas.drawPoint(x, y, mPaint);
                break;

            case MotionEvent.ACTION_MOVE:
                int n = event.getHistorySize();
                for (int i = 0; i < n; i++) {
                    float hx = event.getHistoricalX(i);
                    float hy = event.getHistoricalY(i);
                    drawSegment(mLastX, mLastY, hx, hy);
                    mLastX = hx;
                    mLastY = hy;
                }
                drawSegment(mLastX, mLastY, x, y);
                mLastX = x;
                mLastY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                drawSegment(mLastX, mLastY, x, y);
                break;
        }

        invalidate();
        return true;
    }

    // Dibujo suave entre dos puntos
    private void drawSegment(float x0, float y0, float x1, float y1) {
        Path path = new Path();
        path.moveTo(x0, y0);

        float midX = (x0 + x1) / 2f;
        float midY = (y0 + y1) / 2f;

        path.quadTo(x0, y0, midX, midY);
        path.lineTo(x1, y1);

        mCanvas.drawPath(path, mPaint);
    }

    // ─────────────────────────────────────────

    public void clear() {
        mHasFirma = false;
        if (mBitmap != null) {
            mBitmap.eraseColor(Color.WHITE);
        }
        invalidate();
    }

    public boolean hasFirma() {
        return mHasFirma;
    }

    public Bitmap toBitmap() {
        if (mBitmap == null) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
        return mBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }
}