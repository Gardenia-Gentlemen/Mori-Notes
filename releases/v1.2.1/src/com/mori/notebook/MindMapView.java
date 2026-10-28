package com.mori.notebook;

/* loaded from: classes.dex */
public class MindMapView extends android.view.View {
    private float boxH;
    private final android.graphics.Paint boxPaint;
    private float boxW;
    private int cOne;
    private int cRoot;
    private int cText;
    private int cTwo;
    private float colW;
    private float density;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> depths;
    private final android.graphics.Paint linePaint;
    private int maxDepth;
    private final java.util.List<com.mori.notebook.MindNode> ordered;
    private final java.util.Map<com.mori.notebook.MindNode, android.graphics.RectF> rects;
    private final java.util.List<com.mori.notebook.MindNode> roots;
    private float rowH;
    private final android.text.TextPaint textPaint;
    private final android.graphics.RectF tmp;

    public MindMapView(android.content.Context context) {
        this(context, null);
    }

    public MindMapView(android.content.Context context, android.util.AttributeSet attributeSet) {
        super(context, attributeSet);
        this.roots = new java.util.ArrayList();
        this.rects = new java.util.HashMap();
        this.depths = new java.util.HashMap();
        this.ordered = new java.util.ArrayList();
        this.boxPaint = new android.graphics.Paint(1);
        this.linePaint = new android.graphics.Paint(1);
        this.textPaint = new android.text.TextPaint(1);
        this.tmp = new android.graphics.RectF();
        this.maxDepth = 0;
        init();
    }

    private void init() {
        float f = getResources().getDisplayMetrics().density;
        this.density = f;
        this.colW = 176.0f * f;
        this.rowH = 54.0f * f;
        this.boxW = 148.0f * f;
        this.boxH = 38.0f * f;
        this.textPaint.setTextSize(f * 13.0f);
        this.linePaint.setStyle(android.graphics.Paint.Style.STROKE);
        this.linePaint.setStrokeWidth(this.density * 1.6f);
        this.boxPaint.setStyle(android.graphics.Paint.Style.FILL);
        this.cRoot = attrColor(com.google.android.material.R.attr.colorPrimaryContainer, -1384961);
        this.cOne = attrColor(com.google.android.material.R.attr.colorSecondaryContainer, -1515784);
        this.cTwo = attrColor(com.google.android.material.R.attr.colorSurfaceContainerHighest, -1646359);
        int attrColor = attrColor(com.google.android.material.R.attr.colorOnSurface, -14935265);
        this.cText = attrColor;
        this.textPaint.setColor(attrColor);
        this.linePaint.setColor(this.cText);
        this.linePaint.setAlpha(110);
    }

