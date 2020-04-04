package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonWriter;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.ResultButton;

public class MainActivity extends Activity implements TouchCallback {
    /**
     * We still have to put custom font in `assets` folder but not the `res` folder
     * because accessing font via `id` requires minimum API 26.
     */
    public static final String KANJI_FONT_PATH = "fonts/HGKyokashotai_Medium.ttf";
    public static final String SAVE_DIRECTORY_NAME = "handwriting_data";
    public static final String WRITING_LOG_BASENAME = "writing_history_";
    /**
     * 5 KB for text file.
     * <p>
     * Each Japanese character takes up around 3 bytes. Each sentence (line) is about 16 characters.
     * 5 KB will give us 100 lines for each file.
     */
    public static final int MAX_LOG_SIZE = 5 * 1024;

    private HandwritingCanvas canvas;
    private KanjiClassifier tflite;
    private LinearLayout resultContainer;
    private int resultViewWidth;
    private EditText textRenderer;
    private boolean autoEvaluate;
    private boolean autoClear;
    private Typeface kanjiTypeface;
    private EditText customLabelEditText;
    private Bitmap currentEvaluatingImage;
    private List<List<CanvasPoint2D>> currentEvaluatingWritingStrokes;
    // I set this to `true` because the text is empty.
    private boolean isTextSaved = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = findViewById(R.id.canvas);
        resultContainer = findViewById(R.id.result_container);
        resultViewWidth = (int) getResources().getDimension(R.dimen.result_size);
        textRenderer = findViewById(R.id.text_renderer);
        customLabelEditText = findViewById(R.id.custom_label);

