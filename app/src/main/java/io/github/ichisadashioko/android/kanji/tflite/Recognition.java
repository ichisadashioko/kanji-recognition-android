package io.github.ichisadashioko.android.kanji.tflite;

public class Recognition {
    public final int id;
    public final String title;
    public final float confidence;

    public Recognition(int id, String title, float confidence){
        this.id = id;
        this.title = title;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - (%.1f)", id, title, confidence);
    }
}