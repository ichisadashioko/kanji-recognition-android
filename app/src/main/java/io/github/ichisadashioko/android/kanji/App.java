package io.github.ichisadashioko.android.kanji;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.OldClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;

import static android.os.SystemClock.elapsedRealtime;

public class App extends Activity {
    private HandwritingCanvas canvas;
    private OldClassifier tflite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = (HandwritingCanvas) findViewById(R.id.canvas);
        try {
            tflite = new OldClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearCanvas(View view) {
        if (canvas != null) {
            canvas.clearCanvas();
        }
    }

    public void runClassifier(View view) {
        if (canvas == null || tflite == null) {
            return;
        }

        long startTime = SystemClock.elapsedRealtime();
        List<Recognition> results = tflite.recognizeImage(canvas.getImage());
        long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println(String.format("Inference took %d ms.", evaluateDuration));
    }
}