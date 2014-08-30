package com.codexperiments.leakeeper.test.task.helpers;

import com.codexperiments.leakeeper.task.TaskManager;

import java.lang.ref.WeakReference;

class ClassicAsyncTaskMock extends AsyncTaskMock {
    private WeakReference<AsyncTaskActivityMock> mActivityRef;

    ClassicAsyncTaskMock(TaskManager pTaskManager, AsyncTaskActivityMock pActivity) {
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