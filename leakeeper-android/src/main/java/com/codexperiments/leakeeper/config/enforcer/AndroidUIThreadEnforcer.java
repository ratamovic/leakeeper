package com.codexperiments.leakeeper.config.enforcer;

import android.os.Looper;
import com.codexperiments.leakeeper.CallbackException;

public class AndroidUIThreadEnforcer implements ThreadEnforcer {
    private final Looper mUILooper = Looper.getMainLooper();

    @Override
    public void enforce() {
        if (Looper.myLooper() != mUILooper) {
            throw new CallbackException("Must be executed from the UI-Thread only.");
        }
    }
}