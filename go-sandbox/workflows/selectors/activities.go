package selectors

import (
	"context"
	"time"

	"go.temporal.io/sdk/activity"
)

func ExampleActivity(ctx context.Context) (string, error) {
	return "done", nil
}

func ProcessItemActivity(ctx context.Context, item string) (string, error) {
	return "processed " + item, nil
}

// OrderProcessingActivity stands in for slow order processing.
func OrderProcessingActivity(ctx context.Context, processingTime time.Duration) error {
	select {
	case <-time.After(processingTime):
		return nil
	case <-ctx.Done():
		return ctx.Err()
	}
}

func SendEmailActivity(ctx context.Context) error {
	activity.GetLogger(ctx).Info("Sending email: order processing is taking longer than expected")
	return nil
}
