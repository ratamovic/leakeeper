package com.codexperiments.leakeeper.task.util;

import com.codexperiments.leakeeper.task.handler.TaskIdentifiable;
import com.codexperiments.leakeeper.task.handler.TaskResult;

public interface IdentifiableProgressTaskResult<TResult> extends TaskResult/*<TResult>*/, TaskIdentifiable
{
}
