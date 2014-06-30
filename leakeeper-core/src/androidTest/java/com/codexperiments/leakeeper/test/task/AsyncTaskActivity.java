package com.codexperiments.leakeeper.test.task;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import com.codexperiments.leakeeper.task.TaskManager;
import com.codexperiments.leakeeper.task.handler.Task;
import com.codexperiments.leakeeper.test.common.TestApplicationContext;
import com.codexperiments.leakeeper.test.task.helper.ValueHolder;

import java.util.concurrent.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AsyncTaskActivity extends Activity {
    private TaskManager<Task> mTaskManager;
    private ValueHolder<String> mResult = new ValueHolder<>();
    private CountDownLatch mStartedLatch = new CountDownLatch(1);
    private CountDownLatch mDestroyedLatch = new CountDownLatch(1);

    //region Utilities
    static Double someInputData() {
        return Math.random();
    }

    static String expectedResult(Double value) {
        return Double.toString(value);
    }
    //endregion

    //region Accessors
    ValueHolder<String> result() {
        return mResult;
    }

    public boolean waitStarted() throws InterruptedException {
        return mStartedLatch.await(10, TimeUnit.SECONDS);
    }

    public boolean waitTerminated() throws InterruptedException {
        return mDestroyedLatch.await(10, TimeUnit.SECONDS);
    }
    //endregion

    //region Activity lifecycle
    @Override
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);
        if (pBundle != null) {
            mResult.set(pBundle.getString("TaskResult"));
        }

        mTaskManager = TestApplicationContext.from(this).getManager(TaskManager.class);
        mTaskManager.manage(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTaskManager.manage(this);
        mStartedLatch.countDown();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTaskManager.unmanage(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyedLatch.countDown();
    }
    //endregion

    //region Inner AsyncTask
    InnerAsyncTask createInnerAsyncTask() {
        return new InnerAsyncTask(1);
    }

    class InnerAsyncTask extends AsyncTask<Double, Integer, String> {
        private final int mStepCount;
        private final CyclicBarrier mStep = new CyclicBarrier(2);
        private final CountDownLatch mFinished = new CountDownLatch(1);

        InnerAsyncTask(int pStepCount) {
            mStepCount = pStepCount;
        }

        //region Accessors
        ValueHolder<String> result() {
            return mResult;
        }

        boolean doStep() throws InterruptedException {
            try {
                mStep.await(10, TimeUnit.SECONDS);
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
            return mFinished.await(10, TimeUnit.SECONDS);
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
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            if (AsyncTaskActivity.this != null) {
                mResult.set(result);
            }
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
        //endregion
    }
    //endregion
}
