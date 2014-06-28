package com.codexperiments.leakeeper.test.common;

import android.app.Activity;
import android.app.Application;

public class TestApplication implements TestApplicationContext.Provider
{
    private static volatile TestApplication sInstance;

    private TestApplicationContext mApplicationContext;
    private volatile Application mApplication;
    private volatile Activity mCurrentActivity;

    public static TestApplication getInstance(final com.codexperiments.leakeeper.test.common.TestCase<?> pTestCase)
    {
        // Patch to synchronize Application and Test initialization, as Application initialization occurs
        // on the main thread whereas Test initialization occurs on the Instrumentation thread...
        while (pTestCase.getInstrumentation().getTargetContext().getApplicationContext() == null) {
            pTestCase.getInstrumentation().runOnMainSync(new Runnable() {
                public void run()
                {
                    // No op.
                }
            });
        }
        TestApplication.sInstance = new TestApplication((Application) pTestCase.getInstrumentation().getTargetContext().getApplicationContext());
        return TestApplication.sInstance;
    }

    public static TestApplication getInstance()
    {
        return TestApplication.sInstance;
    }

    public TestApplication(Application pApplication)
    {
        super();
        mApplicationContext = new TestApplicationContext(pApplication);
        mApplication = pApplication;
        sInstance = this;
    }


    @Override
    public TestApplicationContext provideContext()
    {
        return mApplicationContext;
    }

    public Activity getCurrentActivity()
    {
        return mCurrentActivity;
    }

    public void setCurrentActivity(Activity pCurrentActivity)
    {
        mCurrentActivity = pCurrentActivity;
    }

    public Application getApplication()
    {
        return mApplication;
    }
}
