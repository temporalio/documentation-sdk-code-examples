package greeting_test

import (
	"testing"

	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/workers/lambda/greeting"
)

func TestSampleWorkflow(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	env := suite.NewTestWorkflowEnvironment()
	env.RegisterActivity(greeting.HelloActivity)

	env.ExecuteWorkflow(greeting.SampleWorkflow, "Temporal")
	require.True(t, env.IsWorkflowCompleted())
	require.NoError(t, env.GetWorkflowError())

	var result string
	require.NoError(t, env.GetWorkflowResult(&result))
	require.Equal(t, "Hello, Temporal!", result)
}
