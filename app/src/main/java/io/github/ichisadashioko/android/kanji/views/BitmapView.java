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
            int compWidth = getWidth();
            int compHeight = getHeight();

            float widthRatio = (float) bitmapWidth / (float) compWidth;
            float heightRatio = (float) bitmapHeight / (float) compHeight;
            float ratio = Math.min(widthRatio, heightRatio);
            float renderWidth = compWidth * ratio;
            float renderHeight = compHeight * ratio;
            float offsetLeft = (compWidth - renderWidth) / 2f;
            float offsetTop = (compHeight - renderHeight) / 2f;
            float renderRight = offsetLeft + renderWidth;
            float renderBottom = offsetTop + renderHeight;
            Rect dest =
                    new Rect(
                            Math.round(offsetLeft),
                            Math.round(offsetTop),
                            Math.round(renderRight),
                            Math.round(renderBottom));
            canvas.drawBitmap(this.bitmap, null, dest, null);
        }
    }
}
