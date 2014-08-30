package com.codexperiments.leakeeper.test.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import com.codexperiments.leakeeper.test.task.helpers.AsyncTaskActivityMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestCase<TActivity extends Activity> extends ActivityInstrumentationTestCase2<TActivity> {
    private Class<?> mActivityClass;
    private TestApplication mApplication;
    private TestApplicationContext mApplicationContext;

    public TestCase(Class<TActivity> pActivityClass) {
        super(pActivityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Patch for Mockito bug: https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mApplication = TestApplication.getInstance(this);
        mApplicationContext = mApplication.provideContext();

        // Execute initialization code on UI Thread.
        final List<Exception> lThrowableHolder = new ArrayList<Exception>(1);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    setUpOnUIThread();
                } catch (Exception eException) {
                    lThrowableHolder.add(eException);
                }
            }
        });
        // If an exception occurred during UI Thread initialization, re-throw the exception.
        if (lThrowableHolder.size() > 0) {
            throw lThrowableHolder.get(0);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mApplication.setCurrentActivity(null);
        mApplicationContext.removeManagers();
    }

    @Override
    @SuppressWarnings("unchecked")
    public TActivity getActivity() {
        Activity lActivity = mApplication.getCurrentActivity();
        if (lActivity == null) {
            TActivity lNewActivity = super.getActivity();
            mActivityClass = lNewActivity.getClass();
            mApplication.setCurrentActivity(lNewActivity);
            return lNewActivity;
        } else {
            if (mActivityClass.isInstance(lActivity)) {
                return (TActivity) lActivity;
            } else {
                throw new RuntimeException(String.format("Wrong Activity type retrieved (%1$s).", lActivity.getClass()));
            }
        }
    }

    public TActivity getActivity(Intent pIntent) {
        setActivityIntent(pIntent);
        return getActivity();
    }

    @SuppressWarnings("unchecked")
    public <TOtherActivity extends Activity> TOtherActivity getOtherActivity(Class<TOtherActivity> pOtherActivityClass) {
        return (TOtherActivity) mApplication.getCurrentActivity();
    }

    protected void setUpOnUIThread() throws Exception {
    }

    // TODO Refactor generics and give more flexibility on the lifecycle.
    protected AsyncTaskActivityMock recreateActivity() {
        // Wait some time before turning.
        try {
            sleep(500);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }

        // When activity is started, rotate it.
        Resources resources = getInstrumentation().getTargetContext().getResources();
        AsyncTaskActivityMock initialActivity = (AsyncTaskActivityMock) mApplication.getCurrentActivity();
        initialActivity.waitStarted();
        if (resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            initialActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            initialActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        // When activity is terminated, get the new activity reference.
        initialActivity.waitTerminated();
        AsyncTaskActivityMock newActivity = (AsyncTaskActivityMock) mApplication.getCurrentActivity();
        while (newActivity == initialActivity) {
            newActivity = (AsyncTaskActivityMock) mApplication.getCurrentActivity();
        }
        setActivity(newActivity);

        // Wait for the new activity to be started.
        newActivity.waitStarted();
        return newActivity;
    }

    protected TActivity terminateActivity(TActivity pActivity) {
        pActivity.finish();
        setActivity(null);
        mApplication.setCurrentActivity(null);
        if (pActivity instanceof AsyncTaskActivityMock) {
            assertThat(((AsyncTaskActivityMock) pActivity).waitTerminated(), equalTo(true));
        }
        return null;
    }

    protected void garbageCollect() {
        for (int i = 0; i < 3; ++i) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(interruptedException);
            }
            System.gc();
            System.runFinalization();
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    System.gc();
                }
            });
        }
    }

    protected boolean isServiceRunning(final Class<?> pServiceClass) {
        final AtomicBoolean lResult = new AtomicBoolean();
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                ActivityManager lManager = (ActivityManager) mApplication.getApplication().getSystemService(Context.ACTIVITY_SERVICE);
                for (RunningServiceInfo service : lManager.getRunningServices(Integer.MAX_VALUE)) {
                    if (pServiceClass.getName().equals(service.service.getClassName())) {
                        lResult.set(true);
                    }
                }
            }
        });
        return lResult.get();
    }

    public TestApplication getApplication() {
        return mApplication;
    }

    public TestApplicationContext getApplicationContext() {
        return mApplicationContext;
    }
}
