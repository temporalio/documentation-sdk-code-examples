namespace TemporalioDocs.Workflows.WorkflowBasics;

using Temporalio.Activities;

public record MyActivityParams(string Greeting, string Name);

public class MyActivities
{
    [Activity]
    public string MyActivity(MyActivityParams input) => $"{input.Greeting}, {input.Name}!";
}
