package com.codexperiments.leakeeper.test.common;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.codexperiments.leakeeper.test.task.AsyncTaskActivity;
import com.codexperiments.leakeeper.test.task.AsyncTaskTest;
import com.codexperiments.leakeeper.test.task.helper.TaskActivity;

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

    protected void rotateActivitySeveralTimes(int pCount) throws InterruptedException {
        for (int i = 0; i < pCount; ++i) {
            // Wait some time before turning.
            sleep(500);

            Resources lResources = getInstrumentation().getTargetContext().getResources();
            Configuration lConfiguration = lResources.getConfiguration();
            TaskActivity lCurrentActivity = (TaskActivity) mApplication.getCurrentActivity();
            lCurrentActivity.waitStarted();
            if (lConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                lCurrentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                lCurrentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            lCurrentActivity.waitTerminated();

            TaskActivity lNewActivity = (TaskActivity) mApplication.getCurrentActivity();
            while (lNewActivity == lCurrentActivity) {
                sleep(1);
                lNewActivity = (TaskActivity) mApplication.getCurrentActivity();
            }
            assertThat(lNewActivity, not(equalTo(lCurrentActivity)));
            lNewActivity.waitStarted();
        }
    }

    protected TActivity terminateActivity(TActivity pActivity) throws InterruptedException {
        pActivity.finish();
        setActivity(null);
        mApplication.setCurrentActivity(null);
        if (pActivity instanceof TaskActivity) {
            ((TaskActivity) pActivity).waitTerminated();
        }
        if (pActivity instanceof AsyncTaskActivity) {
            assertThat(((AsyncTaskActivity) pActivity).waitTerminated(), equalTo(true));
        }
        return null;
    }

    protected void garbageCollect() throws InterruptedException {
        for (int i = 0; i < 3; ++i) {
            System.gc();
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
