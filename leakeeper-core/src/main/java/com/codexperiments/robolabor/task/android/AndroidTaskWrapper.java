package com.codexperiments.robolabor.task.android;

import com.codexperiments.robolabor.task.TaskRef;
import com.codexperiments.robolabor.task.handler.Task;

class AndroidTaskWrapper<TParam, TResult> implements Task<TParam, TResult> {
    private final TaskRef<TResult> mTaskRef;

    AndroidTaskWrapper(TaskRef<TResult> pTaskRef) {
        mTaskRef = pTaskRef;
    }

    @Override
    public void onFinish(Object pResult) {

    }

    @Override
    public void onFail(Throwable pException) {

    }

    @Override
    public TaskRef<TResult> toRef() {
        return mTaskRef;
    }
}

