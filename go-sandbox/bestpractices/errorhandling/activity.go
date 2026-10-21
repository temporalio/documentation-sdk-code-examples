package errorhandling

import (
	"context"
	"time"

	"go.temporal.io/sdk/temporal"
)

const (
	CustomErrTypeA = "CustomErrTypeA"
	CustomErrTypeB = "CustomErrTypeB"
)

// YourActivity fails in the way that failure names, so the Workflow can show each error type.
func YourActivity(ctx context.Context, failure string) error {
	switch failure {
	case CustomErrTypeA, CustomErrTypeB:
		return temporal.NewNonRetryableApplicationError("activity failed", failure, nil, "string details")
	case "timeout":
		time.Sleep(time.Minute)
	case "panic":
		panic("activity panicked")
	}
	return nil
}
