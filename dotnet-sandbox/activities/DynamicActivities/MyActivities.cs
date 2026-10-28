namespace TemporalioDocs.Activities.DynamicActivities;

using Temporalio.Activities;
using Temporalio.Converters;

public record MyActivityParams(string Greeting, string Name);

// @@@SNIPSTART dotnet-dynamic-activity
public class MyActivities
{
    [Activity(Dynamic = true)]
    public string DynamicActivity(IRawValue[] args)
    {
        var input = ActivityExecutionContext.Current.PayloadConverter.ToValue<MyActivityParams>(args.Single());
        return $"{input.Greeting}, {input.Name}!";
    }
}
// @@@SNIPEND
