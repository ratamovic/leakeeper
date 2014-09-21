package com.codexperiments.leakeeper.rxjava;

import com.codexperiments.leakeeper.CallbackContainer;
import com.codexperiments.leakeeper.CallbackManager;
import rx.Observer;

public class LeakSafeObserver<T> implements Observer<T> {
    private final Observer<T> mObserver;
    private final CallbackContainer<Observer<? extends Object>> mContainer;

    public static <T, TObserver extends Observer<T>> Observer<T> wrap(TObserver pObserver, CallbackManager<Observer<?>> pCallbackManager) {
        return new LeakSafeObserver<>(pObserver, pCallbackManager.wrap(pObserver));
    }

    public static <T, TObserver extends Observer<T>> Observer<T> wrap2(TObserver pObserver, CallbackManager<Observer<?>> pCallbackManager) {
        return pObserver;
    }

    public LeakSafeObserver(Observer<T> pObserver, CallbackContainer<Observer<? extends Object>> pContainer) {
        mObserver = pObserver;
        mContainer = pContainer;
    }

    @Override
    public void onNext(T t) {
        mContainer.referenceEmitter(false);
        try {
            mObserver.onNext(t);
        } finally {
            mContainer.dereferenceEmitter();
        }
    }

    @Override
    public void onCompleted() {
        mContainer.referenceEmitter(false);
        try {
            mObserver.onCompleted();
        } finally {
            mContainer.dereferenceEmitter();
        }
    }

    @Override
    public void onError(Throwable error) {
        mContainer.referenceEmitter(false);
        try {
            mObserver.onError(error);
        } finally {
            mContainer.dereferenceEmitter();
        }
    }
}
