package io.github.ichisadashioko.android.kanji;

import android.app.Activity;
import android.os.Bundle;

public class App extends Activity {
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

//        initializeClassifier(this);
    }
}