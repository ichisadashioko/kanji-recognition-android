package io.github.ichisadashioko.android.kanji.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class ResultButton extends View
{
    public static final int LABEL_SIZE_RATIO_BITS_SHIFT      = 1;
    public static final int CONFIDENCE_SIZE_RATIO_BITS_SHIFT = 2;

    public static Typeface LABEL_FONT      = Typeface.MONOSPACE;
    public static Typeface CONFIDENCE_FONT = Typeface.MONOSPACE;

    public static String HIRAGANA_CHARS = "あいうえおぁぃぅぇぉかきくけこがぎぐげごさしすせそざじずぜぞたちつてとっだぢづでどなにぬねのまみむめもはひふへほばびぶべぼぱぴぷぺぽやゆよゃゅょらりるれろわん";
    public static String KATAKANA_CHARS = "アイウエオァィゥェォカキクケコガギグゲゴサシスセソザジズゼゾタチツテトッダヂヅデドナニヌネノマミムメモハヒフヘホバビブベボパピプペポヤユヨャュョラリルレロワンー";

    public final String label;
    public final float confidence;

    public TextPaint mTextPaint;

    public int labelFontSize;
    public float labelTextWidth;
    public float labelLeft;
    public float labelBottom;

    public int confidenceFontSize;
    public float confidenceTextWidth;
    public float confidenceLeft;
    public float confidenceBottom;

    public String charTypeHintText;
    public int hintFontSize;
    public float hintTextWidth;
    public float hintTextLeft;
    public float hintTextBottom;

    public ResultButton(Context context, AttributeSet attrs, String label, float confidence)
    {
        super(context, attrs);
        this.label      = label;
        this.confidence = confidence;

        this.charTypeHintText = null;

        for (int i = 0; i < HIRAGANA_CHARS.length(); i++)
        {
            if (this.label.equals(HIRAGANA_CHARS.substring(i, i + 1)))
            {
                this.charTypeHintText = "あ";
                break;
            }
        }

        if (charTypeHintText == null)
        {
            for (int i = 0; i < KATAKANA_CHARS.length(); i++)
            {
                if (this.label.equals(KATAKANA_CHARS.substring(i, i + 1)))
                {
                    this.charTypeHintText = "ア";
                    break;
                }
            }
        }

        if (charTypeHintText == null)
        {
            charTypeHintText = "漢";
        }

        mTextPaint = new TextPaint();
    }

    public String confidenceToString()
    {
        return String.format(Locale.ENGLISH, "%.1f%%", confidence * 100);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        mTextPaint.setColor(Color.argb(50, 255, 255, 255));
        mTextPaint.setTextSize(hintFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        canvas.drawText(charTypeHintText, hintTextLeft, hintTextBottom, mTextPaint);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(labelFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        canvas.drawText(label, labelLeft, labelBottom, mTextPaint);
        mTextPaint.setTextSize(confidenceFontSize);
        mTextPaint.setTypeface(CONFIDENCE_FONT);
        canvas.drawText(confidenceToString(), confidenceLeft, confidenceBottom, mTextPaint);

        // draw bounding boxes for debug
        // Paint p = new Paint();
        // p.setStyle(Paint.Style.STROKE);
        // p.setStrokeWidth(2f);
        // p.setColor(Color.RED);
        // canvas.drawRect(labelLeft, labelBottom - labelFontSize, labelLeft + labelTextWidth, labelBottom, p);
        // p.setColor(Color.GREEN);
        // canvas.drawRect(confidenceLeft, confidenceBottom - confidenceFontSize, confidenceLeft + confidenceTextWidth, confidenceBottom, p);
        // p.setColor(Color.BLUE);
        // canvas.drawRect(hintTextLeft, hintTextBottom - hintFontSize, hintTextLeft + hintTextWidth, hintTextBottom, p);

        // show origin points
        // p.setStyle(Paint.Style.FILL);
        // canvas.drawCircle(labelLeft, labelBottom, 5, p);
        // canvas.drawCircle(confidenceLeft, confidenceBottom, 5, p);
        // canvas.drawCircle(hintTextLeft, hintTextBottom, 5, p);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        super.onSizeChanged(w, h, oldW, oldH);
        hintFontSize = Math.min(w, h);
        mTextPaint.setTextSize(hintFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        hintTextWidth  = mTextPaint.measureText(charTypeHintText);
        hintTextLeft   = Math.max(0, (w - hintTextWidth)) / 2f;
        hintTextBottom = (h - ((h - hintFontSize) / 2f)) * 0.9f;

        // change font size and measure text size to center them
        labelFontSize = h >> LABEL_SIZE_RATIO_BITS_SHIFT;
        mTextPaint.setTextSize(labelFontSize);
        mTextPaint.setTypeface(LABEL_FONT);
        labelTextWidth = mTextPaint.measureText(label);
        labelLeft      = Math.max(0, (w - labelTextWidth)) / 2;
        labelBottom    = h * 0.6f;

        confidenceFontSize = h >> CONFIDENCE_SIZE_RATIO_BITS_SHIFT;
        mTextPaint.setTextSize(confidenceFontSize);
        mTextPaint.setTypeface(CONFIDENCE_FONT);
        confidenceTextWidth = mTextPaint.measureText(confidenceToString());
        confidenceLeft      = Math.max(0, (w - confidenceTextWidth)) / 2;
        confidenceBottom    = h * 0.9f;
    }
}
