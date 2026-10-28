package com.mori.notebook;

/* loaded from: classes.dex */
public final class ExportUtils {
    private ExportUtils() {
    }

    public static android.content.Intent textIntent(java.lang.String str) {
        android.content.Intent intent = new android.content.Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("text/plain");
        if (!str.endsWith(".txt")) {
            str = str + ".txt";
        }
        intent.putExtra("android.intent.extra.TITLE", str);
        return intent;
    }

    public static android.content.Intent imageIntent(java.lang.String str) {
        android.content.Intent intent = new android.content.Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("image/png");
        if (!str.endsWith(".png")) {
            str = str + ".png";
        }
        intent.putExtra("android.intent.extra.TITLE", str);
        return intent;
    }

    public static void writeText(android.content.Context context, android.net.Uri uri, java.lang.String str) throws java.lang.Exception {
        java.io.OutputStream openOutputStream = context.getContentResolver().openOutputStream(uri);
        if (openOutputStream == null) {
            throw new java.lang.Exception("无法写入");
        }
        if (str == null) {
            str = "";
        }
        try {
            openOutputStream.write(str.getBytes("UTF-8"));
            openOutputStream.flush();
            openOutputStream.close();
        } catch (java.lang.Throwable th) {
            openOutputStream.close();
            throw th;
        }
    }

    public static void writeBitmap(android.content.Context context, android.net.Uri uri, android.graphics.Bitmap bitmap) throws java.lang.Exception {
        java.io.OutputStream openOutputStream = context.getContentResolver().openOutputStream(uri);
        if (openOutputStream == null) {
            throw new java.lang.Exception("无法写入");
        }
        try {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, openOutputStream);
            openOutputStream.flush();
        } finally {
            openOutputStream.close();
        }
    }

    public static android.graphics.Bitmap renderMarkdown(android.content.Context context, java.lang.String str, int i) {
        android.widget.TextView textView = new android.widget.TextView(context);
        int i2 = (int) (context.getResources().getDisplayMetrics().density * 20.0f);
        textView.setPadding(i2, i2, i2, i2);
        textView.setTextSize(2, 15.0f);
        textView.setTypeface(android.graphics.Typeface.DEFAULT);
        textView.setTextColor(-14935265);
        textView.setBackgroundColor(-1);
        if (str == null) {
            str = "";
        }
        textView.setText(com.mori.notebook.MarkdownRenderer.render(str));
        textView.measure(android.view.View.MeasureSpec.makeMeasureSpec(i, androidx.constraintlayout.solver.widgets.analyzer.BasicMeasure.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(0, 0));
        int max = java.lang.Math.max(textView.getMeasuredHeight(), 1);
        textView.layout(0, 0, i, max);
        android.graphics.Bitmap createBitmap = android.graphics.Bitmap.createBitmap(i, max, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(createBitmap);
        canvas.drawColor(-1);
        textView.draw(canvas);
        return createBitmap;
    }

    public static android.graphics.Bitmap renderMind(android.content.Context context, java.util.List<com.mori.notebook.MindNode> list, int i) {
        com.mori.notebook.MindMapView mindMapView = new com.mori.notebook.MindMapView(context);
        mindMapView.setRoots(list);
        return mindMapView.drawToBitmap(i);
    }
}
