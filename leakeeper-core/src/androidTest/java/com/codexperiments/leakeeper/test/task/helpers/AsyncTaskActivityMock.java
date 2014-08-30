package com.codexperiments.leakeeper.test.task.helpers;

import android.content.Intent;
import android.os.Bundle;
import com.codexperiments.leakeeper.task.TaskManager;
import com.codexperiments.leakeeper.task.handler.Task;
import com.codexperiments.leakeeper.test.common.TestActivity;
import com.codexperiments.leakeeper.test.common.TestCase;

import java.lang.ref.WeakReference;

/**
 * Activity that executes some AsyncTasks.
 */
public class AsyncTaskActivityMock extends TestActivity {
    private final ValueHolder<String> mResult = new ValueHolder<>();

    private TaskManager<Task> mTaskManager;
    private boolean mManaged;

    public static Intent unmanaged() {
        Intent intent = new Intent();
        intent.putExtra("MANAGED", false);
        return intent;
    }

    public ValueHolder<String> result() {
        return mResult;
    }


    //region Lifecycle
    @Override
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);

        mTaskManager = TestCase.inject(this, TaskManager.class);
        mManaged = getIntent().getBooleanExtra("MANAGED", true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mManaged) mTaskManager.unmanage(this);
    }

    @Override
    protected void onStart() {
        if (mManaged) mTaskManager.manage(this);
        super.onStart();
    }
    //endregion


    //region Task factories
    public interface AsyncTaskMockFactory {
        AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity);
    }


    /**
     * AsyncTask made of a "classic" Java class.
     */
    public static final AsyncTaskMockFactory classicTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createClassicAsyncTask();
            }
        };
    }

    private AsyncTaskMock createClassicAsyncTask() {
        return new ClassicAsyncTaskMock(mTaskManager, this);
    }


    /**
     * AsyncTask made of a static class.
     */
    public static final AsyncTaskMockFactory staticTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
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
                activity.result().set(pResult);
            }
        }
    }


    /**
     * AsyncTask made of an inner class.
     */
    public static final AsyncTaskMockFactory innerTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
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
                AsyncTaskActivityMock.this.result().set(pResult);
            }
        }
    }


    /**
     * AsyncTask made of an anonymous class.
     */
    public static final AsyncTaskMockFactory anonymousTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
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
                    AsyncTaskActivityMock.this.result().set(pResult);
                }
            }
        };
    }


    /**
     * AsyncTask made of a local class.
     */
    public static final AsyncTaskMockFactory localTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
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
                    AsyncTaskActivityMock.this.result().set(pResult);
                }
            }
        }
        return new LocalAsyncTaskMock(mTaskManager);
    }


    /**
     * Inner AsyncTask executing a child anonymous AsyncTask.
     */
    public static final AsyncTaskMockFactory hierarchicalTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
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
                        AsyncTaskActivityMock.this.result().set(pResult);
                    }
                }
            };
            mChildAsyncTask.execute(Double.valueOf(pResult));
        }
    }
    //endregion
}
