package io.github.ichisadashioko.android.kanji.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HandwritingCanvas extends View {
    public static final int IMAGE_WIDTH = 64;
    public static final int IMAGE_HEIGHT = 64;
    private static final Rect IMAGE_RECT = new Rect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    public static final int VIEW_BACKGROUND_COLOR = Color.argb(255, 22, 22, 22);
    public static final int STROKE_COLOR = Color.WHITE;
    public static final int IMAGE_BACKGROUND_COLOR = Color.BLACK;
    public static final float STROKE_WIDTH = 2.5f;

    // variables for scaling the `canvasImage` on the View
    private int imageScale;
    private Point imageOffset;
    private Rect scaledRect;

    /**
     * We don't support multi-touch so this variable is used to indicate the writing
     * state.
     */
    private boolean penDown;

    /**
     * `canvasImage` is the area that the user will write/draw on. It will also be
     * used to export the data for using in the recognition interpreter.
     */
    private Bitmap canvasImage;
    /**
     * We cannot draw directly on `canvasImage`. We have to draw on a `Canvas` that
     * wraps our `canvasImage`.
     */
    private Canvas drawingCanvas;
    /**
     * `imagePath` is used to store information about what user had drawn on the
     * View. It is then later be used to render on the `canvasImage`.
     */
    private Path imagePath;

    public HandwritingCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        canvasImage = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        drawingCanvas = new Canvas(canvasImage);
        imagePath = new Path();

        imageOffset = new Point();
        imageScale = 1;
        scaledRect = new Rect(IMAGE_RECT);
        clearCanvas();

        penDown = false;
    }

    public void clearCanvas() {
        imagePath.reset();
        invalidate();
    }

    private float scaleBitmapX(float x) {
        return (x - imageOffset.x) / imageScale;
    }

    private float scaleBitmapY(float y) {
        return (y - imageOffset.y) / imageScale;
    }

    private void actionDown(float x, float y) {
        if (!penDown) {
            penDown = true;
            imagePath.moveTo(scaleBitmapX(x), scaleBitmapY(y));
        }
    }

    private void actionUp() {
        penDown = false;
    }

    private void actionMove(float x, float y) {
        if (penDown) {
            imagePath.lineTo(scaleBitmapX(x), scaleBitmapY(y));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                actionDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(event.getX(), event.getY());
                break;
        }
        invalidate();
        return true;
    }

    private void drawViewBackground(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(VIEW_BACKGROUND_COLOR);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);

        Rect rect = new Rect(0, 0, getWidth(), getHeight());
        canvas.drawRect(rect, paint);
    }

    private void drawImageBackground() {
        Paint paint = new Paint();
        paint.setColor(IMAGE_BACKGROUND_COLOR);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);

        drawingCanvas.drawRect(IMAGE_RECT, paint);
    }

    private void drawStrokes() {
        Paint paint = new Paint();
        paint.setColor(STROKE_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setAntiAlias(false);

        drawingCanvas.drawPath(imagePath, paint);
    }

    private void prepareImage() {
        drawImageBackground();
        drawStrokes();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawViewBackground(canvas);
        prepareImage();
        canvas.drawBitmap(canvasImage, IMAGE_RECT, scaledRect, null);
    }

    private void modifyImageScale(int viewWidth, int viewHeight) {
        imageScale = Math.min((viewWidth / IMAGE_WIDTH), (viewHeight / IMAGE_HEIGHT));
        int offsetX = ((viewWidth - (IMAGE_WIDTH * imageScale)) / 2);
        int offsetY = ((viewHeight - (IMAGE_HEIGHT * imageScale)) / 2);

        imageOffset.set(offsetX, offsetY);
        scaledRect.set(offsetX, offsetY, offsetX + (IMAGE_WIDTH * imageScale), offsetY + (IMAGE_HEIGHT * imageScale));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        modifyImageScale(w, h);
        invalidate();
    }

    public Bitmap getImage() {
        return canvasImage;
    }
}