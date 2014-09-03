package com.codexperiments.leakeeper.test.task.helpers;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.codexperiments.leakeeper.task.LeakContainer;
import com.codexperiments.leakeeper.task.LeakManager;

import java.util.concurrent.*;

import static org.junit.Assert.fail;

/**
 * Base AsyncTask class that transforms an input double value into an output string.
 */
public abstract class AsyncTaskMock extends AsyncTask<Double, Integer, String> {
    private static final int MAX_WAIT_TIME = 10;

    // Dependencies
    protected static LeakManager sLeakManager;
    // Internal state
    private LeakContainer mContainer = null;
    private final CyclicBarrier mStepBarrier;
    private boolean mRequestStop = false, mStop = false;
    private final CountDownLatch mFinishedLatch = new CountDownLatch(1);
    private String mResult = null;

    AsyncTaskMock(LeakManager pLeakManager) {
        sLeakManager = pLeakManager;
        mStepBarrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                mStop = mRequestStop;
            }
        });
    }


    //region Extensions
    protected abstract AsyncTaskActivityMock getActivity();

    protected abstract void onSaveResult(String result);
    //endregion


    //region Control
    public AsyncTaskMock doExecute(final double pInput) {
        final CountDownLatch startedLatch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                execute(pInput);
                startedLatch.countDown();
            }
        });

        try {
            startedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
        return this;
    }

    public boolean doStep() {
        try {
            mStepBarrier.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
            return true;
        } catch (BrokenBarrierException | TimeoutException exception) {
            return false;
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }

    public boolean doFinish() {
        mRequestStop = true;
        if (!doStep()) return false;

        try {
            return mFinishedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }
    //endregion


    //region Lifecycle
    @Override
    protected final String doInBackground(Double... pParams) {
        if (pParams.length != 1) throw new IllegalArgumentException();

        boolean finish = false;
        while (!finish) {
            try {
                Thread.sleep(1000, 0);
                mStepBarrier.await();
                // mStop is reevaluated by the step barrier.
                finish = mStop;
            } catch (InterruptedException | BrokenBarrierException exception) {
                fail();
            }
        }
        return expectedResult(pParams[0]);
    }

    @Override
    protected final void onPreExecute() {
        mContainer = sLeakManager.wrap(this);
    }

    @Override
    protected final void onPostExecute(String result) {
        mContainer.unguard();

        // Saves result.
        mResult = result;
        onSaveResult(result);
        mContainer.guard();
        mFinishedLatch.countDown();
    }

    @Override
    protected final void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    @TargetApi(11)
    protected final void onCancelled(String result) {
        super.onCancelled(result);
    }

    @Override
    protected final void onCancelled() {
        super.onCancelled();
    }
    //endregion


    //region Utilities
    public String result() {
        return mResult;
    }

    public static Double someInputData() {
        return Math.random();
    }

    public static String expectedResult(Double pValue) {
        return Double.toString(pValue);
    }
    //endregion
}