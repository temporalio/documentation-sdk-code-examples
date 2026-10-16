package asynccompletion_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/activities/asynccompletion"
)

// recordingExternalSystem keeps the Task Token so the test can complete the Activity.
type recordingExternalSystem struct {
	taskToken []byte
}

func (r *recordingExternalSystem) Submit(ctx context.Context, taskToken []byte, request asynccompletion.ApprovalRequest) error {
	r.taskToken = taskToken
	return nil
}

type AsyncCompletionTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestAsyncCompletionTestSuite(t *testing.T) {
	suite.Run(t, new(AsyncCompletionTestSuite))
}

func (s *AsyncCompletionTestSuite) runWorkflow(result interface{}, completionErr error) (string, error) {
	env := s.NewTestWorkflowEnvironment()
	external := &recordingExternalSystem{}
	env.RegisterActivity(&asynccompletion.Activities{External: external})

	env.RegisterDelayedCallback(func() {
		s.NotEmpty(external.taskToken)
		s.NoError(env.CompleteActivity(external.taskToken, result, completionErr))
	}, time.Minute)

	env.ExecuteWorkflow(asynccompletion.ApprovalWorkflow, asynccompletion.ApprovalRequest{OrderID: "order-123"})
	s.True(env.IsWorkflowCompleted())

	var approver string
	if err := env.GetWorkflowError(); err != nil {
		return "", err
	}
	s.NoError(env.GetWorkflowResult(&approver))
	return approver, nil
}

func (s *AsyncCompletionTestSuite) Test_CompleteActivity() {
	approver, err := s.runWorkflow("approver@example.com", nil)
	s.NoError(err)
	s.Equal("approver@example.com", approver)
}

func (s *AsyncCompletionTestSuite) Test_FailActivity() {
	_, err := s.runWorkflow(nil, temporal.NewNonRetryableApplicationError("order rejected", "Rejected", nil))
	s.Error(err)
	var appErr *temporal.ApplicationError
	s.True(errors.As(err, &appErr))
	s.Equal("Rejected", appErr.Type())
}
