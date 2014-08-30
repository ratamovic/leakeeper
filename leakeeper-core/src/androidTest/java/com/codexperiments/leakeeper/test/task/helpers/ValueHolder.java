package com.codexperiments.leakeeper.test.task.helpers;

public class ValueHolder<T> {
    private T mValue = null;

    public T value() {
        return mValue;
    }

    public void set(T value) {
        mValue = value;
    }
}
