package io.temporal.docs.workershutdown;

import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

class HeartbeatDuringShutdownExamples {
  static Worker newWorkerThatHeartbeatsDuringShutdown(WorkerFactory factory) {
    // @@@SNIPSTART java-worker-heartbeat-during-shutdown
    WorkerOptions options =
        WorkerOptions.newBuilder().setAllowActivityHeartbeatDuringShutdown(true).build();
    Worker worker = factory.newWorker("your-task-queue", options);
    // @@@SNIPEND
    return worker;
  }
}
