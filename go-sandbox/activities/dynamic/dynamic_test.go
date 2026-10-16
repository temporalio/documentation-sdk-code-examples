package dynamic_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/activities/dynamic"
)

type DynamicActivityTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestDynamicActivityTestSuite(t *testing.T) {
	suite.Run(t, new(DynamicActivityTestSuite))
}

func (s *DynamicActivityTestSuite) Test_UnregisteredActivityRunsDynamicActivity() {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterWorkflow(dynamic.CallUnregisteredActivityWorkflow)
	env.RegisterDynamicActivity(dynamic.DynamicActivity, activity.DynamicRegisterOptions{})

	env.ExecuteWorkflow(dynamic.CallUnregisteredActivityWorkflow, "first", "second")
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result string
	s.NoError(env.GetWorkflowResult(&result))
	s.Equal("CallUnregisteredActivityWorkflow - first - second", result)
}
