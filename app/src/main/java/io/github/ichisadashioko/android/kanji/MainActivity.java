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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonWriter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.ResultButton;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;

public class MainActivity extends Activity implements TouchCallback, SharedPreferences.OnSharedPreferenceChangeListener
{
    /**
     * We still have to put custom font in `assets` folder but not the `res` folder because
     * accessing font via `id` requires minimum API 26.
     */
    public static final String KANJI_FONT_PATH = "fonts/HGKyokashotai_Medium.ttf";

    /**
     * Writing data will be stored in the Downloads directory.
     *
     * <p>~/Download/handwriting_data/
     */
    public static final String SAVE_DIRECTORY_NAME = "handwriting_data";

    // ~/Download/handwriting_data/stroke_data/
    public static final String WRITING_STROKE_DATA_DIR_NAME = "stroke_data";

    // ~/Download/handwriting_data/writing_history/
    public static final String WRITING_LOG_DIR_NAME = "writing_history";

    /**
     * 5 KBs for text file.
     *
     * <p>Each Japanese character takes up around 3 bytes. Each sentence (line) is about 16
     * characters. 5 KBs will give us 100 lines for each file.
     */
    public static final int MAX_LOG_SIZE = 5 * 1024;

    public HandwritingCanvas canvas;
    public KanjiClassifier tflite;

    /**
     * I keep track of this view to scroll to start when we populate the result list.
     */
    public HorizontalScrollView resultListScrollView;

    public LinearLayout resultContainer;

    /**
     * This variable is used to store the pixel value converted from dp value stored in dimens.xml.
     * I use this value to set the size for the result view.
     */
    public int resultViewWidth;

    // The EditText is used to store the input text.
    public EditText textRenderer;

    /**
     * flags for clearing the canvas or evaluating the image data while it's being drawn.
     */
    public boolean autoEvaluate;

    public boolean autoClear;

    /**
     * Sometimes, we want to store data that the model was not trained or the model give the correct
     * label to low accuracy point that the correct label does not show in the result list. We have
     * to manually type the correct label and save it ourselves for future training.
     */
    public EditText customLabelEditText;

    /**
     * Variables to keep track of the data that we are currently seeing. I need these to store
     * custom labels that the model does not have or the model evaluates the data wrong (not showing
     * in the result list).
     */
    public Bitmap currentEvaluatingImage;

    public List<List<CanvasPoint2D>> currentEvaluatingWritingStrokes;

    // I set this to `true` because the text is empty.
    public boolean isTextSaved = true;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvas               = findViewById(R.id.canvas);
        resultContainer      = findViewById(R.id.result_container);
        resultViewWidth      = (int) getResources().getDimension(R.dimen.result_size);
        textRenderer         = findViewById(R.id.text_renderer);
        customLabelEditText  = findViewById(R.id.custom_label);
        resultListScrollView = findViewById(R.id.result_container_scroll_view);

