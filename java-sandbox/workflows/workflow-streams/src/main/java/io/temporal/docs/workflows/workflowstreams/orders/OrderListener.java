package io.temporal.docs.workflows.workflowstreams.orders;

// @@@SNIPSTART java-workflow-streams-subscribe-listener
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamListener;
import io.temporal.workflowstreams.WorkflowStreamSubscriptionHandle;
import java.util.concurrent.CompletionStage;

public class OrderListener {
  public void listen(WorkflowStreamClient streamClient) {
    SubscribeOptions options = SubscribeOptions.newBuilder().setTopics("status").build();
    WorkflowStreamSubscriptionHandle handle =
        streamClient.subscribe(
            options,
            new WorkflowStreamListener() {
              @Override
              public CompletionStage<Void> onNext(WorkflowStreamItem item) {
                StatusEvent evt =
                    DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                        item.getPayload(), StatusEvent.class, StatusEvent.class);
                System.out.printf(
                    "offset=%d topic=%s state=%s%n", item.getOffset(), item.getTopic(), evt.state);
                return null; // or a pending stage to apply backpressure
              }

              @Override
              public void onCompleted() {
                System.out.println("stream ended");
              }
            });

    // The calling thread is free; wait on the handle when you need to.
    handle.getDoneFuture().join();
  }
}
// @@@SNIPEND
