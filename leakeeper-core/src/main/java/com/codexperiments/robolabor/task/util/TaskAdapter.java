package com.codexperiments.robolabor.task.util;

import com.codexperiments.robolabor.task.handler.Task;
import com.codexperiments.robolabor.task.handler.TaskIdentifiable;
import com.codexperiments.robolabor.task.handler.TaskResult;
import com.codexperiments.robolabor.task.handler.TaskStart;
import com.codexperiments.robolabor.task.id.TaskId;
import com.codexperiments.robolabor.task.id.UniqueTaskId;

public class TaskAdapter<TParam, TResult>
    implements Task<TParam, TResult>, TaskResult<TResult>, TaskStart, TaskIdentifiable
{
    private TaskId mId;

    public TaskAdapter() {
        super();
        mId = new UniqueTaskId();
    }

    @Override
    public TaskId getId() {
        return mId;
    }

    @Override
    public void onStart(boolean pIsRestored) {
    }

    @Override
    public void onFinish(TResult pResult) {
    }

    @Override
    public void onFail(Throwable pException) {
    }
}
