package basics

import (
	"time"

	"github.com/google/uuid"
	"go.temporal.io/sdk/workflow"
)

// @@@SNIPSTART go-workflows-basics-logging
func MyWorkflow(ctx workflow.Context, name string) (string, error) {
	logger := workflow.GetLogger(ctx)
	logger.Info("Starting workflow", "name", name)
	// ...
	return "Hello " + name, nil
}
// @@@SNIPEND

type DeterministicValues struct {
	ID          string
	CurrentTime time.Time
}

// DeterministicValuesWorkflow generates values that stay the same when the
// Workflow replays.
func DeterministicValuesWorkflow(ctx workflow.Context) (*DeterministicValues, error) {
	// @@@SNIPSTART go-workflows-basics-side-effect-uuid
	var id string
	encodedID := workflow.SideEffect(ctx, func(ctx workflow.Context) interface{} {
		return uuid.New().String()
	})
	if err := encodedID.Get(&id); err != nil {
		return nil, err
	}
	// @@@SNIPEND

	// @@@SNIPSTART go-workflows-basics-now
	currentTime := workflow.Now(ctx)
	// @@@SNIPEND

	// @@@SNIPSTART go-workflows-basics-is-replaying
	if !workflow.IsReplaying(ctx) {
		emitMetric("workflow_started", 1)
	}
	// @@@SNIPEND

	return &DeterministicValues{ID: id, CurrentTime: currentTime}, nil
}

// emitMetric stands in for a call to an external metrics system.
func emitMetric(name string, value int) {}
