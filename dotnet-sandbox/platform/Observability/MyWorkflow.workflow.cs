namespace TemporalioDocs.Platform.Observability;

using Temporalio.Common;
using Temporalio.Workflows;

[Workflow]
public class MyWorkflow
{
    [WorkflowRun]
    public async Task RunAsync()
    {
        // @@@SNIPSTART dotnet-observability-upsert-search-attributes
        // These only need to be created once, so it is common to make them static readonly even though we
        // create inline here for demonstration
        var myKeywordAttributeKey = SearchAttributeKey.CreateKeyword("MyKeywordAttribute");
        var myTextAttributeKey = SearchAttributeKey.CreateText("MyTextAttribute");

        // Add/Update the keyword one and remove the text one
        Workflow.UpsertTypedSearchAttributes(
            myKeywordAttributeKey.ValueSet("SomeKeywordValue"),
            myTextAttributeKey.ValueUnset());
        // @@@SNIPEND
    }
}
