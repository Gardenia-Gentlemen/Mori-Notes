package com.mori.notebook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;

/** 简易 HSV 色盘：角度为色相、半径为饱和度，亮度由外部单独调整。 */
public class ColorWheelView extends View {
    public interface OnColorChanged {
        void onChanged(int color);
    }

    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = {210f, 0.75f, 1f};
    private Bitmap wheel;
    private OnColorChanged listener;

    public ColorWheelView(Context ctx) {
        super(ctx);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(4));
        ringPaint.setColor(0x66000000);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeWidth(dp(2.5f));
        dotPaint.setColor(Color.WHITE);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setOnColorChanged(OnColorChanged l) {
        listener = l;
    }

    public void setColor(int color) {
        Color.colorToHSV(color | 0xFF000000, hsv);
        invalidate();
    }

    public void setValue(float v) {
        hsv[2] = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    public float getValue() {
        return hsv[2];
    }

    public int getColor() {
        return Color.HSVToColor(new float[]{hsv[0], hsv[1], hsv[2]});
    }

    private void ensureWheel(int size) {
        if (wheel != null && wheel.getWidth() == size) return;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        float r = size / 2f;
        int[] colors = new int[13];
        for (int i = 0; i <= 12; i++) {
            colors[i] = Color.HSVToColor(new float[]{i * 30f, 1f, 1f});
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new SweepGradient(r, r, colors, null));
        cv.drawCircle(r, r, r, p);
        p.setShader(new RadialGradient(r, r, r, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawCircle(r, r, r, p);
        wheel = b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        ensureWheel(size);
        float ox = (getWidth() - size) / 2f;
        float oy = (getHeight() - size) / 2f;
        canvas.drawBitmap(wheel, ox, oy, null);
        if (hsv[2] < 1f) {
            wheelPaint.setShader(null);
            wheelPaint.setColor(Color.BLACK);
            wheelPaint.setAlpha(Math.round((1f - hsv[2]) * 255f));
            canvas.drawCircle(ox + size / 2f, oy + size / 2f, size / 2f, wheelPaint);
        }
        float rad = (float) Math.toRadians(hsv[0]);
        float rr = hsv[1] * size / 2f;
        float x = ox + size / 2f + (float) Math.cos(rad) * rr;
        float y = oy + size / 2f + (float) Math.sin(rad) * rr;
        canvas.drawCircle(x, y, dp(7), ringPaint);
        canvas.drawCircle(x, y, dp(5.5f), dotPaint);
    }

    private void pick(float px, float py) {
        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = px - cx;
        float dy = py - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float r = size / 2f;
        hsv[1] = Math.max(0f, Math.min(1f, dist / r));
        double deg = Math.toDegrees(Math.atan2(dy, dx));
        if (deg < 0) deg += 360d;
        hsv[0] = (float) (deg % 360d);
        invalidate();
        if (listener != null) listener.onChanged(getColor());
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                getParent().requestDisallowInterceptTouchEvent(true);
                pick(e.getX(), e.getY());
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
