package com.codexperiments.leakeeper.task.handler;

import com.codexperiments.leakeeper.task.LeakContainer;
import com.codexperiments.leakeeper.task.TaskRef;

public interface Task extends TaskResult, LeakContainer {
    TaskRef toRef();
}
