package io.github.ichisadashioko.android.kanji.tflite;

public class Recognition {
    public final int id;
    public final long timestamp;
    public final String title;
    public final float confidence;

    public Recognition(int id, long timestamp, String title, float confidence) {
        this.id = id;
        this.timestamp = timestamp;
        this.title = title;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s - (%.1f)", id, title, confidence);
    }
}