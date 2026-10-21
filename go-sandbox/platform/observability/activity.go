package observability

import (
	"context"

	"go.temporal.io/sdk/activity"
)

func Activity(ctx context.Context, name string) (string, error) {
	activity.GetLogger(ctx).Info("Activity", "name", name)
	return "Hello " + name + "!", nil
}
