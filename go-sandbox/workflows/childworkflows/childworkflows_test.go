package childworkflows_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workflows/childworkflows"
)

type ChildWorkflowsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestChildWorkflowsTestSuite(t *testing.T) {
	suite.Run(t, new(ChildWorkflowsTestSuite))
}

func (s *ChildWorkflowsTestSuite) run(wf interface{}) childworkflows.ParentResp {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterWorkflow(childworkflows.YourOtherWorkflowDefinition)
	env.ExecuteWorkflow(wf, childworkflows.ParentParams{Name: "Temporal"})
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var resp childworkflows.ParentResp
	s.NoError(env.GetWorkflowResult(&resp))
	return resp
}

func (s *ChildWorkflowsTestSuite) Test_ExecuteChildWorkflow() {
	s.Equal("Hello Temporal", s.run(childworkflows.YourWorkflowDefinition).Greeting)
}

func (s *ChildWorkflowsTestSuite) Test_AsyncChildWorkflow() {
	s.NotEmpty(s.run(childworkflows.YourAsyncWorkflowDefinition).ChildWorkflowID)
}

func (s *ChildWorkflowsTestSuite) Test_ParentClosePolicy() {
	s.Equal("Hello Temporal", s.run(childworkflows.YourParentClosePolicyWorkflowDefinition).Greeting)
}
