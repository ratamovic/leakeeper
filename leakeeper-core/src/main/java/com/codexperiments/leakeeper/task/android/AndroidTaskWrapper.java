package com.codexperiments.leakeeper.task.android;

import com.codexperiments.leakeeper.task.LeakContainer;
import com.codexperiments.leakeeper.task.TaskRef;
import com.codexperiments.leakeeper.task.handler.Task;

class AndroidTaskWrapper<TCallback extends Task> implements Task, LeakContainer {
    private final AndroidTaskManager<TCallback>.TaskContainer mContainer;
    private final TaskRef mTaskRef;
//    private final TaskResult<TResult> mTaskResult;

    public static <TCallback extends Task> TCallback wrap(AndroidTaskManager<TCallback>.TaskContainer pContainer, TaskRef pTaskRef, TCallback pCallback) {
        // TODO XXX FIXME
        return (TCallback) new AndroidTaskWrapper<TCallback>(pContainer, pTaskRef, pCallback);
    }

    private AndroidTaskWrapper(AndroidTaskManager<TCallback>.TaskContainer pContainer, TaskRef pTaskRef, TCallback pCallback) {
        mContainer = pContainer;
        mTaskRef = pTaskRef;
//        mTaskResult = pTaskResult;
    }

    @Override
    public void guard() {
        mContainer.guard();
    }

    @Override
    public boolean unguard() {
        return mContainer.unguard();
    }

    @Override
    public void onFinish(/*TResult*/Object pResult) {
        mContainer.doFinish(pResult);
//        mTaskResult.onFinish(pResult);
    }

    @Override
    public void onFail(Throwable pException) {
        mContainer.doFail(pException);
//        mTaskResult.onFail(pException);
    }

    @Override
    public TaskRef toRef() {
        return mTaskRef;
    }
}

