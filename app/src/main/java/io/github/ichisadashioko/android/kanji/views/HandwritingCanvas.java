package io.github.ichisadashioko.android.kanji.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HandwritingCanvas extends View {
    public static final int VIEW_BACKGROUND_COLOR = Color.argb(255, 22, 22, 22);
    public static final int STROKE_COLOR = Color.WHITE;

    public TouchCallback touchCallback;

    /** We don't support multi-touch so this variable is used to indicate the writing state. */
    public boolean penDown;

    public Rect viewRect;
    public Paint viewBackgroundPaint;

    public Paint writingStrokePaint;

    public Path strokePath;

    /**
     * A List used for storing touch points.
     *
     * <p>I use `LinkedList` because it seems to faster than `ArrayList` in `add` operation.
     *
     * <p>https://dzone.com/articles/arraylist-vs-linkedlist-vs
     */
    public List<CanvasPoint2D> currentStroke;

    public List<List<CanvasPoint2D>> writingStrokes;

    public HandwritingCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        penDown = false;

        viewRect = new Rect(0, 0, 1, 1);
        viewBackgroundPaint = new Paint();
        viewBackgroundPaint.setColor(VIEW_BACKGROUND_COLOR);
        viewBackgroundPaint.setStyle(Paint.Style.FILL);
        viewBackgroundPaint.setAntiAlias(false);

        writingStrokePaint = new Paint();
        writingStrokePaint.setColor(STROKE_COLOR);
        writingStrokePaint.setStyle(Paint.Style.STROKE);
        writingStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        writingStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        writingStrokePaint.setAntiAlias(true);
        // TODO set stroke width
        //        writingStrokePaint.setStrokeWidth();

        strokePath = new Path();
        currentStroke = null;
        writingStrokes = Collections.synchronizedList(new LinkedList<>());
    }

    public void setStrokeWidth(float value) {
        writingStrokePaint.setStrokeWidth(value);
    }

    public void clearCanvas() {
        writingStrokes.clear();
        strokePath.reset();
        invalidate();
    }

    public void actionDown(CanvasPoint2D p) {
        if (!penDown) {
            penDown = true;
            currentStroke = Collections.synchronizedList(new LinkedList<>());
            writingStrokes.add(currentStroke);
            currentStroke.add(p);
            strokePath.moveTo(p.x, p.y);
        }
    }

    public void actionUp(CanvasPoint2D p) {
        if (penDown) {
            currentStroke = null;
            penDown = false;
        }
    }

    public void actionMove(CanvasPoint2D p) {
        if (penDown) {
            currentStroke.add(p);
            strokePath.lineTo(p.x, p.y);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        CanvasPoint2D touchPos =
                new CanvasPoint2D(Math.round(event.getX()), Math.round(event.getY()));

        if (action == MotionEvent.ACTION_DOWN) {
            actionDown(touchPos);
        } else if (action == MotionEvent.ACTION_UP) {
            actionUp(touchPos);
        } else if (action == MotionEvent.ACTION_MOVE) {
            actionMove(touchPos);
        }

        invalidate();

        if (touchCallback != null) {
            touchCallback.onTouchEnd();
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(viewRect, viewBackgroundPaint);
        canvas.drawPath(strokePath, writingStrokePaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewRect = new Rect(0, 0, w, h);
        invalidate();
    }
}
