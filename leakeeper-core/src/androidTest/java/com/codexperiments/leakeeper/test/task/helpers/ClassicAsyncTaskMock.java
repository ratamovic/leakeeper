package com.codexperiments.leakeeper.test.task.helpers;

import com.codexperiments.leakeeper.task.LeakManager;

import java.lang.ref.WeakReference;

class ClassicAsyncTaskMock extends AsyncTaskMock {
    private WeakReference<AsyncTaskActivityMock> mActivityRef;

    ClassicAsyncTaskMock(LeakManager pLeakManager, AsyncTaskActivityMock pActivity) {
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