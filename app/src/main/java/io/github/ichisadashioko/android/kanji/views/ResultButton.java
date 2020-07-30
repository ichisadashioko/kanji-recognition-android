package io.github.ichisadashioko.android.kanji.views;

import android.content.Context;
import android.graphics.*;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class ResultButton extends View
{
    public static final int LABEL_SIZE_RATIO_BITS_SHIFT      = 1;
    public static final int CONFIDENCE_SIZE_RATIO_BITS_SHIFT = 2;
    public static Typeface LABEL_FONT                        = Typeface.MONOSPACE;
    public static Typeface CONFIDENCE_FONT                   = Typeface.MONOSPACE;
    public final String label;
    public final float confidence;

    public TextPaint mTextPaint;
    public int labelFontSize;
    public float labelTextWidth;
    public float labelX;
    public float labelY;
    public int confidenceFontSize;
    public float confidenceTextWidth;
    public float confidenceX;
    public float confidenceY;

    public ResultButton(Context context, AttributeSet attrs, String label, float confidence)
    {
        super(context, attrs);
        this.label      = label;
        this.confidence = confidence;

        mTextPaint = new TextPaint();
    }

    public String confidenceToString()
    {
        return String.format("%.1f%%", confidence * 100);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(labelFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        canvas.drawText(label, labelX, labelY, mTextPaint);
        mTextPaint.setTextSize(confidenceFontSize);
        mTextPaint.setTypeface(CONFIDENCE_FONT);
        canvas.drawText(confidenceToString(), confidenceX, confidenceY, mTextPaint);

        // draw bounding boxes for debug
        // Paint p = new Paint();
        // p.setStyle(Paint.Style.STROKE);
        // p.setStrokeWidth(2f);
        // p.setColor(Color.RED);
        // canvas.drawRect(labelX, labelY - labelFontSize, labelX + labelTextWidth, labelY, p);
        // p.setColor(Color.GREEN);
        // canvas.drawRect(confidenceX, confidenceY - confidenceFontSize, confidenceX + confidenceTextWidth, confidenceY, p);

        // show origin points
        // p.setStyle(Paint.Style.FILL);
        // canvas.drawCircle(labelX, labelY, 5, p);
        // canvas.drawCircle(confidenceX, confidenceY, 5, p);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        super.onSizeChanged(w, h, oldW, oldH);

        // change font size and measure text size to center them
        labelFontSize = h >> LABEL_SIZE_RATIO_BITS_SHIFT;
        mTextPaint.setTextSize(labelFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        labelTextWidth = mTextPaint.measureText(label);
        labelX         = Math.max(0, (w - labelTextWidth)) / 2;
        labelY         = h * 0.6f;

        confidenceFontSize = h >> CONFIDENCE_SIZE_RATIO_BITS_SHIFT;
        mTextPaint.setTextSize(confidenceFontSize);
        mTextPaint.setTypeface(CONFIDENCE_FONT);
        confidenceTextWidth = mTextPaint.measureText(confidenceToString());
        confidenceX         = Math.max(0, (w - confidenceTextWidth)) / 2;
        confidenceY         = h * 0.9f;
    }
}
