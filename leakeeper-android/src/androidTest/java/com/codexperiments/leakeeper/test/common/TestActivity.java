package com.codexperiments.leakeeper.test.common;

import android.app.Activity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.codexperiments.leakeeper.test.common.TestCase.MAX_WAIT_TIME;

/**
 * Base Activity that can be manipulated by a TestCase.
 */
public abstract class TestActivity extends Activity {
    private final CountDownLatch mStartedLatch = new CountDownLatch(1);
    private final CountDownLatch mDestroyedLatch = new CountDownLatch(1);

    @Override
    protected void onStart() {
        super.onStart();
        mStartedLatch.countDown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyedLatch.countDown();
    }

    public boolean waitStarted() {
        try {
            return mStartedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }

    public boolean waitTerminated() {
        try {
            return mDestroyedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }
}
