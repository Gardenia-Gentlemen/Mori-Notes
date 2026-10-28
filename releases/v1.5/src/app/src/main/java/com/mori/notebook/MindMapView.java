package com.mori.notebook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 借鉴小米笔记思维笔记：左右双向布局、分支配色卡片、曲线圆点连接、自适应缩放居中。 */
public class MindMapView extends View {

    public interface OnNodeTapListener {
        void onNodeTap(MindNode node);
    }

    private static final int[] PALETTE = {
            0xFF1B6EF3, 0xFFFF8A00, 0xFFE8425C, 0xFF00A68C,
            0xFF8E4DE8, 0xFFD9A400, 0xFF2AA84A, 0xFF2C8FE0
    };

    private final List<MindNode> roots = new ArrayList<>();
    private final List<MindNode> ordered = new ArrayList<>();
    private final Map<MindNode, RectF> rects = new HashMap<>();
    private final Map<MindNode, Integer> depths = new HashMap<>();
    private final Map<MindNode, Integer> branchIdx = new HashMap<>();
    private final Map<MindNode, Integer> sideOf = new HashMap<>();
    private final Map<MindNode, Float> widths = new HashMap<>();
    private final Map<MindNode, Float> centerY = new HashMap<>();

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();

    private float density;
    private float rowH;
    private float gapX;
    private float padX;
    private float minW;
    private float maxW;
    private float radius;
    private int maxDepth;
    private int contentW = 1;
    private int contentH = 1;

    private int bgColor = 0xFFFFFFFF;
    private int inkColor = 0xFF20242B;

    private float viewScale = 1f;
    private float viewTx = 0f;
    private float viewTy = 0f;
    private boolean userMoved;

    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private boolean moved;
    private long lastTapTime;
    private float lastTapX;
    private float lastTapY;
    private OnNodeTapListener tapListener;

    private final ScaleGestureDetector scaleDetector;

    public MindMapView(Context context) {
        this(context, null);
    }

