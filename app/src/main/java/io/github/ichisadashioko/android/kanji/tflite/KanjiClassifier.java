package io.github.ichisadashioko.android.kanji.tflite;

import android.app.Activity;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;

public class KanjiClassifier {
    public static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public static final String MODEL_PATH = "etlcb_9b_model.tflite";
    public static final String LABELS_PATH = "etlcb_9b_labels.txt";

    private Interpreter tflite;
    private final List<String> labels;
    private TensorImage inputImageBuffer;
    private final TensorBuffer outputProbabilityBuffer;
    public final int imageSizeHeight;
    public final int imageSizeWidth;

    public KanjiClassifier(Activity activity) throws IOException {
        // load model file
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(activity, MODEL_PATH);
        // load labels
        labels = FileUtil.loadLabels(activity, LABELS_PATH);

        // configure the interpreter
        Interpreter.Options modelOptions = new Interpreter.Options();
        modelOptions.setNumThreads(NUM_THREADS);
        // create the interpreter
        tflite = new Interpreter(modelFile, modelOptions);

        // allocate tensors
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
        // tensor format NHWC (number of samples, height, width, number of channels)
        imageSizeHeight = imageShape[1];
        imageSizeWidth = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        Tensor probabilityTensor = tflite.getOutputTensor(probabilityTensorIndex);
        int[] probabilityShape = probabilityTensor.shape(); // (1, NUM_CLASSES)
        DataType probabilityDataType = probabilityTensor.dataType();

        // allocate input tensor buffer
        inputImageBuffer = new TensorImage(imageDataType);
        // allocate output tensor buffer
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
    }

    public List<Recognition> recognizeImage(Bitmap image) {
        inputImageBuffer.load(image);

        return null;
    }
}