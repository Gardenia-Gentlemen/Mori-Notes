package com.mori.notebook;

/* loaded from: classes.dex */
public class MindMapView extends android.view.View {
    private static final int[] PALETTE = {-14979341, -30208, -1555876, -16734580, -7451160, -2513920, -13981622, -13856800};
    private int bgColor;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> branchIdx;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Float> centerY;
    private int contentH;
    private int contentW;
    private float density;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> depths;
    private final android.graphics.Paint dot;
    private float downX;
    private float downY;
    private final android.graphics.Paint fill;
    private float gapX;
    private int inkColor;
    private long lastTapTime;
    private float lastTapX;
    private float lastTapY;
    private float lastX;
    private float lastY;
    private final android.graphics.Paint line;
    private int maxDepth;
    private float maxW;
    private float minW;
    private boolean moved;
    private final java.util.List<com.mori.notebook.MindNode> ordered;
    private float padX;
    private final android.graphics.Path path;
    private float radius;
    private final java.util.Map<com.mori.notebook.MindNode, android.graphics.RectF> rects;
    private final java.util.List<com.mori.notebook.MindNode> roots;
    private float rowH;
    private final android.view.ScaleGestureDetector scaleDetector;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> sideOf;
    private final android.graphics.Paint stroke;
    private com.mori.notebook.MindMapView.OnNodeTapListener tapListener;
    private final android.graphics.RectF tmp;
    private final android.text.TextPaint tp;
    private boolean userMoved;
    private float viewScale;
    private float viewTx;
    private float viewTy;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Float> widths;

    public interface OnNodeTapListener {
        void onNodeTap(com.mori.notebook.MindNode mindNode);
    }

    private float minScale() {
        return 0.2f;
    }

    public MindMapView(android.content.Context context) {
        this(context, null);
    }

    public MindMapView(android.content.Context context, android.util.AttributeSet attributeSet) {
        super(context, attributeSet);
        this.roots = new java.util.ArrayList();
        this.ordered = new java.util.ArrayList();
        this.rects = new java.util.HashMap();
        this.depths = new java.util.HashMap();
        this.branchIdx = new java.util.HashMap();
        this.sideOf = new java.util.HashMap();
        this.widths = new java.util.HashMap();
        this.centerY = new java.util.HashMap();
        this.fill = new android.graphics.Paint(1);
        this.stroke = new android.graphics.Paint(1);
        this.line = new android.graphics.Paint(1);
        this.dot = new android.graphics.Paint(1);
        this.tp = new android.text.TextPaint(1);
        this.path = new android.graphics.Path();
        this.tmp = new android.graphics.RectF();
        this.contentW = 1;
        this.contentH = 1;
        this.bgColor = -1;
        this.inkColor = -14670805;
        this.viewScale = 1.0f;
        this.viewTx = 0.0f;
        this.viewTy = 0.0f;
        this.scaleDetector = new android.view.ScaleGestureDetector(context, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() { // from class: com.mori.notebook.MindMapView.1
            @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
            public boolean onScale(android.view.ScaleGestureDetector scaleGestureDetector) {
                com.mori.notebook.MindMapView mindMapView = com.mori.notebook.MindMapView.this;
                mindMapView.resize(mindMapView.viewScale * scaleGestureDetector.getScaleFactor(), com.mori.notebook.MindMapView.this.getWidth() / 2.0f, com.mori.notebook.MindMapView.this.getHeight() / 2.0f);
                com.mori.notebook.MindMapView.this.userMoved = true;
                return true;
            }
        });
        init();
    }

    private void init() {
        float f = getResources().getDisplayMetrics().density;
        this.density = f;
        this.rowH = 44.0f * f;
        this.gapX = 46.0f * f;
        this.padX = 14.0f * f;
        this.minW = 64.0f * f;
        this.maxW = 210.0f * f;
        this.radius = f * 10.0f;
        this.bgColor = themeColor(com.google.android.material.R.attr.colorSurface, -1);
        this.inkColor = themeColor(com.google.android.material.R.attr.colorOnSurface, -14670805);
        this.line.setStyle(android.graphics.Paint.Style.STROKE);
        this.line.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        this.line.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        this.fill.setStyle(android.graphics.Paint.Style.FILL);
        this.stroke.setStyle(android.graphics.Paint.Style.FILL);
        this.tp.setTextAlign(android.graphics.Paint.Align.CENTER);
        setLayerType(1, null);
        setClickable(true);
    }