        ToggleButton autoClearToggleButton = findViewById(R.id.auto_clear);
        autoClear                          = autoClearToggleButton.isChecked();
        autoClearToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                autoClear = isChecked;
            }
        });

        // I add a TouchCallback interface because if we override the event listener,
        // the canvas is not working correctly. Our custom canvas manually handle touch
        // events, because of that add EventListener may break out canvas functionality.
        canvas.touchCallback = this;

        try
        {
            tflite = new KanjiClassifier(this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // This is a Kanji handwriting font. It looks much better than the default font.
        Typeface kanjiTypeface = Typeface.createFromAsset(getApplicationContext().getAssets(), KANJI_FONT_PATH);
        textRenderer.setTypeface(kanjiTypeface);
        ResultButton.LABEL_FONT = kanjiTypeface;

        autoEvaluate = isAutoEvaluateEnabled();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Check if the saving data preference is turned on and if we have permission to write to
     * external storage.
     */
    public boolean canSaveWritingData()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAllowSaving               = sharedPreferences.getBoolean(getString(R.string.pref_key_save_data), false);
        boolean permissionGranted           = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        return isAllowSaving && permissionGranted;
    }

    public boolean isAutoEvaluateEnabled()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(getString(R.string.pref_key_auto_evaluate_input), false);
    }

    /**
     * The location we want to use to save data has been already taken by some files/directories.
     * Rename them to back them up.
     *
     * @param path the taken file path
     * @return the available file path can be used for renaming this path
     */
    public String getBackupFilepath(String path)
    {
        int counter           = 0;
        String backupFilepath = path + "_" + counter;
        File backupFile       = new File(path);
        while (backupFile.exists())
        {
            backupFilepath = path + "_" + counter;
            backupFile     = new File(backupFilepath);
            counter++;
        }
        return backupFilepath;
    }

    /**
     * Escape all invalid file name characters so that we can save the data with this file name.
     * All invalid characters are replace with underscore ('_').
     *
     * @param filename the string that may become a file name
     * @return the valid string for a file name
     */
    public String normalizeFilename(String filename)
    {
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
    public synchronized void exportWritingData(String label, final float confidence, final long timestamp, Bitmap image, List<List<CanvasPoint2D>> writingStrokes)
    {
        long startTime = SystemClock.elapsedRealtime();
        // TODO create preference for save location
        try
        {
            // TODO the image format is ARGB, write our own encoder to encode grayscale PNG
            // Android does not support encoding grayscale PNG from Bitmap.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
            byte[] grayscalePNGImage = byteArrayOutputStream.toByteArray();
            String base64Image       = Base64.encodeToString(grayscalePNGImage, Base64.DEFAULT);
            // convert the base64 string to array of string to shorten the line length in json file.
            String[] wrappedBase64String = base64Image.split("\n");
            // System.out.println(wrappedBase64String.length);

            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String savePath        = downloadDirectory.getAbsolutePath() + "/" + SAVE_DIRECTORY_NAME + "/" + WRITING_STROKE_DATA_DIR_NAME;
            // normalize path separators
            savePath = savePath.replaceAll("/+", "/");
            if (!prepareDirectory(savePath))
            {
                throw new Exception(String.format("Cannot create directory: %s", savePath));
            }

            // make directory for the label
            label               = (label == null || label.isEmpty()) ? "NO_LABEL" : label;
            String labelDirName = normalizeFilename(label);
            String labelDirPath = savePath + "/" + labelDirName;
            labelDirPath        = labelDirPath.replaceAll("/+", "/");
            if (!prepareDirectory(labelDirPath))
            {
                throw new Exception(String.format("Cannot create directory: %s", labelDirPath));
            }

            String dataFilePath  = labelDirPath + "/" + Long.toString(timestamp) + ".json";
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
            while (iterator.hasNext())
            {
                List<CanvasPoint2D> stroke = iterator.next();
                writer.beginArray();
                Iterator<CanvasPoint2D> strokeIterator = stroke.iterator();
                while (strokeIterator.hasNext())
                {
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
            for (String base64Data : wrappedBase64String)
            {
                writer.value(base64Data);
            }
            writer.endArray();
            writer.endObject();

            writer.endObject();
            writer.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        long exportDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println("Export time: " + exportDuration);
    }

    /**
     * We want to make sure there is a directory with path equals `path`. However, there may be it
     * is not existed or it is a file. We will `mkdirs` or `rename` respectively. The `path` must be
     * valid.
     *
     * @param path the directory path we want
     */
    public boolean prepareDirectory(String path)
    {
        File file = new File(path);
        if (file.exists())
        {
            if (file.isFile())
            {
                String backupFilePath = getBackupFilepath(path);
                file.renameTo(new File(backupFilePath));
                file = new File(path);
            }
            else
            {
                return true;
            }
        }

        return file.mkdirs();
    }

    public void pushText(String text)
    {
        isTextSaved = false;
        textRenderer.setText(textRenderer.getText() + text);
        textRenderer.setSelection(textRenderer.getText().length());
    }

    /**
     * Create a view to show the recognition in the UI. I also setup event listener for each view in
     * order to add text if the view is clicked.
     *
     * <p>I also plan to save the image/drawing stroke to file so that I can improve the model
     * later.
     *
     * @param r              the recognition result
     * @param image          the image associated with the result
     * @param writingStrokes list of touch points
     * @return
     */
    public View createButtonFromResult(Recognition r, Bitmap image, List<List<CanvasPoint2D>> writingStrokes)
    {
        ResultButton btn                       = new ResultButton(this, null, r.title, r.confidence);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(resultViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pushText(r.title);
                if (canSaveWritingData())
                {
                    // save image and writing strokes to files
                    exportWritingData(r.title, r.confidence, r.timestamp, image, writingStrokes);
                }
                if (autoClear)
                {
                    clearCanvas(v);
                }
            }
        });
        return btn;
    }

    /**
     * Get the image from the canvas and use the tflite model to evaluate the image. After the
     * results are returned, show them on the UI.
     *
     * @param view the View that triggers this method.
     */
    public synchronized void runClassifier(View view)
    {
        if (canvas == null || tflite == null || resultContainer == null)
        {
            return;
        }

        // long startTime = SystemClock.elapsedRealtime();
        currentEvaluatingImage          = canvas.getImage();
        currentEvaluatingWritingStrokes = canvas.getWritingStrokes();
        // System.out.println("Number of strokes: " + currentEvaluatingWritingStrokes.size());
        List<Recognition> results = tflite.recognizeImage(currentEvaluatingImage);
        // long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        // System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0)
        {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results)
        {
            resultContainer.addView(createButtonFromResult(result, currentEvaluatingImage, currentEvaluatingWritingStrokes));
        }

        // scroll the result list to the start
        resultListScrollView.scrollTo(0, 0);
    }

    public void saveWritingHistory(final String text)
    {
        if (isTextSaved || text.isEmpty())
        {
            return;
        }

        try
        {
            if (!canSaveWritingData())
            {
                return;
            }

            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String rootSavePath    = downloadDirectory.getAbsolutePath() + "/" + SAVE_DIRECTORY_NAME;
            rootSavePath           = rootSavePath.replaceAll("/+", "/");
            if (!prepareDirectory(rootSavePath))
            {
                throw new Exception(String.format("Cannot create directory: %s", rootSavePath));
            }

            String writingHistoryDirectoryPath = rootSavePath + "/" + WRITING_LOG_DIR_NAME;
            if (!prepareDirectory(writingHistoryDirectoryPath))
            {
                throw new Exception(String.format("Cannot create directory: %s", writingHistoryDirectoryPath));
            }

            int indexCounter = 0;
            String saveFilePath;
            File saveFile;

            do
            {
                saveFilePath = writingHistoryDirectoryPath + "/" + String.format("%06d.txt", indexCounter);
                saveFilePath = saveFilePath.replace("/+", "/");
                saveFile     = new File(saveFilePath);

                if (!saveFile.exists())
                {
                    saveFile.createNewFile();
                }
                else
                {
                    if (saveFile.isDirectory())
                    {
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
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Copy the text showed at the UI to clipboard so that users can paste it anywhere they want.
     *
     * @param view the View that triggers this method.
     */
    public void copyTextToClipboard(View view)
    {
        if (textRenderer.getText().length() > 0)
        {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData          = ClipData.newPlainText("text copied from handwriting input", textRenderer.getText());
            clipboard.setPrimaryClip(clipData);
        }
        saveWritingHistory(textRenderer.getText().toString());
    }

    /**
     * Clear text showing in the UI.
     */
    public void clearText(View view)
    {
        saveWritingHistory(textRenderer.getText().toString());
        textRenderer.setText("");
    }

    /**
     * This the callback method that will be called by the drawing canvas because if we attach event
     * listener to the drawing canvas, it will override the drawing logic.
     */
    @Override
    public void onTouchEnd()
    {
        if (autoEvaluate)
        {
            long ts = SystemClock.elapsedRealtime();
            synchronized (InferenceThread.LastCreatedThreadTimeLock)
            {
                InferenceThread.LastCreatedThreadTime = ts;
            }

            System.out.println("Thread " + ts + " started!");
            (new InferenceThread(this, ts)).start();
        }
    }

    /**
     * Open preference settings view.
     *
     * @param view the view that triggers this method
     */
    public void openSettings(View view)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void clearCustomLabelText(View view)
    {
        customLabelEditText.setText("");
    }

    public void clearCanvas(View view)
    {
        canvas.clearCanvas();
        currentEvaluatingImage          = null;
        currentEvaluatingWritingStrokes = null;
        if (resultContainer.getChildCount() > 0)
        {
            resultContainer.removeAllViews();
        }
    }

    public void saveWritingDataWithCustomLabel(View view)
    {
        String customLabel = customLabelEditText.getText().toString();
        if (currentEvaluatingImage != null && currentEvaluatingWritingStrokes != null && !customLabel.isEmpty())
        {
            pushText(customLabel);

            if (canSaveWritingData())
            {
                exportWritingData(customLabel, 1f, System.currentTimeMillis(), currentEvaluatingImage, currentEvaluatingWritingStrokes);
            }

            // After saving the data with custom label, I want to clear the current canvas, clear
            // the text from custom label input and hide my soft input keyboard. That is a lot of
            // activities to continue writing after saving custom label.
            if (autoClear)
            {
                // clear the canvas
                clearCanvas(view);
                // clear the label text
                customLabelEditText.setText("");
                // minimize the virtual input keyboard. Wow, this task seems pretty hard and
                // controversial.
                // https://stackoverflow.com/a/17789187/83644034
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                // I know that the view I want to hide the soft keyboard so this really help me save
                // the trouble of getting focusing view.
                // TODO I am not sure about the flags. Should I use `HIDE_IMPLICIT_ONLY`?
                imm.hideSoftInputFromWindow(customLabelEditText.getWindowToken(), 0);
            }
        }
    }

    public void lookUpMeaningWithJishoDotOrg(View view)
    {
        String japaneseText = this.textRenderer.getText().toString();
        if (!japaneseText.isEmpty())
        {
            saveWritingHistory(japaneseText);

            int selectionStart = this.textRenderer.getSelectionStart();
            int selectionEnd   = this.textRenderer.getSelectionEnd();

            System.out.println("selectionStart: " + selectionStart);
            System.out.println("selectionEnd: " + selectionEnd);
            if ((selectionEnd - selectionStart) > 0)
            {
                japaneseText = japaneseText.substring(selectionStart, selectionEnd);
            }

            System.out.println("Text to be looked up: " + japaneseText);
            try
            {
                String encodedText = URLEncoder.encode(japaneseText, "utf-8");
                System.out.println("Encoded text: " + encodedText);

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK);
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this, Uri.parse("https://jisho.org/search/" + encodedText));
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals(getString(R.string.pref_key_auto_evaluate_input)))
        {
            this.autoEvaluate = sharedPreferences.getBoolean(key, false);
        }
    }
}
