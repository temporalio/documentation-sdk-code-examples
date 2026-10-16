package asynccompletion

import (
	"time"

	"go.temporal.io/sdk/workflow"
)

const (
	TaskQueue          = "async-activity-completion"
	ApprovalActivityID = "request-approval"
)

func ApprovalWorkflow(ctx workflow.Context, request ApprovalRequest) (string, error) {
	// The timeout covers the time the external system takes to complete the Activity.
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		// A known Activity ID lets an external system complete the Activity without a Task Token.
		ActivityID:          ApprovalActivityID,
		StartToCloseTimeout: 24 * time.Hour,
	})

	var a *Activities
	var approver string
	err := workflow.ExecuteActivity(ctx, a.RequestApproval, request).Get(ctx, &approver)
	return approver, err
}
