package com.codexperiments.leakeeper.config.enforcer;

import android.os.Looper;
import com.codexperiments.leakeeper.LeakException;

public class AndroidUIThreadEnforcer implements ThreadEnforcer {
    private final Looper mUILooper = Looper.getMainLooper();

    @Override
    public void enforce() {
        if (Looper.myLooper() != mUILooper) {
            throw new LeakException("Must be executed from the UI-Thread only.");
        }
    }
}