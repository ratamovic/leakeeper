package com.codexperiments.leakeeper.test.asynctask;

import com.codexperiments.leakeeper.CallbackManager;

import java.lang.ref.WeakReference;

class ClassicAsyncTaskMock extends AsyncTaskMock {
    private WeakReference<AsyncTaskActivityMock> mActivityRef;

    ClassicAsyncTaskMock(CallbackManager pCallbackManager, AsyncTaskActivityMock pActivity) {
        super(pCallbackManager);
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