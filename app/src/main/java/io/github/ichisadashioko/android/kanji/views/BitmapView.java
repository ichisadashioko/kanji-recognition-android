package io.github.ichisadashioko.android.kanji.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BitmapView extends View {
    public Bitmap bitmap;

    public BitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bitmap = null;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (this.bitmap != null) {
            int bitmapWidth = this.bitmap.getWidth();
            int bitmapHeight = this.bitmap.getHeight();
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            float widthScale = (float) viewWidth / (float) bitmapWidth;
            float heightScale = (float) viewHeight / (float) bitmapHeight;
            float scale = Math.min(widthScale, heightScale);
            float renderWidth = bitmapWidth * scale;
            float renderHeight = bitmapHeight * scale;
            float offsetLeft = (viewWidth - renderWidth) / 2f;
            float offsetTop = (viewHeight - renderHeight) / 2f;
            float renderRight = offsetLeft + renderWidth;
            float renderBottom = offsetTop + renderHeight;

            int iLeft = Math.round(offsetLeft);
            int iRight = Math.round(renderRight);
            int iTop = Math.round(offsetTop);
            int iBottom = Math.round(renderBottom);
            Rect dest = new Rect(iLeft, iTop, iRight, iBottom);
            canvas.drawBitmap(this.bitmap, null, dest, null);
        }
    }
}
