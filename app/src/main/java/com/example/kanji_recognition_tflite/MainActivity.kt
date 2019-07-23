package com.example.kanji_recognition_tflite

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.text.Layout
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.kotlindrawing.KanjiClassifier

class MainActivity : AppCompatActivity() {
    var tflite: KanjiClassifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (tflite == null) {
            tflite = KanjiClassifier(this)
        }
        val canvas = findViewById<WritingCanvas>(R.id.canvas)

        val clearBtn = findViewById<Button>(R.id.clear_btn)
        clearBtn.setOnClickListener { canvas.clearCanvas() }


        val resultPane = findViewById<LinearLayout>(R.id.result_output)

        val predictBtn = findViewById<Button>(R.id.predict_btn)
        predictBtn.setOnClickListener {
            resultPane.removeAllViews()
            val startTime = SystemClock.uptimeMillis()
            canvas.imageCanvas.save()
            var results = tflite!!.recognizeImage(canvas.canvasImage)
            val inferenceTime = SystemClock.uptimeMillis() - startTime

            Log.d("TFLite", "[INFERENCE] inference time: $inferenceTime")

            for ((idx, result) in results.withIndex()) {
                Log.d("TFLite", "[INFERENCE] $idx - $result")
                val resultLL = LinearLayout(applicationContext)
                resultLL.orientation = LinearLayout.HORIZONTAL

                val label = TextView(applicationContext)
                label.text = result.title
                resultLL.addView(label)

                val confidence = TextView(applicationContext)
                confidence.text = "%.2f %%".format(result.confidence * 100)
                resultLL.addView(confidence)
                resultPane.addView(resultLL)
            }
        }
    }
}