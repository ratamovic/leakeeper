package com.codexperiments.robolabor.task.android;

import com.codexperiments.robolabor.task.TaskRef;
import com.codexperiments.robolabor.task.handler.Task;
import com.codexperiments.robolabor.task.handler.TaskResult;

class AndroidTaskWrapper<TParam, TResult> implements Task<TParam, TResult> {
    private final TaskRef<TResult> mTaskRef;
    private final TaskResult<TResult> mTaskResult;

    AndroidTaskWrapper(TaskRef<TResult> pTaskRef, TaskResult<TResult> pTaskResult) {
        mTaskRef = pTaskRef;
        mTaskResult = pTaskResult;
    }

    @Override
    public void onFinish(TResult pResult) {
        mTaskResult.onFinish(pResult);
    }

    @Override
    public void onFail(Throwable pException) {
        mTaskResult.onFail(pException);
    }

    @Override
    public TaskRef<TResult> toRef() {
        return mTaskRef;
    }
}

