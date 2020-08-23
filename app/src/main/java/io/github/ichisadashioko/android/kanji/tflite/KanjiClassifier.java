package io.github.ichisadashioko.android.kanji.tflite;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

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
    public Interpreter.Options tfliteOptions;
    public MappedByteBuffer tfliteModel;
    public String[] labels;
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

        labels = loadLabelList(activity);

        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * IMAGE_HEIGHT * IMAGE_WIDTH * DIM_PIXEL_SIZE * NUM_BYTES_PER_PIXEL);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[DIM_BATCH_SIZE][labels.length];

        Log.d(LOG_TAG, "Created Kanji Classifier.");
    }

    public MappedByteBuffer loadModelFile(Activity activity) throws IOException
    {
        AssetFileDescriptor afd = activity.getAssets().openFd(MODEL_FILE_PATH);
        FileInputStream fis     = new FileInputStream(afd.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    public static byte[] ReadAssetData(Context androidContext, String assetRelativePath) throws IOException
    {
        AssetManager am        = androidContext.getAssets();
        InputStream is         = am.open(assetRelativePath);
        ArrayList<Byte> buffer = new ArrayList<Byte>();

        int tmpByte;
        while (true)
        {
            tmpByte = is.read();
            if (tmpByte > -1)
            {
                buffer.add((byte) tmpByte);
            }
            else
            {
                break;
            }
        }

        byte[] fileData = new byte[buffer.size()];
        for (int i = 0; i < buffer.size(); i++)
        {
            fileData[i] = buffer.get(i);
        }

        is.close();
        am.close();

        return fileData;
    }

    public static String[] FilterEmptyLines(String[] lines)
    {
        ArrayList<String> buffer = new ArrayList<String>();

        for (int i = 0; i < lines.length; i++)
        {
            if (lines[i].length() == 0)
            {
                continue;
            }
            else
            {
                buffer.add(lines[i]);
            }
        }

        return buffer.toArray(new String[buffer.size()]);
    }

    public String[] loadLabelList(Activity activity) throws IOException
    {
        byte[] fileData    = ReadAssetData(activity, LABEL_FILE_PATH);
        String fileContent = new String(fileData, StandardCharsets.UTF_8);

        String[] textLines = fileContent.split("\n");
        textLines          = FilterEmptyLines(textLines);

        return textLines;
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
        ArrayList<Recognition> results = new ArrayList<Recognition>();

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
        PriorityQueue<Recognition> pq = new PriorityQueue<Recognition>(labels.length, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition a, Recognition b)
            {
                // we want to sort descending
                return Float.compare(b.confidence, a.confidence);
            }
        });

        for (int i = 0; i < labels.length; i++)
        {
            for (int j = 0; j < labels[i].length(); j++)
            {
                pq.add(new Recognition(i, timestamp, labels[i].substring(j, j + 1), labelProbArray[0][i]));
            }
        }

        int returnSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < returnSize; i++)
        {
            results.add(pq.poll());
        }

        return results;
    }
}
