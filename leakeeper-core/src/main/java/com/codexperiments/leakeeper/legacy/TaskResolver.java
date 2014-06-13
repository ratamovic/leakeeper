package com.codexperiments.leakeeper.legacy;

import com.codexperiments.leakeeper.legacy.handler.TaskResult;

public interface TaskResolver {
    TaskEmitterRef resolveRef(Object pEmitter);

    TaskDescriptor resolveDescriptor(Object pEmitter);

    <TResult> TaskDescriptor saveDescriptor(TaskResult<TResult> pTaskResult, TaskDescriptor<?, ?, TResult> pDescriptor);

    void removeDescriptor(TaskContainer<?, ?, ?> pContainer);
}