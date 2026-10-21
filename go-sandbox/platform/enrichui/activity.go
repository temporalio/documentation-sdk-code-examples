package enrichui

import "context"

const TaskQueue = "your-task-queue"

func YourActivity(ctx context.Context, input string) (string, error) {
	return "processed " + input, nil
}
