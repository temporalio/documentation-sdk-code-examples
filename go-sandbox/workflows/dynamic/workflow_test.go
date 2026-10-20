package dynamic_test

import (
	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/workflow"

	dynamicactivity "my-org/quickstart/activities/dynamic"
	"my-org/quickstart/workflows/dynamic"
)

type DynamicWorkflowTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestDynamicWorkflowTestSuite(t *testing.T) {
	suite.Run(t, new(DynamicWorkflowTestSuite))
}

func (s *DynamicWorkflowTestSuite) run(workflowType string) string {
	env := s.NewTestWorkflowEnvironment()
	env.RegisterDynamicWorkflow(dynamic.DynamicWorkflow, workflow.DynamicRegisterOptions{})
	env.RegisterDynamicActivity(dynamicactivity.DynamicActivity, activity.DynamicRegisterOptions{})

	env.ExecuteWorkflow(workflowType, "first", "second")
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())

	var result string
	s.NoError(env.GetWorkflowResult(&result))
	return result
}

func (s *DynamicWorkflowTestSuite) Test_UnregisteredWorkflowType() {
	s.Equal("some-workflow-type - first - second", s.run("some-workflow-type"))
}

func (s *DynamicWorkflowTestSuite) Test_CallsDynamicActivity() {
	// The Dynamic Activity formats its result with the calling Workflow Type.
	s.Equal("dynamic-activity - first - second", s.run("dynamic-activity"))
}
