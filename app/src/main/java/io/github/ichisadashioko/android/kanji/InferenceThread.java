package io.github.ichisadashioko.android.kanji;

public class InferenceThread extends Thread {
    public static long SleepTimeout = 100;
    public static long LastCreatedThreadTime = -1;
    public static Object LastCreatedThreadTimeLock = new Object();

    public final long creationTime;
    public final MainActivity app;

    public InferenceThread(MainActivity app, long creationTime) {
        this.app = app;
        this.creationTime = creationTime;
    }

    public void run() {
        try {
            Thread.sleep(InferenceThread.SleepTimeout);
            synchronized (LastCreatedThreadTimeLock) {
                if (creationTime != LastCreatedThreadTime) {
                    // System.out.println("Thread " + creationTime + " is skipped!");
                    return;
                }
            }

            // System.out.println("Thread " + creationTime + " is OK!");

            app.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            app.runClassifier(null);
                        }
                    });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
