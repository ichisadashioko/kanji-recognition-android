package com.example.kotlindrawing

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.os.Trace
import android.support.annotation.RequiresApi
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList

/**
 * A classifier specialized to label images using TensorFlow Lite
 */
class KanjiClassifier constructor(activity: Activity) {
    companion object {
        // static objects are put here.

        // Number of results to show in the UI.
        const val MAX_RESULTS = 128
        // Dimensions of inputs.
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 1
        const val LOG_TAG = "KanjiClassifier"
    }

    /**
     * Preallocated buffers to store image data in.
     */
    var intValues = IntArray(getImageSizeX() * getImageSizeY())
    /**
     * Options for configuring the Interpreter.
     */
    private var tfliteOptions: Interpreter.Options
    /**
     * The loaded Tensorflow Lite model.
     */
    private var tfliteModel: MappedByteBuffer?
    /**
     * Labels corresponding to the output of the model
     */
    private var labels: List<String>
    /**
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    private var tflite: Interpreter?
    /**
     * The number of threads to use for classification.
     */
    var numThreads = 4
    private var imgData: ByteBuffer

    /**
     * An array to hold inference results, to be fed into Tensorflow Lite as outputs.
     */
    private var labelProbArray: Array<FloatArray>

    /**
     * Get the total number of labels.
     */
    val numLabels: Int
        get() = labels.size

    init {
        // initial Tensorflow Lite Interpreter
        tfliteModel = loadModelFile(activity)
        tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(numThreads)
        tfliteOptions.setUseNNAPI(true)
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        labels = loadLabelList(activity)

        imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                    * getImageSizeY()
                    * getImageSizeX()
                    * DIM_PIXEL_SIZE
                    * getNumBytesPerChannel()
        )
        imgData.order(ByteOrder.nativeOrder())
        Log.d(LOG_TAG, "Created Kanji Classifier")

        labelProbArray = Array(1) { FloatArray(numLabels) }
    }

    fun getNumBytesPerChannel(): Int {
        // Float.SIZE / Byte.SIZE
        // return java.lang.Float.SIZE
        return 4 // java.nio.ByteBuffer.putFloat() only writes 4 bytes to the buffer
    }

    fun getImageSizeX(): Int {
        return 64
    }

    fun getImageSizeY(): Int {
        return 64
    }

    fun getModelPath(): String {
        // model filename in assets folder
        return "etlcb_9b_model.tflite"
    }

    fun getLabelPath(): String {
        // label filename in assets folder
        return "etlcb_9b_labels.txt"
    }

    /**
     * Memory-map the model file in Assets.
     */
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(getModelPath())
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        val fileChannel = inputStream.channel
        var startOffset = fileDescriptor.startOffset
        var declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Reads label list from Assets.
     */
    private fun loadLabelList(activity: Activity): List<String> {
        val labels = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open(getLabelPath())))
        var line = reader.readLine()
        while (line != null) {
            labels.add(line)
            line = reader.readLine()
        }
        return labels
    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        val startTime = SystemClock.uptimeMillis()
        for (i in 0 until getImageSizeX()) {
            for (j in 0 until getImageSizeY()) {
                val pxVal = intValues[pixel++]
                addPixelValue(pxVal)
            }
        }
        val endTime = SystemClock.uptimeMillis() - startTime
        Log.v(LOG_TAG, "Time cost to put values into ByteBuffer: $endTime")
    }

    /**
     * Add pixelValue to byteBuffer.
     *
     * The `pixelValue` from `Bitmap` is a 32-bits integer for ARGB.
     *
     */
    protected fun addPixelValue(pixelValue: Int) {
        // Convert RGB to GRAY
        val red = pixelValue shr 16 and 0xff
        val green = pixelValue shr 8 and 0xff
        val blue = pixelValue and 0xff
        val gray = (0.299f * red + 0.597f * green + 0.114f * blue) / 255f
        imgData.putFloat(gray)

        // Use the below code to put RGB values
//        imgData.putFloat((pixelValue shr 16 and 0xFF) / 255f)
//        imgData.putFloat((pixelValue shr 8 and 0xFF) / 255f)
//        imgData.putFloat((pixelValue and 0xFF) / 255f)
    }

    /**
     * Runs inference and returns the classification results.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun recognizeImage(bitmap: Bitmap): List<Recognition> {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage")

        Trace.beginSection("preprocessBitmap")
        convertBitmapToByteBuffer(bitmap)
        Trace.endSection()

        // Run the inference call.
        Trace.beginSection("runInference")
        val startTime = SystemClock.uptimeMillis()
        runInference()
        val endTime = SystemClock.uptimeMillis() - startTime
        Trace.endSection()
        Log.v(LOG_TAG, "Time cost to run model inference: $endTime")

        // Find the best classification.
        val pq = PriorityQueue(
            3,
            Comparator<Recognition> { lhs, rhs ->
                // Intentionally reversed to put high confidence at the head of the queue.
                java.lang.Float.compare(rhs.confidence, lhs.confidence)
            }
        )
        for (i in labels.indices) {
            pq.add(
                Recognition(
                    "" + i,
                    if (labels.size > i) labels[i] else "unknown",
                    getNormalizedProbability(i)
                )
            )
        }
        val recognitions = ArrayList<Recognition>()
        val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
        for (i in 0 until recognitionsSize) {
            recognitions.add(pq.poll())
        }
        Trace.endSection()
        return recognitions
    }

    /**
     * Get the normalized probability value for the specified label. This is the final value as it will be shown to the user.
     */
    fun getNormalizedProbability(labelIndex: Int): Float {
        return labelProbArray[0][labelIndex]
    }

    /**
     * Run inference using the prepared input in `imgData`. Afterwards, the result will be provided by `getProbability()`.
     *
     * This additional method is necessary, because we don't have a common base for different primitive data types.
     */
    fun runInference() {
        tflite!!.run(imgData, labelProbArray)
    }

    /**
     * Closes the intepreter and model to release resources.
     */
    fun close() {
        tflite!!.close()
        tflite = null
        tfliteModel = null
    }

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     * @param id A unique identifier for what has been recognized. Specific to the class, not the instance of the object
     * @param title Display name for the recognition.
     * @param confidence A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    class Recognition constructor(_id: String?, _title: String?, _confidence: Float) {
        val id = _id
        val title = _title
        val confidence = _confidence

        override fun toString(): String {
            var resultString = "";
            if (id != null) {
                resultString += String.format("%-8s", "[$id]")
            }
            if (title != null) {
                resultString += String.format("%-5s", "$title")
            }
            resultString += String.format("(%.1f%%) ", confidence * 100f)

            return resultString;
        }
    }
}