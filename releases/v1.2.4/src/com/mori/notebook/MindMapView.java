package com.mori.notebook;

/* loaded from: classes.dex */
public class MindMapView extends android.view.View {
    private static final int INK = -14935265;
    private static final int[] PALETTE = {-12426497, -25272, -897426, -16732000, -6598176, -865972, -14176672, -13787941};
    private final android.graphics.Paint boxPaint;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> branchIdx;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Float> centers;
    private float density;
    private float[] depthW;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Integer> depths;
    private float gapX;
    private final android.graphics.Paint linePaint;
    private int maxDepth;
    private float maxW;
    private float minW;
    private final java.util.List<com.mori.notebook.MindNode> ordered;
    private float padX;
    private final android.graphics.Path path;
    private final java.util.Map<com.mori.notebook.MindNode, android.graphics.RectF> rects;
    private final java.util.List<com.mori.notebook.MindNode> roots;
    private float rowH;
    private final android.text.TextPaint textPaint;
    private final android.graphics.RectF tmp;
    private final java.util.Map<com.mori.notebook.MindNode, java.lang.Float> widths;
    private float[] xAt;

    private static int tint(int i, float f) {
        float f2 = 1.0f - f;
        float f3 = f * 255.0f;
        return ((int) (((i & 255) * f2) + f3)) | (-16777216) | (((int) ((((i >> 16) & 255) * f2) + f3)) << 16) | (((int) ((((i >> 8) & 255) * f2) + f3)) << 8);
    }

    public MindMapView(android.content.Context context) {
        this(context, null);
    }

    public MindMapView(android.content.Context context, android.util.AttributeSet attributeSet) {
        super(context, attributeSet);
        this.roots = new java.util.ArrayList();
        this.rects = new java.util.HashMap();
        this.depths = new java.util.HashMap();
        this.branchIdx = new java.util.HashMap();
        this.widths = new java.util.HashMap();
        this.centers = new java.util.HashMap();
        this.ordered = new java.util.ArrayList();
        this.boxPaint = new android.graphics.Paint(1);
        this.linePaint = new android.graphics.Paint(1);
        this.textPaint = new android.text.TextPaint(1);
        this.tmp = new android.graphics.RectF();
        this.path = new android.graphics.Path();
        this.maxDepth = 0;
        this.depthW = new float[1];
        this.xAt = new float[1];
        init();
    }

    private void init() {
        float f = getResources().getDisplayMetrics().density;
        this.density = f;
        this.rowH = 42.0f * f;
        this.gapX = 54.0f * f;
        this.padX = 15.0f * f;
        this.minW = 68.0f * f;
        this.maxW = f * 220.0f;
        this.linePaint.setStyle(android.graphics.Paint.Style.STROKE);
        this.linePaint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        this.linePaint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        this.boxPaint.setStyle(android.graphics.Paint.Style.FILL);
        setLayerType(1, null);
    }

    private float heightFor(int i) {
        if (i == 0) {
            return this.density * 46.0f;
        }
        return this.density * (i == 1 ? 36.0f : 31.0f);
    }

    private float textSizeFor(int i) {
        if (i == 0) {
            return this.density * 16.0f;
        }
        return this.density * (i == 1 ? 14.0f : 13.0f);
    }

    private int colorOf(com.mori.notebook.MindNode mindNode) {
        java.lang.Integer num = this.branchIdx.get(mindNode);
        int intValue = num == null ? 0 : num.intValue();
        int[] iArr = PALETTE;
        return iArr[java.lang.Math.abs(intValue) % iArr.length];
    }

    public void setRoots(java.util.List<com.mori.notebook.MindNode> list) {
        this.roots.clear();
        if (list != null) {
            this.roots.addAll(list);
        }
        relayout();
    }

