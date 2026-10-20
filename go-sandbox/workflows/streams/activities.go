package streams

import "context"

const TaskQueue = "workflow-streams"

func ValidateOrder(ctx context.Context, orderID string) error { return nil }
func ChargePayment(ctx context.Context, orderID string) error { return nil }
func DispatchOrder(ctx context.Context, orderID string) error { return nil }

type Delta struct {
	Text string
}

type ProgressEvent struct {
	Message string
}

// generateDeltas stands in for a source that produces output incrementally.
func generateDeltas(orderID string) <-chan Delta {
	ch := make(chan Delta)
	go func() {
		defer close(ch)
		for _, text := range []string{"Order ", orderID, " is ", "on its way."} {
			ch <- Delta{Text: text}
		}
	}()
	return ch
}

func firstPhase() []Delta  { return []Delta{{Text: "phase one"}} }
func secondPhase(checkpointID string) []Delta {
	return []Delta{{Text: "phase two after " + checkpointID}}
}

// recordPhaseOneComplete stands in for writing a checkpoint to an external system.
func recordPhaseOneComplete(ctx context.Context) (string, error) { return "checkpoint-1", nil }
