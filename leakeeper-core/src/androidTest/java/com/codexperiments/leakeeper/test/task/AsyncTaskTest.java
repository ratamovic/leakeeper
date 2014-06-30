package com.codexperiments.leakeeper.test.task;

import com.codexperiments.leakeeper.task.TaskManagerConfig;
import com.codexperiments.leakeeper.task.android.AndroidTaskManager;
import com.codexperiments.leakeeper.task.android.AndroidTaskManagerConfig;
import com.codexperiments.leakeeper.task.handler.TaskResult;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.task.AsyncTaskActivity.InnerAsyncTask;
import com.codexperiments.leakeeper.test.task.helper.ValueHolder;

import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.expectedResult;
import static com.codexperiments.leakeeper.test.task.AsyncTaskActivity.someInputData;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
        TaskManagerConfig config = new AndroidTaskManagerConfig(getApplication().getApplication());
        mTaskManager = new AndroidTaskManager(getApplication().getApplication(), config, TaskResult.class);
        getApplicationContext().registerManager(mTaskManager);
    }

    public void testExecute_innerAsyncTask_activity_managed_normal() throws InterruptedException {
        double input = someInputData();
        AsyncTaskActivity activity = getActivity();
        InnerAsyncTask asyncTask = activity.createInnerAsyncTask();

        asyncTask.execute(input);
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        assertThat(asyncTask.result().value(), equalTo(expectedResult(input)));
        assertThat(activity.result().value(), equalTo(expectedResult(input)));
    }

    public void testExecute_innerAsyncTask_activity_managed_destroyed() throws InterruptedException {
        double input = someInputData();
        AsyncTaskActivity activity = getActivity();
        ValueHolder<String> activityResult = activity.result();
        InnerAsyncTask asyncTask = activity.createInnerAsyncTask();

        asyncTask.execute(input);
        activity = terminateActivity(activity);
        assertThat(asyncTask.doAllSteps(), equalTo(true));
        assertThat(asyncTask.awaitFinished(), equalTo(true));

        assertThat(activity, equalTo(null));
        assertThat(asyncTask.result().value(), equalTo(expectedResult(input)));
        assertThat(activityResult.value(), equalTo(expectedResult(input))); // TODO XXX FIXME
    }
}
