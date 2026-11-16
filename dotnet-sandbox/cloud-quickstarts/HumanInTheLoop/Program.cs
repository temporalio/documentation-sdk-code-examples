using Microsoft.Extensions.Logging;
using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Exceptions;
using Temporalio.Worker;
using TemporalioDocs.CloudQuickstarts.HumanInTheLoop;

const string Usage = """
    Usage:
      dotnet run -- worker
      dotnet run -- client [request]
      dotnet run -- approve|reject [notes]
    """;
const string DefaultRequest = "Delete all test data from the production database";

switch (args.FirstOrDefault())
{
    case "worker":
        await RunWorkerAsync();
        break;
    case "client":
        await RunClientAsync(args.ElementAtOrDefault(1) ?? DefaultRequest);
        break;
    case "approve" or "reject":
        await SendApprovalAsync(args[0] == "approve", args.ElementAtOrDefault(1));
        break;
    default:
        Console.WriteLine(Usage);
        break;
}

static TemporalClientConnectOptions LoadConnectOptions()
{
    // LoadClientConnectOptions reads the TEMPORAL_PROFILE env var (default
    // "default"), loads that profile from the TOML config file (default OS config
    // dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
    // connection details are hardcoded, so the same Worker and clients run against
    // a local dev server or Temporal Cloud.
    var options = ClientEnvConfig.LoadClientConnectOptions();
    // With no config file and no env overrides the loaded options are empty, so
    // fall back to the local dev server address.
    options.TargetHost ??= "localhost:7233";
    // Show the Workflow and Activity log lines in the Worker's terminal.
    options.LoggerFactory = LoggerFactory.Create(builder =>
        builder.AddSimpleConsole(o => o.SingleLine = true).SetMinimumLevel(LogLevel.Information));
    return options;
}

static async Task RunWorkerAsync()
{
    var options = LoadConnectOptions();
    var client = await TemporalClient.ConnectAsync(options);

    using var tokenSource = new CancellationTokenSource();
    Console.CancelKeyPress += (_, eventArgs) =>
    {
        tokenSource.Cancel();
        eventArgs.Cancel = true;
    };

    using var worker = new TemporalWorker(
        client,
        new TemporalWorkerOptions(Shared.TaskQueue)
            .AddWorkflow<HumanInTheLoopWorkflow>()
            .AddActivity(AgentActivities.ProposeActionAsync)
            .AddActivity(AgentActivities.NotifyApprovalNeeded)
            .AddActivity(AgentActivities.ExecuteAction));

    Console.WriteLine(
        $"Worker connected to {options.TargetHost} (namespace: {options.Namespace}), " +
        $"polling task queue '{Shared.TaskQueue}'");
    try
    {
        await worker.ExecuteAsync(tokenSource.Token);
    }
    catch (OperationCanceledException)
    {
        Console.WriteLine("Worker stopped");
    }
}

static async Task RunClientAsync(string userRequest)
{
    var options = LoadConnectOptions();
    var client = await TemporalClient.ConnectAsync(options);

    WorkflowHandle<HumanInTheLoopWorkflow, string> handle;
    try
    {
        handle = await client.StartWorkflowAsync(
            (HumanInTheLoopWorkflow wf) => wf.RunAsync(new WorkflowInput(userRequest, ApprovalTimeoutSeconds: 300)),
            new(id: Shared.WorkflowId, taskQueue: Shared.TaskQueue));
    }
    catch (WorkflowAlreadyStartedException)
    {
        Console.WriteLine(
            $"Workflow {Shared.WorkflowId} is already running. Approve or reject it, wait for it " +
            $"to time out, or terminate it:\n  temporal workflow terminate --workflow-id {Shared.WorkflowId}");
        Environment.Exit(1);
        return;
    }

    Console.WriteLine(
        $"Started Workflow {handle.Id} against {options.TargetHost} (namespace: {options.Namespace})...");
    Console.WriteLine($"Request: {userRequest}");
    Console.WriteLine("If the action is risky, the Worker prints how to approve or reject it.");

    Console.WriteLine($"Result: {await handle.GetResultAsync()}");
}

static async Task SendApprovalAsync(bool approved, string? notes)
{
    var client = await TemporalClient.ConnectAsync(LoadConnectOptions());

    // Deliver the reviewer's decision to the waiting Workflow as a Signal. With no
    // RequestId, it applies to whichever request is pending.
    var decision = new ApprovalDecision(
        Approved: approved,
        ReviewerNotes: notes,
        DecidedAt: DateTimeOffset.UtcNow.ToString("O"));
    var handle = client.GetWorkflowHandle<HumanInTheLoopWorkflow>(Shared.WorkflowId);
    await handle.SignalAsync(wf => wf.ApprovalDecisionAsync(decision));

    Console.WriteLine($"Sent {(approved ? "approve" : "reject")} decision to Workflow {Shared.WorkflowId}");
}
