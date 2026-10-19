package continueasnew_test

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/workflows/continueasnew"
)

type ContinueAsNewTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestContinueAsNewTestSuite(t *testing.T) {
	suite.Run(t, new(ContinueAsNewTestSuite))
}

func (s *ContinueAsNewTestSuite) requireContinueAsNew(env *testsuite.TestWorkflowEnvironment) {
	s.True(env.IsWorkflowCompleted())
	err := env.GetWorkflowError()
	var canErr *workflow.ContinueAsNewError
	s.True(errors.As(err, &canErr), "expected Continue-As-New, got %v", err)
	s.Equal("ClusterManagerWorkflow", canErr.WorkflowType.Name)
}

func (s *ContinueAsNewTestSuite) Test_ContinuesAsNewWithTestHook() {
	env := s.NewTestWorkflowEnvironment()
	// The test environment doesn't count Events, so report a length above the test hook's limit.
	env.SetCurrentHistoryLength(200)
	env.ExecuteWorkflow(continueasnew.ClusterManagerWorkflow, continueasnew.ClusterManagerInput{TestContinueAsNew: true})
	s.requireContinueAsNew(env)
}

func (s *ContinueAsNewTestSuite) Test_ContinuesAsNewWhenSuggested() {
	env := s.NewTestWorkflowEnvironment()
	env.SetContinueAsNewSuggested(true)
	env.ExecuteWorkflow(continueasnew.ClusterManagerWorkflow, continueasnew.ClusterManagerInput{})
	s.requireContinueAsNew(env)
}

func (s *ContinueAsNewTestSuite) Test_CompletesWithoutTestHook() {
	env := s.NewTestWorkflowEnvironment()
	env.SetCurrentHistoryLength(200)
	env.ExecuteWorkflow(continueasnew.ClusterManagerWorkflow, continueasnew.ClusterManagerInput{})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result continueasnew.ClusterManagerResult
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal(50, result.JobsProcessed)
}

func (s *ContinueAsNewTestSuite) Test_ResumesFromState() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(continueasnew.ClusterManagerWorkflow, continueasnew.ClusterManagerInput{
		State: &continueasnew.ClusterManagerState{JobsProcessed: 48},
	})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result continueasnew.ClusterManagerResult
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal(50, result.JobsProcessed)
}
