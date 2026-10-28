namespace TemporalioDocs.Activities.BenignExceptions;

// @@@SNIPSTART dotnet-benign-exceptions-activity
using Temporalio.Activities;
using Temporalio.Api.Enums.V1;
using Temporalio.Exceptions;

public partial class MyActivities
{
    [Activity]
    public async Task<string> MyActivityAsync()
    {
        try
        {
            return await CallExternalServiceAsync();
        }
        catch (Exception e)
        {
            // Mark this error as benign since it's expected
            throw new ApplicationFailureException(
                "Service is down",
                inner: e,
                category: ApplicationErrorCategory.Benign);
        }
    }
}
// @@@SNIPEND
