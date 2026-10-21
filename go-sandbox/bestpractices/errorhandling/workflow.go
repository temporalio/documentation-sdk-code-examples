package errorhandling

import (
	"errors"
	"time"

	enumspb "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/workflow"
)

// HandleErrorsWorkflow runs YourActivity and returns which kind of error it handled.
func HandleErrorsWorkflow(ctx workflow.Context, failure string) (string, error) {
	ctx = workflow.WithActivityOptions(ctx, workflow.ActivityOptions{
		StartToCloseTimeout: 5 * time.Second,
		RetryPolicy:         &temporal.RetryPolicy{MaximumAttempts: 1},
	})
	outcome := "no error"

	// @@@SNIPSTART go-error-handling-activity-errors
	err := workflow.ExecuteActivity(ctx, YourActivity, failure).Get(ctx, nil)
	if err != nil {
		var applicationErr *temporal.ApplicationError
		if errors.As(err, &applicationErr) {
			// retrieve error message
			workflow.GetLogger(ctx).Info("Application error", "error", applicationErr.Error())

			// handle Activity errors (created via NewApplicationError() API)
			var detailMsg string // assuming Activity return error by NewApplicationError("message", "ErrType", "string details")
			if err := applicationErr.Details(&detailMsg); err != nil { // extract strong typed details
				workflow.GetLogger(ctx).Warn("Unable to decode error details", "error", err)
			}

			// handle Activity errors (errors created other than using NewApplicationError() API)
			switch applicationErr.Type() {
			case "CustomErrTypeA":
				// handle CustomErrTypeA
				outcome = "CustomErrTypeA: " + detailMsg
			case CustomErrTypeB:
				// handle CustomErrTypeB
				outcome = "CustomErrTypeB: " + detailMsg
			default:
				// newer version of Activity could return new errors that Workflow was not aware of.
				outcome = "unknown application error"
			}
		}

		var canceledErr *temporal.CanceledError
		if errors.As(err, &canceledErr) {
			// handle cancellation
			outcome = "canceled"
		}

		var timeoutErr *temporal.TimeoutError
		if errors.As(err, &timeoutErr) {
			// handle timeout, could check timeout type by timeoutErr.TimeoutType()
			switch timeoutErr.TimeoutType() {
			case enumspb.TIMEOUT_TYPE_SCHEDULE_TO_START:
				// Handle ScheduleToStart timeout.
				outcome = "ScheduleToStart timeout"
			case enumspb.TIMEOUT_TYPE_START_TO_CLOSE:
				// Handle StartToClose timeout.
				outcome = "StartToClose timeout"
			case enumspb.TIMEOUT_TYPE_HEARTBEAT:
				// Handle heartbeat timeout.
				outcome = "Heartbeat timeout"
			default:
			}
		}

		var panicErr *temporal.PanicError
		if errors.As(err, &panicErr) {
			// handle panic, message and call stack are available by panicErr.Error() and panicErr.StackTrace()
			outcome = "panic"
		}
	}
	// @@@SNIPEND
	return outcome, nil
}
