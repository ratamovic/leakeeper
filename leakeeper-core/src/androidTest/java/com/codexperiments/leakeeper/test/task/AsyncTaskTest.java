package com.codexperiments.leakeeper.test.task;

import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.TWO_STEPS;
import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.expectedResult;
import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.someInputData;
import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.unmanaged;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import android.app.Activity;
import android.app.Fragment;

import com.codexperiments.leakeeper.task.TaskManagerConfig;
import com.codexperiments.leakeeper.task.android.AndroidTaskManager;
import com.codexperiments.leakeeper.task.android.AndroidTaskManagerConfig;
import com.codexperiments.leakeeper.task.handler.TaskResult;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.task.AsyncTaskActivity.InnerAsyncTask;
import com.codexperiments.leakeeper.test.task.helper.ValueHolder;

public class AsyncTaskTest extends TestCase<AsyncTaskActivity> {
    private AndroidTaskManager mTaskManager;

    public AsyncTaskTest() {
        super(AsyncTaskActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void setUpOnUIThread() throws Exception {
        super.setUpOnUIThread();
    }

    //region Given
    private AsyncTaskActivity givenActivityManaged() {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                TaskManagerConfig config = new AndroidTaskManagerConfig(getApplication().getApplication());
                mTaskManager = new AndroidTaskManager(getApplication().getApplication(), config, TaskResult.class);
                getApplicationContext().registerManager(mTaskManager);
            }
        });
        return getActivity();
    }

    private AsyncTaskActivity givenActivityUnmanaged() {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                TaskManagerConfig taskManagerConfig = new AndroidTaskManagerConfig(getApplication().getApplication()) {
                    @Override
                    public Object resolveEmitterId(Object pEmitter) {
                        return null;
                    }

                    @Override
                    protected Object resolveActivityId(Activity pActivity) {
                        return null;
                    }

                    @Override
                    protected Object resolveFragmentId(Fragment pFragment) {
                        return null;
                    }

                    @Override
                    protected Object resolveFragmentId(android.support.v4.app.Fragment pFragment) {
                        return null;
                    }
                };
                mTaskManager = new AndroidTaskManager(getApplication().getApplication(), taskManagerConfig, TaskResult.class);
                getApplicationContext().registerManager(mTaskManager);
            }
        });
        return getActivity(unmanaged());
    }
    //endregion


    //region Unanaged activity tests
    public void testExecute_innerAsyncTask_activity_unmanaged_normal() throws InterruptedException {
        // GIVEN Activity is not managed.
        final AsyncTaskActivity initialActivity = givenActivityUnmanaged();
        final double input = someInputData();

        // WHEN Activity is living during the whole AsyncTask lifecycle.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input);
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
    }

    public void testExecute_innerAsyncTask_activity_unmanaged_notGarbageCollected() throws InterruptedException {
        // GIVEN Activity is not managed.
        AsyncTaskActivity initialActivity = givenActivityUnmanaged();
        final double input = someInputData();

        // WHEN Activity is destroyed while AsyncTask is running but not garbage collected yet.
        // AND Activity is recreated.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input, TWO_STEPS);
        assertThat(asyncTask.doStep(), equalTo(true));
        final AsyncTaskActivity recreatedActivity = recreateActivity(); // Look here.
        assertThat(asyncTask.doStep(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND initial Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
        // AND recreated Activity is not managed so not notified of the result.
        assertThat(recreatedActivity, notNullValue());
        assertThat(recreatedActivity.result().value(), nullValue());
    }

    public void testExecute_innerAsyncTask_activity_unmanaged_garbageCollected() throws InterruptedException {
        // GIVEN Activity is not managed.
        AsyncTaskActivity initialActivity = givenActivityUnmanaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();
        final double input = someInputData();

        // WHEN Activity is destroyed and garbage collected while AsyncTask is running.
        // AND Activity is recreated.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input);
        final AsyncTaskActivity recreatedActivity = recreateActivity(); // Look here.
        // Try to ensure the emitter gets garbage collected. WARNING: We don't have full control on the garbage collector so we
        // can guarantee this will work! This test may fail at any moment although it works for now. Such a failure occur may
        // occur in the BackgroundTask when checking if emitter is null (it should be null but it won't be in case of failure). A
        // failure could also mean there is a memory-leak somewhere...
        initialActivity = null;
        garbageCollect(); // And here.
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is not notified of the result.
        assertThat(initialActivity, nullValue());
        assertThat(initialActivityResult.value(), nullValue());
        // AND recreated Activity is not managed so not notified of the result.
        assertThat(recreatedActivity, notNullValue());
        assertThat(recreatedActivity.result().value(), nullValue());
    }
    //endregion


    //region Managed activity tests
    public void testExecute_innerAsyncTask_activity_managed_normal() throws InterruptedException {
        // GIVEN Activity is managed.
        final AsyncTaskActivity initialActivity = givenActivityManaged();
        final double input = someInputData();

        // WHEN Activity is living during the whole AsyncTask lifecycle.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input);
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
    }

    public void testExecute_innerAsyncTask_activity_managed_destroyed() throws InterruptedException {
        // GIVEN Activity is managed.
        AsyncTaskActivity initialActivity = givenActivityManaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();
        final double input = someInputData();

        // WHEN Activity is destroyed while AsyncTask is running.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input);
        initialActivity = terminateActivity(initialActivity); // Look here.
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is not notified of the result.
        assertThat(initialActivity, nullValue());
        assertThat(initialActivityResult.value(), nullValue());
    }

    public void testExecute_innerAsyncTask_activity_managed_recreated() throws InterruptedException {
        // GIVEN Activity is managed.
        AsyncTaskActivity initialActivity = givenActivityManaged();
        final double input = someInputData();

        // WHEN Activity is recreated while AsyncTask is running.
        InnerAsyncTask asyncTask = initialActivity.executeInnerAsyncTask(input, TWO_STEPS);
        assertThat(asyncTask.doStep(), equalTo(true));
        final AsyncTaskActivity recreatedActivity = recreateActivity(); // Look here.
        assertThat(asyncTask.doStep(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND initial Activity is not notified of the result since it has been replaced.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), nullValue());
        // AND recreated Activity is notified of the result.
        assertThat(recreatedActivity, notNullValue());
        assertThat(recreatedActivity.result().value(), equalTo(expectedResult(input)));
    }
    //endregion
}
