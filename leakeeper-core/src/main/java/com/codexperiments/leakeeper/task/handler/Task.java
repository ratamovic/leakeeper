package com.codexperiments.leakeeper.task.handler;

import com.codexperiments.leakeeper.task.TaskRef;

public interface Task/*<TParam, TResult>*/ extends TaskResult/*<TResult>*/ {
    TaskRef/*<TResult>*/ toRef();
}
