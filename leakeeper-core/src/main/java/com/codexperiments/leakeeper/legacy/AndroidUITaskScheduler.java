package com.codexperiments.leakeeper.legacy;

import android.os.Handler;
import android.os.Looper;

public class AndroidUITaskScheduler implements TaskScheduler {
    private Handler mUIQueue;
    private Looper mUILooper;

    public AndroidUITaskScheduler() {
        super();
        mUILooper = Looper.getMainLooper();
        mUIQueue = new Handler(mUILooper);
    }

    @Override
    public void schedule(Runnable pRunnable) {
        mUIQueue.post(pRunnable);
    }

    @Override
    public void scheduleIfNecessary(Runnable pRunnable) {
        if (Looper.myLooper() == mUILooper) {
            pRunnable.run();
        } else {
            mUIQueue.post(pRunnable);
        }
    }
}
