namespace TemporalioDocs.Workers.RunWorker;

using Temporalio.Worker;

public static class RegisterTypes
{
    public static TemporalWorkerOptions CreateOptions(DatabaseClient databaseClient)
    {
        // @@@SNIPSTART dotnet-run-worker-register-types
        var options = new TemporalWorkerOptions("my-task-queue");
        options.AddWorkflow<GreetingWorkflow>();
        options.AddWorkflow<OrderWorkflow>();
        options.AddAllActivities(new MyActivities(databaseClient));
        // @@@SNIPEND
        return options;
    }
}
