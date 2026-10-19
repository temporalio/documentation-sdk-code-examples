package selectors_test

import (
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/selectors"
)

type SelectorsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestSelectorsTestSuite(t *testing.T) {
	suite.Run(t, new(SelectorsTestSuite))
}

func (s *SelectorsTestSuite) Test_SampleWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(selectors.ExampleActivity)
	env.ExecuteWorkflow(selectors.SampleWorkflow)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
}

func (s *SelectorsTestSuite) Test_ProcessAllWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterActivity(selectors.ProcessItemActivity)
	env.ExecuteWorkflow(selectors.ProcessAllWorkflow, []string{"a", "b", "c"})
	s.NoError(env.GetWorkflowError())

	var results []string
	s.NoError(env.GetWorkflowResult(&results))
	s.ElementsMatch([]string{"processed a", "processed b", "processed c"}, results)
}

func (s *SelectorsTestSuite) Test_ProcessAllWorkflowNoItems() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(selectors.ProcessAllWorkflow, []string{})
	s.NoError(env.GetWorkflowError())
}

func (s *SelectorsTestSuite) runOrder(processingTime time.Duration) bool {
	env := s.NewTestWorkflowEnvironment()
	env.OnActivity(selectors.OrderProcessingActivity, mock.Anything, mock.Anything).After(processingTime).Return(nil)
	env.RegisterActivity(selectors.SendEmailActivity)
	env.ExecuteWorkflow(selectors.OrderProcessingWorkflow, processingTime, time.Minute)
	s.NoError(env.GetWorkflowError())

	var emailSent bool
	s.NoError(env.GetWorkflowResult(&emailSent))
	return emailSent
}

func (s *SelectorsTestSuite) Test_OrderProcessingFast() {
	s.False(s.runOrder(10 * time.Second))
}

func (s *SelectorsTestSuite) Test_OrderProcessingSlowSendsEmail() {
	s.True(s.runOrder(5 * time.Minute))
}