    private int themeColor(int i, int i2) {
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(i, typedValue, true)) {
                if (typedValue.type >= 28 && typedValue.type <= 31) {
                    return typedValue.data;
                }
                if (typedValue.resourceId != 0) {
                    return getResources().getColor(typedValue.resourceId);
                }
            }
        } catch (java.lang.Exception unused) {
        }
        return i2;
    }

    public void setOnNodeTapListener(com.mori.notebook.MindMapView.OnNodeTapListener onNodeTapListener) {
        this.tapListener = onNodeTapListener;
    }

    private float heightFor(int i) {
        if (i == 0) {
            return this.density * 44.0f;
        }
        return this.density * (i == 1 ? 36.0f : 31.0f);
    }

    private float textSizeFor(int i) {
        if (i == 0) {
            return this.density * 15.5f;
        }
        return this.density * (i == 1 ? 14.0f : 13.0f);
    }

    private int colorOf(com.mori.notebook.MindNode mindNode) {
        java.lang.Integer num = this.branchIdx.get(mindNode);
        int intValue = num == null ? 0 : num.intValue();
        int[] iArr = PALETTE;
        return iArr[java.lang.Math.abs(intValue) % iArr.length];
    }

    private int mix(int i, float f) {
        int i2 = this.bgColor;
        float f2 = 1.0f - f;
        return ((int) (((i & 255) * f2) + ((i2 & 255) * f))) | (-16777216) | (((int) ((((i >> 16) & 255) * f2) + (((i2 >> 16) & 255) * f))) << 16) | (((int) ((((i >> 8) & 255) * f2) + (((i2 >> 8) & 255) * f))) << 8);
    }

    public void setRoots(java.util.List<com.mori.notebook.MindNode> list) {
        this.roots.clear();
        if (list != null) {
            this.roots.addAll(list);
        }
        layoutAll();
        requestLayout();
        if (this.userMoved) {
            centerContent();
        } else {
            fitToScreen();
        }
        invalidate();
    }

    private int rowsOf(com.mori.notebook.MindNode mindNode) {
        if (mindNode.children.isEmpty()) {
            return 1;
        }
        java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
        int i = 0;
        while (it.hasNext()) {
            i += rowsOf(it.next());
        }
        return java.lang.Math.max(1, i);
    }

    private float blockHeight(com.mori.notebook.MindNode mindNode) {
        int size = mindNode.children.size();
        if (size == 0) {
            return this.rowH;
        }
        int i = (size + 1) / 2;
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            int rowsOf = rowsOf(mindNode.children.get(i4));
            if (i4 < i) {
                i2 += rowsOf;
            } else {
                i3 += rowsOf;
            }
        }
        return java.lang.Math.max(i2, i3) * this.rowH;
    }

    private void layoutAll() {
        int i;
        float f;
        android.graphics.RectF rectF;
        this.rects.clear();
        this.depths.clear();
        this.branchIdx.clear();
        this.sideOf.clear();
        this.widths.clear();
        this.centerY.clear();
        this.ordered.clear();
        this.maxDepth = 0;
        float f2 = 0.0f;
        for (int i2 = 0; i2 < this.roots.size(); i2++) {
            com.mori.notebook.MindNode mindNode = this.roots.get(i2);
            float blockHeight = blockHeight(mindNode);
            placeRoot(mindNode, i2, f2, blockHeight);
            f2 += blockHeight + (this.rowH * 0.7f);
        }
        int i3 = 1;
        if (this.ordered.isEmpty()) {
            this.contentW = 1;
            this.contentH = 1;
            return;
        }
        int i4 = this.maxDepth;
        float[] fArr = new float[i4 + 2];
        float[] fArr2 = new float[i4 + 2];
        float f3 = this.minW;
        for (com.mori.notebook.MindNode mindNode2 : this.ordered) {
            int intValue = this.depths.get(mindNode2).intValue();
            int intValue2 = this.sideOf.get(mindNode2).intValue();
            float floatValue = this.widths.get(mindNode2).floatValue();
            if (intValue == 0) {
                if (floatValue > f3) {
                    f3 = floatValue;
                }
            } else if (intValue2 < 0) {
                if (floatValue > fArr[intValue]) {
                    fArr[intValue] = floatValue;
                }
            } else if (floatValue > fArr2[intValue]) {
                fArr2[intValue] = floatValue;
            }
        }
        int i5 = 1;
        while (true) {
            i = this.maxDepth;
            if (i5 > i) {
                break;
            }
            float f4 = fArr[i5];
            float f5 = this.minW;
            if (f4 < f5) {
                fArr[i5] = f5;
            }
            if (fArr2[i5] < f5) {
                fArr2[i5] = f5;
            }
            i5++;
        }
        float[] fArr3 = new float[i + 2];
        float[] fArr4 = new float[i + 2];
        float f6 = 2.0f;
        float f7 = f3 / 2.0f;
        fArr3[1] = this.gapX + f7;
        int i6 = 1;
        while (i6 < this.maxDepth) {
            int i7 = i6 + 1;
            fArr3[i7] = fArr3[i6] + fArr2[i6] + this.gapX;
            i6 = i7;
        }
        float f8 = -f7;
        fArr4[1] = f8 - this.gapX;
        while (i3 < this.maxDepth) {
            int i8 = i3 + 1;
            fArr4[i8] = (fArr4[i3] - fArr[i3]) - this.gapX;
            i3 = i8;
        }
        float f9 = -3.4028235E38f;
        float f10 = Float.MAX_VALUE;
        float f11 = Float.MAX_VALUE;
        float f12 = -3.4028235E38f;
        for (com.mori.notebook.MindNode mindNode3 : this.ordered) {
            int intValue3 = this.depths.get(mindNode3).intValue();
            float floatValue2 = this.widths.get(mindNode3).floatValue();
            float heightFor = heightFor(intValue3);
            float floatValue3 = this.centerY.get(mindNode3).floatValue();
            if (intValue3 == 0) {
                float f13 = heightFor / f6;
                rectF = new android.graphics.RectF(f8, floatValue3 - f13, f7, floatValue3 + f13);
                f = f8;
            } else if (this.sideOf.get(mindNode3).intValue() < 0) {
                float f14 = fArr4[intValue3];
                float f15 = heightFor / 2.0f;
                f = f8;
                rectF = new android.graphics.RectF(f14 - floatValue2, floatValue3 - f15, f14, floatValue3 + f15);
            } else {
                f = f8;
                float f16 = fArr3[intValue3];
                float f17 = heightFor / 2.0f;
                rectF = new android.graphics.RectF(f16, floatValue3 - f17, floatValue2 + f16, floatValue3 + f17);
            }
            this.rects.put(mindNode3, rectF);
            if (rectF.left < f10) {
                f10 = rectF.left;
            }
            if (rectF.right > f9) {
                f9 = rectF.right;
            }
            if (rectF.top < f11) {
                f11 = rectF.top;
            }
            if (rectF.bottom > f12) {
                f12 = rectF.bottom;
            }
            f8 = f;
            f6 = 2.0f;
        }
        float f18 = this.density * 22.0f;
        float f19 = f18 - f10;
        float f20 = f18 - f11;
        java.util.Iterator<android.graphics.RectF> it = this.rects.values().iterator();
        while (it.hasNext()) {
            it.next().offset(f19, f20);
        }
        float f21 = f18 * 2.0f;
        this.contentW = (int) ((f9 - f10) + f21);
        this.contentH = (int) ((f12 - f11) + f21);
    }

    private void placeRoot(com.mori.notebook.MindNode mindNode, int i, float f, float f2) {
        this.depths.put(mindNode, 0);
        this.branchIdx.put(mindNode, java.lang.Integer.valueOf(i));
        this.sideOf.put(mindNode, 0);
        this.ordered.add(mindNode);
        this.widths.put(mindNode, java.lang.Float.valueOf(measureWidth(mindNode, 0)));
        float f3 = f + (f2 / 2.0f);
        this.centerY.put(mindNode, java.lang.Float.valueOf(f3));
        int size = mindNode.children.size();
        if (size == 0) {
            return;
        }
        int i2 = (size + 1) / 2;
        float f4 = 0.0f;
        float f5 = 0.0f;
        for (int i3 = 0; i3 < size; i3++) {
            int rowsOf = rowsOf(mindNode.children.get(i3));
            if (i3 < i2) {
                f4 += rowsOf;
            } else {
                f5 += rowsOf;
            }
        }
        float f6 = this.rowH;
        float[] fArr = {f3 - ((f4 * f6) / 2.0f)};
        float[] fArr2 = {f3 - ((f5 * f6) / 2.0f)};
        int i4 = 0;
        while (i4 < size) {
            com.mori.notebook.MindNode mindNode2 = mindNode.children.get(i4);
            boolean z = i4 < i2;
            place(mindNode2, 1, i4, z ? -1 : 1, z ? fArr : fArr2);
            i4++;
        }
    }

    private void place(com.mori.notebook.MindNode mindNode, int i, int i2, int i3, float[] fArr) {
        float f;
        this.depths.put(mindNode, java.lang.Integer.valueOf(i));
        this.branchIdx.put(mindNode, java.lang.Integer.valueOf(i2));
        this.sideOf.put(mindNode, java.lang.Integer.valueOf(i3));
        this.ordered.add(mindNode);
        if (i > this.maxDepth) {
            this.maxDepth = i;
        }
        this.widths.put(mindNode, java.lang.Float.valueOf(measureWidth(mindNode, i)));
        float f2 = fArr[0];
        if (mindNode.children.isEmpty()) {
            float f3 = this.rowH;
            f = (f3 / 2.0f) + f2;
            fArr[0] = f2 + f3;
        } else {
            java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
            while (it.hasNext()) {
                place(it.next(), i + 1, i2, i3, fArr);
            }
            f = (f2 + fArr[0]) / 2.0f;
        }
        this.centerY.put(mindNode, java.lang.Float.valueOf(f));
    }

    private float measureWidth(com.mori.notebook.MindNode mindNode, int i) {
        this.tp.setTextSize(textSizeFor(i));
        this.tp.setFakeBoldText(i == 0);
        java.lang.String trim = mindNode.text == null ? "" : mindNode.text.toString().trim();
        if (trim.isEmpty()) {
            trim = "（空）";
        }
        float measureText = this.tp.measureText(trim) + (this.padX * 2.4f);
        this.tp.setFakeBoldText(false);
        float f = this.minW;
        if (measureText < f) {
            measureText = f;
        }
        float f2 = this.maxW;
        return measureText > f2 ? f2 : measureText;
    }

    private void fitToScreen() {
        int i;
        int i2;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || (i = this.contentW) <= 1 || (i2 = this.contentH) <= 1) {
            return;
        }
        float f = this.density * 12.0f * 2.0f;
        float min = java.lang.Math.min((width - f) / i, (height - f) / i2);
        if (min > 1.2f) {
            min = 1.2f;
        }
        if (min < minScale()) {
            min = minScale();
        }
        this.viewScale = min;
        centerContent();
    }

    private void centerContent() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        this.viewTx = (getWidth() - (this.contentW * this.viewScale)) / 2.0f;
        this.viewTy = (getHeight() - (this.contentH * this.viewScale)) / 2.0f;
        invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resize(float f, float f2, float f3) {
        float max = java.lang.Math.max(minScale(), java.lang.Math.min(f, 4.0f));
        float f4 = f2 - this.viewTx;
        float f5 = this.viewScale;
        float f6 = (f3 - this.viewTy) / f5;
        this.viewScale = max;
        this.viewTx = f2 - ((f4 / f5) * max);
        this.viewTy = f3 - (f6 * max);
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.userMoved) {
            centerContent();
        } else {
            fitToScreen();
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int mode = android.view.View.MeasureSpec.getMode(i);
        int mode2 = android.view.View.MeasureSpec.getMode(i2);
        int size = android.view.View.MeasureSpec.getSize(i);
        int size2 = android.view.View.MeasureSpec.getSize(i2);
        if (mode == 0 || size == 0) {
            size = this.contentW;
        }
        if (mode2 == 0 || size2 == 0) {
            size2 = this.contentH;
        }
        setMeasuredDimension(java.lang.Math.max(size, 1), java.lang.Math.max(size2, 1));
    }

    @Override // android.view.View
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(this.bgColor);
        if (this.ordered.isEmpty()) {
            return;
        }
        canvas.save();
        canvas.translate(this.viewTx, this.viewTy);
        float f = this.viewScale;
        canvas.scale(f, f);
        java.util.Iterator<com.mori.notebook.MindNode> it = this.ordered.iterator();
        while (it.hasNext()) {
            drawChildren(canvas, it.next());
        }
        java.util.Iterator<com.mori.notebook.MindNode> it2 = this.ordered.iterator();
        while (it2.hasNext()) {
            drawNode(canvas, it2.next());
        }
        canvas.restore();
    }

    private void drawChildren(android.graphics.Canvas canvas, com.mori.notebook.MindNode mindNode) {
        android.graphics.RectF rectF = this.rects.get(mindNode);
        if (rectF == null) {
            return;
        }
        int intValue = this.depths.get(mindNode).intValue();
        for (com.mori.notebook.MindNode mindNode2 : mindNode.children) {
            android.graphics.RectF rectF2 = this.rects.get(mindNode2);
            if (rectF2 != null) {
                boolean z = rectF2.centerX() < rectF.centerX();
                float f = z ? rectF.left : rectF.right;
                float centerY = rectF.centerY();
                float f2 = z ? rectF2.right : rectF2.left;
                float centerY2 = rectF2.centerY();
                float f3 = (f + f2) / 2.0f;
                int colorOf = colorOf(mindNode2);
                this.line.setColor(colorOf);
                this.line.setAlpha(intValue == 0 ? 220 : 175);
                this.line.setStrokeWidth((intValue == 0 ? 2.1f : 1.6f) * this.density);
                this.path.reset();
                this.path.moveTo(f, centerY);
                this.path.cubicTo(f3, centerY, f3, centerY2, f2, centerY2);
                canvas.drawPath(this.path, this.line);
                this.dot.setColor(colorOf);
                this.dot.setAlpha(235);
                canvas.drawCircle(f, centerY, this.density * 2.6f, this.dot);
            }
        }
    }

    private void drawNode(android.graphics.Canvas canvas, com.mori.notebook.MindNode mindNode) {
        android.graphics.RectF rectF = this.rects.get(mindNode);
        if (rectF == null) {
            return;
        }
        int intValue = this.depths.get(mindNode).intValue();
        int colorOf = colorOf(mindNode);
        this.tmp.set(rectF);
        this.fill.clearShadowLayer();
        if (intValue == 0) {
            this.fill.setColor(colorOf);
            android.graphics.Paint paint = this.fill;
            float f = this.density;
            paint.setShadowLayer(7.0f * f, 0.0f, f * 3.0f, 805306368);
            android.graphics.RectF rectF2 = this.tmp;
            float f2 = this.radius;
            canvas.drawRoundRect(rectF2, f2, f2, this.fill);
            this.fill.clearShadowLayer();
        } else {
            this.fill.setColor(mix(colorOf, intValue == 1 ? 0.88f : 0.93f));
            android.graphics.Paint paint2 = this.fill;
            float f3 = this.density;
            paint2.setShadowLayer(f3 * 4.0f, 0.0f, f3 * 2.0f, 402653184);
            android.graphics.RectF rectF3 = this.tmp;
            float f4 = this.radius;
            canvas.drawRoundRect(rectF3, f4, f4, this.fill);
            this.fill.clearShadowLayer();
            this.stroke.setColor(colorOf);
            this.stroke.setAlpha(intValue == 1 ? 255 : 150);
            android.graphics.RectF rectF4 = new android.graphics.RectF(rectF.left, rectF.top + (this.density * 3.0f), rectF.left + (this.density * 3.4f), rectF.bottom - (this.density * 3.0f));
            float f5 = this.density;
            canvas.drawRoundRect(rectF4, f5 * 1.7f, f5 * 1.7f, this.stroke);
            this.stroke.setAlpha(255);
        }
        this.tp.setTextSize(textSizeFor(intValue));
        this.tp.setFakeBoldText(intValue == 0);
        this.tp.setColor(intValue == 0 ? -1 : this.inkColor);
        java.lang.String trim = mindNode.text == null ? "" : mindNode.text.toString().trim();
        if (trim.isEmpty()) {
            trim = "（空）";
        }
        java.lang.String obj = android.text.TextUtils.ellipsize(trim, this.tp, java.lang.Math.max(1.0f, rectF.width() - ((this.padX + (intValue == 0 ? 0.0f : this.density * 4.0f)) * 2.0f)), android.text.TextUtils.TruncateAt.END).toString();
        android.graphics.Paint.FontMetrics fontMetrics = this.tp.getFontMetrics();
        canvas.drawText(obj, rectF.centerX(), rectF.centerY() - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.tp);
        this.tp.setFakeBoldText(false);
    }

    private com.mori.notebook.MindNode hitTest(float f, float f2) {
        for (int size = this.ordered.size() - 1; size >= 0; size--) {
            com.mori.notebook.MindNode mindNode = this.ordered.get(size);
            android.graphics.RectF rectF = this.rects.get(mindNode);
            if (rectF != null && rectF.contains(f, f2)) {
                return mindNode;
            }
        }
        return null;
    }

    @Override // android.view.View
    public boolean onTouchEvent(android.view.MotionEvent motionEvent) {
        this.scaleDetector.onTouchEvent(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.downX = motionEvent.getX();
            float y = motionEvent.getY();
            this.downY = y;
            this.lastX = this.downX;
            this.lastY = y;
            this.moved = false;
            return true;
        }
        if (actionMasked == 1) {
            if (!this.moved) {
                handleTap(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }
        if (actionMasked != 2) {
            if (actionMasked != 3) {
                if (actionMasked == 5) {
                    this.moved = true;
                } else {
                    return super.onTouchEvent(motionEvent);
                }
            }
            return true;
        }
        if (!this.scaleDetector.isInProgress()) {
            float x = motionEvent.getX() - this.lastX;
            float y2 = motionEvent.getY() - this.lastY;
            if (java.lang.Math.abs(motionEvent.getX() - this.downX) > this.density * 8.0f || java.lang.Math.abs(motionEvent.getY() - this.downY) > this.density * 8.0f) {
                this.moved = true;
            }
            if (this.moved) {
                this.viewTx += x;
                this.viewTy += y2;
                this.userMoved = true;
                invalidate();
            }
        }
        this.lastX = motionEvent.getX();
        this.lastY = motionEvent.getY();
        return true;
    }

    private void handleTap(float f, float f2) {
        com.mori.notebook.MindMapView.OnNodeTapListener onNodeTapListener;
        long currentTimeMillis = java.lang.System.currentTimeMillis();
        if (currentTimeMillis - this.lastTapTime < 320 && java.lang.Math.abs(f - this.lastTapX) < this.density * 40.0f && java.lang.Math.abs(f2 - this.lastTapY) < this.density * 40.0f) {
            this.lastTapTime = 0L;
            this.userMoved = false;
            fitToScreen();
            return;
        }
        this.lastTapTime = currentTimeMillis;
        this.lastTapX = f;
        this.lastTapY = f2;
        float f3 = f - this.viewTx;
        float f4 = this.viewScale;
        com.mori.notebook.MindNode hitTest = hitTest(f3 / f4, (f2 - this.viewTy) / f4);
        if (hitTest == null || (onNodeTapListener = this.tapListener) == null) {
            return;
        }
        onNodeTapListener.onNodeTap(hitTest);
    }

    public android.graphics.Bitmap drawToBitmap(int i) {
        int i2;
        float f = (i <= 0 || (i2 = this.contentW) <= i) ? 1.0f : i / i2;
        android.graphics.Bitmap createBitmap = android.graphics.Bitmap.createBitmap(java.lang.Math.max(1, (int) (this.contentW * f)), java.lang.Math.max(1, (int) (this.contentH * f)), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(createBitmap);
        canvas.drawColor(this.bgColor);
        canvas.save();
        canvas.scale(f, f);
        java.util.Iterator<com.mori.notebook.MindNode> it = this.ordered.iterator();
        while (it.hasNext()) {
            drawChildren(canvas, it.next());
        }
        java.util.Iterator<com.mori.notebook.MindNode> it2 = this.ordered.iterator();
        while (it2.hasNext()) {
            drawNode(canvas, it2.next());
        }
        canvas.restore();
        return createBitmap;
    }
}
