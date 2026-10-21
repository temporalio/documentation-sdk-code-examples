package externalstorage

import (
	"strings"

	"go.temporal.io/sdk/workflow"
)

// LargePayloadWorkflow returns a large result. With External Storage configured, the
// result is stored by the driver and Event History holds only a reference to it.
func LargePayloadWorkflow(ctx workflow.Context, size int) (string, error) {
	return strings.Repeat("x", size), nil
}
