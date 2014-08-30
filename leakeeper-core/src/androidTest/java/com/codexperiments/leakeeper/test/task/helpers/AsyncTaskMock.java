package com.codexperiments.leakeeper.test.task.helpers;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.codexperiments.leakeeper.task.TaskManager;
import com.codexperiments.leakeeper.task.TaskRef;
import com.codexperiments.leakeeper.task.handler.Task;

import java.util.concurrent.*;

import static com.codexperiments.leakeeper.test.task.helpers.AsyncTaskActivityMock.expectedResult;
import static org.junit.Assert.fail;

public abstract class AsyncTaskMock extends AsyncTask<Double, Integer, String> implements Task {
    private static final int MAX_WAIT_TIME = 10;

    // Dependencies
    protected final TaskManager mTaskManager;
    // Internal state
    private final CyclicBarrier mStepBarrier;
    private boolean mRequestStop = false, mStop = false;
    private final CountDownLatch mFinishedLatch = new CountDownLatch(1);
    private String mResult = null;
    // Rubbishes
    private Task mTempTaskRef = null;

    AsyncTaskMock(TaskManager pTaskManager) {
        mTaskManager = pTaskManager;
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
        mTempTaskRef = mTaskManager.execute(this);
    }

    @Override
    protected final void onPostExecute(String result) {
        mTempTaskRef.unguard();

        // Saves result.
        mResult = result;
        onSaveResult(result);
        mTempTaskRef.guard();
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

    @Override
    public TaskRef toRef() {
        return null;
    }

    @Override
    public void onFinish(Object pResult) {
        throw new IllegalAccessError();
    }

    @Override
    public void onFail(Throwable pException) {
        throw new IllegalAccessError();
    }

    @Override
    public void guard() {
        throw new IllegalAccessError();
    }

    @Override
    public boolean unguard() {
        throw new IllegalAccessError();
    }
    //endregion
}