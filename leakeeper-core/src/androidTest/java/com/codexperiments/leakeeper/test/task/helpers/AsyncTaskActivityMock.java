package com.codexperiments.leakeeper.test.task.helpers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.codexperiments.leakeeper.task.TaskManager;
import com.codexperiments.leakeeper.task.handler.Task;
import com.codexperiments.leakeeper.test.common.TestApplicationContext;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncTaskActivityMock extends Activity {
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

    public static Double someInputData() {
        return Math.random();
    }

    public static String expectedResult(Double pValue) {
        return Double.toString(pValue);
    }
    //endregion


    //region Activity control
    public ValueHolder<String> result() {
        return mResult;
    }

    public void updateResult(String pResult) {
        mResult.set(pResult);
    }

    public boolean waitStarted() {
        try {
            return mStartedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }

    public boolean waitTerminated() {
        try {
            return mDestroyedLatch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
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


    //region Task factories
    public interface AsyncTaskMockFactory {
        AsyncTaskMock create(AsyncTaskActivityMock pActivity);
    }


    public static final AsyncTaskMockFactory classicTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createClassicAsyncTask();
            }
        };
    }

    private AsyncTaskMock createClassicAsyncTask() {
        return new ClassicAsyncTaskMock(mTaskManager, this);
    }


    public static final AsyncTaskMockFactory staticTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createStaticAsyncTask();
            }
        };
    }

    private AsyncTaskMock createStaticAsyncTask() {
        return new StaticAsyncTaskMock(mTaskManager, this);
    }

    private static class StaticAsyncTaskMock extends AsyncTaskMock {
        private WeakReference<AsyncTaskActivityMock> mActivityRef;

        StaticAsyncTaskMock(TaskManager pTaskManager, AsyncTaskActivityMock pActivity) {
            super(pTaskManager);
            mActivityRef = new WeakReference<>(pActivity);
        }

        @Override
        protected AsyncTaskActivityMock getActivity() {
            return mActivityRef.get();
        }

        @Override
        protected void onSaveResult(String pResult) {
            AsyncTaskActivityMock activity = mActivityRef.get();
            if (activity != null) {
                activity.updateResult(pResult);
            }
        }
    }


    public static final AsyncTaskMockFactory innerTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createInnerAsyncTask();
            }
        };
    }

    private AsyncTaskMock createInnerAsyncTask() {
        return new InnerAsyncTaskMock(mTaskManager);
    }

    private class InnerAsyncTaskMock extends AsyncTaskMock {
        InnerAsyncTaskMock(TaskManager pTaskManager) {
            super(pTaskManager);
        }

        @Override
        protected AsyncTaskActivityMock getActivity() {
            return AsyncTaskActivityMock.this;
        }

        @Override
        protected void onSaveResult(String pResult) {
            if (AsyncTaskActivityMock.this != null) {
                AsyncTaskActivityMock.this.updateResult(pResult);
            }
        }
    }


    public static final AsyncTaskMockFactory anonymousTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createAnonymousAsyncTask();
            }
        };
    }

    private AsyncTaskMock createAnonymousAsyncTask() {
        return new AsyncTaskMock(mTaskManager) {
            @Override
            protected AsyncTaskActivityMock getActivity() {
                return AsyncTaskActivityMock.this;
            }

            @Override
            protected void onSaveResult(String pResult) {
                if (AsyncTaskActivityMock.this != null) {
                    AsyncTaskActivityMock.this.updateResult(pResult);
                }
            }
        };
    }


    public static final AsyncTaskMockFactory localTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createLocalAsyncTask();
            }
        };
    }

    private AsyncTaskMock createLocalAsyncTask() {
        class LocalAsyncTaskMock extends AsyncTaskMock {
            LocalAsyncTaskMock(TaskManager pTaskManager) {
                super(pTaskManager);
            }

            @Override
            protected AsyncTaskActivityMock getActivity() {
                return AsyncTaskActivityMock.this;
            }

            @Override
            protected void onSaveResult(String pResult) {
                if (AsyncTaskActivityMock.this != null) {
                    AsyncTaskActivityMock.this.updateResult(pResult);
                }
            }
        }
        return new LocalAsyncTaskMock(mTaskManager);
    }


    public static final AsyncTaskMockFactory hierarchicalTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock create(AsyncTaskActivityMock pActivity) {
                return pActivity.createHierarchicalAsyncTask();
            }
        };
    }

    private AsyncTaskMock createHierarchicalAsyncTask() {
        return new HierarchicalAsyncTaskMock(mTaskManager);
    }

    private class HierarchicalAsyncTaskMock extends AsyncTaskMock {
        private AsyncTaskMock mChildAsyncTask;

        HierarchicalAsyncTaskMock(TaskManager pTaskManager) {
            super(pTaskManager);
        }

        @Override
        public boolean doFinish() {
            if (!super.doFinish()) return false;

            // Run a child AsyncTask and wait until it finishes.
            return mChildAsyncTask.doFinish();
        }

        @Override
        protected AsyncTaskActivityMock getActivity() {
            return AsyncTaskActivityMock.this;
        }

        @Override
        protected void onSaveResult(String pResult) {
            // Run a child AsyncTask that processes the result.
            // Note that the result is not given back to the activity here but in the child task.
            mChildAsyncTask = new AsyncTaskMock(mTaskManager) {
                @Override
                protected AsyncTaskActivityMock getActivity() {
                    return AsyncTaskActivityMock.this;
                }

                @Override
                protected void onSaveResult(String pResult) {
                    if (AsyncTaskActivityMock.this != null) {
                        AsyncTaskActivityMock.this.updateResult(pResult);
                    }
                }
            };
            mChildAsyncTask.execute(Double.valueOf(pResult));
        }
    }
    //endregion
}
