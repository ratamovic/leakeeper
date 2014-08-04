package com.codexperiments.leakeeper.test.task;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import com.codexperiments.leakeeper.task.TaskManager;
import com.codexperiments.leakeeper.task.TaskRef;
import com.codexperiments.leakeeper.task.handler.Task;
import com.codexperiments.leakeeper.test.common.TestApplicationContext;
import com.codexperiments.leakeeper.test.task.helper.ValueHolder;

public class AsyncTaskActivity extends Activity {
    public static final int TWO_STEPS = 2;
    private static final int MAX_WAIT_TIME = 10;

    private TaskManager<Task> mTaskManager;
    private final ValueHolder<String> mResult = new ValueHolder<>();
    private final CountDownLatch mStartedLatch = new CountDownLatch(1);
    private final CountDownLatch mDestroyedLatch = new CountDownLatch(1);

    private boolean mManaged;


    //region Utilities
    public static Intent unmanaged() {
        Intent intent = new Intent();
        intent.putExtra("MANAGED", false);
        return intent;
    }

    static Double someInputData() {
        return Math.random();
    }

    static String expectedResult(Double value) {
        return Double.toString(value);
    }
    //endregion

    //region Activity control
    ValueHolder<String> result() {
        return mResult;
    }

    public boolean waitStarted() throws InterruptedException {
        return mStartedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
    }

    public boolean waitTerminated() throws InterruptedException {
        return mDestroyedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
    }
    //endregion

    //region Activity lifecycle
    @Override
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);

        mTaskManager = TestApplicationContext.from(this).getManager(TaskManager.class);
        mManaged = getIntent().getBooleanExtra("MANAGED", true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mManaged) {
            mTaskManager.manage(this);
        }
        mStartedLatch.countDown();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mManaged) {
            mTaskManager.unmanage(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyedLatch.countDown();
    }
    //endregion


    //region Inner AsyncTask
    InnerAsyncTask executeInnerAsyncTask(final double pInput) throws InterruptedException {
        return executeInnerAsyncTask(pInput, 1);
    }

    InnerAsyncTask executeInnerAsyncTask(final double pInput, int pStepCount) throws InterruptedException {
        final InnerAsyncTask innerAsyncTask = new InnerAsyncTask(pStepCount);
        final CountDownLatch executeLatch = new CountDownLatch(1);
        new Handler(getMainLooper()).post(new Runnable() {
            public void run() {
                innerAsyncTask.execute(pInput); executeLatch.countDown();
            }
        });
        executeLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        return innerAsyncTask;
    }

    class InnerAsyncTask extends AsyncTask<Double, Integer, String> implements Task {
        private String mResult = null;
        private final int mStepCount;
        private final CyclicBarrier mStep = new CyclicBarrier(2);
        private final CountDownLatch mFinished = new CountDownLatch(1);

        private Task mTempTaskRef = null;

        InnerAsyncTask(int pStepCount) {
            mStepCount = pStepCount;
            mResult = null;
        }

        //region AsyncTask control
        String result() {
            return mResult;
        }

        boolean doStep() throws InterruptedException {
            try {
                mStep.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
                return true;
            } catch (BrokenBarrierException | TimeoutException e) {
                return false;
            }
        }

        boolean doAllSteps() throws InterruptedException {
            boolean awaitCompleted = true;
            for (int i = 0; (i < mStepCount) && awaitCompleted; ++i) {
                awaitCompleted = doStep();
            }
            return awaitCompleted;
        }

        boolean awaitFinished() throws InterruptedException {
            return mFinished.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        }
        //endregion

        //region AsyncTask lifecycle
        @Override
        protected String doInBackground(Double... pParams) {
            assertThat(pParams.length, equalTo(1));
            for (int i = 0; i < mStepCount; ++i) {
                try {
                    Thread.sleep(1000, 0);
                    mStep.await();
                } catch (InterruptedException | BrokenBarrierException exception) {
                    fail();
                }
            }
            return expectedResult(pParams[0]);
        }

        @Override
        protected void onPreExecute() {
            mTempTaskRef = mTaskManager.execute(this);
        }

        @Override
        protected void onPostExecute(String result) {
            //mTempTaskRef.onFinish(this);
            mTempTaskRef.unguard();

            // Saves result.
            mResult = result;
            if (AsyncTaskActivity.this != null) {
                AsyncTaskActivity.this.mResult.set(result);
            }
            mTempTaskRef.guard();
            mFinished.countDown();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        @TargetApi(11)
        protected void onCancelled(String result) {
            super.onCancelled(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
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
    //endregion
}
