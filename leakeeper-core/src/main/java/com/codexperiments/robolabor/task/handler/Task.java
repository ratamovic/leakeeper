package com.codexperiments.robolabor.task.handler;

import com.codexperiments.robolabor.task.TaskRef;

public interface Task/*<TParam, TResult>*/ extends TaskResult/*<TResult>*/ {
    TaskRef/*<TResult>*/ toRef();
}
