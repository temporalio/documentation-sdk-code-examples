namespace TemporalioDocs.Workflows.WorkflowBasics;

// @@@SNIPSTART dotnet-activity-basics-define
using Temporalio.Activities;

public class MyActivities
{
    // Activities can be async and/or static too. We just demonstrate instance methods since many
    // use them that way.
    [Activity]
    public string MyActivity(MyActivityParams input) =>
        $"{input.Greeting}, {input.Name}!";
}
// @@@SNIPEND

public record MyActivityParams(string Greeting, string Name);