    public MindMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                resize(viewScale * d.getScaleFactor(), getWidth() / 2f, getHeight() / 2f);
                userMoved = true;
                return true;
            }
        });
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        rowH = 44 * density;
        gapX = 46 * density;
        padX = 14 * density;
        minW = 64 * density;
        maxW = 210 * density;
        radius = 10 * density;
        bgColor = themeColor(com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        inkColor = themeColor(com.google.android.material.R.attr.colorOnSurface, 0xFF20242B);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.FILL);
        tp.setTextAlign(Paint.Align.CENTER);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
    }

    private int themeColor(int attr, int fallback) {
        try {
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(attr, tv, true)) {
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return tv.data;
                }
                if (tv.resourceId != 0) return getResources().getColor(tv.resourceId);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public void setOnNodeTapListener(OnNodeTapListener l) {
        tapListener = l;
    }

    private float heightFor(int depth) {
        if (depth == 0) return 44 * density;
        return depth == 1 ? 36 * density : 31 * density;
    }

    private float textSizeFor(int depth) {
        if (depth == 0) return 15.5f * density;
        return depth == 1 ? 14 * density : 13 * density;
    }

    private int colorOf(MindNode node) {
        Integer b = branchIdx.get(node);
        int i = b == null ? 0 : b;
        return PALETTE[Math.abs(i) % PALETTE.length];
    }

    /** 与主题背景混色，浅色下偏白、深色下偏深。 */
    private int mix(int color, float baseWeight) {
        float w = baseWeight;
        int br = bgColor >> 16 & 0xFF;
        int bg = bgColor >> 8 & 0xFF;
        int bb = bgColor & 0xFF;
        int r = (int) ((color >> 16 & 0xFF) * (1 - w) + br * w);
        int g = (int) ((color >> 8 & 0xFF) * (1 - w) + bg * w);
        int b = (int) ((color & 0xFF) * (1 - w) + bb * w);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void setRoots(List<MindNode> list) {
        roots.clear();
        if (list != null) roots.addAll(list);
        layoutAll();
        requestLayout();
        if (userMoved) centerContent();
        else fitToScreen();
        invalidate();
    }

    private int rowsOf(MindNode n) {
        if (n.children.isEmpty()) return 1;
        int s = 0;
        for (MindNode c : n.children) s += rowsOf(c);
        return Math.max(1, s);
    }

    private float blockHeight(MindNode root) {
        int n = root.children.size();
        if (n == 0) return rowH;
        int leftCount = (n + 1) / 2;
        int lr = 0;
        int rr = 0;
        for (int i = 0; i < n; i++) {
            int rows = rowsOf(root.children.get(i));
            if (i < leftCount) lr += rows;
            else rr += rows;
        }
        return Math.max(lr, rr) * rowH;
    }

    private void layoutAll() {
        rects.clear();
        depths.clear();
        branchIdx.clear();
        sideOf.clear();
        widths.clear();
        centerY.clear();
        ordered.clear();
        maxDepth = 0;
        float baseY = 0f;
        for (int i = 0; i < roots.size(); i++) {
            MindNode r = roots.get(i);
            float block = blockHeight(r);
            placeRoot(r, i, baseY, block);
            baseY += block + rowH * 0.7f;
        }
        if (ordered.isEmpty()) {
            contentW = 1;
            contentH = 1;
            return;
        }
        float[] wl = new float[maxDepth + 2];
        float[] wr = new float[maxDepth + 2];
        float rootW = minW;
        for (MindNode n : ordered) {
            int d = depths.get(n);
            int s = sideOf.get(n);
            float w = widths.get(n);
            if (d == 0) {
                if (w > rootW) rootW = w;
                continue;
            }
            if (s < 0) {
                if (w > wl[d]) wl[d] = w;
            } else {
                if (w > wr[d]) wr[d] = w;
            }
        }
        for (int d = 1; d <= maxDepth; d++) {
            if (wl[d] < minW) wl[d] = minW;
            if (wr[d] < minW) wr[d] = minW;
        }
        float[] xr = new float[maxDepth + 2];
        float[] xl = new float[maxDepth + 2];
        float half = rootW / 2f;
        xr[1] = half + gapX;
        for (int d = 1; d < maxDepth; d++) xr[d + 1] = xr[d] + wr[d] + gapX;
        xl[1] = -half - gapX;
        for (int d = 1; d < maxDepth; d++) xl[d + 1] = xl[d] - wl[d] - gapX;

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (MindNode n : ordered) {
            int d = depths.get(n);
            float w = widths.get(n);
            float h = heightFor(d);
            float cy = centerY.get(n);
            RectF r;
            if (d == 0) {
                r = new RectF(-half, cy - h / 2f, half, cy + h / 2f);
            } else if (sideOf.get(n) < 0) {
                r = new RectF(xl[d] - w, cy - h / 2f, xl[d], cy + h / 2f);
            } else {
                r = new RectF(xr[d], cy - h / 2f, xr[d] + w, cy + h / 2f);
            }
            rects.put(n, r);
            if (r.left < minX) minX = r.left;
            if (r.right > maxX) maxX = r.right;
            if (r.top < minY) minY = r.top;
            if (r.bottom > maxY) maxY = r.bottom;
        }
        float pad = 22 * density;
        float dx = pad - minX;
        float dy = pad - minY;
        for (RectF r : rects.values()) r.offset(dx, dy);
        contentW = (int) (maxX - minX + pad * 2);
        contentH = (int) (maxY - minY + pad * 2);
    }

    private void placeRoot(MindNode root, int index, float baseY, float block) {
        depths.put(root, 0);
        branchIdx.put(root, index);
        sideOf.put(root, 0);
        ordered.add(root);
        widths.put(root, measureWidth(root, 0));
        float cy = baseY + block / 2f;
        centerY.put(root, cy);
        int n = root.children.size();
        if (n == 0) return;
        int leftCount = (n + 1) / 2;
        float lr = 0;
        float rr = 0;
        for (int i = 0; i < n; i++) {
            int rows = rowsOf(root.children.get(i));
            if (i < leftCount) lr += rows;
            else rr += rows;
        }
        float[] lc = {cy - lr * rowH / 2f};
        float[] rc = {cy - rr * rowH / 2f};
        for (int i = 0; i < n; i++) {
            MindNode c = root.children.get(i);
            boolean left = i < leftCount;
            place(c, 1, i, left ? -1 : 1, left ? lc : rc);
        }
    }

    private void place(MindNode n, int depth, int branch, int side, float[] cursor) {
        depths.put(n, depth);
        branchIdx.put(n, branch);
        sideOf.put(n, side);
        ordered.add(n);
        if (depth > maxDepth) maxDepth = depth;
        widths.put(n, measureWidth(n, depth));
        float top = cursor[0];
        float cy;
        if (n.children.isEmpty()) {
            cy = top + rowH / 2f;
            cursor[0] = top + rowH;
        } else {
            for (MindNode c : n.children) place(c, depth + 1, branch, side, cursor);
            cy = (top + cursor[0]) / 2f;
        }
        centerY.put(n, cy);
    }

    private float measureWidth(MindNode node, int depth) {
        tp.setTextSize(textSizeFor(depth));
        tp.setFakeBoldText(depth == 0);
        String t = node.text == null ? "" : node.text.toString().trim();
        if (t.isEmpty()) t = "（空）";
        float w = tp.measureText(t) + padX * 2.4f;
        tp.setFakeBoldText(false);
        if (w < minW) w = minW;
        if (w > maxW) w = maxW;
        return w;
    }

    private float minScale() {
        return 0.2f;
    }

    private void fitToScreen() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || contentW <= 1 || contentH <= 1) return;
        float margin = 12 * density;
        float s = Math.min((w - margin * 2) / (float) contentW, (h - margin * 2) / (float) contentH);
        if (s > 1.2f) s = 1.2f;
        if (s < minScale()) s = minScale();
        viewScale = s;
        centerContent();
    }

    private void centerContent() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        viewTx = (getWidth() - contentW * viewScale) / 2f;
        viewTy = (getHeight() - contentH * viewScale) / 2f;
        invalidate();
    }

    private void resize(float newScale, float anchorX, float anchorY) {
        float s = Math.max(minScale(), Math.min(newScale, 4f));
        float ix = (anchorX - viewTx) / viewScale;
        float iy = (anchorY - viewTy) / viewScale;
        viewScale = s;
        viewTx = anchorX - ix * s;
        viewTy = anchorY - iy * s;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!userMoved) fitToScreen();
        else centerContent();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (wMode == MeasureSpec.UNSPECIFIED || w == 0) w = contentW;
        if (hMode == MeasureSpec.UNSPECIFIED || h == 0) h = contentH;
        setMeasuredDimension(Math.max(w, 1), Math.max(h, 1));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(bgColor);
        if (ordered.isEmpty()) return;
        canvas.save();
        canvas.translate(viewTx, viewTy);
        canvas.scale(viewScale, viewScale);
        for (MindNode n : ordered) drawChildren(canvas, n);
        for (MindNode n : ordered) drawNode(canvas, n);
        canvas.restore();
    }

    private void drawChildren(Canvas canvas, MindNode p) {
        RectF pr = rects.get(p);
        if (pr == null) return;
        int d = depths.get(p);
        for (MindNode c : p.children) {
            RectF cr = rects.get(c);
            if (cr == null) continue;
            boolean left = cr.centerX() < pr.centerX();
            float sx = left ? pr.left : pr.right;
            float sy = pr.centerY();
            float ex = left ? cr.right : cr.left;
            float ey = cr.centerY();
            float mid = (sx + ex) / 2f;
            int color = colorOf(c);
            line.setColor(color);
            line.setAlpha(d == 0 ? 220 : 175);
            line.setStrokeWidth((d == 0 ? 2.1f : 1.6f) * density);
            path.reset();
            path.moveTo(sx, sy);
            path.cubicTo(mid, sy, mid, ey, ex, ey);
            canvas.drawPath(path, line);
            dot.setColor(color);
            dot.setAlpha(235);
            canvas.drawCircle(sx, sy, 2.6f * density, dot);
        }
    }

    private void drawNode(Canvas canvas, MindNode n) {
        RectF r = rects.get(n);
        if (r == null) return;
        int d = depths.get(n);
        int color = colorOf(n);
        tmp.set(r);
        fill.clearShadowLayer();
        if (d == 0) {
            fill.setColor(color);
            fill.setShadowLayer(7 * density, 0, 3 * density, 0x30000000);
            canvas.drawRoundRect(tmp, radius, radius, fill);
            fill.clearShadowLayer();
        } else {
            fill.setColor(mix(color, d == 1 ? 0.88f : 0.93f));
            fill.setShadowLayer(4 * density, 0, 2 * density, 0x18000000);
            canvas.drawRoundRect(tmp, radius, radius, fill);
            fill.clearShadowLayer();
            stroke.setColor(color);
            stroke.setAlpha(d == 1 ? 255 : 150);
            RectF bar = new RectF(r.left, r.top + 3 * density, r.left + 3.4f * density, r.bottom - 3 * density);
            canvas.drawRoundRect(bar, 1.7f * density, 1.7f * density, stroke);
            stroke.setAlpha(255);
        }
        tp.setTextSize(textSizeFor(d));
        tp.setFakeBoldText(d == 0);
        tp.setColor(d == 0 ? 0xFFFFFFFF : inkColor);
        String t = n.text == null ? "" : n.text.toString().trim();
        if (t.isEmpty()) t = "（空）";
        float inner = padX + (d == 0 ? 0 : 4 * density);
        String label = TextUtils.ellipsize(t, tp, Math.max(1f, r.width() - inner * 2f), TextUtils.TruncateAt.END).toString();
        Paint.FontMetrics fm = tp.getFontMetrics();
        float baseline = r.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, r.centerX(), baseline, tp);
        tp.setFakeBoldText(false);
    }

    private MindNode hitTest(float x, float y) {
        for (int i = ordered.size() - 1; i >= 0; i--) {
            MindNode n = ordered.get(i);
            RectF r = rects.get(n);
            if (r != null && r.contains(x, y)) return n;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                lastX = downX;
                lastY = downY;
                moved = false;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                moved = true;
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress()) {
                    float dx = e.getX() - lastX;
                    float dy = e.getY() - lastY;
                    if (Math.abs(e.getX() - downX) > 8 * density || Math.abs(e.getY() - downY) > 8 * density) {
                        moved = true;
                    }
                    if (moved) {
                        viewTx += dx;
                        viewTy += dy;
                        userMoved = true;
                        invalidate();
                    }
                }
                lastX = e.getX();
                lastY = e.getY();
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!moved) handleTap(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void handleTap(float x, float y) {
        long now = System.currentTimeMillis();
        if (now - lastTapTime < 320 && Math.abs(x - lastTapX) < 40 * density && Math.abs(y - lastTapY) < 40 * density) {
            lastTapTime = 0;
            userMoved = false;
            fitToScreen();
            return;
        }
        lastTapTime = now;
        lastTapX = x;
        lastTapY = y;
        float ix = (x - viewTx) / viewScale;
        float iy = (y - viewTy) / viewScale;
        MindNode hit = hitTest(ix, iy);
        if (hit != null && tapListener != null) tapListener.onNodeTap(hit);
    }

    /** 导出用：整图按内容尺寸渲染，与当前缩放手势无关。 */
    public Bitmap drawToBitmap(int targetWidth) {
        float s = 1f;
        if (targetWidth > 0 && contentW > targetWidth) s = targetWidth / (float) contentW;
        int w = Math.max(1, (int) (contentW * s));
        int h = Math.max(1, (int) (contentH * s));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(bgColor);
        c.save();
        c.scale(s, s);
        for (MindNode n : ordered) drawChildren(c, n);
        for (MindNode n : ordered) drawNode(c, n);
        c.restore();
        return bmp;
    }
}