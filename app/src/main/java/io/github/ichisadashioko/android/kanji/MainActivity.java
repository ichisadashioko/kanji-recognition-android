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
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonWriter;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    private HandwritingCanvas canvas;
    private KanjiClassifier tflite;
    private LinearLayout resultContainer;
    private int resultViewWidth;
    private EditText textRenderer;
    private boolean autoEvaluate;
    private boolean autoClear;
    private Typeface kanjiTypeface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas = (HandwritingCanvas) findViewById(R.id.canvas);
        resultContainer = (LinearLayout) findViewById(R.id.result_container);
        resultViewWidth = (int) getResources().getDimension(R.dimen.result_size);
        textRenderer = (EditText) findViewById(R.id.text_renderer);

        ToggleButton autoEvaluateToggleButton = (ToggleButton) findViewById(R.id.auto_evaluate);
        autoEvaluate = autoEvaluateToggleButton.isChecked();
        autoEvaluateToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoEvaluate = isChecked;
            }
        });

        ToggleButton autoClearToggleButton = (ToggleButton) findViewById(R.id.auto_clear);
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

    public void clearCanvas(View view) {
        canvas.clearCanvas();
        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }
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

    private void exportWritingData(Recognition r, Bitmap image, List<List<CanvasPoint2D>> writingStrokes) {
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
            String label = r.title.isEmpty() ? "NO_LABEL" : r.title;
            String labelDirName = normalizeFilename(label);
            String labelDirPath = savePath + "/" + labelDirName;
            labelDirPath = labelDirPath.replaceAll("/+", "/");
            if (!prepareDirectory(labelDirPath)) {
                throw new Exception(String.format("Cannot create directory: %s", labelDirPath));
            }

            String dataFilePath = labelDirPath + "/" + Long.toString(r.timestamp) + ".json";
            FileOutputStream out = new FileOutputStream(dataFilePath);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("label").value(r.title);
            writer.name("timestamp").value(r.timestamp);
            writer.name("confidence").value(r.confidence);

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
            writer.name("description").value("PNG image in ARGB format");
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
    }

    /**
     * We want to make sure there is a directory with path equals `path`.
     * However, there may be it is not existed or it is a file.
     * We will `mkdirs` or `rename` respectively.
     * The `path` must be valid.
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

    /**
     * Create a view to show the recognition in the UI. I also setup event listener for each view
     * in order to add text if the view is clicked.
     * <p>
     * I also plan to save the image/drawing stroke to file so that I can improve the model later.
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
                textRenderer.setText(textRenderer.getText() + btn.label);
                textRenderer.setSelection(textRenderer.getText().length());
                if (canSaveWritingData()) {
                    // save image and writing strokes to files
                    exportWritingData(r, image, writingStrokes);
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
        Bitmap image = canvas.getImage();
        List<List<CanvasPoint2D>> writingStrokes = canvas.getWritingStrokes();
        System.out.println("Number of strokes: " + writingStrokes.size());
        List<Recognition> results = tflite.recognizeImage(image);
        // long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        // System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results) {
            resultContainer.addView(createButtonFromResult(result, image, writingStrokes));
        }
    }

    /**
     * Copy the text showed at the UI to clipboard so that users can paste it anywhere they want.
     *
     * @param view the View that triggers this method.
     */
    public void copyTextToClipboard(View view) {
        if (textRenderer.getText().length() > 0) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text copied from handwriting input", textRenderer.getText());
            clipboard.setPrimaryClip(clipData);
        }
    }

    /**
     * Clear text showing in the UI.
     */
    public void clearText(View view) {
        textRenderer.setText("");
    }

    /**
     * This the callback method that will be called by the drawing canvas because if we attach
     * event listener to the drawing canvas, it will override the drawing logic.
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
}