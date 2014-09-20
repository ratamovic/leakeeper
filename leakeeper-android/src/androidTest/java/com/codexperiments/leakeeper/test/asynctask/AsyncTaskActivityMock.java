package com.codexperiments.leakeeper.test.asynctask;

import android.content.Intent;
import android.os.Bundle;
import com.codexperiments.leakeeper.CallbackManager;
import com.codexperiments.leakeeper.test.common.TestActivity;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.common.ValueHolder;

import java.lang.ref.WeakReference;

/**
 * Activity that executes some AsyncTasks.
 */
public class AsyncTaskActivityMock extends TestActivity {
    private final ValueHolder<String> mResult = new ValueHolder<>();

    private CallbackManager<AsyncTaskMock> mCallbackManager;
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
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);

        mCallbackManager = (CallbackManager<AsyncTaskMock>) TestCase.inject(this, CallbackManager.class);
        mManaged = getIntent().getBooleanExtra("MANAGED", true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mManaged) mCallbackManager.unmanage(this);
    }

    @Override
    protected void onStart() {
        if (mManaged) mCallbackManager.manage(this);
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
    public static AsyncTaskMockFactory classicTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createClassicAsyncTask();
            }
        };
    }

    private AsyncTaskMock createClassicAsyncTask() {
        return new ClassicAsyncTaskMock(mCallbackManager, this);
    }


    /**
     * AsyncTask made of a static class.
     */
    public static AsyncTaskMockFactory staticTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createStaticAsyncTask();
            }
        };
    }

    private AsyncTaskMock createStaticAsyncTask() {
        return new StaticAsyncTaskMock(mCallbackManager, this);
    }

    private static class StaticAsyncTaskMock extends AsyncTaskMock {
        private WeakReference<AsyncTaskActivityMock> mActivityRef;

        StaticAsyncTaskMock(CallbackManager<AsyncTaskMock> pCallbackManager, AsyncTaskActivityMock pActivity) {
            super(pCallbackManager);
            mActivityRef = new WeakReference<>(pActivity);
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
    public static AsyncTaskMockFactory innerTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createInnerAsyncTask();
            }
        };
    }

    private AsyncTaskMock createInnerAsyncTask() {
        return new InnerAsyncTaskMock(mCallbackManager);
    }

    private class InnerAsyncTaskMock extends AsyncTaskMock {
        InnerAsyncTaskMock(CallbackManager<AsyncTaskMock> pCallbackManager) {
            super(pCallbackManager);
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
    public static AsyncTaskMockFactory anonymousTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createAnonymousAsyncTask();
            }
        };
    }

    private AsyncTaskMock createAnonymousAsyncTask() {
        return new AsyncTaskMock(mCallbackManager) {
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
    public static AsyncTaskMockFactory localTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createLocalAsyncTask();
            }
        };
    }

    private AsyncTaskMock createLocalAsyncTask() {
        class LocalAsyncTaskMock extends AsyncTaskMock {
            LocalAsyncTaskMock(CallbackManager<AsyncTaskMock> pLeakManager) {
                super(pLeakManager);
            }

            @Override
            protected void onSaveResult(String pResult) {
                if (AsyncTaskActivityMock.this != null) {
                    AsyncTaskActivityMock.this.result().set(pResult);
                }
            }
        }
        return new LocalAsyncTaskMock(mCallbackManager);
    }


    /**
     * Inner AsyncTask executing a child anonymous AsyncTask.
     */
    public static AsyncTaskMockFactory hierarchicalTaskFactory() {
        return new AsyncTaskMockFactory() {
            @Override
            public AsyncTaskMock createFrom(AsyncTaskActivityMock pActivity) {
                return pActivity.createHierarchicalAsyncTask();
            }
        };
    }

    private AsyncTaskMock createHierarchicalAsyncTask() {
        return new HierarchicalAsyncTaskMock(mCallbackManager);
    }

    private class HierarchicalAsyncTaskMock extends AsyncTaskMock {
        private final CallbackManager<AsyncTaskMock> mCallbackManager; // Copy stored here because Activity may not be accessible.
        private AsyncTaskMock mChildAsyncTask;

        HierarchicalAsyncTaskMock(CallbackManager<AsyncTaskMock> pCallbackManager) {
            super(pCallbackManager);
            mCallbackManager = pCallbackManager;
        }

        @Override
        public boolean doFinish() {
            if (!super.doFinish()) return false;

            // Run a child AsyncTask and wait until it finishes.
            return mChildAsyncTask.doFinish();
        }

        @Override
        protected void onSaveResult(String pResult) {
            // Run a child AsyncTask that processes the result.
            // Note that the result is not given back to the activity here but in the child task.
            mChildAsyncTask = new AsyncTaskMock(mCallbackManager) {
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
