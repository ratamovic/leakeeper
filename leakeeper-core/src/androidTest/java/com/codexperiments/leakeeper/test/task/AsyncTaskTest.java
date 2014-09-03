package com.codexperiments.leakeeper.test.task;

import android.app.Activity;
import android.app.Fragment;
import com.codexperiments.leakeeper.task.LeakManager;
import com.codexperiments.leakeeper.task.LeakManagerConfig;
import com.codexperiments.leakeeper.task.android.AndroidLeakManager;
import com.codexperiments.leakeeper.task.android.AndroidLeakManagerConfig;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.task.helpers.ValueHolder;
import com.codexperiments.leakeeper.test.task.helpers.AsyncTaskActivityMock;
import com.codexperiments.leakeeper.test.task.helpers.AsyncTaskMock;

import static com.codexperiments.leakeeper.test.task.helpers.AsyncTaskActivityMock.*;
import static com.codexperiments.leakeeper.test.task.helpers.AsyncTaskMock.expectedResult;
import static com.codexperiments.leakeeper.test.task.helpers.AsyncTaskMock.someInputData;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test Leakeeper applied to AsyncTasks.
 */
public class AsyncTaskTest extends TestCase<AsyncTaskActivityMock> {
    public AsyncTaskTest() {
        super(AsyncTaskActivityMock.class);
    }


