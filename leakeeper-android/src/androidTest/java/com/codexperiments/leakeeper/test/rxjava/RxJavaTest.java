package com.codexperiments.leakeeper.test.rxjava;

import com.codexperiments.leakeeper.CallbackManager;
import com.codexperiments.leakeeper.config.enforcer.AndroidUIThreadEnforcer;
import com.codexperiments.leakeeper.config.enforcer.ThreadEnforcer;
import com.codexperiments.leakeeper.config.resolver.AndroidEmitterResolver;
import com.codexperiments.leakeeper.config.resolver.EmitterResolver;
import com.codexperiments.leakeeper.test.common.TestCase;
import com.codexperiments.leakeeper.test.common.ValueHolder;
import org.hamcrest.Matchers;
import rx.Observer;
import rx.subjects.PublishSubject;

import static com.codexperiments.leakeeper.test.asynctask.AsyncTaskMock.expectedResult;
import static com.codexperiments.leakeeper.test.asynctask.AsyncTaskMock.someInputData;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test Leakeeper applied to RxJava.
 */
public class RxJavaTest extends TestCase<RxJavaActivityMock> {
    public RxJavaTest() {
        super(RxJavaActivityMock.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    //region Given
    private RxJavaActivityMock givenActivityManaged() {
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                EmitterResolver emitterResolver = new AndroidEmitterResolver();
                ThreadEnforcer threadEnforcer = new AndroidUIThreadEnforcer();
                register(CallbackManager.class, CallbackManager.singleThreaded(CallbackManager.class, emitterResolver, threadEnforcer));
            }
        });
        return getActivity();
    }
    //endregion

    //region Tests
    public void atest1() throws InterruptedException {
        // GIVEN Activity is managed.
        final RxJavaActivityMock initialActivity = givenActivityManaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();

        // WHEN Activity is living while the task is running.
        final PublishSubject<Integer> subject = PublishSubject.create();
        initialActivity.startTask(subject);

        subject.onNext(1);
        subject.onNext(2);

        terminateActivity();

        subject.onNext(3);
        subject.onNext(4);
        subject.onCompleted();

        // THEN ...
        initialActivity.awaitCompleted();
        assertThat(initialActivityResult.value(), equalTo("125"));
    }

    public void test2() throws InterruptedException {
        // GIVEN Activity is managed.
        final RxJavaActivityMock initialActivity = givenActivityManaged();
        final ValueHolder<String> initialActivityResult = initialActivity.result();

        // WHEN Activity is living while the task is running.
        final PublishSubject<Integer> subject = PublishSubject.create();
        initialActivity.startTask(subject);

        subject.onNext(1);
        subject.onNext(2);

        final RxJavaActivityMock recreatedActivity = recreateActivity();

        subject.onNext(3);
        subject.onNext(4);
        subject.onCompleted();

        // THEN ...
        recreatedActivity.awaitCompleted();
        assertThat(initialActivityResult.value(), equalTo("12"));
        assertThat(recreatedActivity.result().value(), equalTo("34"));
    }
    //endregion
}
