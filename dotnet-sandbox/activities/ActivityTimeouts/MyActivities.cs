namespace TemporalioDocs.Activities.ActivityTimeouts;

using Temporalio.Activities;
using Temporalio.Exceptions;

public record MyActivityParams(string Greeting, string Name);

public class MyActivities
{
    [Activity]
    public string MyActivity(MyActivityParams input) => $"{input.Greeting}, {input.Name}!";

    [Activity]
    public void MyFailingActivity()
    {
        // @@@SNIPSTART dotnet-activity-timeouts-next-retry-delay
        var attempt = ActivityExecutionContext.Current.Info.Attempt;

        throw new ApplicationFailureException(
            $"Something bad happened on attempt {attempt}",
            errorType: "my_failure_type",
            nextRetryDelay: TimeSpan.FromSeconds(3 * attempt));
        // @@@SNIPEND
    }
}
