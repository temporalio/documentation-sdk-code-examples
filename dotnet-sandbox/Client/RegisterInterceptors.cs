using MyNamespace;
using Temporalio.Client;
using Temporalio.Extensions.OpenTelemetry;
using Temporalio.Worker;
using Temporalio.Worker.Interceptors;

public static class RegisterInterceptors
{
    public static async Task<TemporalClient> ConnectWithTracingAsync()
    {
        // @@@SNIPSTART dotnet-interceptors-register-client
        var interceptor = new TracingInterceptor();

        var client = await TemporalClient.ConnectAsync(new()
        {
            TargetHost = "localhost:7233",
            Interceptors = [interceptor],
        });
        // @@@SNIPEND
        return client;
    }

    public static async Task RunWorkerWithTracingAsync(
        ITemporalClient client, CancellationToken cancellationToken)
    {
        var interceptor = new TracingInterceptor();
        var activities = new MyActivities();

        // @@@SNIPSTART dotnet-interceptors-register-worker
        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions("my-task-queue")
            {
                Interceptors = [interceptor]
            }
            .AddActivity(activities.SayHello)
            .AddWorkflow<SayHelloWorkflow>()
        );
        // @@@SNIPEND
        await worker.ExecuteAsync(cancellationToken);
    }

    public static async Task RunWorkerWithMetricsAsync(
        ITemporalClient client, CancellationToken cancellationToken)
    {
        var activities = new MyActivities();

        // @@@SNIPSTART dotnet-interceptors-register-worker-activity-metrics
        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions("my-task-queue")
            {
                Interceptors = new IWorkerInterceptor[]
                {
                    new SimpleWorkerInterceptor(),
                },
            }
            .AddActivity(activities.SayHello)
            .AddWorkflow<SayHelloWorkflow>());

        await worker.ExecuteAsync(cancellationToken);
        // @@@SNIPEND
    }
}
