package io.github.ichisadashioko.android.kanji;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;

import java.util.Iterator;
import java.util.List;

public class RenderingUtils {
    public static int RenderedImageWidth = 64;
    public static int RenderedImageHeight = 64;
    public static float RenderedImagePaddingRatio = 0.125f;
    public static float RenderedStrokeWidthRatio = 0.0625f;
    public static int BackgroundColor = Color.BLACK;
    public static int StrokeColor = Color.WHITE;

    public static Bitmap renderImageFromStrokes(List<List<CanvasPoint2D>> strokes) {
        Bitmap outputImage =
                Bitmap.createBitmap(
                        RenderedImageWidth, RenderedImageHeight, Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(outputImage);
        Path renderedPath = new Path();

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(BackgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        imageCanvas.drawRect(
                new Rect(0, 0, RenderedImageWidth, RenderedImageHeight), backgroundPaint);

        Paint strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(StrokeColor);
        strokePaint.setStrokeWidth(RenderedImageWidth * RenderedStrokeWidthRatio);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        int minX = Integer.MAX_VALUE,
                maxX = Integer.MIN_VALUE,
                minY = Integer.MAX_VALUE,
                maxY = Integer.MIN_VALUE;
        Iterator<List<CanvasPoint2D>> strokeIter;
        Iterator<CanvasPoint2D> pointIter;
        CanvasPoint2D point2D;
        List<CanvasPoint2D> stroke;

        strokeIter = strokes.iterator();

        while (strokeIter.hasNext()) {
            stroke = strokeIter.next();
            pointIter = stroke.iterator();

            while (pointIter.hasNext()) {
                point2D = pointIter.next();

                minX = Math.min(point2D.x, minX);
                maxX = Math.max(point2D.x, maxX);

                minY = Math.min(point2D.y, minY);
                maxY = Math.max(point2D.y, maxY);
            }
        }

        // TODO handle negative coordinates
        if (minX < 0) {
            minX = 0;
        }

        if (minY < 0) {
            minY = 0;
        }

        int charWidth = maxX - minX;
        int charHeight = maxY - minY;

        int offsetLeft = Math.round((float) RenderedImageWidth * RenderedImagePaddingRatio);
        int renderWidth = RenderedImageWidth - (2 * offsetLeft);

        int offsetTop = Math.round((float) RenderedImageHeight * RenderedImagePaddingRatio);
        int renderHeight = RenderedImageHeight - (2 * offsetTop);

        float scaleRatioX = (float) renderWidth / (float) charWidth;
        float scaleRatioY = (float) renderHeight / (float) charHeight;
        float scaleRatio = Math.min(scaleRatioX, scaleRatioY);

        int centeringOffsetX =
                offsetLeft + Math.round((renderWidth - (charWidth * scaleRatio)) / 2f);
        int centeringOffsetY =
                offsetTop + Math.round((renderHeight - (charHeight * scaleRatio)) / 2f);

        strokeIter = strokes.iterator();

        boolean isFirstPoint;

        while (strokeIter.hasNext()) {
            stroke = strokeIter.next();
            isFirstPoint = true;
            pointIter = stroke.iterator();

            while (pointIter.hasNext()) {
                point2D = pointIter.next();

                int scaledX = centeringOffsetX + Math.round((point2D.x - minX) * scaleRatio);
                int scaledY = centeringOffsetY + Math.round((point2D.y - minY) * scaleRatio);

                if (isFirstPoint) {
                    isFirstPoint = false;
                    renderedPath.moveTo(scaledX, scaledY);
                } else {
                    renderedPath.lineTo(scaledX, scaledY);
                }
            }
        }

        imageCanvas.drawPath(renderedPath, strokePaint);

        return outputImage;
    }
}
