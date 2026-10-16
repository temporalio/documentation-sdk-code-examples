package asynccompletion

import (
	"context"

	"go.temporal.io/sdk/activity"
)

type ApprovalRequest struct {
	OrderID string
}

// ExternalSystem is the service that finishes the work and completes the Activity,
// such as a human approval queue or a third-party job runner.
type ExternalSystem interface {
	Submit(ctx context.Context, taskToken []byte, request ApprovalRequest) error
}

type Activities struct {
	External ExternalSystem
}

func (a *Activities) RequestApproval(ctx context.Context, request ApprovalRequest) (string, error) {
	// @@@SNIPSTART go-async-activity-get-task-token
	// Retrieve the Activity information needed to asynchronously complete the Activity.
	activityInfo := activity.GetInfo(ctx)
	taskToken := activityInfo.TaskToken
	// Send the taskToken to the external service that will complete the Activity.
	if err := a.External.Submit(ctx, taskToken, request); err != nil {
		return "", err
	}
	// @@@SNIPEND

	// @@@SNIPSTART go-async-activity-return-pending
	return "", activity.ErrResultPending
	// @@@SNIPEND
}
