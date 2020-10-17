package io.github.ichisadashioko.android.kanji;

import android.graphics.Bitmap;
import android.widget.LinearLayout;

import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;

public class InferenceThread extends Thread
{
    public static long SleepTimeout                = 100;
    public static long LastCreatedThreadTime       = -1;
    public static Object LastCreatedThreadTimeLock = new Object();

    public final long creationTime;
    public final MainActivity app;

    public InferenceThread(MainActivity app, long creationTime)
    {
        this.app          = app;
        this.creationTime = creationTime;
    }

    public void run()
    {
        try
        {
            Thread.sleep(InferenceThread.SleepTimeout);
            synchronized (LastCreatedThreadTimeLock)
            {
                if (creationTime != LastCreatedThreadTime)
                {
                    // System.out.println("Thread " + creationTime + " is skipped!");
                    return;
                }
            }

            // System.out.println("Thread " + creationTime + " is OK!");

            app.runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    app.runClassifier(null);
                }
            });
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
