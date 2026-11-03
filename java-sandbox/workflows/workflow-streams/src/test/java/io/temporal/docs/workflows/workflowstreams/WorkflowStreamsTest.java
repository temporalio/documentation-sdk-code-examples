package io.temporal.docs.workflows.workflowstreams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.docs.workflows.workflowstreams.closing.ChatWorkflow;
import io.temporal.docs.workflows.workflowstreams.closing.FixedSleepWorkflowImpl;
import io.temporal.docs.workflows.workflowstreams.llm.ChatWorkflowImpl;
import io.temporal.docs.workflows.workflowstreams.llm.Consumer;
import io.temporal.docs.workflows.workflowstreams.llm.LlmActivitiesImpl;
import io.temporal.docs.workflows.workflowstreams.longrunning.ExplicitContinueAsNewWorkflowImpl;
import io.temporal.docs.workflows.workflowstreams.longrunning.LongRunningWorkflow;
import io.temporal.docs.workflows.workflowstreams.longrunning.LongRunningWorkflowImpl;
import io.temporal.docs.workflows.workflowstreams.longrunning.WorkflowInput;
import io.temporal.docs.workflows.workflowstreams.orders.OrderActivities;
import io.temporal.docs.workflows.workflowstreams.orders.OrderInput;
import io.temporal.docs.workflows.workflowstreams.orders.OrderListener;
import io.temporal.docs.workflows.workflowstreams.orders.OrderWatcher;
import io.temporal.docs.workflows.workflowstreams.orders.OrderWorkflow;
import io.temporal.docs.workflows.workflowstreams.orders.OrderWorkflowImpl;
import io.temporal.testing.TestEnvironmentOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflowstreams.SubscribeOptions;
import io.temporal.workflowstreams.WorkflowStreamClient;
import io.temporal.workflowstreams.WorkflowStreamItem;
import io.temporal.workflowstreams.WorkflowStreamSubscription;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(60)
public class WorkflowStreamsTest {

    private static final String TASK_QUEUE = "workflow-streams";

    private TestWorkflowEnvironment env;
    private Worker worker;
    private WorkflowClient client;

    /** Order Activities that wait until the test has attached its subscribers. */
    static class GatedOrderActivities implements OrderActivities {
        final CountDownLatch gate = new CountDownLatch(1);

        private void await() {
            try {
                gate.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void validateOrder(String orderId) {
            await();
        }

        @Override
        public void chargePayment(String orderId) {}

        @Override
        public void dispatchOrder(String orderId) {}
    }

    @BeforeEach
    public void setUp() {
        // Subscribers long-poll in real time, so don't let the test server skip time.
        TestEnvironmentOptions.Builder options = TestEnvironmentOptions.newBuilder().setUseTimeskipping(false);
        String address = System.getenv("TEMPORAL_ADDRESS");
        if (address != null) {
            // Run against a dev server instead of the in-memory test server.
            options.setUseExternalService(true).setTarget(address);
        }
        env = TestWorkflowEnvironment.newInstance(options.build());
        worker = env.newWorker(TASK_QUEUE);
        client = env.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
        env.close();
    }

    private <T> T stub(Class<T> type, String workflowId) {
        return client.newWorkflowStub(
            type, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static List<String> collect(WorkflowClient client, String workflowId, String... topics) {
        return collect(new CopyOnWriteArrayList<>(), client, workflowId, topics);
    }

    private static List<String> collect(
        List<String> seen, WorkflowClient client, String workflowId, String... topics) {
        try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, workflowId);
            WorkflowStreamSubscription subscription =
                stream.subscribe(SubscribeOptions.newBuilder().setTopics(topics).build())) {
            for (WorkflowStreamItem item : subscription) {
                seen.add(item.getTopic() + ":" + item.getPayload().getData().toStringUtf8());
            }
        }
        return seen;
    }

    @Test
    public void enableStreamingWorkflowRuns() {
        worker.registerWorkflowImplementationTypes(
            io.temporal.docs.workflows.workflowstreams.enable.OrderWorkflowImpl.class);
        env.start();

        WorkflowStub workflow = client.newUntypedWorkflowStub("OrderWorkflow",
            WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("enable").build());
        workflow.start(java.util.Map.of("orderId", "order-1"));
        workflow.getResult(Void.class);
    }

    @Test
    public void orderStatusReachesIteratorListenerAndClientPublishers() throws Exception {
        GatedOrderActivities activities = new GatedOrderActivities();
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        env.start();

        OrderWorkflow workflow = stub(OrderWorkflow.class, "order-1");
        WorkflowClient.start(workflow::execute, new OrderInput("order-1"));

        CompletableFuture<Void> watcher = CompletableFuture.runAsync(
            () -> new OrderWatcher().watchOrder(client, "order-1"), executor);
        CompletableFuture<Void> listener = CompletableFuture.runAsync(() -> {
            try (WorkflowStreamClient streamClient = WorkflowStreamClient.newInstance(client, "order-1")) {
                new OrderListener().listen(streamClient);
            }
        }, executor);
        List<String> multiTopic = new CopyOnWriteArrayList<>();
        CompletableFuture.runAsync(() -> collect(multiTopic, client, "order-1", "status", "progress"), executor);

        new io.temporal.docs.workflows.workflowstreams.orders.StatusPublisher().publishStatus(client, "order-1");
        try (WorkflowStreamClient streamClient = WorkflowStreamClient.newInstance(client, "order-1")) {
            streamClient.topic("progress").publish(java.util.Map.of("message", "halfway"));
        }
        // Release the Workflow once the multi-topic subscriber has the client-published events.
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline
            && !(String.join("\n", multiTopic).contains("\"started\"")
                && String.join("\n", multiTopic).contains("progress:"))) {
            Thread.sleep(100);
        }
        activities.gate.countDown();

        watcher.get(30, TimeUnit.SECONDS);
        WorkflowStub.fromTyped(workflow).getResult(Void.class);
        listener.get(30, TimeUnit.SECONDS);
        String items = String.join("\n", multiTopic);
        assertTrue(items.contains("\"started\""), items);
        assertTrue(items.contains("progress:"), items);
    }

