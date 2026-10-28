// @@@SNIPSTART dotnet-lambda-worker-handler
namespace TemporalioDocs.Workers.LambdaWorker;

using Amazon.Lambda.Core;
using Temporalio.Common;
using Temporalio.Extensions.Aws.Lambda;

public class LambdaFunction
{
    private static readonly Func<object?, ILambdaContext, Task> WorkerHandler =
        TemporalLambdaWorker.CreateHandler(
            new WorkerDeploymentVersion("my-app", "build-1"),
            config =>
            {
                config.WorkerOptions.TaskQueue = "my-task-queue";
                config.WorkerOptions.AddWorkflow<SampleWorkflow>();
                config.WorkerOptions.AddActivity(Activities.HelloActivity);
            });

    public Task HandlerAsync(Stream input, ILambdaContext context) =>
        WorkerHandler(input, context);
}
// @@@SNIPEND
