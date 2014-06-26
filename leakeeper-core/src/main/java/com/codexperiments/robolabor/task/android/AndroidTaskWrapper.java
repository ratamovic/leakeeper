package com.codexperiments.robolabor.task.android;

import com.codexperiments.robolabor.task.TaskRef;
import com.codexperiments.robolabor.task.handler.Task;
import com.codexperiments.robolabor.task.handler.TaskResult;

class AndroidTaskWrapper<TCallback extends Task, TParam, TResult> implements Task<TParam, TResult> {
    private final AndroidTaskManager<TCallback>.TaskContainer<TParam, TResult> mContainer;
    private final TaskRef<TResult> mTaskRef;
//    private final TaskResult<TResult> mTaskResult;

    AndroidTaskWrapper(AndroidTaskManager<TCallback>.TaskContainer<TParam, TResult> pContainer, TaskRef<TResult> pTaskRef, TaskResult<TResult> pTaskResult) {
        mContainer = pContainer;
        mTaskRef = pTaskRef;
//        mTaskResult = pTaskResult;
    }

    @Override
    public void onFinish(TResult pResult) {
        mContainer.doFinish(pResult);
//        mTaskResult.onFinish(pResult);
    }

    @Override
    public void onFail(Throwable pException) {
        mContainer.doFail(pException);
//        mTaskResult.onFail(pException);
    }

    @Override
    public TaskRef<TResult> toRef() {
        return mTaskRef;
    }
}

