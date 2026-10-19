package cancellation_test

import (
	"errors"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/cancellation"
)

type CancellationTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestCancellationTestSuite(t *testing.T) {
	suite.Run(t, new(CancellationTestSuite))
}

func (s *CancellationTestSuite) Test_CancelRunsCleanup() {
	env := s.NewTestWorkflowEnvironment()
	activities := &cancellation.Activities{}
	env.RegisterActivity(activities)

	// The Activity Heartbeats on a real-time ticker, so allow more than the default 3s test timeout.
	env.SetTestTimeout(30 * time.Second)
	env.RegisterDelayedCallback(env.CancelWorkflow, 2*time.Second)
	env.ExecuteWorkflow(cancellation.YourWorkflow)

	s.True(env.IsWorkflowCompleted())
	var canceledErr *temporal.CanceledError
	s.True(errors.As(env.GetWorkflowError(), &canceledErr), "expected CanceledError, got %v", env.GetWorkflowError())
	s.Equal(int32(1), activities.CleanupCalls.Load())
}
