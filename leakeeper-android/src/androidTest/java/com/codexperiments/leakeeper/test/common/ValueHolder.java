package com.codexperiments.leakeeper.test.common;

public class ValueHolder<T> {
    private T mValue;

    public ValueHolder() {
        mValue = null;
    }

    public ValueHolder(T pValue) {
        mValue = pValue;
    }

    public T value() {
        return mValue;
    }

    public void set(T value) {
        mValue = value;
    }
}
