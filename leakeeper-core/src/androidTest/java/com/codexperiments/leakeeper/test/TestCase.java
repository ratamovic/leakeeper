package com.codexperiments.leakeeper.test;

import static java.lang.Thread.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;

public class TestCase<TActivity extends Activity> extends ActivityInstrumentationTestCase2<TActivity> {
    private volatile Application mApplication;
    private Class<?> mActivityClass;
    private TActivity mCurrentActivity;

    public TestCase(Class<TActivity> pActivityClass) {
        super(pActivityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Patch to synchronize Application and Test initialization, as Application initialization occurs
        // on the main thread whereas Test initialization occurs on the Instrumentation thread...
        while (mApplication == null) {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    // No op.
                }
            });
        }

        // Execute initialization code on UI Thread. If an exception occurs during thread initialization, re-throw the
        // exception on the instrumentation thread.
        final Exception[] throwableHolder = new Exception[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    setUpOnUIThread();
                } catch (Exception exception) {
                    throwableHolder[0] = exception;
                }
            }
        });
        if (throwableHolder[0] != null) throw new RuntimeException("UIThread test setup failed", throwableHolder[0]);
    }

    protected void setUpOnUIThread() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mApplication = null;
    }

    public Application getApplication() {
        return mApplication;
    }

    @Override
    public TActivity getActivity() {
        return mCurrentActivity;
    }

    public TActivity createActivity() {
        if (mCurrentActivity != null) throw new RuntimeException("Activity already created");

        mCurrentActivity = super.getActivity();
        return mCurrentActivity;
    }

    public TActivity createActivity(Intent pIntent) {
        setActivityIntent(pIntent);
        return createActivity();
    }

    protected TActivity terminateActivity(TActivity pActivity) throws InterruptedException {
        pActivity.finish();
        setActivity(null);
        mCurrentActivity = null;
        return null;
    }

    protected void rotateActivitySeveralTimes(int pTimes) throws InterruptedException {
        for (int i = 0; i < pTimes; ++i) {
            // Wait some time before turning.
            sleep(500);

            Resources resources = getInstrumentation().getTargetContext().getResources();
            Configuration configuration = resources.getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    protected void garbageCollect() throws InterruptedException {
        // Although there is no guarantee memory will be perfectly cleaned, Android Garbage Collector is sufficiently
        // aggressive to make this code "relatively" reliable.
        for (int i = 0; i < 3; ++i) {
            System.gc();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    System.gc();
                }
            });
        }
    }
}
