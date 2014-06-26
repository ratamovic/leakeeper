package com.codexperiments.robolabor.test.task.helper;

import android.util.Log;
import com.codexperiments.robolabor.task.TaskRef;
import com.codexperiments.robolabor.task.handler.Task;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BackgroundTask implements Task/*<Integer, Integer>*/ {
    public static final int TASK_STEP_COUNT = 5;
    // At least one test must wait until this delay has ended. So please avoid increasing it except for debugging purpose.
    public static final int TASK_TIMEOUT_MS = 10000;

    private Task/*<Integer, Integer>*/ mTaskRef;
    private Boolean mCheckEmitterNull;
    private boolean mStepByStep;
    private int mStepCounter;
    private Integer mTaskResult;
    private Throwable mTaskException;

    private CountDownLatch mTaskFinished;

    public BackgroundTask(Integer pTaskResult, Boolean pCheckEmitterNull, boolean pStepByStep) {
        this(pTaskResult, null, pCheckEmitterNull, pStepByStep);

    }

    public BackgroundTask(Exception pTaskException, Boolean pCheckEmitterNull, boolean pStepByStep) {
        this(null, pTaskException, pCheckEmitterNull, pStepByStep);

    }

    public BackgroundTask(Integer pTaskResult, Exception pTaskException, Boolean pCheckEmitterNull, boolean pStepByStep) {
        super();

        mCheckEmitterNull = pCheckEmitterNull;
        mStepByStep = pStepByStep;
        mStepCounter = pStepByStep ? TASK_STEP_COUNT : 0;
        mTaskResult = null;
        mTaskException = null;

        mTaskFinished = new CountDownLatch(1);
    }

//    public Integer onProcess(Integer pParam) throws Exception {
//        assertThat(mTaskFinished.getCount(), equalTo(1l)); // Ensure task is executed only once.
//        // We have two cases here: either we loop until step by step is over or until we have performed all iterations.
//        // We know that we are in step by step mode if mStepByStep is true at the beginning of the loop.
//        // for (int i = mStepByStep ? TASK_STEP_COUNT : 0; mStepByStep || (i < TASK_STEP_COUNT); ++i) {
//        while (true) {
//            awaitStart();
//            if (mAwaitFinished && (mStepCounter >= TASK_STEP_COUNT)) break;
//
//            Thread.sleep(TASK_STEP_DURATION_MS);
//            ++mStepCounter;
//            notifyEnded();
//        }
//        if (mExpectedTaskException == null) {
//            return mExpectedTaskResult;
//        } else {
//            throw mExpectedTaskException;
//        }
//    }

    @Override
    public void onFinish(/*Integer*/Object pTaskResult) {
        // Check if outer object reference has been restored (or not).
        if (mCheckEmitterNull != null) {
            if (mCheckEmitterNull) {
                Object lEmitter = getEmitter();
                try {
                    Thread.sleep(1000, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertThat(lEmitter, nullValue());
            } else {
                assertThat(getEmitter(), not(nullValue()));
            }
        }

        // Save result. TODO This is stupid since result is already initialized... Create a new variable.
        mTaskResult = (Integer) pTaskResult;
        // Notify listeners that task execution is finished.
        assertThat(mTaskFinished.getCount(), equalTo(1l)); // Ensure termination handler is executed only once.
        mTaskFinished.countDown();
    }

    @Override
    public void onFail(Throwable pTaskException) {
        mTaskException = pTaskException;
        assertThat(mTaskFinished.getCount(), equalTo(1l)); // Ensure termination handler is executed only once.
        mTaskFinished.countDown();
    }

//    private void awaitStart() {
//        if (mTaskStepStart != null) {
//            try {
//                mTaskStepStart.await(TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
//                if (mAwaitFinished) {
//                    mTaskStepStart = null;
//                } else {
//                    mTaskStepStart.reset();
//                }
//            } catch (TimeoutException | InterruptedException | BrokenBarrierException exception) {
//                fail();
//            }
//        }
//    }

//    private void notifyEnded() {
//        if (mTaskStepStart != null) {
//            mTaskStepEnd.countDown();
//        }
//    }

    /**
     * Works in step by step mode only.
     */
//    public boolean awaitStepExecuted() {
//        if (mTaskStepStart == null) return false;
//
//        try {
//            if (!(mTaskStepStart.await(TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS) >= 0)) return false;
//            boolean lResult = mTaskStepEnd.await(TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
//            mTaskStepEnd = new CountDownLatch(1);
//            return lResult;
//        } catch (TimeoutException eTimeoutException) {
//            return false;
//        } catch (InterruptedException eInterruptedException) {
//            fail();
//            return false;
//        } catch (BrokenBarrierException eBrokenBarrierException) {
//            fail();
//            return false;
//        }
//    }

    /**
     * Works in step by step mode only.
     */
    public boolean awaitFinished() {
        try {
            return mTaskFinished.await(TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException eInterruptedException) {
            fail();
            return false;
        }
    }

    public void reset() {
        mTaskFinished = new CountDownLatch(1);
    }

    public Integer getTaskResult() {
        return mTaskResult;
    }

    public Throwable getTaskException() {
        return mTaskException;
    }

    protected Boolean getCheckEmitterNull() {
        return mCheckEmitterNull;
    }

    protected Boolean getStepByStep() {
        return mStepByStep;
    }

    public Task/*<Integer, Integer>*/ getTaskRef() {
        return mTaskRef;
    }

    public void setTaskRef(Task/*<Integer, Integer>*/ pTaskRef) {
        mTaskRef = pTaskRef;
    }

    /**
     * Override in child classes to handle "inner tasks".
     * 
     * @return Task emitter (i.e. the outer class containing the task).
     */
    public Object getEmitter() {
        return null;
    }

    @Override
    public TaskRef<Integer> toRef() {
        return null; // TODO XXX FIXME
    }

    @Override
    public String toString() {
        return "BackgroundTask [mTaskResult=" + mTaskResult + ", mTaskException=" + mTaskException + ", mStepCounter="
                        + mStepCounter + "]";
    }
}
