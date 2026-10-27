namespace TemporalioDocs.Workflows.Versioning;

using Temporalio.Activities;

public class MyActivities
{
    [Activity]
    public string PrePatchActivity() => "pre-patch";

    [Activity]
    public string PostPatchActivity() => "post-patch";
}
