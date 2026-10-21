package timeskip

// @@@SNIPSTART go-testing-auto-time-skipping
import (
	"testing"

	"go.temporal.io/sdk/testsuite"
)

func TestWorkflow_AutoTimeSkipping(t *testing.T) {
	var ts testsuite.WorkflowTestSuite
	env := ts.NewTestWorkflowEnvironment()

	env.ExecuteWorkflow(MyWorkflow)

	if !env.IsWorkflowCompleted() {
		t.Fatal("workflow did not complete")
	}

	if err := env.GetWorkflowError(); err != nil {
		t.Fatal(err)
	}
}
// @@@SNIPEND
