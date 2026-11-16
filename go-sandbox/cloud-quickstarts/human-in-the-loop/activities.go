package humanintheloop

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/openai/openai-go/v3"
	"github.com/openai/openai-go/v3/option"
	"github.com/openai/openai-go/v3/responses"
	"go.temporal.io/sdk/activity"
)

// OpenAIResponsesRequest is the input for the ProposeAction Activity.
type OpenAIResponsesRequest struct {
	Model        string `json:"model"`
	Instructions string `json:"instructions"`
	Input        string `json:"input"`
}

// proposedActionSchema is the JSON schema for ProposedAction. Strict
// structured outputs require every property to be listed as required and
// additionalProperties to be false.
var proposedActionSchema = map[string]any{
	"type": "object",
	"properties": map[string]any{
		"action_type":  map[string]any{"type": "string"},
		"description":  map[string]any{"type": "string"},
		"reasoning":    map[string]any{"type": "string"},
		"risky_action": map[string]any{"type": "boolean"},
	},
	"required":             []string{"action_type", "description", "reasoning", "risky_action"},
	"additionalProperties": false,
}

// MockProposedAction is a fixed, risky action used when no OpenAI API key is
// set, so the sample runs end to end (including the approval step) without an
// LLM.
func MockProposedAction(userRequest string) ProposedAction {
	return ProposedAction{
		ActionType:  "delete_test_data",
		Description: "Mock action for the request: " + userRequest,
		Reasoning:   "OPENAI_API_KEY is not set, so this action is mocked instead of LLM-generated.",
		RiskyAction: true,
	}
}

// ProposeAction asks the LLM for a proposed action. If OPENAI_API_KEY isn't
// set, it returns MockProposedAction instead.
func ProposeAction(ctx context.Context, request OpenAIResponsesRequest) (ProposedAction, error) {
	if os.Getenv("OPENAI_API_KEY") == "" {
		activity.GetLogger(ctx).Warn(
			"OPENAI_API_KEY is not set; returning a mock proposed action instead of calling OpenAI.")
		return MockProposedAction(request.Input), nil
	}

	// Let Temporal own retries: disable the OpenAI client's built-in retry logic.
	client := openai.NewClient(option.WithMaxRetries(0))

	// Structured outputs: the API constrains the response to ProposedAction's
	// JSON schema.
	resp, err := client.Responses.New(ctx, responses.ResponseNewParams{
		Model:        request.Model,
		Instructions: openai.String(request.Instructions),
		Input:        responses.ResponseNewParamsInputUnion{OfString: openai.String(request.Input)},
		Text: responses.ResponseTextConfigParam{
			Format: responses.ResponseFormatTextConfigUnionParam{
				OfJSONSchema: &responses.ResponseFormatTextJSONSchemaConfigParam{
					Name:   "proposed_action",
					Schema: proposedActionSchema,
					Strict: openai.Bool(true),
				},
			},
		},
	}, option.WithRequestTimeout(30*time.Second))
	if err != nil {
		return ProposedAction{}, err
	}

	// Parse here, not in the Workflow: if the output is unusable, this Activity
	// attempt fails and Temporal retries the LLM call. Failing in Workflow code
	// would instead replay the same bad result.
	var action ProposedAction
	if err := json.Unmarshal([]byte(resp.OutputText()), &action); err != nil {
		return ProposedAction{}, fmt.Errorf("LLM returned no parsable ProposedAction: %q: %w", resp.OutputText(), err)
	}
	return action, nil
}

// NotifyApprovalNeeded tells a human that an action is waiting for approval.
//
// This sample prints to the Worker's terminal. A real system might send an
// email, post to Slack, or update an approval queue UI instead.
func NotifyApprovalNeeded(ctx context.Context, request ApprovalRequest) error {
	workflowID := activity.GetInfo(ctx).WorkflowExecution.ID
	activity.GetLogger(ctx).Info("Approval needed", "RequestID", request.RequestID)

	line := strings.Repeat("=", 60)
	fmt.Printf("\n%s\nAPPROVAL REQUIRED\n%s\n", line, line)
	fmt.Printf("Workflow ID: %s\n", workflowID)
	fmt.Printf("Request ID:  %s\n", request.RequestID)
	fmt.Printf("Action:      %s\n", request.ProposedAction.ActionType)
	fmt.Printf("Description: %s\n", request.ProposedAction.Description)
	fmt.Printf("Reasoning:   %s\n", request.ProposedAction.Reasoning)
	fmt.Println("\nTo approve or reject, run:")
	fmt.Println(`  go run ./send-approval approve "Looks good"`)
	fmt.Println(`  go run ./send-approval reject "Too risky"`)
	fmt.Println("\nOr with the Temporal CLI:")
	fmt.Printf("  temporal workflow signal --workflow-id %s --name %s --input '{\"approved\": true}'\n",
		workflowID, ApprovalSignal)
	fmt.Printf("%s\n\n", line)
	return nil
}

// ExecuteAction carries out the approved action.
//
// This sample only logs the action. A real system would call an external API,
// change a database, or invoke a tool the LLM selected.
func ExecuteAction(ctx context.Context, action ProposedAction) (string, error) {
	activity.GetLogger(ctx).Info("Executing action", "ActionType", action.ActionType)
	return "Successfully executed: " + action.ActionType, nil
}
