package com.codexperiments.leakeeper.test.common;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import com.codexperiments.leakeeper.CallbackManager;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Base test class with some facilities to initialize and manipulate activities.
 */
public class TestCase<TActivity extends TestActivity> extends ActivityInstrumentationTestCase2<TActivity> {
    public static int MAX_WAIT_TIME = 100000;
    private static TestCase<?> sInstance;

    private volatile TActivity mCurrentActivity;
    private Map<Class<?>, Object> mManagers = new HashMap<>();

    public TestCase(Class<TActivity> pActivityClass) {
        super(pActivityClass);
    }


    //region Lifecycle
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sInstance = this;

        // Patch for Mockito bug: https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        // Patch to synchronize Application and Test initialization, as Application initialization occurs
        // on the main thread whereas Test initialization occurs on the Instrumentation thread...
        Application application;
        do {
            getInstrumentation().runOnMainSync(new Runnable() {
                public void run() {
                    // No op.
                }
            });
            application = (Application) getInstrumentation().getTargetContext().getApplicationContext();
        } while (application == null);

        // Execute initialization code on UI Thread.
        final Exception[] throwableHolder = new Exception[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    setUpOnUIThread();
                } catch (Exception eException) {
                    throwableHolder[0] = (eException);
                }
            }
        });
        // If an exception occurred during UI Thread initialization, re-throw it.
        if (throwableHolder[0] != null) {
            throw throwableHolder[0];
        }
    }

    protected void setUpOnUIThread() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
        mCurrentActivity = null;
        mManagers.clear();
        super.tearDown();
    }
    //endregion


    //region Accessors
    @Override
    public TActivity getActivity() {
        TActivity currentActivity = mCurrentActivity;
        if (currentActivity != null) return currentActivity;
        else {
            currentActivity = super.getActivity();
            mCurrentActivity = currentActivity;
            return currentActivity;
        }
    }

    public TActivity getActivity(Intent pIntent) {
        setActivityIntent(pIntent);
        return getActivity();
    }

    public void register(Class<?> pManagerClass, CallbackManager pCallbackManager) {
        // There is no guaranteed thread-safety if activity is already started.
        if (mCurrentActivity != null) throw new IllegalStateException("Activity already started");
        mManagers.put(pManagerClass, pCallbackManager);
    }

    @SuppressWarnings("unchecked")
    public static <TActivity extends TestActivity, TManager>
    TManager inject(TActivity pActivity, Class<TManager> pManagerClass) {
        // We assume all activities will need to get a manager during initialization. Thus, at this point in the code, we can
        // assume a new activity has just been created and so save a reference to it. It's a bit implicit but I see no better way.
        ((TestCase<TActivity>) sInstance).mCurrentActivity = pActivity;
        return (TManager) sInstance.mManagers.get(pManagerClass);
    }
    //endregion


    //region Activity control
    protected TActivity recreateActivity() {
        // Wait some time before turning.
        try {
            sleep(500);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }

        // When activity is started, rotate it.
        Resources resources = getInstrumentation().getTargetContext().getResources();
        TActivity initialActivity = mCurrentActivity;
        initialActivity.waitStarted();
        if (resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            initialActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            initialActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        // When activity is terminated, get the new activity reference.
        initialActivity.waitTerminated();
        TActivity newActivity;
        do {
            newActivity = mCurrentActivity;
        } while (newActivity == initialActivity);
        setActivity(newActivity);

        // Wait for the new activity to be started.
        newActivity.waitStarted();
        return newActivity;
    }

    protected TActivity terminateActivity() {
        mCurrentActivity.finish();
        assertThat(mCurrentActivity.waitTerminated(), equalTo(true));

        setActivity(null);
        mCurrentActivity = null;
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
    //endregion
}
