package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.handler.TaskResult;

public interface TaskDescriptorFactory {
    <TParam, TProgress, TResult> TaskDescriptor<TParam, TProgress, TResult> create(TaskResult<TResult> pTaskResult, TaskResolver pTaskResolver);
}
