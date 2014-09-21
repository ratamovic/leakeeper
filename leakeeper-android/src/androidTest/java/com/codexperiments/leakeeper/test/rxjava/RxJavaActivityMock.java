package com.codexperiments.leakeeper.test.rxjava;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import com.codexperiments.leakeeper.CallbackManager;
import com.codexperiments.leakeeper.rxjava.LeakSafeObserver;
import com.codexperiments.leakeeper.test.common.TestActivity;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.common.ValueHolder;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.codexperiments.leakeeper.test.common.TestCase.MAX_WAIT_TIME;

/**
 * Activity that executes some AsyncTasks.
 */
public class RxJavaActivityMock extends TestActivity {
    private final ValueHolder<String> mResult = new ValueHolder<>("");

    private CallbackManager<Observer<?>> mCallbackManager;
    private CountDownLatch mCompleted = new CountDownLatch(1);

    public ValueHolder<String> result() {
        return mResult;
    }


    //region Lifecycle
    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle pBundle) {
        super.onCreate(pBundle);

        mCallbackManager = (CallbackManager) TestCase.inject(this, CallbackManager.class);
    }

    @Override
    protected void onResume() {
        mCallbackManager.manage(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCallbackManager.unmanage(this);
    }
    //endregion


    //region Task management
    public void startTask(final Observable<Integer> pObservable) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            final CountDownLatch completed = new CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startTask(pObservable);
                    completed.countDown();
                }
            });
            try {
                completed.await(MAX_WAIT_TIME, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            Observer<Integer> observer = new Observer<Integer>() {
                @Override
                public void onNext(Integer t) {
                    if (RxJavaActivityMock.this != null) {
                        mResult.set(mResult.value() + t);
                    }
                }

                @Override
                public void onCompleted() {
                    if (RxJavaActivityMock.this != null) {
                        mCompleted.countDown();
                    }
                    Log.e(RxJavaActivityMock.class.getSimpleName(), "Completed");
                }

                @Override
                public void onError(Throwable error) {
                    Log.e(RxJavaActivityMock.class.getSimpleName(), "Error", error);
                }
            };
            pObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(LeakSafeObserver.wrap(observer, mCallbackManager));
        }
    }

    public void awaitCompleted() {
        try {
            if (!mCompleted.await(MAX_WAIT_TIME, TimeUnit.SECONDS)) throw new RuntimeException("Timeout???");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    //endregion
}
