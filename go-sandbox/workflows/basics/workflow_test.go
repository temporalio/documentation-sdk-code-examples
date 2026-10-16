package basics_test

import (
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/basics"
)

type WorkflowBasicsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestWorkflowBasicsTestSuite(t *testing.T) {
	suite.Run(t, new(WorkflowBasicsTestSuite))
}

func (s *WorkflowBasicsTestSuite) Test_MyWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(basics.MyWorkflow, "Temporal")
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result string
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal("Hello Temporal", result)
}

func (s *WorkflowBasicsTestSuite) Test_DeterministicValuesWorkflow() {
	env := s.NewTestWorkflowEnvironment()
	env.ExecuteWorkflow(basics.DeterministicValuesWorkflow)
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result basics.DeterministicValues
	s.NoError(env.GetWorkflowResult(&result))
	_, err := uuid.Parse(result.ID)
	s.NoError(err)
	s.False(result.CurrentTime.IsZero())
}
