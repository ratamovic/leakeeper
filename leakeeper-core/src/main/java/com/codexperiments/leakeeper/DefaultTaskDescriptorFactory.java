package com.codexperiments.leakeeper;

import com.codexperiments.leakeeper.handler.TaskResult;

public class DefaultTaskDescriptorFactory implements TaskDescriptorFactory {
    private LockingStrategy mLockingStrategy;

    public DefaultTaskDescriptorFactory(LockingStrategy pLockingStrategy) {
        super();
        mLockingStrategy = pLockingStrategy;
    }

    @Override
    public <TParam, TProgress, TResult> TaskDescriptor<TParam, TProgress, TResult> create(TaskResult<TResult> pTaskResult, TaskResolver pTaskResolver) {
        return new TaskDescriptor<>(pTaskResult, pTaskResolver, mLockingStrategy);
    }
}
