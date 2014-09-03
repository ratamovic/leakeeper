package com.codexperiments.leakeeper.test.task.helpers;

import android.content.Intent;
import android.os.Bundle;
import com.codexperiments.leakeeper.task.LeakManager;
import com.codexperiments.leakeeper.test.common.TestActivity;
import com.codexperiments.leakeeper.test.common.TestCase;

import java.lang.ref.WeakReference;

/**
 * Activity that executes some AsyncTasks.
 */
public class AsyncTaskActivityMock extends TestActivity {
    private final ValueHolder<String> mResult = new ValueHolder<>();

    private LeakManager<AsyncTaskActivityMock> mLeakManager;
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

        mLeakManager = TestCase.inject(this, LeakManager.class);
        mManaged = getIntent().getBooleanExtra("MANAGED", true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mManaged) mLeakManager.unmanage(this);
    }

    @Override
    protected void onStart() {
        if (mManaged) mLeakManager.manage(this);
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
        return new ClassicAsyncTaskMock(mLeakManager, this);
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
        return new StaticAsyncTaskMock(mLeakManager, this);
    }

    private static class StaticAsyncTaskMock extends AsyncTaskMock {
        private WeakReference<AsyncTaskActivityMock> mActivityRef;

        StaticAsyncTaskMock(LeakManager pLeakManager, AsyncTaskActivityMock pActivity) {
            super(pLeakManager);
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
        return new InnerAsyncTaskMock(mLeakManager);
    }

    private class InnerAsyncTaskMock extends AsyncTaskMock {
        InnerAsyncTaskMock(LeakManager pLeakManager) {
            super(pLeakManager);
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
        return new AsyncTaskMock(mLeakManager) {
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
            LocalAsyncTaskMock(LeakManager pLeakManager) {
                super(pLeakManager);
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
        return new LocalAsyncTaskMock(mLeakManager);
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
        return new HierarchicalAsyncTaskMock(mLeakManager);
    }

    private class HierarchicalAsyncTaskMock extends AsyncTaskMock {
        private AsyncTaskMock mChildAsyncTask;

        HierarchicalAsyncTaskMock(LeakManager pLeakManager) {
            super(pLeakManager);
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
            mChildAsyncTask = new AsyncTaskMock(sLeakManager) {
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
