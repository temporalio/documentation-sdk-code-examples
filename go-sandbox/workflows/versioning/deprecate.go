package versioning

import "go.temporal.io/sdk/workflow"

// NotificationWorkflow supports only versions 2 and 3 of the "ChangedNotificationActivityType" change.
func NotificationWorkflow(ctx workflow.Context) error {
	ctx = workflow.WithActivityOptions(ctx, activityOptions)
	var err error

	// @@@SNIPSTART go-versioning-deprecate
	version := workflow.GetVersion(ctx, "ChangedNotificationActivityType", 2, 3)
	if version == 2 {
		err = workflow.ExecuteActivity(ctx, SendTextMessage).Get(ctx, nil)
	} else {
		err = workflow.ExecuteActivity(ctx, SendTweet).Get(ctx, nil)
	}
	// @@@SNIPEND
	return err
}