    private int attrColor(int i, int i2) {
        android.content.res.TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{i});
        int color = obtainStyledAttributes.getColor(0, i2);
        obtainStyledAttributes.recycle();
        return color;
    }

    public void setRoots(java.util.List<com.mori.notebook.MindNode> list) {
        this.roots.clear();
        if (list != null) {
            this.roots.addAll(list);
        }
        relayout();
    }

    private void relayout() {
        this.rects.clear();
        this.depths.clear();
        this.ordered.clear();
        this.maxDepth = 0;
        float[] fArr = {0.0f};
        java.util.Iterator<com.mori.notebook.MindNode> it = this.roots.iterator();
        while (it.hasNext()) {
            place(it.next(), 0, fArr);
        }
        requestLayout();
        invalidate();
    }

    private float place(com.mori.notebook.MindNode mindNode, int i, float[] fArr) {
        float f;
        this.depths.put(mindNode, java.lang.Integer.valueOf(i));
        this.ordered.add(mindNode);
        if (i > this.maxDepth) {
            this.maxDepth = i;
        }
        if (mindNode.children.isEmpty()) {
            float f2 = fArr[0];
            f = this.rowH * f2;
            fArr[0] = f2 + 1.0f;
        } else {
            java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
            float f3 = 0.0f;
            int i2 = 0;
            while (it.hasNext()) {
                f3 += place(it.next(), i + 1, fArr);
                i2++;
            }
            f = i2 == 0 ? fArr[0] * this.rowH : f3 / i2;
            if (i2 == 0) {
                fArr[0] = fArr[0] + 1.0f;
            }
        }
        float f4 = (this.density * 16.0f) + (i * this.colW);
        this.rects.put(mindNode, new android.graphics.RectF(f4, f, this.boxW + f4, this.boxH + f));
        return f;
    }

    private int contentWidth() {
        float f = this.density;
        return (int) ((f * 16.0f) + ((this.maxDepth + 1) * this.colW) + (f * 16.0f));
    }

    private int contentHeight() {
        float f = 0.0f;
        while (this.roots.iterator().hasNext()) {
            f += java.lang.Math.max(1, countLeaves(r0.next()));
        }
        return (int) ((((int) java.lang.Math.max(1.0f, f)) * this.rowH) + (this.density * 24.0f));
    }

    private int countLeaves(com.mori.notebook.MindNode mindNode) {
        if (mindNode.children.isEmpty()) {
            return 1;
        }
        java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
        int i = 0;
        while (it.hasNext()) {
            i += countLeaves(it.next());
        }
        return i;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(java.lang.Math.max(resolveSize(contentWidth(), i), 1), java.lang.Math.max(resolveSize(contentHeight(), i2), 1));
    }

    @Override // android.view.View
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        if (this.roots.isEmpty()) {
            return;
        }
        canvas.drawColor(attrColor(com.google.android.material.R.attr.colorSurface, -1));
        java.util.Iterator<com.mori.notebook.MindNode> it = this.ordered.iterator();
        while (it.hasNext()) {
            drawLines(canvas, it.next());
        }
        java.util.Iterator<com.mori.notebook.MindNode> it2 = this.ordered.iterator();
        while (it2.hasNext()) {
            drawBox(canvas, it2.next());
        }
    }

    private void drawLines(android.graphics.Canvas canvas, com.mori.notebook.MindNode mindNode) {
        android.graphics.RectF rectF = this.rects.get(mindNode);
        if (rectF == null) {
            return;
        }
        java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
        while (it.hasNext()) {
            android.graphics.RectF rectF2 = this.rects.get(it.next());
            if (rectF2 != null) {
                float f = rectF.right;
                float centerY = rectF.centerY();
                float f2 = rectF2.left;
                float centerY2 = rectF2.centerY();
                float f3 = (f + f2) / 2.0f;
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(f, centerY);
                path.cubicTo(f3, centerY, f3, centerY2, f2, centerY2);
                canvas.drawPath(path, this.linePaint);
            }
        }
    }

    private void drawBox(android.graphics.Canvas canvas, com.mori.notebook.MindNode mindNode) {
        android.graphics.RectF rectF = this.rects.get(mindNode);
        if (rectF == null) {
            return;
        }
        int intValue = this.depths.containsKey(mindNode) ? this.depths.get(mindNode).intValue() : 0;
        this.boxPaint.setColor(intValue == 0 ? this.cRoot : intValue == 1 ? this.cOne : this.cTwo);
        this.tmp.set(rectF);
        android.graphics.RectF rectF2 = this.tmp;
        float f = this.density;
        canvas.drawRoundRect(rectF2, f * 12.0f, f * 12.0f, this.boxPaint);
        java.lang.String obj = android.text.TextUtils.ellipsize(mindNode.text == null ? "" : mindNode.text.trim(), this.textPaint, this.boxW - (this.density * 16.0f), android.text.TextUtils.TruncateAt.END).toString();
        android.graphics.Paint.FontMetrics fontMetrics = this.textPaint.getFontMetrics();
        canvas.drawText(obj, rectF.centerX() - (this.textPaint.measureText(obj) / 2.0f), rectF.centerY() - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.textPaint);
    }

    public android.graphics.Bitmap drawToBitmap(int i) {
        measure(android.view.View.MeasureSpec.makeMeasureSpec(i, Integer.MIN_VALUE), android.view.View.MeasureSpec.makeMeasureSpec(0, 0));
        int max = java.lang.Math.max(getMeasuredWidth(), (int) (i * 0.5f));
        int max2 = java.lang.Math.max(getMeasuredHeight(), 1);
        layout(0, 0, max, max2);
        android.graphics.Bitmap createBitmap = android.graphics.Bitmap.createBitmap(max, max2, android.graphics.Bitmap.Config.ARGB_8888);
        draw(new android.graphics.Canvas(createBitmap));
        return createBitmap;
    }
}
