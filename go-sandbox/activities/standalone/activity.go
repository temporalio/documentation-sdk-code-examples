package standalone

import (
	"context"
	"fmt"
)

const (
	TaskQueue  = "standalone-activity-helloworld"
	ActivityID = "standalone_activity_helloworld_ActivityID"
)

func Activity(ctx context.Context, name string) (string, error) {
	return fmt.Sprintf("Hello %s!", name), nil
}
