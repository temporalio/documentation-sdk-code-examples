package io.temporal.docs.nexus.standaloneoperations;

import io.temporal.client.NexusClient;
import io.temporal.client.NexusClientOptions;
import io.temporal.client.NexusOperationException;
import io.temporal.client.NexusOperationExecutionCount;
import io.temporal.client.NexusOperationExecutionMetadata;
import io.temporal.client.NexusOperationHandle;
import io.temporal.client.NexusServiceClient;
import io.temporal.client.StartNexusOperationOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.docs.nexus.standaloneoperations.service.ClientOptions;
import io.temporal.docs.nexus.standaloneoperations.service.GreetingNexusService;
import io.temporal.docs.nexus.standaloneoperations.service.GreetingNexusService.GreetingInput;
import io.temporal.docs.nexus.standaloneoperations.service.GreetingNexusService.GreetingOutput;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Sample client for standalone Nexus operations — operations started and managed directly by a
// client rather than from within a workflow. Each capability is shown in its own method, called in
// turn from main(): executing an operation and reading its result, starting an operation and
// waiting on its handle, cancelling a running operation, and querying operations via Visibility.
public class StandaloneClientStarter {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneClientStarter.class);

  // Must match the Nexus endpoint configured on the server (see README).
  public static final String ENDPOINT_NAME = "my-nexus-endpoint";

  public static void main(String[] args) throws Exception {
    WorkflowClient client = ClientOptions.getWorkflowClient();
    WorkflowServiceStubs stubs = client.getWorkflowServiceStubs();
    String namespace = client.getOptions().getNamespace();

    // @@@SNIPSTART java-nexus-standalone-operations-client
    // A single NexusClient is the entry point: it serves Visibility queries (list/count) and
    // produces service-bound clients.
    NexusClient nexusClient =
        NexusClient.newInstance(
            stubs, NexusClientOptions.newBuilder().setNamespace(namespace).build());
    // Typed service client: dispatches operations by method reference on the service interface.
    NexusServiceClient<GreetingNexusService> greetingClient =
        nexusClient.newNexusServiceClient(GreetingNexusService.class, ENDPOINT_NAME);
    // @@@SNIPEND

    demonstrateExecuteAndGettingHandleById(nexusClient, greetingClient);
    demonstrateStartAndGetResult(greetingClient);
    demonstrateStartAndCancel(greetingClient);
    demonstrateVisibility(nexusClient, ENDPOINT_NAME);
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // execute  — run a standalone Nexus operation and return its result in one call.
  // getHandle — reconnect to an existing operation by its ID and read its result.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  public static GreetingOutput demonstrateExecuteAndGettingHandleById(
      NexusClient nexusClient, NexusServiceClient<GreetingNexusService> greetingClient)
      throws Exception {
    // @@@SNIPSTART java-nexus-standalone-operations-execute
    // execute(...) starts the operation and blocks until it completes, returning the result in one
    // call. Used here on the synchronous 'greet' operation.
    String operationId = "execute-nexus";
    GreetingOutput executed =
        greetingClient.execute(
            GreetingNexusService::greet,
            StartNexusOperationOptions.newBuilder()
                .setId(operationId)
                .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                .build(),
            new GreetingInput("execute"));
    logger.info("execute() returned: {}", executed.getMessage());
    // @@@SNIPEND

    // @@@SNIPSTART java-nexus-standalone-operations-execute-async
    // executeAsync(...) is the same as execute(...), but returns a CompletableFuture instead of
    // blocking.
    CompletableFuture<GreetingOutput> future =
        greetingClient.executeAsync(
            GreetingNexusService::greet,
            basicOptions("execute-async-nexus"),
            new GreetingInput("executeAsync"));
    GreetingOutput executedAsync = future.get();
    logger.info("executeAsync() returned: {}", executedAsync.getMessage());
    // @@@SNIPEND

    // Reconnect to that same operation purely by its ID — nothing below references the execute()
    // call above. This is how a separate process (or a later run) addresses an operation it did not
    // start: NexusClient.getHandle(operationId, runId, resultClass) returns a typed handle (pass a
    // null runId to target the latest run). getResult() then blocks until the operation is closed;
    // since this one already completed, it returns the stored result immediately.
    NexusOperationHandle<GreetingOutput> handle =
        nexusClient.getHandle(operationId, null, GreetingOutput.class);
    GreetingOutput viaHandle = handle.getResult();
    logger.info(
        "getHandle(id={}) then getResult() returned: {}",
        handle.getNexusOperationId(),
        viaHandle.getMessage());
    return viaHandle;
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // start — launch a Nexus operation and immediately return a handle, then wait on the handle.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  public static GreetingOutput demonstrateStartAndGetResult(
      NexusServiceClient<GreetingNexusService> greetingClient) {
    // @@@SNIPSTART java-nexus-standalone-operations-start
    // start(...) returns a NexusOperationHandle as soon as the operation is started.
    NexusOperationHandle<GreetingOutput> handle =
        greetingClient.start(
            GreetingNexusService::greet,
            basicOptions("start-nexus"),
            new GreetingInput("start"));

    // getResult() blocks until the operation completes and returns its result.
    GreetingOutput greeting = handle.getResult();
    logger.info(
        "start(id={}) then getResult() returned: {}",
        handle.getNexusOperationId(),
        greeting.getMessage());
    // @@@SNIPEND
    return greeting;
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // start - launch a Nexus operation and immediately return. Does not wait for the result.
  // cancel — cooperative for workflow-backed operations (see GreetingWorkflowImpl comment).
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  public static NexusOperationException demonstrateStartAndCancel(
      NexusServiceClient<GreetingNexusService> nexusClient) {
    // The backing workflow blocks indefinitely — giving cancellation something to act on.
    NexusOperationHandle<GreetingOutput> handle =
        nexusClient.start(
            GreetingNexusService::startGreeting,
            basicOptions("start-and-cancel-nexus"),
            new GreetingInput("start-and-cancel"));
    logger.info("Started 'to-cancel' id={}, requesting cancellation", handle.getNexusOperationId());
    handle.cancel("standalone-nexus sample: cancel demo");
    // getResult() blocks until the operation reaches a terminal state. A cancelled operation
    // reports completion by throwing NexusOperationException rather than returning a result.
    try {
      handle.getResult();
      logger.warn(
          "Operation id={} unexpectedly returned a result after cancel",
          handle.getNexusOperationId());
      return null;
    } catch (NexusOperationException e) {
      logger.info(
          "Operation id={} ended as expected after cancel: {}",
          handle.getNexusOperationId(),
          e.getMessage());
      return e;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────────────────────────
  // Visibility — list (filtered) and count (total and grouped) standalone operations.
  // ─────────────────────────────────────────────────────────────────────────────────────────────
  public static NexusOperationExecutionCount demonstrateVisibility(
      NexusClient nexusClient, String endpointName) {
    // @@@SNIPSTART java-nexus-standalone-operations-list
    // listNexusOperationExecutions accepts a List Filter query. Here it filters by the built-in
    // Endpoint attribute. Query syntax is documented at https://docs.temporal.io/list-filter .
    String query = "Endpoint = \"" + endpointName + "\"";
    List<NexusOperationExecutionMetadata> operations =
        nexusClient.listNexusOperationExecutions(query).collect(Collectors.toList());
    for (NexusOperationExecutionMetadata op : operations) {
      logger.info(
          "OperationId: {}, Operation: {}, Status: {}",
          op.getOperationId(),
          op.getOperation(),
          op.getStatus());
    }
    // @@@SNIPEND

    // @@@SNIPSTART java-nexus-standalone-operations-count
    // countNexusOperationExecutions accepts the same List Filter query.
    NexusOperationExecutionCount total = nexusClient.countNexusOperationExecutions(query);
    logger.info("Total Nexus operations: {}", total.getCount());

    // A GROUP BY query returns a count per group value.
    NexusOperationExecutionCount grouped =
        nexusClient.countNexusOperationExecutions(query + " GROUP BY ExecutionStatus");
    for (NexusOperationExecutionCount.AggregationGroup group : grouped.getGroups()) {
      logger.info("  group values={} count={}", group.getGroupValues(), group.getCount());
    }
    // @@@SNIPEND
    return grouped;
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────

  /** Builds the per-call options used to start a Nexus operation. */
  private static StartNexusOperationOptions basicOptions(String name) {
    return StartNexusOperationOptions.newBuilder()
        // Required: a namespace-unique operation ID. The SDK never generates one for you, so you
        // must supply your own.
        .setId(name)
        // Total time the caller is willing to wait for the operation to complete, including any
        // server-side retries. Defaults to none (bounded only by server limits) if not set.
        .setScheduleToCloseTimeout(Duration.ofMinutes(5))
        // Other optional per-call options (not set here, shown for reference):
        //   .setScheduleToStartTimeout(...) — max time the start request may wait before a handler
        //       picks it up. Default: unset (no limit).
        //   .setStartToCloseTimeout(...)    — max time for a single start attempt. Default: unset.
        //   .setTypedSearchAttributes(...)  — Visibility search attributes to index the operation
        //       by; each attribute must be registered on the namespace first. Default: none.
        //   .setSummary(...)                — short text shown in the UI and returned by
        //       describe().getStaticSummary(). Default: none.
        //   .setIdReusePolicy(...)          — behavior when the ID was used by a previously CLOSED
        //       operation. Default: ALLOW_DUPLICATE (a new run may reuse the ID).
        //   .setIdConflictPolicy(...)       — behavior when the ID belongs to a currently RUNNING
        //       operation. Default: FAIL (reject with NexusOperationAlreadyStartedException).
        .build();
  }
}
