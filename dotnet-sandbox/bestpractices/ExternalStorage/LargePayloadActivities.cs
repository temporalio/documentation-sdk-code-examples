using Temporalio.Activities;

namespace TemporalioDocs.ExternalStorageExample;

public static class LargePayloadActivities
{
    [Activity]
    public static string Echo(string input) => input;
}
