package com.mori.notebook;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import java.io.OutputStream;
import java.util.List;

public final class ExportUtils {

    private ExportUtils() {
    }

    public static Intent textIntent(String filename) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, filename.endsWith(".txt") ? filename : filename + ".txt");
        return i;
    }

    public static Intent imageIntent(String filename) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/png");
        i.putExtra(Intent.EXTRA_TITLE, filename.endsWith(".png") ? filename : filename + ".png");
        return i;
    }

    public static void writeText(Context ctx, Uri uri, String text) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        OutputStream os = cr.openOutputStream(uri);
        if (os == null) throw new Exception("无法写入");
        try {
            os.write((text == null ? "" : text).getBytes("UTF-8"));
            os.flush();
        } finally {
            os.close();
        }
    }

    public static void writeBitmap(Context ctx, Uri uri, Bitmap bmp) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        OutputStream os = cr.openOutputStream(uri);
        if (os == null) throw new Exception("无法写入");
        try {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
        } finally {
            os.close();
        }
    }

    public static Bitmap renderMarkdown(Context ctx, String markdown, int widthPx) {
        TextView tv = new TextView(ctx);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT);
        tv.setTextColor(0xFF1C1B1F);
        tv.setBackgroundColor(0xFFFFFFFF);
        tv.setText(MarkdownRenderer.render(markdown == null ? "" : markdown));
        int wSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tv.measure(wSpec, hSpec);
        int w = widthPx;
        int h = Math.max(tv.getMeasuredHeight(), 1);
        tv.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(0xFFFFFFFF);
        tv.draw(canvas);
        return bmp;
    }

    public static Bitmap renderMind(Context ctx, List<MindNode> roots, int widthPx) {
        MindMapView view = new MindMapView(ctx);
        view.setRoots(roots);
        return view.drawToBitmap(widthPx);
    }
}
