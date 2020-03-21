package io.github.ichisadashioko.android.kanji;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;

public class App extends Activity {
    private HandwritingCanvas canvas;
    private Button clearButton;
//    private KanjiClassifier classifier;

//    private void initializeClassifier(Activity activity) {
//        try {
//            classifier = new KanjiClassifier(activity);
//        } catch (Exception ex) {
//            System.out.println("Cannot initialize TFLite classifier!!!");
//            ex.printStackTrace();
//            classifier = null;
//        }
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = (HandwritingCanvas) findViewById(R.id.canvas);
        clearButton = (Button) findViewById(R.id.clear);
//        initializeClassifier(this);
    }

    public void clearCanvas(View view) {
        if (canvas != null) {
            canvas.clearCanvas();
        }
    }
}