    @Test
    public void fixedSleepLetsSubscriberSeeTerminator() throws Exception {
        worker.registerWorkflowImplementationTypes(FixedSleepWorkflowImpl.class);
        env.start();

        ChatWorkflow workflow = stub(ChatWorkflow.class, "fixed-sleep");
        WorkflowClient.start(workflow::complete, new io.temporal.docs.workflows.workflowstreams.closing.ChatInput("hi"));
        List<String> items = new CopyOnWriteArrayList<>();
        CompletableFuture.runAsync(() -> collect(items, client, "fixed-sleep", "status"), executor);

        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline && !String.join("", items).contains("completed")) {
            Thread.sleep(100);
        }
        // The terminator arrives during the 30-second sleep, before the Workflow returns.
        assertTrue(String.join("", items).contains("completed"));
        WorkflowStub.fromTyped(workflow).terminate("test finished");
    }

    @Test
    public void ackHandshakeReturnsAfterSignal() {
        worker.registerWorkflowImplementationTypes(io.temporal.docs.workflows.workflowstreams.closing.ChatWorkflowImpl.class);
        env.start();

        ChatWorkflow workflow = stub(ChatWorkflow.class, "ack");
        WorkflowClient.start(workflow::complete, new io.temporal.docs.workflows.workflowstreams.closing.ChatInput("hi"));
        workflow.subscriberAcknowledgedTerminator();
        assertEquals("done: hi", WorkflowStub.fromTyped(workflow).getResult(String.class));
    }

    @Test
    public void longRunningWorkflowPublishesItems() throws Exception {
        worker.registerWorkflowImplementationTypes(LongRunningWorkflowImpl.class);
        env.start();

        LongRunningWorkflow workflow = stub(LongRunningWorkflow.class, "long-running");
        WorkflowClient.start(workflow::execute, new WorkflowInput());

        List<String> seen = new ArrayList<>();
        try (WorkflowStreamClient stream = WorkflowStreamClient.newInstance(client, "long-running");
            WorkflowStreamSubscription subscription =
                stream.subscribe(SubscribeOptions.newBuilder().setTopics("items").build())) {
            for (WorkflowStreamItem item : subscription) {
                seen.add(item.getPayload().getData().toStringUtf8());
                if (seen.size() == 3) {
                    break;
                }
            }
        }
        WorkflowStub.fromTyped(workflow).terminate("test finished");
        assertEquals(List.of("0", "1", "2"), seen);
    }

    @Test
    public void explicitContinueAsNewCarriesStreamAcrossRuns() throws Exception {
        worker.registerWorkflowImplementationTypes(ExplicitContinueAsNewWorkflowImpl.class);
        env.newWorker("other-tq").registerWorkflowImplementationTypes(ExplicitContinueAsNewWorkflowImpl.class);
        env.start();

        LongRunningWorkflow workflow = stub(LongRunningWorkflow.class, "explicit-can");
        WorkflowClient.start(workflow::execute, new WorkflowInput());
        CompletableFuture<List<String>> items =
            CompletableFuture.supplyAsync(() -> collect(client, "explicit-can", "items"), executor);

        List<String> seen = items.get(30, TimeUnit.SECONDS);
        assertEquals(List.of("items:0", "items:1", "items:2"), seen);
    }

    @Test
    public void llmDeltasReachConsumer() throws Exception {
        HttpServer fakeOpenAi = HttpServer.create(new InetSocketAddress(18089), 0);
        fakeOpenAi.createContext("/v1/chat/completions", exchange -> {
            StringBuilder body = new StringBuilder();
            for (String text : new String[] {"Hello", ", ", "world"}) {
                body.append("data: {\"id\":\"c\",\"object\":\"chat.completion.chunk\",\"created\":0,"
                    + "\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\""
                    + text + "\"},\"finish_reason\":null}]}\n\n");
            }
            body.append("data: [DONE]\n\n");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        fakeOpenAi.start();
        try {
            worker.registerWorkflowImplementationTypes(ChatWorkflowImpl.class);
            worker.registerActivitiesImplementations(new LlmActivitiesImpl());
            env.start();

            WorkflowStub chat = client.newUntypedWorkflowStub("ChatWorkflow",
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("chat").build());
            chat.start(java.util.Map.of("prompt", "Say hello"));
            CompletableFuture<String> consumed =
                CompletableFuture.supplyAsync(() -> new Consumer().streamChat(client, "chat"), executor);

            assertEquals("Hello, world", consumed.get(30, TimeUnit.SECONDS));
            assertEquals("Hello, world", chat.getResult(String.class));
        } finally {
            fakeOpenAi.stop(0);
        }
    }
}
