// The OpenAI .NET SDK marks its Responses API as experimental (OPENAI001).
#pragma warning disable OPENAI001

using System.ClientModel.Primitives;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using OpenAI.Responses;
using Temporalio.Activities;

namespace TemporalioDocs.CloudQuickstarts.HumanInTheLoop;

/// <summary>Input for <see cref="AgentActivities.ProposeActionAsync"/>.</summary>
public record OpenAIResponsesRequest(string Model, string Instructions, string Input);

/// <summary>Activities for the human-in-the-loop agent.</summary>
public static class AgentActivities
{
    // JSON schema for ProposedAction. Strict structured outputs require every property
    // to be listed as required and additionalProperties to be false.
    private static readonly BinaryData ProposedActionSchema = BinaryData.FromString("""
        {
          "type": "object",
          "properties": {
            "action_type": { "type": "string" },
            "description": { "type": "string" },
            "reasoning": { "type": "string" },
            "risky_action": { "type": "boolean" }
          },
          "required": ["action_type", "description", "reasoning", "risky_action"],
          "additionalProperties": false
        }
        """);

    /// <summary>
    /// A fixed, risky action used when no OpenAI API key is set, so the sample runs end
    /// to end (including the approval step) without an LLM.
    /// </summary>
    public static ProposedAction MockProposedAction(string userRequest) => new(
        ActionType: "delete_test_data",
        Description: $"Mock action for the request: {userRequest}",
        Reasoning: "OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.",
        RiskyAction: true);

    /// <summary>
    /// Asks the LLM for a proposed action. If OPENAI_API_KEY isn't set, returns
    /// <see cref="MockProposedAction"/> instead.
    /// </summary>
    [Activity]
    public static async Task<ProposedAction> ProposeActionAsync(OpenAIResponsesRequest request)
    {
        var context = ActivityExecutionContext.Current;
        var apiKey = Environment.GetEnvironmentVariable("OPENAI_API_KEY");
        if (string.IsNullOrEmpty(apiKey))
        {
            context.Logger.LogWarning(
                "OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI.");
            return MockProposedAction(request.Input);
        }

        // Let Temporal own retries: disable the OpenAI client's built-in retry logic.
        var client = new ResponsesClient(
            new System.ClientModel.ApiKeyCredential(apiKey),
            new ResponsesClientOptions
            {
                RetryPolicy = new ClientRetryPolicy(maxRetries: 0),
                NetworkTimeout = TimeSpan.FromSeconds(30),
            });

        // Structured outputs: the API constrains the response to ProposedAction's JSON
        // schema.
        var options = new CreateResponseOptions(
            request.Model, [ResponseItem.CreateUserMessageItem(request.Input)])
        {
            Instructions = request.Instructions,
            TextOptions = new ResponseTextOptions
            {
                TextFormat = ResponseTextFormat.CreateJsonSchemaFormat(
                    "proposed_action", ProposedActionSchema, jsonSchemaIsStrict: true),
            },
        };
        ResponseResult response = await client.CreateResponseAsync(options, context.CancellationToken);
        var text = response.GetOutputText();

        // Parse here, not in the Workflow: if the output is unusable, this Activity
        // attempt fails and Temporal retries the LLM call. Failing in Workflow code would
        // instead replay the same bad result.
        try
        {
            return JsonSerializer.Deserialize<ProposedAction>(text)
                ?? throw new JsonException("Response was null");
        }
        catch (JsonException e)
        {
            throw new InvalidOperationException($"LLM returned no parsable ProposedAction: {text}", e);
        }
    }

    /// <summary>
    /// Tells a human that an action is waiting for approval.
    /// </summary>
    /// <remarks>
    /// This sample prints to the Worker's terminal. A real system might send an email,
    /// post to Slack, or update an approval queue UI instead.
    /// </remarks>
    [Activity]
    public static void NotifyApprovalNeeded(ApprovalRequest request)
    {
        var context = ActivityExecutionContext.Current;
        var workflowId = context.Info.WorkflowId;
        context.Logger.LogInformation("Approval needed for request: {RequestId}", request.RequestId);

        var line = new string('=', 60);
        Console.WriteLine($"""

            {line}
            APPROVAL REQUIRED
            {line}
            Workflow ID: {workflowId}
            Request ID:  {request.RequestId}
            Action:      {request.ProposedAction.ActionType}
            Description: {request.ProposedAction.Description}
            Reasoning:   {request.ProposedAction.Reasoning}

            To approve or reject, run:
              dotnet run -- approve "Looks good"
              dotnet run -- reject "Too risky"

            Or with the Temporal CLI:
              temporal workflow signal --workflow-id {workflowId} --name {Shared.ApprovalSignal} --input '{"{"}"approved": true{"}"}'
            {line}

            """);
    }

    /// <summary>Carries out the approved action.</summary>
    /// <remarks>
    /// This sample only logs the action. A real system would call an external API, change
    /// a database, or invoke a tool the LLM selected.
    /// </remarks>
    [Activity]
    public static string ExecuteAction(ProposedAction action)
    {
        ActivityExecutionContext.Current.Logger.LogInformation(
            "Executing action: {ActionType}", action.ActionType);
        return $"Successfully executed: {action.ActionType}";
    }
}