    private void relayout() {
        int i;
        this.rects.clear();
        this.depths.clear();
        this.branchIdx.clear();
        this.widths.clear();
        this.centers.clear();
        this.ordered.clear();
        int i2 = 0;
        this.maxDepth = 0;
        float[] fArr = {0.0f};
        for (int i3 = 0; i3 < this.roots.size(); i3++) {
            place(this.roots.get(i3), 0, i3, fArr);
        }
        this.depthW = new float[this.maxDepth + 2];
        for (com.mori.notebook.MindNode mindNode : this.ordered) {
            int intValue = this.depths.get(mindNode).intValue();
            java.lang.Integer num = this.branchIdx.get(mindNode);
            float floatValue = this.widths.get(mindNode).floatValue();
            float[] fArr2 = this.depthW;
            if (floatValue > fArr2[intValue]) {
                fArr2[intValue] = floatValue;
            }
            if (num == null) {
                this.branchIdx.put(mindNode, 0);
            }
        }
        int i4 = 0;
        while (true) {
            i = this.maxDepth;
            if (i4 > i) {
                break;
            }
            float[] fArr3 = this.depthW;
            float f = fArr3[i4];
            float f2 = this.minW;
            if (f < f2) {
                fArr3[i4] = f2;
            }
            i4++;
        }
        float[] fArr4 = new float[i + 2];
        this.xAt = fArr4;
        fArr4[0] = this.density * 20.0f;
        while (i2 <= this.maxDepth) {
            float[] fArr5 = this.xAt;
            int i5 = i2 + 1;
            fArr5[i5] = fArr5[i2] + this.depthW[i2] + this.gapX;
            i2 = i5;
        }
        for (com.mori.notebook.MindNode mindNode2 : this.ordered) {
            int intValue2 = this.depths.get(mindNode2).intValue();
            float floatValue2 = this.widths.get(mindNode2).floatValue();
            float heightFor = heightFor(intValue2);
            float floatValue3 = this.centers.get(mindNode2).floatValue();
            java.util.Map<com.mori.notebook.MindNode, android.graphics.RectF> map = this.rects;
            float f3 = this.xAt[intValue2];
            float f4 = heightFor / 2.0f;
            map.put(mindNode2, new android.graphics.RectF(f3, floatValue3 - f4, floatValue2 + f3, floatValue3 + f4));
        }
        requestLayout();
        invalidate();
    }

    private float place(com.mori.notebook.MindNode mindNode, int i, int i2, float[] fArr) {
        float f;
        this.depths.put(mindNode, java.lang.Integer.valueOf(i));
        if (i == 0) {
            this.branchIdx.put(mindNode, java.lang.Integer.valueOf(i2));
        } else {
            this.branchIdx.put(mindNode, java.lang.Integer.valueOf(i2));
        }
        this.ordered.add(mindNode);
        if (i > this.maxDepth) {
            this.maxDepth = i;
        }
        this.widths.put(mindNode, java.lang.Float.valueOf(measureWidth(mindNode, i)));
        if (mindNode.children.isEmpty()) {
            float f2 = fArr[0];
            f = this.rowH * f2;
            fArr[0] = f2 + 1.0f;
        } else {
            java.util.Iterator<com.mori.notebook.MindNode> it = mindNode.children.iterator();
            float f3 = 0.0f;
            int i3 = 0;
            while (it.hasNext()) {
                f3 += place(it.next(), i + 1, i2, fArr);
                i3++;
            }
            f = i3 == 0 ? fArr[0] * this.rowH : f3 / i3;
            if (i3 == 0) {
                fArr[0] = fArr[0] + 1.0f;
            }
        }
        this.centers.put(mindNode, java.lang.Float.valueOf(f));
        return f;
    }

    private float measureWidth(com.mori.notebook.MindNode mindNode, int i) {
        this.textPaint.setTextSize(textSizeFor(i));
        java.lang.String trim = mindNode.text == null ? "" : mindNode.text.toString().trim();
        if (trim.isEmpty()) {
            trim = " ";
        }
        float measureText = this.textPaint.measureText(trim) + (this.padX * 2.0f);
        float f = this.minW;
        if (measureText < f) {
            measureText = f;
        }
        float f2 = this.maxW;
        return measureText > f2 ? f2 : measureText;
    }

