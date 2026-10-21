package enrichui

// @@@SNIPSTART go-enrich-ui-timer-summary
import (
	"time"

	"go.temporal.io/sdk/workflow"
)

func TimerSummaryWorkflow(ctx workflow.Context, input string) (string, error) {
	// Create a timer with options including summary
	timerFuture := workflow.NewTimerWithOptions(ctx, 5*time.Minute, workflow.TimerOptions{
		Summary: "Waiting for payment confirmation",
	})

	// Wait for the timer
	err := timerFuture.Get(ctx, nil)
	if err != nil {
		return "", err
	}

	return "Timer completed", nil
}
// @@@SNIPEND
