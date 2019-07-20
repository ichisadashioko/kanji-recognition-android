package com.example.kanji_recognition_tflite

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.example.kotlindrawing.KanjiClassifier

class MainActivity : AppCompatActivity() {
    var tflite: KanjiClassifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val canvas = findViewById<Canvas>(R.id.canvas)
        val tvLog = findViewById<TextView>(R.id.tv_log)
        /**
         * Log the position of touch
         */
//        canvas.tvLog = tvLog

        val clearBtn = findViewById<Button>(R.id.btn_clear)
        clearBtn.setOnClickListener { canvas.clearCanvas() }

        val predictBtn = findViewById<Button>(R.id.btn_predict)
        predictBtn.setOnClickListener {
            tvLog.text = "Inference is running!"
            val startTime = SystemClock.uptimeMillis()
            canvas.bmCanvas.save()
            var results = tflite!!.recognizeImage(canvas.characterBitmap)
            val inferenceTime = SystemClock.uptimeMillis() - startTime

            tvLog.text = ""
            tvLog.append("Inference Time: $inferenceTime ms\n")
//            var logStr = ""
            Log.d("TFLite", "[INFERENCE] inference time: $inferenceTime")


            for ((idx, label) in results.withIndex()) {
                Log.d("TFLite", "[INFERENCE] $idx - $label")
                tvLog.append("$label\n")
            }
        }

        if (tflite == null) {
            tflite = KanjiClassifier(this)
        }
    }
}
