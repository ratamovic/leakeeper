package com.codexperiments.leakeeper.test.common;

public class ValueHolder<T> {
    private T mValue = null;

    public T value() {
        return mValue;
    }

    public void set(T value) {
        mValue = value;
    }
}
