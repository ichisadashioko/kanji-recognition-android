package io.github.ichisadashioko.android.kanji;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.OldClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;

public class App extends Activity {
    private HandwritingCanvas canvas;
    private OldClassifier tflite;
    private LinearLayout resultContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = (HandwritingCanvas) findViewById(R.id.canvas);
        resultContainer = (LinearLayout) findViewById(R.id.result_container);
        try {
            tflite = new OldClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearCanvas(View view) {
        canvas.clearCanvas();
        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }
    }

    private View createButtonFromResult(Recognition r) {
        Button btn = new Button(this);
        btn.setText(r.title);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(layoutParams);
        return btn;
    }

    public void runClassifier(View view) {
        if (canvas == null || tflite == null || resultContainer == null) {
            return;
        }

        long startTime = SystemClock.elapsedRealtime();
        List<Recognition> results = tflite.recognizeImage(canvas.getImage());
        long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results) {
            resultContainer.addView(createButtonFromResult(result));
        }
    }
}