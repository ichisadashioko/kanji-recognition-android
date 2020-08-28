package io.github.ichisadashioko.android.kanji.tflite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class KanjiClassifier
{
    public static final int MAX_RESULTS    = 16;
    public static final int DIM_BATCH_SIZE = 1;
    public static final int DIM_PIXEL_SIZE = 1;
    public static final String LOG_TAG     = "KanjiClassifier";
    public static final int IMAGE_WIDTH    = 64;
    public static final int IMAGE_HEIGHT   = 64;
    // we will use 32-bit float to store pixel value
    public static final int NUM_BYTES_PER_PIXEL = 4;
    public static final String MODEL_FILE_PATH  = "etlcb_9b_model.tflite";
    public static final String LABEL_FILE_PATH  = "etlcb_9b_labels.txt";

    // pre-allocated buffers to store image data
    public int[] intValues = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
    public final int NUM_LABELS;
    public Interpreter.Options tfliteOptions;
    public MappedByteBuffer tfliteModel;
    public List<String> labels;
    public Interpreter tflite;
    public ByteBuffer imgData;
    public float[][] labelProbArray;

    public KanjiClassifier(Activity activity) throws IOException
    {
        tfliteModel   = loadModelFile(activity);
        tfliteOptions = new Interpreter.Options();
        tfliteOptions.setNumThreads(Runtime.getRuntime().availableProcessors());
        tfliteOptions.setUseNNAPI(true);
        tflite = new Interpreter(tfliteModel, tfliteOptions);

        labels     = loadLabelList(activity);
        NUM_LABELS = labels.size();

        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * IMAGE_HEIGHT * IMAGE_WIDTH * DIM_PIXEL_SIZE * NUM_BYTES_PER_PIXEL);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[DIM_BATCH_SIZE][NUM_LABELS];

        Log.d(LOG_TAG, "Created Kanji Classifier.");
    }

    public MappedByteBuffer loadModelFile(Activity activity) throws IOException
    {
        AssetFileDescriptor afd = activity.getAssets().openFd(MODEL_FILE_PATH);
        FileInputStream fis     = new FileInputStream(afd.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    public List<String> loadLabelList(Activity activity) throws IOException
    {
        ArrayList<String> labels = new ArrayList<>();
        BufferedReader reader    = new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_FILE_PATH)));
        String line              = reader.readLine();
        while (line != null)
        {
            labels.add(line);
            line = reader.readLine();
        }
        return labels;
    }

    public float normalizePixelValue(int pixelValue)
    {
        int r      = (pixelValue >> 16) & 0xFF;
        int g      = (pixelValue >> 8) & 0xFF;
        int b      = pixelValue & 0xFF;
        float gray = (0.299f * r + 0.597f * g + 0.114f * b) / 255f;
        return gray;
    }

    public void populateByteBuffer(Bitmap bitmap) throws Exception
    {
        if ((bitmap.getWidth() != IMAGE_WIDTH) && (bitmap.getHeight() != IMAGE_HEIGHT))
        {
            throw new Exception(String.format("The image with shape (%d, %d) is not equals (%d, %d)!!!", bitmap.getWidth(), bitmap.getHeight(), IMAGE_WIDTH, IMAGE_HEIGHT));
        }
        // reset `imgData`
        imgData.rewind();
        // populate `intValues` with the bitmap data
        bitmap.getPixels(intValues, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        int index = 0;
        for (int i = 0; i < IMAGE_WIDTH; i++)
        {
            for (int j = 0; j < IMAGE_HEIGHT; j++)
            {
                int pixelValue = intValues[index++];
                imgData.putFloat(normalizePixelValue(pixelValue));
            }
        }
    }

    public synchronized List<Recognition> recognizeImage(Bitmap bitmap)
    {
        ArrayList<Recognition> results = new ArrayList<>();

        try
        {
            populateByteBuffer(bitmap);
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG, "There is some problem with the Bitmap!");
            ex.printStackTrace();
            return results;
        }

        tflite.run(imgData, labelProbArray);
        long timestamp = System.currentTimeMillis();

        // sort the result by confidence
        PriorityQueue<Recognition> pq = new PriorityQueue<>(NUM_LABELS, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition a, Recognition b)
            {
                // we want to sort descending
                return Float.compare(b.confidence, a.confidence);
            }
        });

        for (int i = 0; i < NUM_LABELS; i++)
        {
            pq.add(new Recognition(i, timestamp, labels.get(i), labelProbArray[0][i]));
        }

        int returnSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < returnSize; i++)
        {
            results.add(pq.poll());
        }

        return results;
    }
}
