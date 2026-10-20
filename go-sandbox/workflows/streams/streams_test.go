package streams_test

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/workflows/streams"
)

type StreamsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestStreamsTestSuite(t *testing.T) {
	suite.Run(t, new(StreamsTestSuite))
}

func (s *StreamsTestSuite) Test_OrderWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(streams.ValidateOrder)
	env.RegisterActivity(streams.ChargePayment)
	env.RegisterActivity(streams.DispatchOrder)
	env.ExecuteWorkflow(streams.OrderWorkflow, streams.OrderInput{OrderID: "order-1"})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *StreamsTestSuite) requireContinueAsNew(wf interface{}) *workflow.ContinueAsNewError {
	env := s.NewTestWorkflowEnvironment()
	env.SetContinueAsNewSuggested(true)
	env.ExecuteWorkflow(wf, streams.WorkflowInput{})
	s.True(env.IsWorkflowCompleted())

	var canErr *workflow.ContinueAsNewError
	s.True(errors.As(env.GetWorkflowError(), &canErr), "expected Continue-As-New, got %v", env.GetWorkflowError())
	return canErr
}

func (s *StreamsTestSuite) Test_LongRunningWorkflowContinuesAsNew() {
	canErr := s.requireContinueAsNew(streams.LongRunningWorkflow)
	s.Equal("LongRunningWorkflow", canErr.WorkflowType.Name)
}

func (s *StreamsTestSuite) Test_LongRunningWorkflowOtherQueue() {
	canErr := s.requireContinueAsNew(streams.LongRunningWorkflowOtherQueue)
	s.Equal("other-tq", canErr.TaskQueueName)
}
