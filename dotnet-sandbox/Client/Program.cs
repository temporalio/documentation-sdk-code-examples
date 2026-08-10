using MyNamespace;
using Temporalio.Client;
using Temporalio.Workflows;

// Create a client to localhost on "default" namespace
var interceptor = new SimpleWorkerInterceptor();

var client = await TemporalClient.ConnectAsync(new()
{
    TargetHost = "localhost:7233",
    Interceptors = [],
});

// Run workflow
var result = await client.ExecuteWorkflowAsync(
    (SayHelloWorkflow wf) => wf.RunAsync("Temporal"),
    new(id: $"my-workflow-id-{Guid.NewGuid()}", taskQueue: "my-task-queue")
);

Console.WriteLine("Workflow result: {0}", result);