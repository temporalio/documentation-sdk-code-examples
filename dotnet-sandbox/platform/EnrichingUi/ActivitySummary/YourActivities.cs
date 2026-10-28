namespace TemporalioDocs.Platform.EnrichingUi.ActivitySummary;

using Temporalio.Activities;

public class YourActivities
{
    [Activity]
    public Task<string> YourActivityAsync(string input) => Task.FromResult($"Processed {input}");
}
