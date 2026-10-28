namespace TemporalioDocs.Activities.BenignExceptions;

public partial class MyActivities
{
    // Simulates an external service that isn't ready yet.
    private static async Task<string> CallExternalServiceAsync()
    {
        await Task.Delay(100);
        throw new HttpRequestException("Service unavailable");
    }
}