    //region Given
    private AsyncTaskActivityMock givenActivityManaged() {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                LeakManagerConfig config = new AndroidLeakManagerConfig(getApplication());
                AndroidLeakManager leakManager = new AndroidLeakManager<>(AsyncTaskMock.class, config);
                register(LeakManager.class, leakManager);
            }
        });
        return getActivity();
    }

    private AsyncTaskActivityMock givenActivityUnmanaged() {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                LeakManagerConfig config = new AndroidLeakManagerConfig(getApplication()) {
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
                AndroidLeakManager leakManager = new AndroidLeakManager<>(AsyncTaskMock.class, config);
                register(LeakManager.class, leakManager);
            }
        });
        return getActivity(unmanaged());
    }
    //endregion


    //region Classic class tests
    public void testExecute_classicTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(classicTaskFactory());
    }

    public void testExecute_classicTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(classicTaskFactory());
    }

    public void testExecute_classicTask_activity_managed_normal() {
        testExecute_activity_managed_normal(classicTaskFactory());
    }

    public void testExecute_classicTask_activity_managed_destroyedAndCollected() {
        testExecute_activity_managed_destroyedAndCollected(classicTaskFactory());
    }
    //endregion


    //region Static class tests
    public void testExecute_staticTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(staticTaskFactory());
    }

    public void testExecute_staticTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(staticTaskFactory());
    }

    public void testExecute_staticTask_activity_managed_normal() {
        testExecute_activity_managed_normal(staticTaskFactory());
    }

    public void testExecute_staticTask_activity_managed_destroyedAndCollected() {
        testExecute_activity_managed_destroyedAndCollected(staticTaskFactory());
    }
    //endregion


    //region Inner class tests
    public void testExecute_innerTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(innerTaskFactory());
    }

    public void testExecute_innerTask_activity_unmanaged_destroyedButNotCollected() {
        testExecute_activity_unmanaged_destroyedButNotCollected(innerTaskFactory());
    }

    public void testExecute_innerTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(innerTaskFactory());
    }

    public void testExecute_innerTask_activity_managed_normal() {
        testExecute_activity_managed_normal(innerTaskFactory());
    }

    public void testExecute_innerTask_activity_managed_destroyed() {
        testExecute_activity_managed_destroyed(innerTaskFactory());
    }

    public void testExecute_innerTask_activity_managed_recreated() {
        testExecute_activity_managed_recreated(innerTaskFactory());
    }
    //endregion


    //region Anonymous class tests
    public void testExecute_anonymousTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(anonymousTaskFactory());
    }

    public void testExecute_anonymousTask_activity_unmanaged_destroyedButNotCollected() {
        testExecute_activity_unmanaged_destroyedButNotCollected(anonymousTaskFactory());
    }

    public void testExecute_anonymousTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(anonymousTaskFactory());
    }

    public void testExecute_anonymousTask_activity_managed_normal() {
        testExecute_activity_managed_normal(anonymousTaskFactory());
    }

    public void testExecute_anonymousTask_activity_managed_destroyed() {
        testExecute_activity_managed_destroyed(anonymousTaskFactory());
    }

    public void testExecute_anonymousTask_activity_managed_recreated() {
        testExecute_activity_managed_recreated(anonymousTaskFactory());
    }
    //endregion


    //region Local class tests
    public void testExecute_localTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(localTaskFactory());
    }

    public void testExecute_localTask_activity_unmanaged_destroyedButNotCollected() {
        testExecute_activity_unmanaged_destroyedButNotCollected(localTaskFactory());
    }

    public void testExecute_localTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(localTaskFactory());
    }

    public void testExecute_localTask_activity_managed_normal() {
        testExecute_activity_managed_normal(localTaskFactory());
    }

    public void testExecute_localTask_activity_managed_destroyed() {
        testExecute_activity_managed_destroyed(localTaskFactory());
    }

    public void testExecute_localTask_activity_managed_recreated() {
        testExecute_activity_managed_recreated(localTaskFactory());
    }
    //endregion


    //region Hierarchical inner class tests
    public void testExecute_hierarchicalTask_activity_unmanaged_normal() {
        testExecute_activity_unmanaged_normal(hierarchicalTaskFactory());
    }

    public void testExecute_hierarchicalTask_activity_unmanaged_destroyedButNotCollected() {
        testExecute_activity_unmanaged_destroyedButNotCollected(hierarchicalTaskFactory());
    }

    public void testExecute_hierarchicalTask_activity_unmanaged_destroyedAndCollected() {
        testExecute_activity_unmanaged_destroyedAndCollected(hierarchicalTaskFactory());
    }

    public void testExecute_hierarchicalTask_activity_managed_normal() {
        testExecute_activity_managed_normal(hierarchicalTaskFactory());
    }

    public void testExecute_hierarchicalTask_activity_managed_destroyed() {
        testExecute_activity_managed_destroyed(hierarchicalTaskFactory());
    }

    public void testExecute_hierarchicalTask_activity_managed_recreated() {
        testExecute_activity_managed_recreated(hierarchicalTaskFactory());
    }
    //endregion


    //region Unmanaged activity test templates
    private void testExecute_activity_unmanaged_normal(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is not managed.
        final AsyncTaskActivityMock initialActivity = givenActivityUnmanaged();
        final double input = someInputData();

        // WHEN Activity is living during the whole AsyncTask lifecycle.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        assertThat(asyncTask.doFinish(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
    }

    private void testExecute_activity_unmanaged_destroyedButNotCollected(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is not managed.
        AsyncTaskActivityMock initialActivity = givenActivityUnmanaged();
        final double input = someInputData();

        // WHEN Activity is destroyed while AsyncTask is running but not garbage collected yet.
        // AND Activity is recreated.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        assertThat(asyncTask.doStep(), equalTo(true));
        final AsyncTaskActivityMock recreatedActivity = recreateActivity(); // Look here.
        assertThat(asyncTask.doFinish(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND initial Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
        // AND recreated Activity is not managed so not notified of the result.
        assertThat(recreatedActivity, notNullValue());
        assertThat(recreatedActivity.result().value(), nullValue());
    }

    private void testExecute_activity_unmanaged_destroyedAndCollected(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is not managed.
        AsyncTaskActivityMock initialActivity = givenActivityUnmanaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();
        final double input = someInputData();

        // WHEN Activity is destroyed and garbage collected while AsyncTask is running.
        // AND Activity is recreated.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        final AsyncTaskActivityMock recreatedActivity = recreateActivity(); // Look here.
        // Try to ensure the emitter gets garbage collected. WARNING: We don't have full control on the garbage collector so we
        // can guarantee this will work! This test may fail at any moment although it works for now. Such a failure occur may
        // occur in the BackgroundTask when checking if emitter is null (it should be null but it won't be in case of failure). A
        // failure could also mean there is a memory-leak somewhere...
        initialActivity = null;
        garbageCollect(); // And here.
        assertThat(asyncTask.doFinish(), equalTo(true));

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


    //region Managed activity test templates
    private void testExecute_activity_managed_normal(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is managed.
        final AsyncTaskActivityMock initialActivity = givenActivityManaged();
        final double input = someInputData();

        // WHEN Activity is living during the whole AsyncTask lifecycle.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        assertThat(asyncTask.doFinish(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), equalTo(expectedResult(input)));
    }

    private void testExecute_activity_managed_destroyedAndCollected(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is managed.
        AsyncTaskActivityMock initialActivity = givenActivityManaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();
        final double input = someInputData();

        // WHEN Activity is destroyed and garbage collected while AsyncTask is running.
        // AND Activity is recreated.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        final AsyncTaskActivityMock recreatedActivity = recreateActivity(); // Look here.
        // Try to ensure the emitter gets garbage collected. WARNING: We don't have full control on the garbage collector so we
        // can guarantee this will work! This test may fail at any moment although it works for now. Such a failure occur may
        // occur in the BackgroundTask when checking if emitter is null (it should be null but it won't be in case of failure). A
        // failure could also mean there is a memory-leak somewhere...
        initialActivity = null;
        garbageCollect(); // And here.
        assertThat(asyncTask.doFinish(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is not notified of the result.
        assertThat(initialActivity, nullValue());
        assertThat(initialActivityResult.value(), nullValue());
        // AND recreated Activity is not managed so not notified of the result.
        assertThat(recreatedActivity, notNullValue());
        assertThat(recreatedActivity.result().value(), nullValue());
    }

    private void testExecute_activity_managed_destroyed(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is managed.
        AsyncTaskActivityMock initialActivity = givenActivityManaged();
        final double input = someInputData();

        // WHEN Activity is destroyed while AsyncTask is running.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        terminateActivity(); // Look here.
        assertThat(asyncTask.doFinish(), equalTo(true));

        // THEN AsyncTask ends successfully.
        assertThat(asyncTask.result(), equalTo(expectedResult(input)));
        // AND Activity is not notified of the result.
        assertThat(initialActivity, notNullValue());
        assertThat(initialActivity.result().value(), nullValue());
    }

    private void testExecute_activity_managed_recreated(AsyncTaskMockFactory pAsyncTaskFactory) {
        // GIVEN Activity is managed.
        AsyncTaskActivityMock initialActivity = givenActivityManaged();
        final double input = someInputData();

        // WHEN Activity is recreated while AsyncTask is running.
        AsyncTaskMock asyncTask = pAsyncTaskFactory.createFrom(initialActivity).doExecute(input);
        assertThat(asyncTask.doStep(), equalTo(true));
        final AsyncTaskActivityMock recreatedActivity = recreateActivity(); // Look here.
        assertThat(asyncTask.doFinish(), equalTo(true));

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
