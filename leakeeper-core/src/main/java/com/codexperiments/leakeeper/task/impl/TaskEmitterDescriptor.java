package com.codexperiments.leakeeper.task.impl;

import java.lang.reflect.Field;

import static com.codexperiments.leakeeper.task.impl.LeakManagerException.internalError;

/**
     * Contains all the information necessary to restore a single emitter on a task handler (its field and its generated Id).
     */
    final class TaskEmitterDescriptor {
        private final Field mEmitterField;
        private final TaskEmitterRef mEmitterRef;

        public TaskEmitterDescriptor(Field pEmitterField, TaskEmitterRef pEmitterRef) {
            mEmitterField = pEmitterField;
            mEmitterRef = pEmitterRef;
        }

        public TaskEmitterRef hasSameType(Field pField) {
            return (pField.getType() == mEmitterField.getType()) ? mEmitterRef : null;
        }

        public boolean usesEmitter(TaskEmitterId pTaskEmitterId) {
            return mEmitterRef.hasSameId(pTaskEmitterId);
        }

        /**
         * Restore reference to the current emitter on the specified task handler.
         * 
         * @param pCallback Emitter to restore.
         * @return True if referencing succeed or false else.
         */
        public boolean reference(Object pCallback) {
            try {
                Object emitter = mEmitterRef.get();
                if (emitter != null) {
                    mEmitterField.set(pCallback, emitter);
                    return true;
                } else {
                    return false;
                }
            } catch (IllegalAccessException | RuntimeException exception) {
                throw internalError(exception);
            }
        }

        /**
         * Clear reference to the given emitter on the specified task handler.
         * 
         * @param pCallback Emitter to dereference.
         */
        public void dereference(Object pCallback) {
            try {
                mEmitterField.set(pCallback, null);
            } catch (IllegalAccessException | RuntimeException exception) {
                throw internalError(exception);
            }
        }

        @Override
        public String toString() {
            return "TaskEmitterDescriptor [mEmitterField=" + mEmitterField + ", mEmitterRef=" + mEmitterRef + "]";
        }
    }