        ToggleButton autoEvaluateToggleButton = findViewById(R.id.auto_evaluate);
        autoEvaluate = autoEvaluateToggleButton.isChecked();
        autoEvaluateToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoEvaluate = isChecked;
            }
        });

        ToggleButton autoClearToggleButton = findViewById(R.id.auto_clear);
        autoClear = autoClearToggleButton.isChecked();
        autoClearToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoClear = isChecked;
            }
        });

        canvas.touchCallback = this;

        try {
            tflite = new KanjiClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        kanjiTypeface = Typeface.createFromAsset(getApplicationContext().getAssets(), KANJI_FONT_PATH);
        textRenderer.setTypeface(kanjiTypeface);
        ResultButton.LABEL_FONT = kanjiTypeface;
    }

    private boolean canSaveWritingData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAllowSaving = sharedPreferences.getBoolean(getString(R.string.pref_key_save_data), false);
        boolean permissionGranted = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        return isAllowSaving && permissionGranted;
    }

    private String getBackupFilepath(String path) {
        int counter = 0;
        String backupFilepath = path + "_" + counter;
        File backupFile = new File(path);
        while (backupFile.exists()) {
            backupFilepath = path + "_" + counter;
            backupFile = new File(backupFilepath);
            counter++;
        }
        return backupFilepath;
    }

    private String normalizeFilename(String filename) {
        return filename.replaceAll("[\\\\\\/\\.\\#\\%\\$\\!\\@\\(\\)\\[\\]\\s]+", "_");
    }

    /**
     * Save writing data to external storage for collection data later to train
     * another model.
     *
     * @param label          the text representation of the input
     * @param confidence     the confidence score range from 0 to 1
     * @param timestamp      the time that this data is created
     * @param image          the drawing image of `label`
     * @param writingStrokes list of strokes that create the writing
     */
    private void exportWritingData(String label, final float confidence, final long timestamp, Bitmap image, List<List<CanvasPoint2D>> writingStrokes) {
        long startTime = SystemClock.elapsedRealtime();
        // TODO create preference for save location
        try {
            // TODO the image format is ARGB, write our own encoder to encode grayscale PNG
            // Android does not support grayscale PNG.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
            byte[] grayscalePNGImage = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(grayscalePNGImage, Base64.DEFAULT);
            String[] wrappedBase64String = base64Image.split("\n");
            // System.out.println(wrappedBase64String.length);

            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String savePath = downloadDirectory.getAbsolutePath() + "/" + SAVE_DIRECTORY_NAME;
            savePath = savePath.replaceAll("/+", "/");
            if (!prepareDirectory(savePath)) {
                throw new Exception(String.format("Cannot create directory: %s", savePath));
            }

            // make directory for the label
            label = (label == null || label.isEmpty()) ? "NO_LABEL" : label;
            String labelDirName = normalizeFilename(label);
            String labelDirPath = savePath + "/" + labelDirName;
            labelDirPath = labelDirPath.replaceAll("/+", "/");
            if (!prepareDirectory(labelDirPath)) {
                throw new Exception(String.format("Cannot create directory: %s", labelDirPath));
            }

            String dataFilePath = labelDirPath + "/" + Long.toString(timestamp) + ".json";
            FileOutputStream out = new FileOutputStream(dataFilePath);
            // serialize to JSON format
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("label").value(label);
            writer.name("timestamp").value(timestamp);
            writer.name("confidence").value(confidence);

            writer.name("touches");
            writer.beginArray();
            Iterator<List<CanvasPoint2D>> iterator = writingStrokes.iterator();
            while (iterator.hasNext()) {
                List<CanvasPoint2D> stroke = iterator.next();
                writer.beginArray();
                Iterator<CanvasPoint2D> strokeIterator = stroke.iterator();
                while (strokeIterator.hasNext()) {
                    CanvasPoint2D p = strokeIterator.next();
                    writer.beginObject();
                    writer.name("x").value(p.x);
                    writer.name("y").value(p.y);
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endArray();

            writer.name("image");
            writer.beginObject();
            writer.name("description").value("PNG image in ARGB format even though this is just a grayscale image.");
            writer.name("data");
            writer.beginArray();
            for (String base64Data : wrappedBase64String) {
                writer.value(base64Data);
            }
            writer.endArray();
            writer.endObject();

            writer.endObject();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        long exportDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println("Export time: " + exportDuration);
    }

    /**
     * We want to make sure there is a directory with path equals `path`. However,
     * there may be it is not existed or it is a file. We will `mkdirs` or `rename`
     * respectively. The `path` must be valid.
     *
     * @param path the directory path we want
     */
    private boolean prepareDirectory(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                String backupFilePath = getBackupFilepath(path);
                file.renameTo(new File(backupFilePath));
                file = new File(path);
            } else {
                return true;
            }
        }

        return file.mkdirs();
    }

    private void pushText(String text) {
        isTextSaved = false;
        textRenderer.setText(textRenderer.getText() + text);
        textRenderer.setSelection(textRenderer.getText().length());
    }

    /**
     * Create a view to show the recognition in the UI. I also setup event listener
     * for each view in order to add text if the view is clicked.
     * <p>
     * I also plan to save the image/drawing stroke to file so that I can improve
     * the model later.
     *
     * @param r              the recognition result
     * @param image          the image associated with the result
     * @param writingStrokes list of touch points
     * @return
     */
    private View createButtonFromResult(Recognition r, Bitmap image, List<List<CanvasPoint2D>> writingStrokes) {
        ResultButton btn = new ResultButton(this, null, r.title, r.confidence);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(resultViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushText(r.title);
                if (canSaveWritingData()) {
                    // save image and writing strokes to files
                    exportWritingData(r.title, r.confidence, r.timestamp, image, writingStrokes);
                }
                if (autoClear) {
                    clearCanvas(v);
                }
            }
        });
        return btn;
    }

    /**
     * Get the image from the canvas and use the tflite model to evaluate the image.
     * After the results are returned, show them on the UI.
     *
     * @param view the View that triggers this method.
     */
    public void runClassifier(View view) {
        if (canvas == null || tflite == null || resultContainer == null) {
            return;
        }

        // long startTime = SystemClock.elapsedRealtime();
        currentEvaluatingImage = canvas.getImage();
        currentEvaluatingWritingStrokes = canvas.getWritingStrokes();
        // System.out.println("Number of strokes: " + currentEvaluatingWritingStrokes.size());
        List<Recognition> results = tflite.recognizeImage(currentEvaluatingImage);
        // long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        // System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results) {
            resultContainer.addView(createButtonFromResult(result, currentEvaluatingImage, currentEvaluatingWritingStrokes));
        }
    }

    private void saveWritingHistory(final String text) {
        if (isTextSaved || text.isEmpty()) {
            return;
        }

        try {
            if (!canSaveWritingData()) {
                return;
            }

            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String rootSavePath = downloadDirectory.getAbsolutePath() + "/" + SAVE_DIRECTORY_NAME;
            rootSavePath = rootSavePath.replaceAll("/+", "/");
            if (!prepareDirectory(rootSavePath)) {
                throw new Exception(String.format("Cannot create directory: %s", rootSavePath));
            }

            int indexCounter = 0;
            String baseSavePath = rootSavePath + "/" + WRITING_LOG_BASENAME;
            String saveFilePath;
            File saveFile;
            do {
                saveFilePath = baseSavePath + String.format("%06d", indexCounter);
                saveFile = new File(saveFilePath);

                if (!saveFile.exists()) {
                    saveFile.createNewFile();
                } else {
                    if (saveFile.isDirectory()) {
                        // backup this directory to take over the file name
                        String backupPath = getBackupFilepath(saveFilePath);
                        saveFile.renameTo(new File(backupPath));
                        saveFile = new File(saveFilePath);
                        saveFile.createNewFile();
                    }
                }
                indexCounter++;
            } while (saveFile.length() > MAX_LOG_SIZE);

            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(saveFile, true), "utf8");
            osw.append(text);
            osw.append('\n');
            osw.flush();
            osw.close();
            isTextSaved = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Copy the text showed at the UI to clipboard so that users can paste it
     * anywhere they want.
     *
     * @param view the View that triggers this method.
     */
    public void copyTextToClipboard(View view) {
        if (textRenderer.getText().length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text copied from handwriting input", textRenderer.getText());
            clipboard.setPrimaryClip(clipData);
        }
        saveWritingHistory(textRenderer.getText().toString());
    }

    /**
     * Clear text showing in the UI.
     */
    public void clearText(View view) {
        saveWritingHistory(textRenderer.getText().toString());
        textRenderer.setText("");
    }

    /**
     * This the callback method that will be called by the drawing canvas because if
     * we attach event listener to the drawing canvas, it will override the drawing
     * logic.
     */
    @Override
    public void onTouchEnd() {
        if (autoEvaluate) {
            runClassifier(null);
        }
    }

    /**
     * Open preference settings view.
     *
     * @param view the view that triggers this method
     */
    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void clearCustomLabelText(View view) {
        customLabelEditText.setText("");
    }

    public void clearCanvas(View view) {
        canvas.clearCanvas();
        currentEvaluatingImage = null;
        currentEvaluatingWritingStrokes = null;
        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }
    }

    public void saveWritingDataWithCustomLabel(View view) {
        String customLabel = customLabelEditText.getText().toString();
        if (currentEvaluatingImage != null && currentEvaluatingWritingStrokes != null && !customLabel.isEmpty()) {
            pushText(customLabel);

            if (canSaveWritingData()) {
                exportWritingData(customLabel, 1f, System.currentTimeMillis(), currentEvaluatingImage, currentEvaluatingWritingStrokes);
            }
        }
    }
}