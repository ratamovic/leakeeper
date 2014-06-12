package com.codexperiments.leakeeper.legacy;

import com.codexperiments.leakeeper.legacy.handler.TaskResult;

public interface TaskDescriptorFactory {
    <TParam, TProgress, TResult> TaskDescriptor<TParam, TProgress, TResult> create(TaskResult<TResult> pTaskResult, TaskResolver pTaskResolver);
}