    private int contentWidth() {
        return (int) (this.xAt[java.lang.Math.min(this.maxDepth, r0.length - 1)] + this.depthW[java.lang.Math.min(this.maxDepth, r1.length - 1)] + (this.density * 24.0f));
    }

    private int contentHeight() {
        float f = 0.0f;
        while (this.roots.iterator().hasNext()) {
            f += java.lang.Math.max(1, countLeaves(r0.next()));
        }
        return (int) ((((int) java.lang.Math.max(1.0f, f)) * this.rowH) + (this.density * 28.0f));
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
        canvas.drawColor(-1);
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
        int intValue = this.depths.get(mindNode).intValue();
        for (com.mori.notebook.MindNode mindNode2 : mindNode.children) {
            android.graphics.RectF rectF2 = this.rects.get(mindNode2);
            if (rectF2 != null) {
                float f = rectF.right;
                float centerY = rectF.centerY();
                float f2 = rectF2.left;
                float centerY2 = rectF2.centerY();
                float f3 = (f + f2) / 2.0f;
                this.linePaint.setColor(colorOf(mindNode2));
                this.linePaint.setStrokeWidth((intValue == 0 ? 3.2f : 2.2f) * this.density);
                this.path.reset();
                this.path.moveTo(f, centerY);
                this.path.cubicTo(f3, centerY, f3, centerY2, f2, centerY2);
                canvas.drawPath(this.path, this.linePaint);
            }
        }
    }

    private void drawBox(android.graphics.Canvas canvas, com.mori.notebook.MindNode mindNode) {
        android.graphics.RectF rectF = this.rects.get(mindNode);
        if (rectF == null) {
            return;
        }
        int intValue = this.depths.get(mindNode).intValue();
        int colorOf = colorOf(mindNode);
        float f = this.density * (intValue == 0 ? 14.0f : 18.0f);
        this.boxPaint.clearShadowLayer();
        this.boxPaint.setStyle(android.graphics.Paint.Style.FILL);
        if (intValue == 0) {
            this.boxPaint.setColor(colorOf);
            android.graphics.Paint paint = this.boxPaint;
            float f2 = this.density;
            paint.setShadowLayer(6.0f * f2, 0.0f, f2 * 3.0f, 855638016);
            this.tmp.set(rectF);
            canvas.drawRoundRect(this.tmp, f, f, this.boxPaint);
            this.boxPaint.clearShadowLayer();
        } else if (intValue == 1) {
            this.boxPaint.setColor(-1);
            android.graphics.Paint paint2 = this.boxPaint;
            float f3 = this.density;
            paint2.setShadowLayer(5.0f * f3, 0.0f, f3 * 2.0f, 637534208);
            this.tmp.set(rectF);
            canvas.drawRoundRect(this.tmp, f, f, this.boxPaint);
            this.boxPaint.clearShadowLayer();
            this.boxPaint.setStyle(android.graphics.Paint.Style.STROKE);
            this.boxPaint.setStrokeWidth(this.density * 2.0f);
            this.boxPaint.setColor(colorOf);
            canvas.drawRoundRect(this.tmp, f, f, this.boxPaint);
            this.boxPaint.setStyle(android.graphics.Paint.Style.FILL);
        } else {
            this.boxPaint.setColor(tint(colorOf, 0.86f));
            this.tmp.set(rectF);
            canvas.drawRoundRect(this.tmp, f, f, this.boxPaint);
        }
        this.textPaint.setTextSize(textSizeFor(intValue));
        this.textPaint.setFakeBoldText(intValue == 0);
        this.textPaint.setColor(intValue != 0 ? INK : -1);
        java.lang.String obj = android.text.TextUtils.ellipsize(mindNode.text == null ? "" : mindNode.text.toString().trim(), this.textPaint, rectF.width() - (this.padX * 2.0f), android.text.TextUtils.TruncateAt.END).toString();
        android.graphics.Paint.FontMetrics fontMetrics = this.textPaint.getFontMetrics();
        canvas.drawText(obj, rectF.centerX() - (this.textPaint.measureText(obj) / 2.0f), rectF.centerY() - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.textPaint);
        this.textPaint.setFakeBoldText(false);
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
