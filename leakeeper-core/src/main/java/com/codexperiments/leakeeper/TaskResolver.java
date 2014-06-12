package com.codexperiments.leakeeper;

public interface TaskResolver {
    TaskEmitterRef resolveRef(Object pEmitter);

    TaskDescriptor resolveDescriptor(Object pEmitter);
}
